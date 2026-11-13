package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.yusufteker.healthtrack.core.model.WidgetTelemetrySnapshot

@Composable
actual fun rememberWidgetBridge(): WidgetBridge = remember {
    object : WidgetBridge {
        override val isWidgetSupportedOnPlatform: Boolean = false
        override val supportsOneTapPinning: Boolean = false
        override fun checkWidgetInstalled(onResult: (Boolean) -> Unit) = onResult(false)
        override fun updateSnapshot(snapshot: WidgetTelemetrySnapshot) {}
        override fun consumePendingWaterDeltaMl(dateIso: String): Int = 0
        override fun requestAddWidget(onResult: (Boolean, String?) -> Unit) = onResult(false, null)
    }
}

