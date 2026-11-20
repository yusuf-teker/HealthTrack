package com.yusufteker.healthtrack.core.utils

import com.yusufteker.healthtrack.core.model.FriendActivityType
import com.yusufteker.healthtrack.core.model.FriendNotificationBus
import com.yusufteker.healthtrack.core.model.FriendNotificationEvent
import com.yusufteker.healthtrack.core.model.FriendNotificationJsonCodec
import com.yusufteker.healthtrack.core.model.PushTokenBus
import com.yusufteker.healthtrack.core.network.TokenStorage
import platform.Foundation.NSUserDefaults

/**
 * Swift bridge for incoming APNs/UNUserNotificationCenter events & push tokens on iOS.
 */
object IosPushSwiftBridge {

    private val rawApnsHexRegex = Regex("^[0-9a-fA-F]{64}$")

    fun onPushTokenUpdated(token: String) {
        val clean = token.trim().takeIf { it.isNotBlank() } ?: return
        if (rawApnsHexRegex.matches(clean)) return
        NSUserDefaults.standardUserDefaults.setObject(clean, forKey = TokenStorage.KEY_FCM_TOKEN)
        NSUserDefaults.standardUserDefaults.synchronize()
        PushTokenBus.onNewToken(clean)
    }

    /**
     * Call from Swift when a notification arrives in foreground (`isForeground = true`)
     * or when the app wakes/opens from a background/killed notification (`isForeground = false`).
     */
    fun onPushReceived(
        id: String?,
        actorName: String?,
        actorUsername: String?,
        actorUserId: Int,
        typeRaw: String?,
        sportType: String? = null,
        title: String?,
        body: String?,
        isForeground: Boolean
    ) {
        if (typeRaw != null && typeRaw.equals("MEAL", ignoreCase = true)) return
        if (typeRaw != null && typeRaw.equals("WORKOUT", ignoreCase = true) && actorUserId > 0) {
            val storage = TokenStorage(com.russhwolf.settings.NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults))
            if (!storage.isFriendWorkoutTrackingEnabled(actorUserId)) {
                return
            }
        }
        val event = FriendNotificationBus.emitFromPushPayload(
            id = id,
            actorName = actorName,
            actorUsername = actorUsername,
            actorUserId = actorUserId,
            typeRaw = typeRaw,
            sportType = sportType,
            title = title,
            body = body
        )
        if (!isForeground) {
            persistPendingEvent(event)
        }
    }

    private fun persistPendingEvent(event: FriendNotificationEvent) {
        val defaults = NSUserDefaults.standardUserDefaults
        val existingRaw = defaults.stringForKey(TokenStorage.KEY_PENDING_FRIEND_NOTIFS)
        val current = FriendNotificationJsonCodec.decodeList(existingRaw).toMutableList()
        if (current.none { it.id == event.id }) {
            current.add(event)
            defaults.setObject(
                FriendNotificationJsonCodec.encodeList(current),
                forKey = TokenStorage.KEY_PENDING_FRIEND_NOTIFS
            )
            defaults.synchronize()
        }
    }
}

