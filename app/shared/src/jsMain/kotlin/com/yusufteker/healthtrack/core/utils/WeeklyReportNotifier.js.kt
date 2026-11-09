package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.yusufteker.healthtrack.core.model.HealthAiReport

@Composable
actual fun rememberWeeklyReportNotifier(): WeeklyReportNotifier {
    return remember {
        object : WeeklyReportNotifier {
            override val hasPermission: Boolean = false
            override fun requestPermission(onResult: (Boolean) -> Unit) { onResult(false) }
            override fun sendReportNotification(report: HealthAiReport, whatsAppPhone: String?, userName: String) {}
            override fun scheduleSundayEveningReminder(hour: Int, minute: Int) {}
        }
    }
}

