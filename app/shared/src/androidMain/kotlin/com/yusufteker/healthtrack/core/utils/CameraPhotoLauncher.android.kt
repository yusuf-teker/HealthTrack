package com.yusufteker.healthtrack.core.utils

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.ByteArrayOutputStream
import java.io.File

class AndroidCameraPhotoLauncher(
    private val context: Context,
    private val tempCameraUri: Uri,
    private val takePictureLauncher: androidx.activity.result.ActivityResultLauncher<Uri>,
    private val pickGalleryLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    private val setCallback: ((CapturedPhoto) -> Unit) -> Unit,
    private val setScanning: (Boolean) -> Unit,
    private val cropAndScanHandler: (CropRect, Int, (CapturedPhoto) -> Unit) -> Unit,
    override val isScanning: Boolean
) : CameraPhotoLauncher {

    override val isCameraAvailable: Boolean
        get() = try {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        } catch (_: Exception) {
            false
        }

    override fun launchCamera(onPhotoCaptured: (CapturedPhoto) -> Unit) {
        setCallback(onPhotoCaptured)
        setScanning(true)
        try {
            takePictureLauncher.launch(tempCameraUri)
        } catch (_: Exception) {
            setScanning(false)
        }
    }

    override fun launchGallery(onPhotoCaptured: (CapturedPhoto) -> Unit) {
        setCallback(onPhotoCaptured)
        setScanning(true)
        try {
            pickGalleryLauncher.launch("image/*")
        } catch (_: Exception) {
            setScanning(false)
        }
    }

    override fun cropAndScan(
        cropRect: CropRect,
        rotationDegrees: Int,
        onPhotoCaptured: (CapturedPhoto) -> Unit
    ) {
        cropAndScanHandler(cropRect, rotationDegrees, onPhotoCaptured)
    }
}

private fun decodeSampledBitmapFromFile(path: String, maxDim: Int = 2048): Bitmap {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeFile(path, options)
    val maxOriginal = maxOf(options.outWidth, options.outHeight)
    var sampleSize = 1
    while (maxOriginal / (sampleSize * 2) >= maxDim) {
        sampleSize *= 2
    }
    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    val rawBitmap = BitmapFactory.decodeFile(path, decodeOptions)
        ?: throw IllegalStateException("Bitmap decode failed for path: $path")
    return rotateBitmapByExif(rawBitmap, path)
}

private fun rotateBitmapByExif(bitmap: Bitmap, path: String): Bitmap {
    return try {
        val exif = ExifInterface(path)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } catch (_: Exception) {
        bitmap
    }
}

private fun optimizeBitmapForOcr(bitmap: Bitmap, targetDim: Int = 1800): Bitmap {
    val maxOriginal = maxOf(bitmap.width, bitmap.height)
    if (maxOriginal in 1100..targetDim) {
        return bitmap
    }
    val ratio = if (maxOriginal < 1100) {
        // Upscale small cropped labels so decimal dots are clear and several pixels wide
        minOf(1600f / maxOriginal, 3.0f)
    } else {
        // Downscale large full photos to save memory while preserving crisp text
        targetDim.toFloat() / maxOriginal
    }
    val newWidth = (bitmap.width * ratio).toInt().coerceAtLeast(1)
    val newHeight = (bitmap.height * ratio).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}

private fun cleanOcrText(raw: String): String {
    var text = raw

    // 1. Join split decimal dots / commas between numbers: "3 . 3" -> "3.3", "6 , 1" -> "6.1"
    text = text.replace(Regex("(\\d+)\\s*[.,]\\s*(\\d+)")) { match ->
        "${match.groupValues[1]}.${match.groupValues[2]}"
    }

    // 2. Fix missed decimal dot where single digit space single digit occurs before unit:
    // e.g. "3 3 g" -> "3.3 g", "6 1 g" -> "6.1 g", "0 5 g" -> "0.5 g", "1 5 g" -> "1.5 g"
    text = text.replace(Regex("(\\d)\\s+(\\d)\\s*([gG]|mg|kcal|cal|ml|gr)\\b")) { match ->
        "${match.groupValues[1]}.${match.groupValues[2]} ${match.groupValues[3]}"
    }

    // 3. Normalize Turkish decimal comma to dot: "3,3 g" -> "3.3 g"
    text = text.replace(Regex("(\\d+),(\\d+)")) { match ->
        "${match.groupValues[1]}.${match.groupValues[2]}"
    }

    return text
}

private fun scaleBitmapDown(bitmap: Bitmap, maxDim: Int = 1800): Bitmap {
    if (bitmap.width <= maxDim && bitmap.height <= maxDim) return bitmap
    val ratio = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
    val newWidth = (bitmap.width * ratio).toInt().coerceAtLeast(1)
    val newHeight = (bitmap.height * ratio).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
}

private fun bitmapToBase64(bitmap: Bitmap, quality: Int = 85): String {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
    return android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)
}

