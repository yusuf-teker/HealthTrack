package com.yusufteker.healthtrack.core.utils

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.russhwolf.settings.Settings
import com.yusufteker.healthtrack.app.shared.R
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.model.ScheduledReminderPayload
import com.yusufteker.healthtrack.core.model.WeightReminderFrequency
import com.yusufteker.healthtrack.core.model.WorkoutReminderFrequency
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.WeightReminderEngine
import com.yusufteker.healthtrack.core.sync.WorkoutIncentiveEngine
import io.github.aakira.napier.Napier
import java.util.Calendar

internal object HealthReminderNotifications {
    const val ACTION_WORKOUT_REMINDER = "com.yusufteker.healthtrack.ACTION_WORKOUT_REMINDER"
    const val ACTION_WEIGHT_REMINDER = "com.yusufteker.healthtrack.ACTION_WEIGHT_REMINDER"
    const val ACTION_WEEKLY_REPORT_REMINDER = "com.yusufteker.healthtrack.ACTION_WEEKLY_REPORT_REMINDER"

    const val WORKOUT_ALARM_REQUEST_CODE = 401
    const val WEIGHT_ALARM_REQUEST_CODE = 402
    const val WEEKLY_ALARM_REQUEST_CODE = 202

    const val WORKOUT_NOTIFICATION_ID = 3001
    const val WEIGHT_NOTIFICATION_ID = 3002
    const val WEEKLY_REPORT_NOTIFICATION_ID = 2026

    const val WEEKLY_REPORT_CHANNEL_ID = "ht_weekly_reports_channel"

