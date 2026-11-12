package com.yusufteker.healthtrack.core.utils

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.yusufteker.healthtrack.core.model.ScheduledReminderPayload
import com.yusufteker.healthtrack.core.sync.WeightReminderEngine
import com.yusufteker.healthtrack.core.sync.WorkoutIncentiveEngine
import io.github.aakira.napier.Napier
import java.util.Calendar

private const val WORKOUT_NOTIFICATION_ID = 3001
private const val WEIGHT_NOTIFICATION_ID = 3002

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
                get() {
                    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }
                }

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
                    createNotificationChannels(context)

                    val appLaunchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra("open_tab", payload.targetTab)
                    }

                    val pendingIntent = if (appLaunchIntent != null) {
                        PendingIntent.getActivity(
                            context,
                            if (payload.targetTab == "WORKOUT") 301 else 302,
                            appLaunchIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    } else null

                    val iconRes = context.applicationInfo.icon.takeIf { it != 0 } ?: android.R.drawable.ic_dialog_info
                    val notificationId = if (payload.targetTab == "WORKOUT") WORKOUT_NOTIFICATION_ID else WEIGHT_NOTIFICATION_ID

                    val notification = NotificationCompat.Builder(context, payload.channelId)
                        .setSmallIcon(iconRes)
                        .setContentTitle(payload.title)
                        .setContentText(payload.body)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(payload.body))
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .build()

                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(notificationId, notification)
                    Napier.i("Notification posted: ${payload.title}", tag = "NOTIFICATION")
                } catch (e: Exception) {
                    Napier.e("Failed to post notification: ${e.message}", e, tag = "NOTIFICATION")
                }
            }

            override fun scheduleEveningWorkoutReminder(hour: Int, minute: Int) {
                scheduleRepeatingAlarm(context, hour, minute, "WORKOUT", 401)
            }

            override fun scheduleMorningWeightReminder(hour: Int, minute: Int) {
                scheduleRepeatingAlarm(context, hour, minute, "WEIGHT", 402)
            }
        }
    }
}

private fun scheduleRepeatingAlarm(
    context: Context,
    hour: Int,
    minute: Int,
    targetTab: String,
    requestCode: Int
) {
    try {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("open_tab", targetTab)
        } ?: return

        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    } catch (_: Exception) {}
}

private fun createNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val workoutChannel = NotificationChannel(
            WorkoutIncentiveEngine.WORKOUT_CHANNEL_ID,
            "Spora Teşvik Bildirimleri",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Akşam saatlerinde kişiselleştirilmiş spora teşvik bildirimleri."
            enableLights(true)
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(workoutChannel)

        val weightChannel = NotificationChannel(
            WeightReminderEngine.WEIGHT_CHANNEL_ID,
            "Tartı ve Kilo Hatırlatıcıları",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Düzenli aralıklarla tartılma hatırlatıcıları."
            enableLights(true)
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(weightChannel)
    }
}

