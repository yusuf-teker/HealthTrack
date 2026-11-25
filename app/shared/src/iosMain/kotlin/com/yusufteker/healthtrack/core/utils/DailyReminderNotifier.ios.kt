package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.model.ScheduledReminderPayload
import com.yusufteker.healthtrack.core.model.WeightReminderFrequency
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import platform.Foundation.NSDateComponents
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.registerForRemoteNotifications
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private const val WORKOUT_REMINDER_ID = "ht_evening_workout_reminder"
private const val WEIGHT_REMINDER_ID = "ht_morning_weight_reminder"
private const val REMINDER_BATCH_COUNT = 14
private const val FOOD_REMINDER_ID = "ht_midday_food_reminder"

@Composable
actual fun rememberDailyReminderNotifier(): DailyReminderNotifier {
    // Null while the initial async iOS settings query is in flight so UI does not flash
    // "blocked" or falsely trigger permission dialogs before UNUserNotificationCenter responds.
    var authorized by remember { mutableStateOf<Boolean?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    fun refreshAuthorizationStatus() {
        UNUserNotificationCenter.currentNotificationCenter()
            .getNotificationSettingsWithCompletionHandler { settings ->
                val status = settings?.authorizationStatus
                val granted = status == UNAuthorizationStatusAuthorized ||
                    status == UNAuthorizationStatusProvisional ||
                    status == UNAuthorizationStatusEphemeral
                dispatch_async(dispatch_get_main_queue()) {
                    authorized = granted
                    if (granted) {
                        UIApplication.sharedApplication.registerForRemoteNotifications()
                    }
                }
            }
    }

    LaunchedEffect(Unit) {
        refreshAuthorizationStatus()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshAuthorizationStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return remember {
        object : DailyReminderNotifier {
            override val hasPermission: Boolean
                get() = authorized != false

            override fun requestPermission(onResult: (Boolean) -> Unit) {
                val center = UNUserNotificationCenter.currentNotificationCenter()
                center.getNotificationSettingsWithCompletionHandler { settings ->
                    val status = settings?.authorizationStatus
                    val alreadyGranted = status == UNAuthorizationStatusAuthorized ||
                        status == UNAuthorizationStatusProvisional ||
                        status == UNAuthorizationStatusEphemeral
                    when {
                        alreadyGranted -> {
                            dispatch_async(dispatch_get_main_queue()) {
                                authorized = true
                                UIApplication.sharedApplication.registerForRemoteNotifications()
                                onResult(true)
                            }
                        }
                        status == UNAuthorizationStatusDenied -> {
                            dispatch_async(dispatch_get_main_queue()) {
                                authorized = false
                                val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
                                if (settingsUrl != null) {
                                    UIApplication.sharedApplication.openURL(
                                        settingsUrl,
                                        options = emptyMap<Any?, Any>(),
                                        completionHandler = null
                                    )
                                }
                                onResult(false)
                            }
                        }
                        else -> {
                            center.requestAuthorizationWithOptions(
                                UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
                            ) { granted, _ ->
                                dispatch_async(dispatch_get_main_queue()) {
                                    authorized = granted
                                    if (granted) {
                                        UIApplication.sharedApplication.registerForRemoteNotifications()
                                    }
                                    onResult(granted)
                                }
                            }
                        }
                    }
                }
            }

            override fun sendNotification(payload: ScheduledReminderPayload) {
                val center = UNUserNotificationCenter.currentNotificationCenter()
                val content = UNMutableNotificationContent().apply {
                    setTitle(payload.title)
                    setBody(payload.body)
                    setSound(UNNotificationSound.defaultSound())
                    setUserInfo(
                        mapOf<Any?, Any?>(
                            "open_tab" to payload.targetTab,
                            "is_local" to true
                        )
                    )
                }
                val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(1.0, repeats = false)
                val identifier = when (payload.targetTab) {
                    "WORKOUT" -> WORKOUT_REMINDER_ID
                    "FOOD" -> FOOD_REMINDER_ID
                    else -> WEIGHT_REMINDER_ID
                }
                val request = UNNotificationRequest.requestWithIdentifier(identifier, content, trigger)
                center.addNotificationRequest(request, withCompletionHandler = null)
            }

            override fun sendFriendActivityNotification(
                id: String,
                title: String,
                body: String,
                targetTab: String
            ) {
                val center = UNUserNotificationCenter.currentNotificationCenter()
                val content = UNMutableNotificationContent().apply {
                    setTitle(title)
                    setBody(body)
                    setSound(UNNotificationSound.defaultSound())
                    setUserInfo(
                        mapOf<Any?, Any?>(
                            "open_tab" to targetTab,
                            "is_local" to true,
                            "notificationId" to id
                        )
                    )
                }
                val identifier = "ht_friend_${id.replace(Regex("[^A-Za-z0-9_.-]"), "_")}"
                val request = UNNotificationRequest.requestWithIdentifier(identifier, content, trigger = null)
                center.addNotificationRequest(request, withCompletionHandler = null)
            }

            override fun scheduleEveningWorkoutReminder(
                hour: Int,
                minute: Int,
                skipToday: Boolean,
                minDaysDelay: Int
            ) {
                cancelEveningWorkoutReminder()
                scheduleRollingCalendarReminders(
                    baseIdentifier = WORKOUT_REMINDER_ID,
                    title = trEn("💪 Akşam Antrenman Vakti!", "💪 Evening Workout Time!"),
                    body = trEn(
                        "Bugünkü hareket hedefini tamamlamaya ne dersin? HealthTrack seninle.",
                        "Ready to crush today's movement goal? HealthTrack is with you."
                    ),
                    targetTab = "WORKOUT",
                    hour = hour,
                    minute = minute,
                    intervalDays = 1,
                    skipToday = skipToday,
                    minDaysDelay = minDaysDelay
                )
            }

            override fun scheduleMorningWeightReminder(
                hour: Int,
                minute: Int,
                frequency: WeightReminderFrequency,
                lastWeightTimestamp: Long?,
                skipToday: Boolean,
                minDaysDelay: Int
            ) {
                cancelMorningWeightReminder()
                if (frequency == WeightReminderFrequency.OFF) return

                val title = trEn("⚖️ Günaydın! Tartılma Zamanı", "⚖️ Good Morning! Time to Weigh In")
                val body = trEn(
                    "Güncel kilonu kaydederek gelişimini ve kalori hedeflerini canlı tut.",
                    "Log your current weight to keep your progress and calorie targets accurate."
                )

                val intervalDays = when (frequency) {
                    WeightReminderFrequency.DAILY -> 1
                    WeightReminderFrequency.EVERY_2_DAYS -> 2
                    WeightReminderFrequency.EVERY_3_DAYS -> 3
                    WeightReminderFrequency.WEEKLY -> 7
                    WeightReminderFrequency.OFF -> return
                }
                schedulePeriodicWeightReminders(
                    title = title,
                    body = body,
                    hour = hour,
                    minute = minute,
                    intervalDays = intervalDays,
                    lastWeightTimestamp = lastWeightTimestamp,
                    skipToday = skipToday,
                    minDaysDelay = minDaysDelay
                )
            }

            override fun scheduleMiddayFoodReminder(
                hour: Int,
                minute: Int,
                skipToday: Boolean,
                minDaysDelay: Int
            ) {
                cancelMiddayFoodReminder()
                scheduleRollingCalendarReminders(
                    baseIdentifier = FOOD_REMINDER_ID,
                    title = trEn("🥗 Bugün Ne Yedin?", "🥗 What Did You Eat Today?"),
                    body = trEn(
                        "Öğününü kaydederek günlük kalori ve makro hedeflerini canlı tut.",
                        "Log your meal to stay on top of your daily calorie and macro targets."
                    ),
                    targetTab = "FOOD",
                    hour = hour,
                    minute = minute,
                    intervalDays = 1,
                    skipToday = skipToday,
                    minDaysDelay = minDaysDelay
                )
            }

            override fun cancelEveningWorkoutReminder() {
                removeBatchReminderIds(WORKOUT_REMINDER_ID)
            }

            override fun cancelMorningWeightReminder() {
                removeBatchReminderIds(WEIGHT_REMINDER_ID)
            }

            override fun cancelMiddayFoodReminder() {
                removeBatchReminderIds(FOOD_REMINDER_ID)
            }
        }
    }
}

private fun removeBatchReminderIds(baseIdentifier: String) {
    val ids = buildList {
        add(baseIdentifier)
        for (i in 0 until REMINDER_BATCH_COUNT) {
            add("${baseIdentifier}_$i")
        }
    }
    UNUserNotificationCenter.currentNotificationCenter()
        .removePendingNotificationRequestsWithIdentifiers(ids)
}

private fun clampToNonQuietTime(hour: Int, minute: Int): Pair<Int, Int> {
    val h = hour.coerceIn(0, 23)
    val m = minute.coerceIn(0, 59)
    val totalMinutes = h * 60 + m
    return when {
        totalMinutes < 7 * 60 + 30 -> 7 to 30
        totalMinutes >= 22 * 60 -> 21 to 30
        else -> h to m
    }
}

private fun scheduleRollingCalendarReminders(
    baseIdentifier: String,
    title: String,
    body: String,
    targetTab: String,
    hour: Int,
    minute: Int,
    intervalDays: Int,
    skipToday: Boolean,
    minDaysDelay: Int
) {
    val center = UNUserNotificationCenter.currentNotificationCenter()
    val tz = TimeZone.currentSystemDefault()
    val nowMs = currentTimeMillis()
    val nowDateTime = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz)
    val (safeHour, safeMinute) = clampToNonQuietTime(hour, minute)
    val targetTime = LocalTime(safeHour, safeMinute)

    val minOffsetDays = maxOf(if (skipToday) 1 else 0, minDaysDelay.coerceAtLeast(0))
    var startDate = nowDateTime.date.plus(DatePeriod(days = minOffsetDays))
    while (LocalDateTime(startDate, targetTime).toInstant(tz).toEpochMilliseconds() <= nowMs) {
        startDate = startDate.plus(DatePeriod(days = intervalDays.coerceAtLeast(1)))
    }

    for (index in 0 until REMINDER_BATCH_COUNT) {
        val scheduledDate = startDate.plus(DatePeriod(days = index * intervalDays.coerceAtLeast(1)))
        val components = NSDateComponents().apply {
            setYear(scheduledDate.year.toLong())
            setMonth(scheduledDate.monthNumber.toLong())
            setDay(scheduledDate.dayOfMonth.toLong())
            setHour(safeHour.toLong())
            setMinute(safeMinute.toLong())
        }
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(UNNotificationSound.defaultSound())
            setUserInfo(mapOf<Any?, Any?>("open_tab" to targetTab, "is_local" to true))
        }
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = false)
        val request = UNNotificationRequest.requestWithIdentifier("${baseIdentifier}_$index", content, trigger)
        center.addNotificationRequest(request, withCompletionHandler = null)
    }
}

