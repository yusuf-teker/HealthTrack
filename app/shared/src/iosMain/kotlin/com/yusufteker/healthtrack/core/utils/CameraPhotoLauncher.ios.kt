package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.yusufteker.healthtrack.core.i18n.trEn
import org.jetbrains.skia.Image
import platform.UIKit.*
import platform.Foundation.*
import platform.Vision.*
import platform.CoreGraphics.*
import platform.AVFoundation.*
import platform.Photos.*
import platform.darwin.NSObject
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.useContents

private fun cleanOcrTextIos(raw: String): String {
    var text = raw

    // 1. Join split decimal dots / commas between numbers: "3 . 3" -> "3.3", "6 , 1" -> "6.1"
    text = text.replace(Regex("(\\d+)\\s*[.,]\\s*(\\d+)")) { match ->
        "${match.groupValues[1]}.${match.groupValues[2]}"
    }

    // 2. Fix missed decimal dot where single digit space single digit occurs before unit:
    text = text.replace(Regex("(\\d)\\s+(\\d)\\s*([gG]|mg|kcal|cal|ml|gr)\\b")) { match ->
        "${match.groupValues[1]}.${match.groupValues[2]} ${match.groupValues[3]}"
    }

    // 3. Normalize Turkish decimal comma to dot: "3,3 g" -> "3.3 g"
    text = text.replace(Regex("(\\d+),(\\d+)")) { match ->
        "${match.groupValues[1]}.${match.groupValues[2]}"
    }

    return text
}

@OptIn(ExperimentalForeignApi::class)
class IosCameraPhotoLauncher : CameraPhotoLauncher {
    override val isCameraAvailable: Boolean
        get() = UIImagePickerController.isSourceTypeAvailable(
            UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        )

    private var _isScanning by mutableStateOf(false)
    override val isScanning: Boolean
        get() = _isScanning

    private var callback: ((CapturedPhoto) -> Unit)? = null
    private var lastCapturedPhoto: CapturedPhoto? = null
    private var lastUiImage: UIImage? = null

