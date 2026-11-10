package com.yusufteker.healthtrack.core.utils

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class AndroidCameraPhotoLauncher(
    private val context: Context,
    private val takePictureLauncher: androidx.activity.result.ActivityResultLauncher<Void?>,
    private val pickGalleryLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    private val setCallback: ((String) -> Unit) -> Unit,
    private val setScanning: (Boolean) -> Unit,
    override val isOcrScanning: Boolean
) : CameraPhotoLauncher {

    override val isCameraAvailable: Boolean
        get() = try {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        } catch (_: Exception) {
            false
        }

    override fun launchCamera(onTextExtracted: (String) -> Unit) {
        setCallback(onTextExtracted)
        setScanning(true)
        try {
            takePictureLauncher.launch(null)
        } catch (e: Exception) {
            setScanning(false)
        }
    }

    override fun launchGallery(onTextExtracted: (String) -> Unit) {
        setCallback(onTextExtracted)
        setScanning(true)
        try {
            pickGalleryLauncher.launch("image/*")
        } catch (e: Exception) {
            setScanning(false)
        }
    }
}

@Composable
actual fun rememberCameraPhotoLauncher(): CameraPhotoLauncher {
    val context = LocalContext.current
    var onResultCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var isOcrScanning by remember { mutableStateOf(false) }

    fun processImageAndExtractText(inputImage: InputImage) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val text = visionText.text.trim()
                isOcrScanning = false
                onResultCallback?.invoke(text)
            }
            .addOnFailureListener {
                isOcrScanning = false
            }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            processImageAndExtractText(inputImage)
        } else {
            isOcrScanning = false
        }
    }

    val pickGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                processImageAndExtractText(inputImage)
            } catch (e: Exception) {
                isOcrScanning = false
            }
        } else {
            isOcrScanning = false
        }
    }

    return remember(context, isOcrScanning) {
        AndroidCameraPhotoLauncher(
            context = context,
            takePictureLauncher = takePictureLauncher,
            pickGalleryLauncher = pickGalleryLauncher,
            setCallback = { onResultCallback = it },
            setScanning = { isOcrScanning = it },
            isOcrScanning = isOcrScanning
        )
    }
}

