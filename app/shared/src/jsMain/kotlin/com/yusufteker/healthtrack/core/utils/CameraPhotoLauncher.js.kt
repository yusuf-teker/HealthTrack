package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberCameraPhotoLauncher(): CameraPhotoLauncher {
    return remember {
        object : CameraPhotoLauncher {
            override val isCameraAvailable: Boolean = false
            override val isScanning: Boolean = false
            override fun launchCamera(onPhotoCaptured: (CapturedPhoto) -> Unit) {}
            override fun launchGallery(onPhotoCaptured: (CapturedPhoto) -> Unit) {}
            override fun cropAndScan(
                cropRect: CropRect,
                rotationDegrees: Int,
                onPhotoCaptured: (CapturedPhoto) -> Unit
            ) {}
        }
    }
}
