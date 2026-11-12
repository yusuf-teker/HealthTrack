package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import com.yusufteker.healthtrack.core.model.ScheduledReminderPayload

/**
 * Platform-agnostic contract for workout incentive and weight tracking notifications.
 */
interface DailyReminderNotifier {
    /**
     * Whether notification permissions are granted on the device.
     */
    val hasPermission: Boolean

    /**
     * Requests notification permission from the user (Android 13+ / iOS).
     */
    fun requestPermission(onResult: (Boolean) -> Unit)

    /**
     * Posts a local notification immediately.
     */
    fun sendNotification(payload: ScheduledReminderPayload)

    /**
     * Schedules the evening workout reminder (default: 18:00).
     */
    fun scheduleEveningWorkoutReminder(hour: Int = 18, minute: Int = 0)

    /**
     * Schedules the morning weight tracking reminder (default: 09:00).
     */
    fun scheduleMorningWeightReminder(hour: Int = 9, minute: Int = 0)
}

/**
 * Creates and remembers a [DailyReminderNotifier] instance for the current platform.
 */
@Composable
expect fun rememberDailyReminderNotifier(): DailyReminderNotifier

