package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import com.yusufteker.healthtrack.core.model.WidgetTelemetrySnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

@Serializable
data class PendingWidgetWorkout(
    val dateIso: String,
    val rawText: String,
    val durationMinutes: Int,
    val caloriesBurned: Int,
    val createdAtMs: Long = 0L
)

@Serializable
data class PendingWidgetWeight(
    val dateIso: String,
    val weightKg: Double,
    val createdAtMs: Long = 0L
)

data class WidgetDeepLinkTarget(
    val tabName: String,
    val action: String? = null,
    val eventId: Long = currentTimeMillis()
)

/**
 * Shared deep-link bus for widget taps (e.g., `healthtrack://open?tab=food&action=add_food`).
 */
object WidgetDeepLinkBus {
    private val _pendingTarget = MutableStateFlow<WidgetDeepLinkTarget?>(null)
    val pendingTarget: StateFlow<WidgetDeepLinkTarget?> = _pendingTarget.asStateFlow()
    private var nextEventSeq: Long = 0L

    fun emit(tabName: String, action: String? = null) {
        nextEventSeq += 1L
        _pendingTarget.value = WidgetDeepLinkTarget(
            tabName = tabName.uppercase(),
            action = action?.lowercase()?.takeIf { it.isNotBlank() },
            eventId = currentTimeMillis() * 1000L + (nextEventSeq % 1000L)
        )
    }

    fun emitFromUri(uriString: String?) {
        if (uriString.isNullOrBlank()) return
        val normalized = uriString.trim()
        if (!normalized.startsWith("healthtrack://", ignoreCase = true)) return

        val queryPart = normalized.substringAfter("?", "")
        var tab: String? = null
        var action: String? = null

        if (queryPart.isNotBlank()) {
            queryPart.split("&").forEach { pair ->
                val eqIndex = pair.indexOf('=')
                if (eqIndex > 0) {
                    val key = pair.substring(0, eqIndex).lowercase()
                    val value = pair.substring(eqIndex + 1)
                    when (key) {
                        "tab" -> tab = value.uppercase()
                        "action" -> action = value.lowercase()
                    }
                }
            }
        }

        if (tab == null) {
            val host = normalized.removePrefix("healthtrack://").substringBefore("?").substringBefore("/").uppercase()
            if (host == "HUAWEI") {
                HuaweiDeepLinkBus.emitFromUri(normalized)
                return
            }
            tab = when (host) {
                "FOOD", "WORKOUT", "WEIGHT", "ANALYTICS" -> host
                else -> "FOOD"
            }
        }

        emit(tabName = tab ?: "FOOD", action = action)
    }

    fun clearTarget() {
        _pendingTarget.value = null
    }

    fun consumeAction(expectedAction: String): Boolean {
        val current = _pendingTarget.value ?: return false
        if (current.action == expectedAction.lowercase()) {
            _pendingTarget.value = null
            return true
        }
        return false
    }
}

data class HuaweiOAuthCallbackEvent(
    val status: String,
    val reason: String? = null,
    val timestampMs: Long = 0L
)

/**
 * Shared deep-link bus for Huawei OAuth callbacks (`healthtrack://huawei/sync-done?status=connected`).
 */
object HuaweiDeepLinkBus {
    private val _pendingEvent = MutableStateFlow<HuaweiOAuthCallbackEvent?>(null)
    val pendingEvent: StateFlow<HuaweiOAuthCallbackEvent?> = _pendingEvent.asStateFlow()

    fun emitFromUri(uriString: String?) {
        if (uriString.isNullOrBlank()) return
        val normalized = uriString.trim()
        if (!normalized.startsWith("healthtrack://huawei", ignoreCase = true)) return

        val queryPart = normalized.substringAfter("?", "")
        var status = "connected"
        var reason: String? = null

        if (queryPart.isNotBlank()) {
            queryPart.split("&").forEach { pair ->
                val eqIndex = pair.indexOf('=')
                if (eqIndex > 0) {
                    val key = pair.substring(0, eqIndex).lowercase()
                    val value = pair.substring(eqIndex + 1)
                    when (key) {
                        "status" -> status = value.lowercase().ifBlank { "connected" }
                        "reason" -> reason = decodePercent(value).takeIf { it.isNotBlank() }
                    }
                }
            }
        }

        _pendingEvent.value = HuaweiOAuthCallbackEvent(
            status = status,
            reason = reason,
            timestampMs = currentTimeMillis()
        )
    }

    fun consume(): HuaweiOAuthCallbackEvent? {
        val current = _pendingEvent.value
        if (current != null) {
            _pendingEvent.value = null
        }
        return current
    }

    private fun decodePercent(raw: String): String {
        val plusAsSpace = raw.replace('+', ' ')
        val out = StringBuilder(plusAsSpace.length)
        var i = 0
        while (i < plusAsSpace.length) {
            val ch = plusAsSpace[i]
            if (ch == '%' && i + 2 < plusAsSpace.length) {
                val hex = plusAsSpace.substring(i + 1, i + 3)
                val code = hex.toIntOrNull(16)
                if (code != null) {
                    out.append(code.toChar())
                    i += 3
                    continue
                }
            }
            out.append(ch)
            i++
        }
        return out.toString()
    }
}

/**
 * Platform-agnostic bridge for Home Screen Widgets (Android AppWidget & iOS WidgetKit).
 */
interface WidgetBridge {
    val isWidgetSupportedOnPlatform: Boolean
    val supportsOneTapPinning: Boolean

    /**
     * Checks whether the user currently has at least one HealthTrack widget placed on their Home/Lock screen.
     */
    fun checkWidgetInstalled(onResult: (Boolean) -> Unit)

    /**
     * Persists the latest telemetry snapshot for widgets and triggers an immediate widget refresh.
     */
    fun updateSnapshot(snapshot: WidgetTelemetrySnapshot)

    /**
     * Consumes any interactive water increments triggered directly from the widget while the app was inactive.
     */
    fun consumePendingWaterDeltaMl(dateIso: String): Int

    /**
     * Consumes any quick workout logs triggered directly from the widget while the app was inactive (e.g. iOS AppIntents).
     */
    fun consumePendingWorkouts(): List<PendingWidgetWorkout> = emptyList()

    /**
     * Consumes any quick weight logs triggered directly from the widget while the app was inactive (e.g. iOS AppIntents).
     */
    fun consumePendingWeights(): List<PendingWidgetWeight> = emptyList()

    /**
     * Requests OS-level widget addition (Android 8.0+ pin dialog) or reports whether manual guide should be shown.
     */
    fun requestAddWidget(onResult: (Boolean, String?) -> Unit)
}

@Composable
expect fun rememberWidgetBridge(): WidgetBridge

