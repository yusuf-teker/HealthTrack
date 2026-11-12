package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.yusufteker.healthtrack.core.model.ScheduledReminderPayload

@Composable
actual fun rememberDailyReminderNotifier(): DailyReminderNotifier {
    return remember {
        object : DailyReminderNotifier {
            override val hasPermission: Boolean get() = false
            override fun requestPermission(onResult: (Boolean) -> Unit) { onResult(false) }
            override fun sendNotification(payload: ScheduledReminderPayload) {}
            override fun scheduleEveningWorkoutReminder(hour: Int, minute: Int) {}
            override fun scheduleMorningWeightReminder(hour: Int, minute: Int) {}
        }
    }
}

