package com.yusufteker.healthtrack.core.utils

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.yusufteker.healthtrack.core.model.ScheduledReminderPayload
import io.github.aakira.napier.Napier
import java.util.Calendar

@Composable
actual fun rememberDailyReminderNotifier(): DailyReminderNotifier {
    val context = LocalContext.current
    var pendingCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingCallback?.invoke(granted)
        pendingCallback = null
    }

    return remember(context, permissionLauncher) {
        object : DailyReminderNotifier {
            override val hasPermission: Boolean
                get() = HealthReminderNotifications.hasPermission(context)

            override fun requestPermission(onResult: (Boolean) -> Unit) {
                if (hasPermission) {
                    onResult(true)
                    return
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pendingCallback = onResult
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onResult(true)
                }
            }

            override fun sendNotification(payload: ScheduledReminderPayload) {
                try {
                    HealthReminderNotifications.postReminderNotification(context, payload)
                    Napier.i("Notification posted: ${payload.title}", tag = "NOTIFICATION")
                } catch (e: Exception) {
                    Napier.e("Failed to post notification: ${e.message}", e, tag = "NOTIFICATION")
                }
            }

            override fun scheduleEveningWorkoutReminder(hour: Int, minute: Int) {
                scheduleDailyBroadcastAlarm(
                    context = context,
                    hour = hour,
                    minute = minute,
                    action = HealthReminderNotifications.ACTION_WORKOUT_REMINDER,
                    requestCode = HealthReminderNotifications.WORKOUT_ALARM_REQUEST_CODE
                )
            }

            override fun scheduleMorningWeightReminder(hour: Int, minute: Int) {
                scheduleDailyBroadcastAlarm(
                    context = context,
                    hour = hour,
                    minute = minute,
                    action = HealthReminderNotifications.ACTION_WEIGHT_REMINDER,
                    requestCode = HealthReminderNotifications.WEIGHT_ALARM_REQUEST_CODE
                )
            }

            override fun cancelEveningWorkoutReminder() {
                HealthReminderNotifications.cancelBroadcastAlarm(
                    context = context,
                    action = HealthReminderNotifications.ACTION_WORKOUT_REMINDER,
                    requestCode = HealthReminderNotifications.WORKOUT_ALARM_REQUEST_CODE
                )
            }

            override fun cancelMorningWeightReminder() {
                HealthReminderNotifications.cancelBroadcastAlarm(
                    context = context,
                    action = HealthReminderNotifications.ACTION_WEIGHT_REMINDER,
                    requestCode = HealthReminderNotifications.WEIGHT_ALARM_REQUEST_CODE
                )
            }
        }
    }
}

private fun scheduleDailyBroadcastAlarm(
    context: Context,
    hour: Int,
    minute: Int,
    action: String,
    requestCode: Int
) {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= System.currentTimeMillis()) {
            add(Calendar.DAY_OF_YEAR, 1)
        }
    }
    HealthReminderNotifications.scheduleBroadcastAlarm(
        context = context,
        triggerAtMillis = calendar.timeInMillis,
        intervalMillis = AlarmManager.INTERVAL_DAY,
        action = action,
        requestCode = requestCode
    )
}

