package com.yusufteker.healthtrack.core.utils

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.yusufteker.healthtrack.core.model.HealthAiReport
import io.github.aakira.napier.Napier
import java.util.Calendar

private const val CHANNEL_ID = "ht_weekly_reports_channel"
private const val CHANNEL_NAME = "Haftalık Sağlık Raporları"
private const val NOTIFICATION_ID = 2026

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

            override fun sendReportNotification(
                report: HealthAiReport,
                whatsAppPhone: String?,
                userName: String
            ) {
                try {
                    createNotificationChannel(context)

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

                    // Tapping the notification or action button opens WhatsApp directly with the report prefilled
                    val whatsappIntent = Intent(Intent.ACTION_VIEW, Uri.parse(urlStr)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    val whatsappPendingIntent = PendingIntent.getActivity(
                        context,
                        101,
                        whatsappIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    // Secondary action opens HealthTrack directly
                    val appLaunchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra("open_tab", "ANALYTICS")
                    }
                    val appPendingIntent = if (appLaunchIntent != null) {
                        PendingIntent.getActivity(
                            context,
                            102,
                            appLaunchIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    } else null

                    val iconRes = context.applicationInfo.icon.takeIf { it != 0 } ?: android.R.drawable.ic_dialog_info

                    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(iconRes)
                        .setContentTitle("📈 Haftalık Raporun Hazır! (${report.periodLabel})")
                        .setContentText("WhatsApp'ta kendine göndermek için dokunun. (Skor: ${report.score}/100)")
                        .setStyle(
                            NotificationCompat.BigTextStyle()
                                .setBigContentTitle("📈 ${report.periodLabel} Sağlık Raporu")
                                .bigText("${report.headline}\n${report.summary}\n\n👉 Dokunarak WhatsApp'ta kendine gönder!")
                        )
                        .setContentIntent(whatsappPendingIntent)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .addAction(
                            android.R.drawable.ic_menu_send,
                            "💬 WhatsApp'ta Aç",
                            whatsappPendingIntent
                        )
                        .apply {
                            if (appPendingIntent != null) {
                                addAction(
                                    android.R.drawable.ic_menu_view,
                                    "📱 Uygulamada Gör",
                                    appPendingIntent
                                )
                            }
                        }
                        .build()

                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(NOTIFICATION_ID, notification)
                    Napier.i("Weekly report notification posted successfully.", tag = "NOTIFICATION")
                } catch (e: Exception) {
                    Napier.e("Failed to post weekly report notification: ${e.message}", e, tag = "NOTIFICATION")
                }
            }

            override fun scheduleSundayEveningReminder(hour: Int, minute: Int) {
                try {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
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

                    val appLaunchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra("open_tab", "ANALYTICS")
                    } ?: return

                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        202,
                        appLaunchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    alarmManager.setInexactRepeating(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        AlarmManager.INTERVAL_DAY * 7,
                        pendingIntent
                    )
                } catch (_: Exception) {}
            }
        }
    }
}

private fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Haftalık AI sağlık analizlerini WhatsApp üzerinden görüntüleme bildirimleri."
            enableLights(true)
            enableVibration(true)
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