    fun hasPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun ensureNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val workoutChannel = NotificationChannel(
                WorkoutIncentiveEngine.WORKOUT_CHANNEL_ID,
                trEn("Spora Teşvik Bildirimleri", "Workout Motivation Reminders"),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = trEn(
                    "Akşam saatlerinde kişiselleştirilmiş spora teşvik bildirimleri.",
                    "Personalized evening workout reminder notifications."
                )
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(workoutChannel)

            val weightChannel = NotificationChannel(
                WeightReminderEngine.WEIGHT_CHANNEL_ID,
                trEn("Tartı ve Kilo Hatırlatıcıları", "Scale & Weight Reminders"),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = trEn(
                    "Düzenli aralıklarla tartılma hatırlatıcıları.",
                    "Periodic weigh-in reminder notifications."
                )
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(weightChannel)

            val weeklyReportChannel = NotificationChannel(
                WEEKLY_REPORT_CHANNEL_ID,
                trEn("Haftalık Sağlık Raporları", "Weekly Health Reports"),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = trEn(
                    "Haftalık AI sağlık analizlerini WhatsApp üzerinden görüntüleme bildirimleri.",
                    "Notifications for weekly AI health insights ready to share on WhatsApp."
                )
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(weeklyReportChannel)
        }
    }

    fun createOpenTabPendingIntent(
        context: Context,
        targetTab: String,
        requestCode: Int
    ): PendingIntent? {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("open_tab", targetTab)
        } ?: return null

        return PendingIntent.getActivity(
            context,
            requestCode,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun postReminderNotification(context: Context, payload: ScheduledReminderPayload) {
        if (!hasPermission(context)) return
        ensureNotificationChannels(context)

        val requestCode = if (payload.targetTab == "WORKOUT") 301 else 302
        val notificationId = if (payload.targetTab == "WORKOUT") {
            WORKOUT_NOTIFICATION_ID
        } else {
            WEIGHT_NOTIFICATION_ID
        }

        val notification = NotificationCompat.Builder(context, payload.channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(payload.title)
            .setContentText(payload.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.body))
            .setContentIntent(createOpenTabPendingIntent(context, payload.targetTab, requestCode))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    fun scheduleBroadcastAlarm(
        context: Context,
        triggerAtMillis: Long,
        intervalMillis: Long,
        action: String,
        requestCode: Int
    ) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val broadcastIntent = Intent(context, HealthReminderReceiver::class.java).apply {
                this.action = action
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                broadcastIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                intervalMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            Napier.w("Alarm schedule skipped for $action: ${e.message}", tag = "NOTIFICATION")
        }
    }

    fun cancelBroadcastAlarm(
        context: Context,
        action: String,
        requestCode: Int
    ) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val broadcastIntent = Intent(context, HealthReminderReceiver::class.java).apply {
                this.action = action
            }
            val existing = PendingIntent.getBroadcast(
                context,
                requestCode,
                broadcastIntent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (existing != null) {
                alarmManager.cancel(existing)
                existing.cancel()
            }
        } catch (e: Exception) {
            Napier.w("Alarm cancel skipped for $action: ${e.message}", tag = "NOTIFICATION")
        }
    }
}

class HealthReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!HealthReminderNotifications.hasPermission(context)) return

        try {
            val settings = Settings()
            LocalizationManager.init(settings)
            val tokenStorage = TokenStorage(settings)
            if (!tokenStorage.isLoggedIn()) return

            val userId = tokenStorage.getUserId()
            val userName = tokenStorage.sessionFlow.value.name
            val todayIso = getTodayIso()

            when (intent?.action) {
                HealthReminderNotifications.ACTION_WORKOUT_REMINDER -> {
                    val freq = tokenStorage.getWorkoutReminderFrequency()
                    if (freq == WorkoutReminderFrequency.OFF) return

                    val workouts = HealthDatabase(settings).getAllWorkoutsForSync(userId)
                    val hasWorkoutToday = workouts.any {
                        it.date == todayIso && !it.isDailySteps && !it.isDeleted
                    }
                    val yesterdayIso = shiftDateIso(todayIso, -1)
                    val yesterdayCalories = workouts
                        .filter { it.date == yesterdayIso && !it.isDeleted }
                        .sumOf { it.caloriesBurned }

                    val isoDayOfWeek = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                        Calendar.MONDAY -> 1
                        Calendar.TUESDAY -> 2
                        Calendar.WEDNESDAY -> 3
                        Calendar.THURSDAY -> 4
                        Calendar.FRIDAY -> 5
                        Calendar.SATURDAY -> 6
                        else -> 7
                    }

                    val shouldSend = WorkoutIncentiveEngine.shouldTriggerWorkoutReminder(
                        frequency = freq,
                        todayIso = todayIso,
                        hasWorkoutToday = hasWorkoutToday,
                        yesterdayCaloriesBurned = yesterdayCalories,
                        lastNotificationDate = tokenStorage.getLastWorkoutReminderDate(),
                        ignoredConsecutiveDays = tokenStorage.getIgnoredWorkoutNotificationCount(),
                        dayOfWeek = isoDayOfWeek
                    )
                    if (!shouldSend) return

                    val payload = WorkoutIncentiveEngine.buildWorkoutReminderPayload(
                        favoriteSport = WorkoutIncentiveEngine.detectFavoriteSport(workouts),
                        userName = userName
                    )
                    HealthReminderNotifications.postReminderNotification(context, payload)
                    tokenStorage.setLastWorkoutReminderDate(todayIso)
                }

                HealthReminderNotifications.ACTION_WEIGHT_REMINDER -> {
                    val freq = tokenStorage.getWeightReminderFrequency()
                    if (freq == WeightReminderFrequency.OFF) return

                    val weights = HealthDatabase(settings).getWeights(userId)
                    val latestWeight = weights.filter { !it.isDeleted }.maxByOrNull { it.date }
                    val shouldSend = WeightReminderEngine.shouldTriggerWeightReminder(
                        frequency = freq,
                        todayIso = todayIso,
                        latestWeightDate = latestWeight?.date,
                        lastNotificationDate = tokenStorage.getLastWeightReminderDate()
                    )
                    if (!shouldSend) return

                    val daysSince = latestWeight?.let {
                        calculateDaysBetween(it.date, todayIso).coerceAtLeast(0)
                    } ?: 999
                    val payload = WeightReminderEngine.buildWeightReminderPayload(
                        latestWeightKg = latestWeight?.weightKg,
                        daysSinceLastWeight = daysSince,
                        userName = userName
                    )
                    HealthReminderNotifications.postReminderNotification(context, payload)
                    tokenStorage.setLastWeightReminderDate(todayIso)
                }

                HealthReminderNotifications.ACTION_WEEKLY_REPORT_REMINDER -> {
                    if (!tokenStorage.isAutoWeeklyWhatsAppEnabled()) return

                    HealthReminderNotifications.ensureNotificationChannels(context)
                    val contentIntent = HealthReminderNotifications.createOpenTabPendingIntent(
                        context = context,
                        targetTab = "ANALYTICS",
                        requestCode = 102
                    )
                    val notification = NotificationCompat.Builder(
                        context,
                        HealthReminderNotifications.WEEKLY_REPORT_CHANNEL_ID
                    )
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(
                            trEn("📈 Haftalık Sağlık Raporun Hazır!", "📈 Your Weekly Health Report Is Ready!")
                        )
                        .setContentText(
                            trEn(
                                "Haftalık analizini görmek ve WhatsApp'ta paylaşmak için dokun.",
                                "Tap to view your weekly summary and share on WhatsApp."
                            )
                        )
                        .setStyle(
                            NotificationCompat.BigTextStyle()
                                .bigText(
                                    trEn(
                                        "Bu haftaki beslenme, spor ve kilo trendin hazır. Detayları Analiz sekmesinden inceleyebilirsin.",
                                        "This week's nutrition, workout, and weight trends are ready. Check the Insights tab for details."
                                    )
                                )
                        )
                        .setContentIntent(contentIntent)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .build()

                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(
                        HealthReminderNotifications.WEEKLY_REPORT_NOTIFICATION_ID,
                        notification
                    )
                }
            }
        } catch (e: Exception) {
            Napier.e("HealthReminderReceiver failed: ${e.message}", e, tag = "NOTIFICATION")
        }
    }
}
