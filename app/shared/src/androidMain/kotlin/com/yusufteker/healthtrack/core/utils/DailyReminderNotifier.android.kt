package com.yusufteker.healthtrack.core.utils

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yusufteker.healthtrack.core.model.ScheduledReminderPayload
import io.github.aakira.napier.Napier
import java.util.Calendar

@Composable
actual fun rememberDailyReminderNotifier(): DailyReminderNotifier {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var authorized by remember(context) { mutableStateOf(HealthReminderNotifications.hasPermission(context)) }
    var pendingCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                authorized = HealthReminderNotifications.hasPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        authorized = granted || HealthReminderNotifications.hasPermission(context)
        pendingCallback?.invoke(authorized)
        pendingCallback = null
    }

    return remember(context, permissionLauncher) {
        object : DailyReminderNotifier {
            override val hasPermission: Boolean
                get() = authorized

            override fun requestPermission(onResult: (Boolean) -> Unit) {
                if (HealthReminderNotifications.hasPermission(context)) {
                    authorized = true
                    onResult(true)
                    return
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pendingCallback = onResult
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    authorized = true
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

            override fun sendFriendActivityNotification(
                id: String,
                title: String,
                body: String,
                targetTab: String
            ) {
                try {
                    HealthReminderNotifications.postFriendActivityNotification(
                        context = context,
                        eventId = id,
                        title = title,
                        body = body,
                        targetTab = targetTab
                    )
                    Napier.i("Friend activity notification posted: $title", tag = "NOTIFICATION")
                } catch (e: Exception) {
                    Napier.e("Failed to post friend activity notification: ${e.message}", e, tag = "NOTIFICATION")
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

            override fun scheduleMorningWeightReminder(
                hour: Int,
                minute: Int,
                frequency: com.yusufteker.healthtrack.core.model.WeightReminderFrequency,
                lastWeightTimestamp: Long?
            ) {
                if (frequency == com.yusufteker.healthtrack.core.model.WeightReminderFrequency.OFF) {
                    cancelMorningWeightReminder()
                    return
                }
                scheduleDailyBroadcastAlarm(
                    context = context,
                    hour = hour,
                    minute = minute,
                    action = HealthReminderNotifications.ACTION_WEIGHT_REMINDER,
                    requestCode = HealthReminderNotifications.WEIGHT_ALARM_REQUEST_CODE
                )
            }

            override fun scheduleMiddayFoodReminder(hour: Int, minute: Int) {
                scheduleDailyBroadcastAlarm(
                    context = context,
                    hour = hour,
                    minute = minute,
                    action = HealthReminderNotifications.ACTION_FOOD_REMINDER,
                    requestCode = HealthReminderNotifications.FOOD_ALARM_REQUEST_CODE
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

            override fun cancelMiddayFoodReminder() {
                HealthReminderNotifications.cancelBroadcastAlarm(
                    context = context,
                    action = HealthReminderNotifications.ACTION_FOOD_REMINDER,
                    requestCode = HealthReminderNotifications.FOOD_ALARM_REQUEST_CODE
                )
            }
        }
    }
}

internal fun scheduleDailyBroadcastAlarm(
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

