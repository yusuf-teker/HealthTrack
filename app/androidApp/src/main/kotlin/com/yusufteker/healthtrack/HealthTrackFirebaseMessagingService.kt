package com.yusufteker.healthtrack

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.yusufteker.healthtrack.app.shared.R as SharedR
import com.yusufteker.healthtrack.core.model.FriendNotificationBus
import com.yusufteker.healthtrack.core.model.FriendNotificationJsonCodec
import com.yusufteker.healthtrack.core.model.PushTokenBus
import com.yusufteker.healthtrack.core.network.TokenStorage
import io.github.aakira.napier.Napier

class HealthTrackFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Napier.d("FCM token refreshed", tag = "FCM")
        saveFcmToken(this, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val title = data["title"] ?: message.notification?.title
        val body = data["body"] ?: message.notification?.body
        val typeRaw = data["activityType"] ?: data["type"]
        if (typeRaw != null && typeRaw.equals("MEAL", ignoreCase = true)) return
        val actorName = data["actorName"] ?: title
        val actorUsername = data["actorUsername"]
        val actorUserId = data["actorUserId"]?.toIntOrNull() ?: 0
        val id = data["notificationId"] ?: message.messageId

        val event = FriendNotificationBus.emitFromPushPayload(
            id = id,
            actorName = actorName,
            actorUsername = actorUsername,
            actorUserId = actorUserId,
            typeRaw = typeRaw,
            title = title,
            body = body
        )

        val foreground = isAppInForeground(this)
        if (!foreground) {
            // Persist so when the user opens the app (even after swipe-dismissing the notification),
            // the top in-app push popup is displayed cleanly on launch.
            persistPendingNotification(this, event)
            showSystemNotification(
                title = title ?: event.actorName,
                body = body ?: event.messageTr,
                eventId = event.id
            )
        }
    }

    private fun showSystemNotification(title: String, body: String, eventId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Friend Activity",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PUSH_NOTIFICATION_ID, eventId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            eventId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(SharedR.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(eventId.hashCode(), notification)
    }

    companion object {
        private const val CHANNEL_ID = "ht_friend_activity_push"
        const val EXTRA_PUSH_NOTIFICATION_ID = "ht_push_notification_id"

        fun saveFcmToken(context: Context, token: String) {
            val cleaned = token.trim().takeIf { it.isNotBlank() } ?: return
            context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
                .edit()
                .putString(TokenStorage.KEY_FCM_TOKEN, cleaned)
                .apply()
            PushTokenBus.onNewToken(cleaned)
        }

        fun persistPendingNotification(
            context: Context,
            event: com.yusufteker.healthtrack.core.model.FriendNotificationEvent
        ) {
            val prefs = context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
            val current = FriendNotificationJsonCodec.decodeList(
                prefs.getString(TokenStorage.KEY_PENDING_FRIEND_NOTIFS, null)
            ).toMutableList()
            if (current.none { it.id == event.id }) {
                current.add(event)
                prefs.edit()
                    .putString(TokenStorage.KEY_PENDING_FRIEND_NOTIFS, FriendNotificationJsonCodec.encodeList(current))
                    .apply()
            }
        }

        private fun isAppInForeground(context: Context): Boolean {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
            val pid = android.os.Process.myPid()
            return am.runningAppProcesses?.any {
                it.pid == pid && it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
            } == true
        }
    }
}

