package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.yusufteker.healthtrack.core.i18n.trEn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Composable
actual fun rememberBleScaleBridge(): BleScaleBridge {
    return remember {
        object : BleScaleBridge {
            override val isSupported: Boolean = false
            private val _state = MutableStateFlow(
                BleScaleLiveState(
                    scanState = BleScaleScanState.NOT_SUPPORTED,
                    statusMessage = trEn(
                        "Web tarayıcısında doğrudan Bluetooth tartı taraması desteklenmemektedir.",
                        "Direct Bluetooth scale scanning is not supported in web browsers."
                    )
                )
            )
            override val state: StateFlow<BleScaleLiveState> = _state.asStateFlow()

            override fun startScan(heightCm: Double, age: Int, isMale: Boolean) {}

            override fun stopScan() {}
        }
    }
}
