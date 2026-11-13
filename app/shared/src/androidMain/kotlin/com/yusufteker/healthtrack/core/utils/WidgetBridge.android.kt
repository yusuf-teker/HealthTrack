package com.yusufteker.healthtrack.core.utils

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.model.WidgetTelemetrySnapshot
import com.yusufteker.healthtrack.widget.AndroidWidgetPinBus
import com.yusufteker.healthtrack.widget.HealthTrackTelemetryWidgetProvider
import com.yusufteker.healthtrack.widget.HealthTrackWidgetPinReceiver
import com.yusufteker.healthtrack.widget.HealthTrackWidgetStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private class AndroidWidgetBridge(private val context: Context) : WidgetBridge {
    override val isWidgetSupportedOnPlatform: Boolean = true

    override val supportsOneTapPinning: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            AppWidgetManager.getInstance(context).isRequestPinAppWidgetSupported

    override fun checkWidgetInstalled(onResult: (Boolean) -> Unit) {
        onResult(HealthTrackWidgetStore.isAnyWidgetInstalled(context))
    }

    override fun updateSnapshot(snapshot: WidgetTelemetrySnapshot) {
        HealthTrackWidgetStore.saveSnapshot(context, snapshot)
        HealthTrackWidgetStore.updateAllWidgets(context)
    }

    override fun consumePendingWaterDeltaMl(dateIso: String): Int = 0

    override fun requestAddWidget(onResult: (Boolean, String?) -> Unit) {
        val manager = AppWidgetManager.getInstance(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager.isRequestPinAppWidgetSupported) {
            val provider = ComponentName(context, HealthTrackTelemetryWidgetProvider::class.java)
            val callbackIntent = Intent(context, HealthTrackWidgetPinReceiver::class.java)
            val successCallback = PendingIntent.getBroadcast(
                context,
                3001,
                callbackIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val requested = manager.requestPinAppWidget(provider, null, successCallback)
            if (requested) {
                CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
                    val confirmed = withTimeoutOrNull(12_000L) {
                        AndroidWidgetPinBus.pinnedEvents.first()
                    } ?: HealthTrackWidgetStore.isAnyWidgetInstalled(context)
                    if (confirmed) {
                        onResult(
                            true,
                            trEn(
                                "HealthTrack widget'ı ana ekranına eklendi!",
                                "HealthTrack widget added to your home screen!"
                            )
                        )
                    } else {
                        onResult(false, null)
                    }
                }
            } else {
                onResult(false, null)
            }
        } else {
            onResult(false, null)
        }
    }
}

@Composable
actual fun rememberWidgetBridge(): WidgetBridge {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidWidgetBridge(context) }
}

