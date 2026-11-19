package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

/**
 * Current connection/measurement lifecycle of the direct BLE smart scale scanner.
 */
enum class BleScaleScanState {
    IDLE,
    REQUESTING_PERMISSION,
    SCANNING,
    MEASURING,
    STABILIZED,
    BLUETOOTH_OFF,
    PERMISSION_DENIED,
    NOT_SUPPORTED,
    ERROR
}

/**
 * Snapshot state exposed by [BleScaleBridge] during live smart scale weigh-ins.
 */
data class BleScaleLiveState(
    val scanState: BleScaleScanState = BleScaleScanState.IDLE,
    val liveReading: BleScaleReading? = null,
    val statusMessage: String? = null
)

/**
 * Cross-platform bridge for scanning and decoding live BLE smart scale advertisements
 * (Xiaomi Mi Smart Scale 1 & Mi Body Composition Scale 2) without requiring third-party bridges.
 */
interface BleScaleBridge {
    /** Whether BLE smart scale scanning is supported on the current platform. */
    val isSupported: Boolean

    /** Observable scan & decoded measurement state. */
    val state: StateFlow<BleScaleLiveState>

    /**
     * Starts scanning for BLE scale advertisements and computing BIA body fat / muscle metrics
     * using the user's anthropometric profile ([heightCm], [age], [isMale]).
     */
    fun startScan(heightCm: Double, age: Int, isMale: Boolean)

    /** Stops active BLE scanning and resets transient errors if appropriate. */
    fun stopScan()
}

/**
 * Creates and remembers a platform-specific [BleScaleBridge] bound to the Compose lifecycle.
 */
@Composable
expect fun rememberBleScaleBridge(): BleScaleBridge