@Composable
actual fun rememberCameraPhotoLauncher(): CameraPhotoLauncher {
    val context = LocalContext.current
    var onResultCallback by remember { mutableStateOf<((CapturedPhoto) -> Unit)?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var currentSourceBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val tempCameraFile = remember(context) {
        File(context.cacheDir, "camera_photo_capture.jpg")
    }

    val tempCameraUri = remember(context, tempCameraFile) {
        try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempCameraFile
            )
        } catch (_: Exception) {
            Uri.EMPTY
        }
    }

    fun processBitmap(
        bitmap: Bitmap,
        runOcr: Boolean = true,
        isCropped: Boolean = false,
        callback: ((CapturedPhoto) -> Unit)? = onResultCallback
    ) {
        val scaled = optimizeBitmapForOcr(bitmap, 1800)
        val base64 = bitmapToBase64(scaled, 85)
        val imageBitmap = scaled.asImageBitmap()

        if (!runOcr) {
            isScanning = false
            callback?.invoke(
                CapturedPhoto(
                    imageBitmap = imageBitmap,
                    base64 = base64,
                    extractedText = null,
                    isCropped = isCropped
                )
            )
            return
        }

        val inputImage = InputImage.fromBitmap(scaled, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val formattedLines = visionText.textBlocks
                    .flatMap { it.lines }
                    .map { it.text.trim() }
                    .filter { it.isNotBlank() }
                    .joinToString("\n")
                val rawText = formattedLines.ifBlank { visionText.text.trim() }
                val cleanedText = cleanOcrText(rawText)

                isScanning = false
                callback?.invoke(
                    CapturedPhoto(
                        imageBitmap = imageBitmap,
                        base64 = base64,
                        extractedText = cleanedText.takeIf { it.isNotBlank() },
                        isCropped = isCropped
                    )
                )
            }
            .addOnFailureListener {
                isScanning = false
                callback?.invoke(
                    CapturedPhoto(
                        imageBitmap = imageBitmap,
                        base64 = base64,
                        extractedText = null,
                        isCropped = isCropped
                    )
                )
            }
    }

    fun handleCropAndScan(
        cropRect: CropRect,
        rotationDegrees: Int,
        callback: (CapturedPhoto) -> Unit
    ) {
        val source = currentSourceBitmap ?: return
        isScanning = true
        try {
            var workingBitmap = source
            if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                workingBitmap = Bitmap.createBitmap(
                    workingBitmap,
                    0,
                    0,
                    workingBitmap.width,
                    workingBitmap.height,
                    matrix,
                    true
                )
            }

            val l = (cropRect.left.coerceIn(0f, 1f) * workingBitmap.width).toInt()
                .coerceIn(0, workingBitmap.width - 1)
            val t = (cropRect.top.coerceIn(0f, 1f) * workingBitmap.height).toInt()
                .coerceIn(0, workingBitmap.height - 1)
            val r = (cropRect.right.coerceIn(0f, 1f) * workingBitmap.width).toInt()
                .coerceIn(l + 1, workingBitmap.width)
            val b = (cropRect.bottom.coerceIn(0f, 1f) * workingBitmap.height).toInt()
                .coerceIn(t + 1, workingBitmap.height)

            val width = (r - l).coerceAtLeast(1)
            val height = (b - t).coerceAtLeast(1)

            val cropped = Bitmap.createBitmap(workingBitmap, l, t, width, height)
            processBitmap(cropped, runOcr = true, isCropped = true, callback = callback)
        } catch (_: Exception) {
            isScanning = false
        }
    }

    // High-resolution camera contract: writes to tempCameraFile via FileProvider Uri
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempCameraFile.exists()) {
            try {
                val fullBitmap = decodeSampledBitmapFromFile(tempCameraFile.absolutePath, 2048)
                currentSourceBitmap = fullBitmap
                processBitmap(fullBitmap, runOcr = true, isCropped = false)
            } catch (_: Exception) {
                isScanning = false
            }
        } else {
            isScanning = false
        }
    }

    val pickGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val tempGalleryFile = File(context.cacheDir, "gallery_photo_capture.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempGalleryFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val fullBitmap = decodeSampledBitmapFromFile(tempGalleryFile.absolutePath, 2048)
                currentSourceBitmap = fullBitmap
                processBitmap(fullBitmap, runOcr = true, isCropped = false)
            } catch (_: Exception) {
                isScanning = false
            }
        } else {
            isScanning = false
        }
    }

    return remember(context, isScanning, tempCameraUri, currentSourceBitmap) {
        AndroidCameraPhotoLauncher(
            context = context,
            tempCameraUri = tempCameraUri,
            takePictureLauncher = takePictureLauncher,
            pickGalleryLauncher = pickGalleryLauncher,
            setCallback = { onResultCallback = it },
            setScanning = { isScanning = it },
            cropAndScanHandler = ::handleCropAndScan,
            isScanning = isScanning
        )
    }
}
