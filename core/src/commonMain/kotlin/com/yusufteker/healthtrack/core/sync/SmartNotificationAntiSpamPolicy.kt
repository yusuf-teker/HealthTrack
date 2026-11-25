package com.yusufteker.healthtrack.core.sync

import com.yusufteker.healthtrack.core.model.HealthReminderType
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.utils.calculateDaysBetween

enum class AntiSpamBlockReason {
    QUIET_HOURS,
    DAILY_CAP_REACHED,
    GLOBAL_COOLDOWN_ACTIVE,
    USER_RECENTLY_ACTIVE,
    IGNORED_BACKOFF_ACTIVE
}

data class AntiSpamDecision(
    val allowed: Boolean,
    val blockReason: AntiSpamBlockReason? = null
)

/**
 * Centralized anti-spam guardrail for all local health reminder notifications
 * (Weight, Food, Workout, Weekly Report).
 *
 * Rules:
 * 1. Quiet Hours: Never fire between 22:00 and 07:29 local time.
 * 2. Global Daily Cap: At most [MAX_DAILY_LOCAL_REMINDERS] local reminders per calendar day.
 * 3. Inter-Notification Cooldown: At least [MIN_COOLDOWN_MINUTES] (3h) between any two local reminders.
 * 4. Recent In-App Activity Suppression: Skip routine reminders if the user actively used the app
 *    within the last [RECENT_APP_ACTIVITY_SUPPRESSION_MINUTES] (90 min).
 * 5. Adaptive Ignore Backoff: If the user repeatedly ignores a reminder category without logging,
 *    automatically widen the interval (2 ign -> 2d, 3-4 ign -> 3d, 5+ ign -> 7d).
 */
object SmartNotificationAntiSpamPolicy {

    const val QUIET_HOUR_START = 22 // 22:00+
    const val QUIET_HOUR_END_HOUR = 7
    const val QUIET_HOUR_END_MINUTE = 30 // before 07:30
    const val MAX_DAILY_LOCAL_REMINDERS = 2
    const val MIN_COOLDOWN_MINUTES = 180L // 3 hours
    const val RECENT_APP_ACTIVITY_SUPPRESSION_MINUTES = 90L // 1.5 hours

    fun isQuietHours(hourOfDay: Int, minuteOfHour: Int): Boolean {
        val totalMinutes = hourOfDay * 60 + minuteOfHour
        val quietStartMinutes = QUIET_HOUR_START * 60 // 1320 (22:00)
        val quietEndMinutes = QUIET_HOUR_END_HOUR * 60 + QUIET_HOUR_END_MINUTE // 450 (07:30)
        return totalMinutes >= quietStartMinutes || totalMinutes < quietEndMinutes
    }

    /**
     * Adaptive backoff required days since the last reminder of the same category
     * based on how many consecutive reminders went unacted-upon.
     */
    fun requiredBackoffDays(ignoredConsecutiveCount: Int): Int {
        return when {
            ignoredConsecutiveCount >= 5 -> 7
            ignoredConsecutiveCount >= 3 -> 3
            ignoredConsecutiveCount == 2 -> 2
            else -> 0
        }
    }

    fun evaluate(
        todayIso: String,
        hourOfDay: Int,
        minuteOfHour: Int,
        nowMs: Long,
        dailySentCountToday: Int,
        lastAnyReminderSentAtMs: Long,
        lastAppActiveAtMs: Long,
        lastSameTypeReminderDateIso: String?,
        ignoredConsecutiveCountForType: Int,
        applyRecentAppActivityCheck: Boolean = true
    ): AntiSpamDecision {
        // 1. Quiet hours (22:00 - 07:29)
        if (isQuietHours(hourOfDay, minuteOfHour)) {
            return AntiSpamDecision(allowed = false, blockReason = AntiSpamBlockReason.QUIET_HOURS)
        }

        // 2. Global daily cap across all reminder channels
        if (dailySentCountToday >= MAX_DAILY_LOCAL_REMINDERS) {
            return AntiSpamDecision(allowed = false, blockReason = AntiSpamBlockReason.DAILY_CAP_REACHED)
        }

        // 3. Minimum cooldown gap since the last local notification of any type
        if (lastAnyReminderSentAtMs > 0L) {
            val elapsedMs = nowMs - lastAnyReminderSentAtMs
            if (elapsedMs in 0L until (MIN_COOLDOWN_MINUTES * 60_000L)) {
                return AntiSpamDecision(allowed = false, blockReason = AntiSpamBlockReason.GLOBAL_COOLDOWN_ACTIVE)
            }
        }

        // 4. Suppress routine reminder if user was actively using the app very recently
        if (applyRecentAppActivityCheck && lastAppActiveAtMs > 0L) {
            val activeAgoMs = nowMs - lastAppActiveAtMs
            if (activeAgoMs in 0L until (RECENT_APP_ACTIVITY_SUPPRESSION_MINUTES * 60_000L)) {
                return AntiSpamDecision(allowed = false, blockReason = AntiSpamBlockReason.USER_RECENTLY_ACTIVE)
            }
        }

        // 5. Adaptive ignore backoff for repeated unacted notifications
        val minBackoffDays = requiredBackoffDays(ignoredConsecutiveCountForType)
        if (minBackoffDays > 0 && !lastSameTypeReminderDateIso.isNullOrBlank()) {
            val daysSinceLast = calculateDaysBetween(lastSameTypeReminderDateIso, todayIso).coerceAtLeast(0)
            if (daysSinceLast < minBackoffDays) {
                return AntiSpamDecision(allowed = false, blockReason = AntiSpamBlockReason.IGNORED_BACKOFF_ACTIVE)
            }
        }

        return AntiSpamDecision(allowed = true)
    }

