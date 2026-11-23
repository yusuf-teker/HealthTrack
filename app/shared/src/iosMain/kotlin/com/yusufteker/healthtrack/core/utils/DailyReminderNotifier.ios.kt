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
private const val WEIGHT_REMINDER_BATCH_COUNT = 14
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

            override fun scheduleEveningWorkoutReminder(hour: Int, minute: Int) {
                scheduleDailyCalendarReminder(
                    identifier = WORKOUT_REMINDER_ID,
                    title = trEn("💪 Akşam Antrenman Vakti!", "💪 Evening Workout Time!"),
                    body = trEn(
                        "Bugünkü hareket hedefini tamamlamaya ne dersin? HealthTrack seninle.",
                        "Ready to crush today's movement goal? HealthTrack is with you."
                    ),
                    targetTab = "WORKOUT",
                    hour = hour,
                    minute = minute
                )
            }

            override fun scheduleMorningWeightReminder(
                hour: Int,
                minute: Int,
                frequency: WeightReminderFrequency,
                lastWeightTimestamp: Long?
            ) {
                cancelMorningWeightReminder()
                if (frequency == WeightReminderFrequency.OFF) return

                val title = trEn("⚖️ Günaydın! Tartılma Zamanı", "⚖️ Good Morning! Time to Weigh In")
                val body = trEn(
                    "Güncel kilonu kaydederek gelişimini ve kalori hedeflerini canlı tut.",
                    "Log your current weight to keep your progress and calorie targets accurate."
                )

                if (frequency == WeightReminderFrequency.DAILY) {
                    scheduleDailyCalendarReminder(
                        identifier = WEIGHT_REMINDER_ID,
                        title = title,
                        body = body,
                        targetTab = "WEIGHT",
                        hour = hour,
                        minute = minute
                    )
                    return
                }

                val intervalDays = when (frequency) {
                    WeightReminderFrequency.EVERY_2_DAYS -> 2
                    WeightReminderFrequency.EVERY_3_DAYS -> 3
                    WeightReminderFrequency.WEEKLY -> 7
                    else -> 1
                }
                schedulePeriodicWeightReminders(
                    title = title,
                    body = body,
                    hour = hour,
                    minute = minute,
                    intervalDays = intervalDays,
                    lastWeightTimestamp = lastWeightTimestamp
                )
            }

            override fun scheduleMiddayFoodReminder(hour: Int, minute: Int) {
                scheduleDailyCalendarReminder(
                    identifier = FOOD_REMINDER_ID,
                    title = trEn("🥗 Bugün Ne Yedin?", "🥗 What Did You Eat Today?"),
                    body = trEn(
                        "Öğününü kaydederek günlük kalori ve makro hedeflerini canlı tut.",
                        "Log your meal to stay on top of your daily calorie and macro targets."
                    ),
                    targetTab = "FOOD",
                    hour = hour,
                    minute = minute
                )
            }

            override fun cancelEveningWorkoutReminder() {
                UNUserNotificationCenter.currentNotificationCenter()
                    .removePendingNotificationRequestsWithIdentifiers(listOf(WORKOUT_REMINDER_ID))
            }

            override fun cancelMorningWeightReminder() {
                val ids = buildList {
                    add(WEIGHT_REMINDER_ID)
                    for (i in 0 until WEIGHT_REMINDER_BATCH_COUNT) {
                        add("${WEIGHT_REMINDER_ID}_$i")
                    }
                }
                UNUserNotificationCenter.currentNotificationCenter()
                    .removePendingNotificationRequestsWithIdentifiers(ids)
            }

            override fun cancelMiddayFoodReminder() {
                UNUserNotificationCenter.currentNotificationCenter()
                    .removePendingNotificationRequestsWithIdentifiers(listOf(FOOD_REMINDER_ID))
            }
        }
    }
}

private fun scheduleDailyCalendarReminder(
    identifier: String,
    title: String,
    body: String,
    targetTab: String,
    hour: Int,
    minute: Int
) {
    val center = UNUserNotificationCenter.currentNotificationCenter()
    val components = NSDateComponents().apply {
        setHour(hour.toLong())
        setMinute(minute.toLong())
    }
    val content = UNMutableNotificationContent().apply {
        setTitle(title)
        setBody(body)
        setSound(UNNotificationSound.defaultSound())
        setUserInfo(mapOf<Any?, Any?>("open_tab" to targetTab, "is_local" to true))
    }
    val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = true)
    val request = UNNotificationRequest.requestWithIdentifier(identifier, content, trigger)
    center.addNotificationRequest(request, withCompletionHandler = null)
}

private fun schedulePeriodicWeightReminders(
    title: String,
    body: String,
    hour: Int,
    minute: Int,
    intervalDays: Int,
    lastWeightTimestamp: Long?
) {
    val center = UNUserNotificationCenter.currentNotificationCenter()
    val tz = TimeZone.currentSystemDefault()
    val nowMs = currentTimeMillis()
    val nowDateTime = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(tz)

    var nextDate = if (lastWeightTimestamp != null && lastWeightTimestamp > 0L) {
        Instant.fromEpochMilliseconds(lastWeightTimestamp).toLocalDateTime(tz).date
            .plus(DatePeriod(days = intervalDays))
    } else {
        nowDateTime.date
    }

    val targetTime = LocalTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    while (LocalDateTime(nextDate, targetTime).toInstant(tz).toEpochMilliseconds() <= nowMs) {
        nextDate = nextDate.plus(DatePeriod(days = intervalDays))
    }

    for (index in 0 until WEIGHT_REMINDER_BATCH_COUNT) {
        val scheduledDate = nextDate.plus(DatePeriod(days = index * intervalDays))
        val components = NSDateComponents().apply {
            setYear(scheduledDate.year.toLong())
            setMonth(scheduledDate.monthNumber.toLong())
            setDay(scheduledDate.dayOfMonth.toLong())
            setHour(hour.toLong())
            setMinute(minute.toLong())
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

