package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Normalized crop rectangle with coordinates from 0.0f to 1.0f.
 */
data class CropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
)

/**
 * Result data of a captured or selected photo.
 *
 * @param imageBitmap Displayable Compose [ImageBitmap] for on-screen UI preview.
 * @param base64 Base64 encoded JPEG string (e.g. for Gemini vision API payload).
 * @param extractedText Text extracted via on-device OCR (ML Kit / Vision), if any.
 * @param isCropped Whether this photo has been cropped.
 */
data class CapturedPhoto(
    val imageBitmap: ImageBitmap? = null,
    val base64: String? = null,
    val extractedText: String? = null,
    val isCropped: Boolean = false
)

/**
 * Platform-agnostic camera & gallery photo picker contract.
 */
interface CameraPhotoLauncher {
    val isCameraAvailable: Boolean
    val isScanning: Boolean

    /**
     * Launches the system camera to take a picture at full resolution.
     * Invokes [onPhotoCaptured] with bitmap, base64 and optional OCR text.
     */
    fun launchCamera(onPhotoCaptured: (CapturedPhoto) -> Unit)

    /**
     * Launches the gallery/photo picker to select an existing picture.
     * Invokes [onPhotoCaptured] with bitmap, base64 and optional OCR text.
     */
    fun launchGallery(onPhotoCaptured: (CapturedPhoto) -> Unit)

    /**
     * Crops and re-scans the active photo using normalized crop bounds and optional rotation.
     * Invokes [onPhotoCaptured] with the cropped bitmap, base64 and clean OCR text.
     */
    fun cropAndScan(
        cropRect: CropRect,
        rotationDegrees: Int = 0,
        onPhotoCaptured: (CapturedPhoto) -> Unit
    )
}

/**
 * Creates and remembers a [CameraPhotoLauncher] instance for the current platform.
 */
@Composable
expect fun rememberCameraPhotoLauncher(): CameraPhotoLauncher