    fun evaluateFromStorage(
        tokenStorage: TokenStorage,
        type: HealthReminderType?,
        todayIso: String,
        hourOfDay: Int,
        minuteOfHour: Int,
        nowMs: Long
    ): AntiSpamDecision {
        val lastSameTypeDate = when (type) {
            HealthReminderType.WORKOUT -> tokenStorage.getLastWorkoutReminderDate()
            HealthReminderType.WEIGHT -> tokenStorage.getLastWeightReminderDate()
            HealthReminderType.FOOD -> tokenStorage.getLastFoodReminderDate()
            null -> tokenStorage.getLastWeeklyReportReminderDate()
        }
        val ignoredCount = when (type) {
            HealthReminderType.WORKOUT -> tokenStorage.getIgnoredWorkoutNotificationCount()
            HealthReminderType.WEIGHT -> tokenStorage.getIgnoredWeightNotificationCount()
            HealthReminderType.FOOD -> tokenStorage.getIgnoredFoodNotificationCount()
            null -> 0
        }
        return evaluate(
            todayIso = todayIso,
            hourOfDay = hourOfDay,
            minuteOfHour = minuteOfHour,
            nowMs = nowMs,
            dailySentCountToday = tokenStorage.getDailyReminderCountForDate(todayIso),
            lastAnyReminderSentAtMs = tokenStorage.getLastLocalReminderSentAtMs(),
            lastAppActiveAtMs = tokenStorage.getLastAppActiveAtMs(),
            lastSameTypeReminderDateIso = lastSameTypeDate,
            ignoredConsecutiveCountForType = ignoredCount,
            applyRecentAppActivityCheck = type != null
        )
    }

    /**
     * Records that a local reminder was delivered so global daily caps, cooldowns,
     * and per-category ignore backoff stay accurate.
     */
    fun recordSent(
        tokenStorage: TokenStorage,
        type: HealthReminderType?,
        todayIso: String,
        nowMs: Long
    ) {
        tokenStorage.incrementDailyReminderCount(todayIso)
        tokenStorage.setLastLocalReminderSentAtMs(nowMs)
        when (type) {
            HealthReminderType.WORKOUT -> {
                tokenStorage.setLastWorkoutReminderDate(todayIso)
                tokenStorage.setIgnoredWorkoutNotificationCount(
                    (tokenStorage.getIgnoredWorkoutNotificationCount() + 1).coerceAtMost(10)
                )
            }
            HealthReminderType.WEIGHT -> {
                tokenStorage.setLastWeightReminderDate(todayIso)
                tokenStorage.setIgnoredWeightNotificationCount(
                    (tokenStorage.getIgnoredWeightNotificationCount() + 1).coerceAtMost(10)
                )
            }
            HealthReminderType.FOOD -> {
                tokenStorage.setLastFoodReminderDate(todayIso)
                tokenStorage.setIgnoredFoodNotificationCount(
                    (tokenStorage.getIgnoredFoodNotificationCount() + 1).coerceAtMost(10)
                )
            }
            null -> {
                tokenStorage.setLastWeeklyReportReminderDate(todayIso)
            }
        }
    }

    /**
     * Resets the consecutive ignored counter when the user actively logs data in that domain.
     */
    fun markDomainEngaged(
        tokenStorage: TokenStorage,
        type: HealthReminderType,
        nowMs: Long
    ) {
        tokenStorage.setLastAppActiveAtMs(nowMs)
        when (type) {
            HealthReminderType.WORKOUT -> tokenStorage.setIgnoredWorkoutNotificationCount(0)
            HealthReminderType.WEIGHT -> tokenStorage.setIgnoredWeightNotificationCount(0)
            HealthReminderType.FOOD -> tokenStorage.setIgnoredFoodNotificationCount(0)
        }
    }
}
