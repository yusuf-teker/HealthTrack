package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.yusufteker.healthtrack.core.model.DeviceHealthSnapshot
import com.yusufteker.healthtrack.core.model.HealthPlatformStatus

@Composable
actual fun rememberDeviceHealthBridge(): DeviceHealthBridge {
    return remember {
        object : DeviceHealthBridge {
            override val status: HealthPlatformStatus = HealthPlatformStatus.NOT_SUPPORTED
            override val platformName: String = "Health Connect"

            override fun syncLastDays(
                days: Int,
                onResult: (Result<DeviceHealthSnapshot>) -> Unit
            ) {
                onResult(
                    Result.failure(
                        IllegalStateException("Web tarayıcısında doğrudan cihaz sağlık köprüsü desteklenmemektedir.")
                    )
                )
            }

            override fun openHealthSettings() {}
        }
    }
}

