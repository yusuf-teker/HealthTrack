package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import com.yusufteker.healthtrack.core.model.DeviceHealthSnapshot
import com.yusufteker.healthtrack.core.model.HealthPlatformStatus

/**
 * Platform-agnostic bridge to OS Health Hubs (Android Health Connect & Apple HealthKit).
 * Connects smart scales (Xiaomi / Zepp Life / Mi Fitness) and smartwatches (Huawei Watch / Apple Watch / WearOS)
 * with HealthTrack.
 */
interface DeviceHealthBridge {
    val status: HealthPlatformStatus
    val platformName: String

    /**
     * Requests necessary read permissions (if not already granted) and fetches weight, body fat,
     * and exercise/workout records from the last [days] days.
     */
    fun syncLastDays(
        days: Int = 30,
        onResult: (Result<DeviceHealthSnapshot>) -> Unit
    )

    /**
     * Opens system Health Connect / Apple Health settings or Play Store installation page.
     */
    fun openHealthSettings()
}

/**
 * Creates and remembers a [DeviceHealthBridge] instance bound to the current Compose lifecycle.
 */
@Composable
expect fun rememberDeviceHealthBridge(): DeviceHealthBridge

