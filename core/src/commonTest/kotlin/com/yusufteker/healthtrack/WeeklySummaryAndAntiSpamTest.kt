package com.yusufteker.healthtrack

import com.russhwolf.settings.MapSettings
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.model.FitnessGoal
import com.yusufteker.healthtrack.core.model.FoodItem
import com.yusufteker.healthtrack.core.model.FoodLog
import com.yusufteker.healthtrack.core.model.HealthReminderType
import com.yusufteker.healthtrack.core.model.NutritionGoal
import com.yusufteker.healthtrack.core.model.WaterEntry
import com.yusufteker.healthtrack.core.model.WeightEntry
import com.yusufteker.healthtrack.core.model.WorkoutLog
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.AntiSpamBlockReason
import com.yusufteker.healthtrack.core.sync.SmartNotificationAntiSpamPolicy
import com.yusufteker.healthtrack.core.sync.WeeklySummaryBuilder
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WeeklySummaryAndAntiSpamTest {

    // ─────────────────────────────────────────────────────────────
    // Weekly Summary 1-Time Popup Tests
    // ─────────────────────────────────────────────────────────────

    @Test
    fun weeklySummaryBuilder_computesPreviousWeekMetricsAndScore() = runTest {
        val settings = MapSettings()
        val database = HealthDatabase(settings)
        val tokenStorage = TokenStorage(settings)
        tokenStorage.saveSession(
            userId = 7,
            name = "Yusuf",
            username = "yusuf",
            accessToken = "tok",
            refreshToken = "ref"
        )
        tokenStorage.setUserFitnessGoal(FitnessGoal.MAINTAIN)
        database.upsertNutritionGoal(NutritionGoal(id = "g7", userId = 7, calories = 2000))

        // Today is Wednesday 2026-11-25 -> Previous week is Mon 2026-11-16 .. Sun 2026-11-22
        database.upsertFood(
            FoodLog(
                id = "f1",
                userId = 7,
                date = "2026-11-16",
                rawText = "Tavuk 200g",
                totalCalories = 1950,
                items = listOf(
                    FoodItem(id = "i1", name = "Tavuk", formattedPortion = "200g", calories = 1950, proteinG = 140.0)
                )
            )
        )
        database.upsertFood(
            FoodLog(
                id = "f2",
                userId = 7,
                date = "2026-11-18",
                rawText = "Yulaf 1 kase",
                totalCalories = 2050,
                items = listOf(
                    FoodItem(id = "i2", name = "Yulaf", formattedPortion = "1 kase", calories = 2050, proteinG = 120.0)
                )
            )
        )
        database.upsertWorkout(
            WorkoutLog(
                id = "w1",
                userId = 7,
                date = "2026-11-17",
                rawText = "30 dk koşu",
                caloriesBurned = 350,
                durationMinutes = 30
            )
        )
        database.upsertWeight(WeightEntry(id = "wt1", userId = 7, date = "2026-11-16", weightKg = 75.0))
        database.upsertWeight(WeightEntry(id = "wt2", userId = 7, date = "2026-11-22", weightKg = 74.4))
        database.addWater(userId = 7, date = "2026-11-16", deltaMl = 2200, defaultTargetMl = 2500)

        val builder = WeeklySummaryBuilder(database, tokenStorage)
        val summary = builder.buildPreviousWeekSummary(userId = 7, todayIso = "2026-11-25")

        assertNotNull(summary)
        assertEquals("2026-11-16", summary.weekMondayIso)
        assertEquals("2026-11-22", summary.weekSundayIso)
        assertEquals(7, summary.dailyBars.size)
        assertEquals(4, summary.activeDays) // Mon, Tue, Wed, Sun
        assertEquals(2, summary.foodLoggedDays)
        assertEquals(2000, summary.avgDailyCalories)
        assertEquals(130, summary.avgDailyProteinG)
        assertEquals(1, summary.workoutSessions)
        assertEquals(350, summary.totalBurnedCalories)
        assertEquals(2200, summary.avgDailyWaterMl)
        assertEquals(-0.6, summary.weightDeltaKg)
        assertTrue(summary.hasEnoughData)
        assertTrue(summary.consistencyScore in 1..100)
    }

    @Test
    fun weeklySummaryBuilder_emptyWeekHasEnoughDataFalse() {
        val summary = WeeklySummaryBuilder.buildForRange(
            weekMondayIso = "2026-11-16",
            weekSundayIso = "2026-11-22",
            todayIso = "2026-11-25",
            allFoods = emptyList(),
            allWorkouts = emptyList(),
            allWeights = emptyList(),
            allWaters = emptyList(),
            goal = NutritionGoal(id = "g", userId = 1, calories = 2000),
            fitnessGoal = FitnessGoal.MAINTAIN
        )
        assertFalse(summary.hasEnoughData)
        assertEquals(0, summary.activeDays)
    }

    @Test
    fun tokenStorage_weeklySummaryPopupShownStrictlyOncePerWeek() {
        val tokenStorage = TokenStorage(MapSettings())
        tokenStorage.saveSession(
            userId = 12,
            name = "Test",
            username = "test",
            accessToken = "tok",
            refreshToken = "ref"
        )

        // First check for 2026-11-16 week -> allowed
        assertTrue(
            tokenStorage.shouldShowWeeklySummaryPopup(
                userId = 12,
                prevWeekMondayIso = "2026-11-16",
                hasEnoughData = true
            )
        )
        // Not enough data -> blocked
        assertFalse(
            tokenStorage.shouldShowWeeklySummaryPopup(
                userId = 12,
                prevWeekMondayIso = "2026-11-16",
                hasEnoughData = false
            )
        )

        // Mark seen -> blocked on subsequent opens for the same week
        tokenStorage.markWeeklySummarySeen("2026-11-16", userId = 12)
        assertFalse(
            tokenStorage.shouldShowWeeklySummaryPopup(
                userId = 12,
                prevWeekMondayIso = "2026-11-16",
                hasEnoughData = true
            )
        )

        // Next completed week -> allowed once again
        assertTrue(
            tokenStorage.shouldShowWeeklySummaryPopup(
                userId = 12,
                prevWeekMondayIso = "2026-11-23",
                hasEnoughData = true
            )
        )
    }

    // ─────────────────────────────────────────────────────────────
    // Smart Anti-Spam Notification Policy Tests
    // ─────────────────────────────────────────────────────────────

    @Test
    fun antiSpam_enforcesQuietHours() {
        assertTrue(SmartNotificationAntiSpamPolicy.isQuietHours(22, 0))
        assertTrue(SmartNotificationAntiSpamPolicy.isQuietHours(23, 45))
        assertTrue(SmartNotificationAntiSpamPolicy.isQuietHours(3, 15))
        assertTrue(SmartNotificationAntiSpamPolicy.isQuietHours(7, 29))

        assertFalse(SmartNotificationAntiSpamPolicy.isQuietHours(7, 30))
        assertFalse(SmartNotificationAntiSpamPolicy.isQuietHours(13, 30))
        assertFalse(SmartNotificationAntiSpamPolicy.isQuietHours(21, 59))

        val decision = SmartNotificationAntiSpamPolicy.evaluate(
            todayIso = "2026-11-25",
            hourOfDay = 23,
            minuteOfHour = 10,
            nowMs = 1_000_000_000L,
            dailySentCountToday = 0,
            lastAnyReminderSentAtMs = 0L,
            lastAppActiveAtMs = 0L,
            lastSameTypeReminderDateIso = null,
            ignoredConsecutiveCountForType = 0
        )
        assertFalse(decision.allowed)
        assertEquals(AntiSpamBlockReason.QUIET_HOURS, decision.blockReason)
    }

    @Test
    fun antiSpam_enforcesDailyCapAndCooldownAndRecentActivity() {
        val nowMs = 1_700_000_000_000L

        // Daily cap (max 2/day)
        val capBlocked = SmartNotificationAntiSpamPolicy.evaluate(
            todayIso = "2026-11-25",
            hourOfDay = 18,
            minuteOfHour = 0,
            nowMs = nowMs,
            dailySentCountToday = 2,
            lastAnyReminderSentAtMs = nowMs - 4 * 3600_000L,
            lastAppActiveAtMs = nowMs - 4 * 3600_000L,
            lastSameTypeReminderDateIso = null,
            ignoredConsecutiveCountForType = 0
        )
        assertFalse(capBlocked.allowed)
        assertEquals(AntiSpamBlockReason.DAILY_CAP_REACHED, capBlocked.blockReason)

        // Cooldown (< 3 hours since last reminder)
        val cooldownBlocked = SmartNotificationAntiSpamPolicy.evaluate(
            todayIso = "2026-11-25",
            hourOfDay = 13,
            minuteOfHour = 30,
            nowMs = nowMs,
            dailySentCountToday = 1,
            lastAnyReminderSentAtMs = nowMs - 120 * 60_000L, // 2h ago
            lastAppActiveAtMs = nowMs - 4 * 3600_000L,
            lastSameTypeReminderDateIso = null,
            ignoredConsecutiveCountForType = 0
        )
        assertFalse(cooldownBlocked.allowed)
        assertEquals(AntiSpamBlockReason.GLOBAL_COOLDOWN_ACTIVE, cooldownBlocked.blockReason)

        // User recently active in app (< 90 min)
        val activeBlocked = SmartNotificationAntiSpamPolicy.evaluate(
            todayIso = "2026-11-25",
            hourOfDay = 13,
            minuteOfHour = 30,
            nowMs = nowMs,
            dailySentCountToday = 0,
            lastAnyReminderSentAtMs = 0L,
            lastAppActiveAtMs = nowMs - 30 * 60_000L, // 30m ago
            lastSameTypeReminderDateIso = null,
            ignoredConsecutiveCountForType = 0
        )
        assertFalse(activeBlocked.allowed)
        assertEquals(AntiSpamBlockReason.USER_RECENTLY_ACTIVE, activeBlocked.blockReason)

        // All clear -> allowed
        val allowed = SmartNotificationAntiSpamPolicy.evaluate(
            todayIso = "2026-11-25",
            hourOfDay = 13,
            minuteOfHour = 30,
            nowMs = nowMs,
            dailySentCountToday = 1,
            lastAnyReminderSentAtMs = nowMs - 180 * 60_000L,
            lastAppActiveAtMs = nowMs - 95 * 60_000L,
            lastSameTypeReminderDateIso = "2026-11-24",
            ignoredConsecutiveCountForType = 1
        )
        assertTrue(allowed.allowed)
        assertNull(allowed.blockReason)
    }

    @Test
    fun antiSpam_adaptiveIgnoreBackoffAndResetOnEngagement() {
        val tokenStorage = TokenStorage(MapSettings())
        val baseMs = 1_700_000_000_000L

        // Send 2 consecutive food reminders on Nov 20 and Nov 21 -> ignored count becomes 2
        SmartNotificationAntiSpamPolicy.recordSent(tokenStorage, HealthReminderType.FOOD, "2026-11-20", baseMs)
        SmartNotificationAntiSpamPolicy.recordSent(tokenStorage, HealthReminderType.FOOD, "2026-11-21", baseMs + 86_400_000L)
        assertEquals(2, tokenStorage.getIgnoredFoodNotificationCount())

        // On Nov 22 (1 day later): blocked because 2 ignores require >= 2 days backoff
        val blockedNextDay = SmartNotificationAntiSpamPolicy.evaluateFromStorage(
            tokenStorage = tokenStorage,
            type = HealthReminderType.FOOD,
            todayIso = "2026-11-22",
            hourOfDay = 13,
            minuteOfHour = 30,
            nowMs = baseMs + 2 * 86_400_000L
        )
        assertFalse(blockedNextDay.allowed)
        assertEquals(AntiSpamBlockReason.IGNORED_BACKOFF_ACTIVE, blockedNextDay.blockReason)

        // On Nov 23 (2 days later): allowed!
        val allowedAfter2Days = SmartNotificationAntiSpamPolicy.evaluateFromStorage(
            tokenStorage = tokenStorage,
            type = HealthReminderType.FOOD,
            todayIso = "2026-11-23",
            hourOfDay = 13,
            minuteOfHour = 30,
            nowMs = baseMs + 3 * 86_400_000L
        )
        assertTrue(allowedAfter2Days.allowed)

        // Record 3 more ignored reminders -> reaches 5 -> requires 7-day cool-down
        SmartNotificationAntiSpamPolicy.recordSent(tokenStorage, HealthReminderType.FOOD, "2026-11-23", baseMs + 3 * 86_400_000L)
        SmartNotificationAntiSpamPolicy.recordSent(tokenStorage, HealthReminderType.FOOD, "2026-11-26", baseMs + 6 * 86_400_000L)
        SmartNotificationAntiSpamPolicy.recordSent(tokenStorage, HealthReminderType.FOOD, "2026-11-29", baseMs + 9 * 86_400_000L)
        assertEquals(5, tokenStorage.getIgnoredFoodNotificationCount())
        assertEquals(7, SmartNotificationAntiSpamPolicy.requiredBackoffDays(5))

        // User logs food -> ignored count resets to 0 immediately
        SmartNotificationAntiSpamPolicy.markDomainEngaged(
            tokenStorage = tokenStorage,
            type = HealthReminderType.FOOD,
            nowMs = baseMs + 10 * 86_400_000L
        )
        assertEquals(0, tokenStorage.getIgnoredFoodNotificationCount())
    }
}
