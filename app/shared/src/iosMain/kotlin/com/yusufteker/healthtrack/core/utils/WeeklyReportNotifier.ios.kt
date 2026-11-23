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
import com.yusufteker.healthtrack.core.model.HealthAiReport
import io.ktor.http.encodeURLParameter
import platform.Foundation.NSDateComponents
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
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

private const val WEEKLY_REPORT_REMINDER_ID = "ht_weekly_report_sunday_reminder"

@Composable
actual fun rememberWeeklyReportNotifier(): WeeklyReportNotifier {
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
        object : WeeklyReportNotifier {
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
                                    onResult(granted)
                                }
                            }
                        }
                    }
                }
            }

            override fun sendReportNotification(
                report: HealthAiReport,
                whatsAppPhone: String?,
                userName: String
            ) {
                val center = UNUserNotificationCenter.currentNotificationCenter()
                val cleanedPhone = whatsAppPhone?.filter { it.isDigit() }?.let { digits ->
                    when {
                        digits.startsWith("0") && digits.length == 11 -> "90${digits.drop(1)}"
                        digits.length == 10 -> "90$digits"
                        else -> digits
                    }
                }?.takeIf { it.length >= 10 }

                val encodedText = report.toWhatsAppMessage(userName).encodeURLParameter()
                val whatsappUrl = if (cleanedPhone != null) {
                    "https://api.whatsapp.com/send?phone=$cleanedPhone&text=$encodedText"
                } else {
                    "whatsapp://send?text=$encodedText"
                }

                val content = UNMutableNotificationContent().apply {
                    setTitle(
                        trEn(
                            "📈 Haftalık Raporun Hazır! (${report.periodLabel})",
                            "📈 Your Weekly Report Is Ready! (${report.periodLabel})"
                        )
                    )
                    setBody(
                        trEn(
                            "WhatsApp'ta kendine göndermek için dokunun. (Skor: ${report.score}/100)",
                            "Tap to send it to yourself on WhatsApp. (Score: ${report.score}/100)"
                        )
                    )
                    setSound(UNNotificationSound.defaultSound())
                    setUserInfo(
                        mapOf<Any?, Any?>(
                            "open_tab" to "ANALYTICS",
                            "whatsapp_url" to whatsappUrl,
                            "is_local" to true
                        )
                    )
                }

                val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(1.0, repeats = false)
                val request = UNNotificationRequest.requestWithIdentifier("weekly_report_notif", content, trigger)
                center.addNotificationRequest(request, withCompletionHandler = null)
            }

            override fun scheduleSundayEveningReminder(hour: Int, minute: Int) {
                val center = UNUserNotificationCenter.currentNotificationCenter()
                val components = NSDateComponents().apply {
                    setWeekday(1L) // 1 = Sunday in Gregorian calendar
                    setHour(hour.toLong())
                    setMinute(minute.toLong())
                }
                val content = UNMutableNotificationContent().apply {
                    setTitle(
                        trEn("📈 Haftalık Sağlık Raporun Hazır!", "📈 Your Weekly Health Report Is Ready!")
                    )
                    setBody(
                        trEn(
                            "Haftalık analizini görmek ve WhatsApp'ta paylaşmak için dokun.",
                            "Tap to view your weekly summary and share on WhatsApp."
                        )
                    )
                    setSound(UNNotificationSound.defaultSound())
                    setUserInfo(mapOf<Any?, Any?>("open_tab" to "ANALYTICS", "is_local" to true))
                }
                val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                    components,
                    repeats = true
                )
                val request = UNNotificationRequest.requestWithIdentifier(
                    WEEKLY_REPORT_REMINDER_ID,
                    content,
                    trigger
                )
                center.addNotificationRequest(request, withCompletionHandler = null)
            }

            override fun cancelSundayEveningReminder() {
                UNUserNotificationCenter.currentNotificationCenter()
                    .removePendingNotificationRequestsWithIdentifiers(listOf(WEEKLY_REPORT_REMINDER_ID))
            }
        }
    }
}

