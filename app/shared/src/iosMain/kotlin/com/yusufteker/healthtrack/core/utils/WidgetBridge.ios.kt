package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.yusufteker.healthtrack.core.model.WidgetTelemetrySnapshot
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

interface IosWidgetCheckCallback {
    fun onResult(installed: Boolean)
}

interface IosWidgetPlatformDelegate {
    fun reloadAllTimelines()
    fun checkWidgetInstalled(callback: IosWidgetCheckCallback)
}

/**
 * Swift bridge registered by [iOSApp] on startup to interact with WidgetKit (`WidgetCenter`).
 */
object IosWidgetSwiftBridge {
    const val APP_GROUP_ID = "group.com.yusufteker.healthtrack"
    const val KEY_WIDGET_SNAPSHOT = "ht_widget_snapshot"
    const val KEY_PENDING_WATER_DELTA_ML = "ht_widget_pending_water_delta_ml"
    const val KEY_PENDING_WATER_DATE = "ht_widget_pending_water_date"

    internal var delegate: IosWidgetPlatformDelegate? = null
        private set

    fun register(delegate: IosWidgetPlatformDelegate) {
        this.delegate = delegate
    }

    fun handleDeepLink(urlString: String) {
        WidgetDeepLinkBus.emitFromUri(urlString)
    }
}

private class IosWidgetBridge : WidgetBridge {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun sharedDefaults(): NSUserDefaults {
        return NSUserDefaults(suiteName = IosWidgetSwiftBridge.APP_GROUP_ID)
            ?: NSUserDefaults.standardUserDefaults
    }

    override val isWidgetSupportedOnPlatform: Boolean = true
    override val supportsOneTapPinning: Boolean = false

    override fun checkWidgetInstalled(onResult: (Boolean) -> Unit) {
        val delegate = IosWidgetSwiftBridge.delegate
        if (delegate == null) {
            onResult(false)
            return
        }
        delegate.checkWidgetInstalled(object : IosWidgetCheckCallback {
            override fun onResult(installed: Boolean) {
                onResult(installed)
            }
        })
    }

    override fun updateSnapshot(snapshot: WidgetTelemetrySnapshot) {
        try {
            val serialized = json.encodeToString(snapshot)
            val defaults = sharedDefaults()
            defaults.setObject(serialized, forKey = IosWidgetSwiftBridge.KEY_WIDGET_SNAPSHOT)
            defaults.synchronize()
            IosWidgetSwiftBridge.delegate?.reloadAllTimelines()
        } catch (_: Throwable) {
        }
    }

    override fun consumePendingWaterDeltaMl(dateIso: String): Int {
        val defaults = sharedDefaults()
        val delta = defaults.integerForKey(IosWidgetSwiftBridge.KEY_PENDING_WATER_DELTA_ML).toInt()
        val pendingDate = defaults.stringForKey(IosWidgetSwiftBridge.KEY_PENDING_WATER_DATE)
        if (delta <= 0) return 0

        defaults.removeObjectForKey(IosWidgetSwiftBridge.KEY_PENDING_WATER_DELTA_ML)
        defaults.removeObjectForKey(IosWidgetSwiftBridge.KEY_PENDING_WATER_DATE)
        defaults.synchronize()

        return if (pendingDate == null || pendingDate == dateIso) delta else 0
    }

    override fun requestAddWidget(onResult: (Boolean, String?) -> Unit) {
        // iOS does not provide a public API to programmatically pin a WidgetKit widget to Home Screen;
        // the UI presents the interactive 3-step visual guide instead.
        onResult(false, null)
    }
}

@Composable
actual fun rememberWidgetBridge(): WidgetBridge = remember { IosWidgetBridge() }

