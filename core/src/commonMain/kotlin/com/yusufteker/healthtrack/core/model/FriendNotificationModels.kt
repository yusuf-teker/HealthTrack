package com.yusufteker.healthtrack.core.model

import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.utils.currentTimeMillis
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class FriendActivityType {
    WORKOUT,
    MEAL,
    STREAK,
    BADGE,
    FOLLOW_REQUEST,
    GENERAL;

    companion object {
        fun fromWire(raw: String?): FriendActivityType {
            return entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: GENERAL
        }
    }
}

@Serializable
data class FriendNotificationEvent(
    val id: String,
    val actorUserId: Int = 0,
    val actorName: String,
    val actorUsername: String = "",
    val type: FriendActivityType = FriendActivityType.GENERAL,
    val sportType: String? = null,
    val titleTr: String,
    val titleEn: String,
    val messageTr: String,
    val messageEn: String,
    val createdAtMs: Long = 0L
) {
    fun localizedTitle(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(titleTr, titleEn, language)

    fun localizedMessage(language: AppLanguage = LocalizationManager.currentLanguage): String =
        trEn(messageTr, messageEn, language)
}

@Serializable
data class FriendBaselineEntry(
    val userId: Int,
    val score: Int = 0,
    val caloriesBurned: Int = 0,
    val workoutMinutes: Int = 0,
    val healthyEatingScore: Int = 0,
    val streakDays: Int = 0
)

/**
 * In-memory reactive bus for live remote/local friend push events while the app process is running.
 * Native push receivers (Android FCM, iOS APNs / UNUserNotificationCenter) also persist events to
 * [FriendNotificationJsonCodec] in shared preferences/UserDefaults so cold launches pick them up.
 */
object FriendNotificationBus {
    private val _liveEvents = MutableSharedFlow<FriendNotificationEvent>(extraBufferCapacity = 32)
    val liveEvents: SharedFlow<FriendNotificationEvent> = _liveEvents.asSharedFlow()

    fun emitLive(event: FriendNotificationEvent) {
        _liveEvents.tryEmit(event)
    }

    fun emitFromPushPayload(
        id: String?,
        actorName: String?,
        actorUsername: String? = null,
        actorUserId: Int = 0,
        typeRaw: String? = null,
        sportType: String? = null,
        title: String? = null,
        body: String? = null
    ): FriendNotificationEvent {
        val rawName = actorName?.trim()?.takeIf { it.isNotBlank() } ?: "HealthTrack"
        val cleanName = rawName.substringBefore(" ").ifBlank { rawName }
        val activityType = FriendActivityType.fromWire(typeRaw)
        val cleanTitle = title?.trim()?.takeIf { it.isNotBlank() }
        val cleanBody = body?.trim()?.takeIf { it.isNotBlank() }
        val eventId = id?.trim()?.takeIf { it.isNotBlank() }
            ?: "push_${activityType.name}_${actorUserId}_${currentTimeMillis()}"
        val event = FriendNotificationEvent(
            id = eventId,
            actorUserId = actorUserId,
            actorName = cleanName,
            actorUsername = actorUsername.orEmpty(),
            type = activityType,
            sportType = sportType?.trim()?.takeIf { it.isNotBlank() },
            titleTr = cleanTitle ?: cleanName,
            titleEn = cleanTitle ?: cleanName,
            messageTr = cleanBody ?: trEn("Yeni bir aktivite paylaştı.", "Shared a new activity.", AppLanguage.TR),
            messageEn = cleanBody ?: trEn("Yeni bir aktivite paylaştı.", "Shared a new activity.", AppLanguage.EN),
            createdAtMs = currentTimeMillis()
        )
        _liveEvents.tryEmit(event)
        return event
    }
}

object PushTokenBus {
    private val _tokenFlow = MutableStateFlow<String?>(null)
    val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

    fun onNewToken(token: String?) {
        val cleaned = token?.trim()?.takeIf { it.isNotBlank() } ?: return
        _tokenFlow.value = cleaned
    }
}

object FriendNotificationJsonCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun decodeList(raw: String?): List<FriendNotificationEvent> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<FriendNotificationEvent>>(raw) }.getOrDefault(emptyList())
    }

    fun encodeList(items: List<FriendNotificationEvent>): String {
        return json.encodeToString(items.takeLast(30))
    }

    fun decodeBaseline(raw: String?): List<FriendBaselineEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<FriendBaselineEntry>>(raw) }.getOrDefault(emptyList())
    }

    fun encodeBaseline(items: List<FriendBaselineEntry>): String {
        return json.encodeToString(items)
    }
}
