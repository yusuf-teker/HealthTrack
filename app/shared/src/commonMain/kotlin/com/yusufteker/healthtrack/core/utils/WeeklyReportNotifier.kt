package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import com.yusufteker.healthtrack.core.model.HealthAiReport

/**
 * Platform-agnostic contract for local/push weekly report notifications that,
 * when tapped, immediately open WhatsApp with the generated health report pre-filled.
 */
interface WeeklyReportNotifier {
    /**
     * Whether notification permissions are granted on the device.
     */
    val hasPermission: Boolean

    /**
     * Requests notification permission from the user (Android 13+ / iOS).
     */
    fun requestPermission(onResult: (Boolean) -> Unit)

    /**
     * Posts a rich local notification immediately (e.g. for testing or when a weekly report is ready).
     * Tapping the notification directly launches WhatsApp with the prefilled report.
     */
    fun sendReportNotification(
        report: HealthAiReport,
        whatsAppPhone: String?,
        userName: String = ""
    )

    /**
     * Schedules a repeating Sunday evening alarm (e.g. at 21:00) to trigger weekly notifications.
     */
    fun scheduleSundayEveningReminder(hour: Int = 21, minute: Int = 0)
}

/**
 * Creates and remembers a [WeeklyReportNotifier] instance for the current platform.
 */
@Composable
expect fun rememberWeeklyReportNotifier(): WeeklyReportNotifier