    private val delegate = object : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
        override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
            val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
            picker.dismissViewControllerAnimated(true, completion = null)
            if (image != null) {
                lastUiImage = image
                processImage(image, isCropped = false)
            } else {
                _isScanning = false
            }
        }

        override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
            picker.dismissViewControllerAnimated(true, completion = null)
            _isScanning = false
        }
    }

    private fun runVisionOcr(jpegData: NSData): String? {
        return try {
            var recognizedLines = mutableListOf<String>()
            val request = VNRecognizeTextRequest { req, err ->
                if (err != null || req == null) return@VNRecognizeTextRequest
                val observations = req.results as? List<*> ?: return@VNRecognizeTextRequest
                for (item in observations) {
                    val obs = item as? VNRecognizedTextObservation ?: continue
                    val topCandidate = obs.topCandidates(1u).firstOrNull() as? VNRecognizedText ?: continue
                    val text = topCandidate.string.trim()
                    if (text.isNotBlank()) {
                        recognizedLines.add(text)
                    }
                }
            }
            request.recognitionLevel = VNRequestTextRecognitionLevelAccurate
            request.usesLanguageCorrection = true
            request.recognitionLanguages = listOf("tr-TR", "en-US")

            val handler = VNImageRequestHandler(data = jpegData, options = emptyMap<Any?, Any>())
            handler.performRequests(listOf(request), null)

            val fullText = recognizedLines.joinToString("\n").trim()
            if (fullText.isNotBlank()) cleanOcrTextIos(fullText) else null
        } catch (_: Throwable) {
            null
        }
    }

    private fun processImage(image: UIImage, isCropped: Boolean, onDone: ((CapturedPhoto) -> Unit)? = null) {
        val jpegData = UIImageJPEGRepresentation(image, 0.85)
        if (jpegData != null) {
            val bytes = jpegData.bytes?.readBytes(jpegData.length.toInt())
            val base64 = jpegData.base64EncodedStringWithOptions(0u)
            val imageBitmap = bytes?.let {
                try {
                    Image.makeFromEncoded(it).toComposeImageBitmap()
                } catch (_: Exception) {
                    null
                }
            }
            val extractedText = runVisionOcr(jpegData)
            val result = CapturedPhoto(
                imageBitmap = imageBitmap,
                base64 = base64,
                extractedText = extractedText,
                isCropped = isCropped
            )
            lastCapturedPhoto = result
            _isScanning = false
            val cb = onDone ?: callback
            cb?.invoke(result)
        } else {
            _isScanning = false
        }
    }

    private fun getTopmostViewController(): UIViewController? {
        val keyWindow = UIApplication.sharedApplication.windows.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true } as? UIWindow
            ?: UIApplication.sharedApplication.keyWindow
        var topVc = keyWindow?.rootViewController
        while (topVc?.presentedViewController != null && !topVc.presentedViewController!!.isBeingDismissed()) {
            topVc = topVc.presentedViewController
        }
        return topVc
    }

    private fun presentPicker(sourceType: UIImagePickerControllerSourceType) {
        val topVc = getTopmostViewController()
        if (topVc == null) {
            _isScanning = false
            return
        }

        val picker = UIImagePickerController().apply {
            this.sourceType = sourceType
            this.delegate = this@IosCameraPhotoLauncher.delegate
        }
        topVc.presentViewController(picker, animated = true, completion = null)
    }

    private fun showPermissionDeniedAlert(title: String, message: String) {
        val topVc = getTopmostViewController() ?: return
        val alert = UIAlertController.alertControllerWithTitle(
            title = title,
            message = message,
            preferredStyle = UIAlertControllerStyleAlert
        )
        val cancelAction = UIAlertAction.actionWithTitle(
            title = trEn("Vazgeç", "Cancel"),
            style = UIAlertActionStyleCancel,
            handler = null
        )
        val settingsAction = UIAlertAction.actionWithTitle(
            title = trEn("Ayarlar'ı Aç", "Open Settings"),
            style = UIAlertActionStyleDefault,
            handler = {
                val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
                if (settingsUrl != null && UIApplication.sharedApplication.canOpenURL(settingsUrl)) {
                    UIApplication.sharedApplication.openURL(settingsUrl, emptyMap<Any?, Any>(), null)
                }
            }
        )
        alert.addAction(cancelAction)
        alert.addAction(settingsAction)
        topVc.presentViewController(alert, animated = true, completion = null)
    }

    override fun launchCamera(onPhotoCaptured: (CapturedPhoto) -> Unit) {
        this.callback = onPhotoCaptured
        this._isScanning = true

        if (!isCameraAvailable) {
            // Kamera donanımı mevcut değilse (ör. Simülatör) galeri seçimine yönlendir
            launchGallery(onPhotoCaptured)
            return
        }

        val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
        when (status) {
            AVAuthorizationStatusAuthorized -> {
                presentPicker(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)
            }
            AVAuthorizationStatusNotDetermined -> {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    NSOperationQueue.mainQueue.addOperationWithBlock {
                        if (granted) {
                            presentPicker(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)
                        } else {
                            _isScanning = false
                        }
                    }
                }
            }
            AVAuthorizationStatusDenied, AVAuthorizationStatusRestricted -> {
                _isScanning = false
                showPermissionDeniedAlert(
                    title = trEn("Kamera İzni Gerekli", "Camera Permission Required"),
                    message = trEn(
                        "Yemek ve besin etiketi fotoğraflarını çekip analiz etmek için kamera iznine ihtiyaç vardır. Lütfen Ayarlar'dan kameraya izin verin.",
                        "Camera access is required to capture and analyze food and nutrition label photos. Please allow camera access in Settings."
                    )
                )
            }
            else -> {
                _isScanning = false
            }
        }
    }

    override fun launchGallery(onPhotoCaptured: (CapturedPhoto) -> Unit) {
        this.callback = onPhotoCaptured
        this._isScanning = true

        val status = PHPhotoLibrary.authorizationStatus()
        when (status) {
            PHAuthorizationStatusAuthorized, PHAuthorizationStatusLimited -> {
                presentPicker(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary)
            }
            PHAuthorizationStatusNotDetermined -> {
                PHPhotoLibrary.requestAuthorization { newStatus ->
                    NSOperationQueue.mainQueue.addOperationWithBlock {
                        if (newStatus == PHAuthorizationStatusAuthorized || newStatus == PHAuthorizationStatusLimited) {
                            presentPicker(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary)
                        } else {
                            _isScanning = false
                        }
                    }
                }
            }
            PHAuthorizationStatusDenied, PHAuthorizationStatusRestricted -> {
                _isScanning = false
                showPermissionDeniedAlert(
                    title = trEn("Galeri İzni Gerekli", "Photo Library Permission Required"),
                    message = trEn(
                        "Besin tablosu ve yemek fotoğraflarını seçip analiz etmek için galeri erişimine ihtiyaç vardır. Lütfen Ayarlar'dan fotoğraflara erişim izni verin.",
                        "Photo library access is required to choose and analyze nutrition label and food photos. Please allow access in Settings."
                    )
                )
            }
            else -> {
                _isScanning = false
            }
        }
    }

    override fun cropAndScan(
        cropRect: CropRect,
        rotationDegrees: Int,
        onPhotoCaptured: (CapturedPhoto) -> Unit
    ) {
        _isScanning = true
        val sourceImage = lastUiImage
        if (sourceImage == null) {
            val fallback = lastCapturedPhoto
            _isScanning = false
            if (fallback != null) onPhotoCaptured(fallback.copy(isCropped = true))
            return
        }

        try {
            val cgImage = sourceImage.CGImage
            if (cgImage != null) {
                val fullWidth = sourceImage.size.useContents { width }
                val fullHeight = sourceImage.size.useContents { height }
                val scale = sourceImage.scale

                val cropX = (cropRect.left.toDouble() * fullWidth * scale).coerceAtLeast(0.0)
                val cropY = (cropRect.top.toDouble() * fullHeight * scale).coerceAtLeast(0.0)
                val cropW = ((cropRect.right - cropRect.left).toDouble() * fullWidth * scale).coerceAtLeast(1.0)
                val cropH = ((cropRect.bottom - cropRect.top).toDouble() * fullHeight * scale).coerceAtLeast(1.0)

                val cgRect = CGRectMake(cropX, cropY, cropW, cropH)
                val croppedCg = CGImageCreateWithImageInRect(cgImage, cgRect)
                if (croppedCg != null) {
                    val croppedUiImage = UIImage.imageWithCGImage(croppedCg, sourceImage.scale, sourceImage.imageOrientation)
                    processImage(croppedUiImage, isCropped = true, onDone = onPhotoCaptured)
                    return
                }
            }
        } catch (_: Throwable) {}

        // Fallback if CGImage crop failed
        processImage(sourceImage, isCropped = true, onDone = onPhotoCaptured)
    }
}

@Composable
actual fun rememberCameraPhotoLauncher(): CameraPhotoLauncher {
    return remember {
        IosCameraPhotoLauncher()
    }
}
