package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable

/**
 * Platform-agnostic camera & gallery photo picker contract with on-device OCR text extraction.
 */
interface CameraPhotoLauncher {
    val isCameraAvailable: Boolean
    val isOcrScanning: Boolean
    fun launchCamera(onTextExtracted: (String) -> Unit)
    fun launchGallery(onTextExtracted: (String) -> Unit)
}

/**
 * Creates and remembers a [CameraPhotoLauncher] instance for the current platform.
 */
@Composable
expect fun rememberCameraPhotoLauncher(): CameraPhotoLauncher

