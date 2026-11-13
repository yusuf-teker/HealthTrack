package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.model.ScheduledReminderPayload
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
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

@Composable
actual fun rememberDailyReminderNotifier(): DailyReminderNotifier {
    var authorized by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        UNUserNotificationCenter.currentNotificationCenter()
            .getNotificationSettingsWithCompletionHandler { settings ->
                val granted = settings?.authorizationStatus == UNAuthorizationStatusAuthorized ||
                    settings?.authorizationStatus == UNAuthorizationStatusProvisional
                dispatch_async(dispatch_get_main_queue()) {
                    authorized = granted
                }
            }
    }

    return remember {
        object : DailyReminderNotifier {
            override val hasPermission: Boolean
                get() = authorized

            override fun requestPermission(onResult: (Boolean) -> Unit) {
                val center = UNUserNotificationCenter.currentNotificationCenter()
                center.requestAuthorizationWithOptions(
                    UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
                ) { granted, _ ->
                    dispatch_async(dispatch_get_main_queue()) {
                        authorized = granted
                        onResult(granted)
                    }
                }
            }

            override fun sendNotification(payload: ScheduledReminderPayload) {
                val center = UNUserNotificationCenter.currentNotificationCenter()
                val content = UNMutableNotificationContent().apply {
                    setTitle(payload.title)
                    setBody(payload.body)
                    setSound(UNNotificationSound.defaultSound())
                    setUserInfo(mapOf<Any?, Any?>("open_tab" to payload.targetTab))
                }
                val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(1.0, repeats = false)
                val identifier = if (payload.targetTab == "WORKOUT") WORKOUT_REMINDER_ID else WEIGHT_REMINDER_ID
                val request = UNNotificationRequest.requestWithIdentifier(identifier, content, trigger)
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

            override fun scheduleMorningWeightReminder(hour: Int, minute: Int) {
                scheduleDailyCalendarReminder(
                    identifier = WEIGHT_REMINDER_ID,
                    title = trEn("⚖️ Günaydın! Tartılma Zamanı", "⚖️ Good Morning! Time to Weigh In"),
                    body = trEn(
                        "Güncel kilonu kaydederek gelişimini ve kalori hedeflerini canlı tut.",
                        "Log your current weight to keep your progress and calorie targets accurate."
                    ),
                    targetTab = "WEIGHT",
                    hour = hour,
                    minute = minute
                )
            }

            override fun cancelEveningWorkoutReminder() {
                UNUserNotificationCenter.currentNotificationCenter()
                    .removePendingNotificationRequestsWithIdentifiers(listOf(WORKOUT_REMINDER_ID))
            }

            override fun cancelMorningWeightReminder() {
                UNUserNotificationCenter.currentNotificationCenter()
                    .removePendingNotificationRequestsWithIdentifiers(listOf(WEIGHT_REMINDER_ID))
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
        setUserInfo(mapOf<Any?, Any?>("open_tab" to targetTab))
    }
    val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = true)
    val request = UNNotificationRequest.requestWithIdentifier(identifier, content, trigger)
    center.addNotificationRequest(request, withCompletionHandler = null)
}