private fun schedulePeriodicWeightReminders(
    title: String,
    body: String,
    hour: Int,
    minute: Int,
    intervalDays: Int,
    lastWeightTimestamp: Long?,
    skipToday: Boolean = false,
    minDaysDelay: Int = 0
) {
    val center = UNUserNotificationCenter.currentNotificationCenter()
    val tz = TimeZone.currentSystemDefault()
    val nowMs = currentTimeMillis()
    val nowDateTime = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz)
    val (safeHour, safeMinute) = clampToNonQuietTime(hour, minute)

    var nextDate = if (lastWeightTimestamp != null && lastWeightTimestamp > 0L) {
        Instant.fromEpochMilliseconds(lastWeightTimestamp).toLocalDateTime(tz).date
            .plus(DatePeriod(days = intervalDays))
    } else {
        nowDateTime.date
    }

    val minOffsetDays = maxOf(if (skipToday) 1 else 0, minDaysDelay.coerceAtLeast(0))
    val earliestAllowedDate = nowDateTime.date.plus(DatePeriod(days = minOffsetDays))
    if (nextDate < earliestAllowedDate) {
        nextDate = earliestAllowedDate
    }

    val targetTime = LocalTime(safeHour, safeMinute)
    while (LocalDateTime(nextDate, targetTime).toInstant(tz).toEpochMilliseconds() <= nowMs) {
        nextDate = nextDate.plus(DatePeriod(days = intervalDays))
    }

    for (index in 0 until REMINDER_BATCH_COUNT) {
        val scheduledDate = nextDate.plus(DatePeriod(days = index * intervalDays))
        val components = NSDateComponents().apply {
            setYear(scheduledDate.year.toLong())
            setMonth(scheduledDate.monthNumber.toLong())
            setDay(scheduledDate.dayOfMonth.toLong())
            setHour(safeHour.toLong())
            setMinute(safeMinute.toLong())
        }
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(UNNotificationSound.defaultSound())
            setUserInfo(mapOf<Any?, Any?>("open_tab" to "WEIGHT", "is_local" to true))
        }
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = false)
        val request = UNNotificationRequest.requestWithIdentifier("${WEIGHT_REMINDER_ID}_$index", content, trigger)
        center.addNotificationRequest(request, withCompletionHandler = null)
    }
}

