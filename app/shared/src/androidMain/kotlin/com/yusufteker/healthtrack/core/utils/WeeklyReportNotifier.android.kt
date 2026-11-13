package com.yusufteker.healthtrack.core.utils

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import com.yusufteker.healthtrack.app.shared.R
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.model.HealthAiReport
import io.github.aakira.napier.Napier
import java.util.Calendar

@Composable
actual fun rememberWeeklyReportNotifier(): WeeklyReportNotifier {
    val context = LocalContext.current
    var pendingCallback by remember { mutableStateOf<((Boolean) -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingCallback?.invoke(granted)
        pendingCallback = null
    }

    return remember(context, permissionLauncher) {
        object : WeeklyReportNotifier {
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

            override fun sendReportNotification(
                report: HealthAiReport,
                whatsAppPhone: String?,
                userName: String
            ) {
                try {
                    if (!hasPermission) return
                    HealthReminderNotifications.ensureNotificationChannels(context)

                    val cleanedPhone = whatsAppPhone?.filter { it.isDigit() }?.let { digits ->
                        when {
                            digits.startsWith("0") && digits.length == 11 -> "90${digits.drop(1)}"
                            digits.length == 10 -> "90$digits"
                            else -> digits
                        }
                    }?.takeIf { it.length >= 10 }

                    val formattedReport = report.toWhatsAppMessage(userName)
                    val encodedText = Uri.encode(formattedReport)
                    val urlStr = if (cleanedPhone != null) {
                        "https://api.whatsapp.com/send?phone=$cleanedPhone&text=$encodedText"
                    } else {
                        "whatsapp://send?text=$encodedText"
                    }

                    val whatsappIntent = Intent(Intent.ACTION_VIEW, Uri.parse(urlStr)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    val whatsappPendingIntent = PendingIntent.getActivity(
                        context,
                        101,
                        whatsappIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val appPendingIntent = HealthReminderNotifications.createOpenTabPendingIntent(
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
                            trEn(
                                "📈 Haftalık Raporun Hazır! (${report.periodLabel})",
                                "📈 Your Weekly Report Is Ready! (${report.periodLabel})"
                            )
                        )
                        .setContentText(
                            trEn(
                                "WhatsApp'ta kendine göndermek için dokunun. (Skor: ${report.score}/100)",
                                "Tap to send it to yourself on WhatsApp. (Score: ${report.score}/100)"
                            )
                        )
                        .setStyle(
                            NotificationCompat.BigTextStyle()
                                .setBigContentTitle(
                                    trEn(
                                        "📈 ${report.periodLabel} Sağlık Raporu",
                                        "📈 ${report.periodLabel} Health Report"
                                    )
                                )
                                .bigText(
                                    trEn(
                                        "${report.headline}\n${report.summary}\n\n👉 Dokunarak WhatsApp'ta kendine gönder!",
                                        "${report.headline}\n${report.summary}\n\n👉 Tap to send to yourself on WhatsApp!"
                                    )
                                )
                        )
                        .setContentIntent(whatsappPendingIntent)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .addAction(
                            android.R.drawable.ic_menu_send,
                            trEn("💬 WhatsApp'ta Aç", "💬 Open in WhatsApp"),
                            whatsappPendingIntent
                        )
                        .apply {
                            if (appPendingIntent != null) {
                                addAction(
                                    android.R.drawable.ic_menu_view,
                                    trEn("📱 Uygulamada Gör", "📱 View in App"),
                                    appPendingIntent
                                )
                            }
                        }
                        .build()

                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(
                        HealthReminderNotifications.WEEKLY_REPORT_NOTIFICATION_ID,
                        notification
                    )
                    Napier.i("Weekly report notification posted successfully.", tag = "NOTIFICATION")
                } catch (e: Exception) {
                    Napier.e("Failed to post weekly report notification: ${e.message}", e, tag = "NOTIFICATION")
                }
            }

            override fun scheduleSundayEveningReminder(hour: Int, minute: Int) {
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.WEEK_OF_YEAR, 1)
                    }
                }
                HealthReminderNotifications.scheduleBroadcastAlarm(
                    context = context,
                    triggerAtMillis = calendar.timeInMillis,
                    intervalMillis = AlarmManager.INTERVAL_DAY * 7,
                    action = HealthReminderNotifications.ACTION_WEEKLY_REPORT_REMINDER,
                    requestCode = HealthReminderNotifications.WEEKLY_ALARM_REQUEST_CODE
                )
            }

            override fun cancelSundayEveningReminder() {
                HealthReminderNotifications.cancelBroadcastAlarm(
                    context = context,
                    action = HealthReminderNotifications.ACTION_WEEKLY_REPORT_REMINDER,
                    requestCode = HealthReminderNotifications.WEEKLY_ALARM_REQUEST_CODE
                )
            }
        }
    }
}

