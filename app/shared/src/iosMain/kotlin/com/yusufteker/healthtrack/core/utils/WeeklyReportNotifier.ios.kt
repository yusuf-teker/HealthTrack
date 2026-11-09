package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.yusufteker.healthtrack.core.model.HealthAiReport
import io.ktor.http.encodeURLParameter
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UserNotifications.*

@Composable
actual fun rememberWeeklyReportNotifier(): WeeklyReportNotifier {
    return remember {
        object : WeeklyReportNotifier {
            override val hasPermission: Boolean = true

            override fun requestPermission(onResult: (Boolean) -> Unit) {
                val center = UNUserNotificationCenter.currentNotificationCenter()
                center.requestAuthorizationWithOptions(
                    UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
                ) { granted, _ ->
                    onResult(granted)
                }
            }

            override fun sendReportNotification(
                report: HealthAiReport,
                whatsAppPhone: String?,
                userName: String
            ) {
                val center = UNUserNotificationCenter.currentNotificationCenter()
                val content = UNMutableNotificationContent().apply {
                    setTitle("📈 Haftalık Raporun Hazır! (${report.periodLabel})")
                    setBody("WhatsApp'ta kendine göndermek için dokunun. (Skor: ${report.score}/100)")
                    setSound(UNNotificationSound.defaultSound())
                }

                val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(1.0, repeats = false)
                val request = UNNotificationRequest.requestWithIdentifier("weekly_report_notif", content, trigger)
                center.addNotificationRequest(request, null)
            }

            override fun scheduleSundayEveningReminder(hour: Int, minute: Int) {}
        }
    }
}

