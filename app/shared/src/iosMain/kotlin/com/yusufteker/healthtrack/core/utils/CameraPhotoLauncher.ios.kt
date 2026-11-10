package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberCameraPhotoLauncher(): CameraPhotoLauncher {
    return remember {
        object : CameraPhotoLauncher {
            override val isCameraAvailable: Boolean = false
            override val isOcrScanning: Boolean = false
            override fun launchCamera(onTextExtracted: (String) -> Unit) {}
            override fun launchGallery(onTextExtracted: (String) -> Unit) {}
        }
    }
}
