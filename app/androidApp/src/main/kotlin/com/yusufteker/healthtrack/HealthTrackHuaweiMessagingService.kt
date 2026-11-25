package com.yusufteker.healthtrack

import com.huawei.hms.push.HmsMessageService
import com.huawei.hms.push.RemoteMessage
import io.github.aakira.napier.Napier

class HealthTrackHuaweiMessagingService : HmsMessageService() {

    override fun onNewToken(token: String?) {
        super.onNewToken(token)
        if (!token.isNullOrBlank()) {
            Napier.d("HMS Push token refreshed", tag = "HMS")
            HealthTrackFirebaseMessagingService.saveFcmToken(this, token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage?) {
        super.onMessageReceived(message)
        if (message == null) return
        val data = message.dataOfMap ?: emptyMap()
        HealthTrackFirebaseMessagingService.handleIncomingPushPayload(
            context = this,
            data = data,
            fallbackTitle = message.notification?.title,
            fallbackBody = message.notification?.body,
            messageId = message.messageId
        )
    }
}
