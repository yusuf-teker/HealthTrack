package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import com.yusufteker.healthtrack.core.model.WidgetTelemetrySnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WidgetDeepLinkTarget(
    val tabName: String,
    val action: String? = null
)

/**
 * Shared deep-link bus for widget taps (e.g., `healthtrack://open?tab=food&action=add_food`).
 */
object WidgetDeepLinkBus {
    private val _pendingTarget = MutableStateFlow<WidgetDeepLinkTarget?>(null)
    val pendingTarget: StateFlow<WidgetDeepLinkTarget?> = _pendingTarget.asStateFlow()

    fun emit(tabName: String, action: String? = null) {
        _pendingTarget.value = WidgetDeepLinkTarget(
            tabName = tabName.uppercase(),
            action = action?.lowercase()?.takeIf { it.isNotBlank() }
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
     * Requests OS-level widget addition (Android 8.0+ pin dialog) or reports whether manual guide should be shown.
     */
    fun requestAddWidget(onResult: (Boolean, String?) -> Unit)
}

@Composable
expect fun rememberWidgetBridge(): WidgetBridge

