package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import com.yusufteker.healthtrack.core.model.ScheduledReminderPayload
import com.yusufteker.healthtrack.core.model.WeightReminderFrequency

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
     * Posts an immediate system notification banner for a friend's workout or follow request.
     */
    fun sendFriendActivityNotification(
        id: String,
        title: String,
        body: String,
        targetTab: String = "LEADERBOARD"
    ) {}

    /**
     * Schedules the evening workout reminder (default: 18:00).
     * [skipToday] suppresses today's occurrence (e.g. when a workout was already logged today).
     * [minDaysDelay] pushes the earliest occurrence forward for anti-spam ignore backoff.
     */
    fun scheduleEveningWorkoutReminder(
        hour: Int = 18,
        minute: Int = 0,
        skipToday: Boolean = false,
        minDaysDelay: Int = 0
    )

    /**
     * Schedules the morning weight tracking reminder (default: 08:00) respecting [frequency].
     */
    fun scheduleMorningWeightReminder(
        hour: Int = 8,
        minute: Int = 0,
        frequency: WeightReminderFrequency = WeightReminderFrequency.DAILY,
        lastWeightTimestamp: Long? = null,
        skipToday: Boolean = false,
        minDaysDelay: Int = 0
    )

    /**
     * Schedules the midday meal logging reminder (default: 13:30).
     * [skipToday] suppresses today's occurrence (e.g. when a meal was already logged today).
     * [minDaysDelay] pushes the earliest occurrence forward for anti-spam ignore backoff.
     */
    fun scheduleMiddayFoodReminder(
        hour: Int = 13,
        minute: Int = 30,
        skipToday: Boolean = false,
        minDaysDelay: Int = 0
    )

    /**
     * Cancels any scheduled evening workout reminder alarm.
     */
    fun cancelEveningWorkoutReminder()

    /**
     * Cancels any scheduled morning weight reminder alarm.
     */
    fun cancelMorningWeightReminder()

    /**
     * Cancels any scheduled midday meal reminder alarm.
     */
    fun cancelMiddayFoodReminder()
}

/**
 * Creates and remembers a [DailyReminderNotifier] instance for the current platform.
 */
@Composable
expect fun rememberDailyReminderNotifier(): DailyReminderNotifier

