package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import platform.UIKit.*
import platform.Foundation.*
import platform.darwin.NSObject
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes

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

    private val delegate = object : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
        override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
            val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
            picker.dismissViewControllerAnimated(true, completion = null)
            if (image != null) {
                processImage(image)
            } else {
                _isScanning = false
            }
        }

        override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
            picker.dismissViewControllerAnimated(true, completion = null)
            _isScanning = false
        }
    }

    private fun processImage(image: UIImage) {
        val jpegData = UIImageJPEGRepresentation(image, 0.8)
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
            val result = CapturedPhoto(
                imageBitmap = imageBitmap,
                base64 = base64,
                extractedText = null
            )
            lastCapturedPhoto = result
            _isScanning = false
            callback?.invoke(result)
        } else {
            _isScanning = false
        }
    }

    private fun presentPicker(sourceType: UIImagePickerControllerSourceType) {
        val rootVc = UIApplication.sharedApplication.keyWindow?.rootViewController
            ?: UIApplication.sharedApplication.windows.firstOrNull { (it as? UIWindow)?.isKeyWindow() == true }?.let { (it as UIWindow).rootViewController }

        val picker = UIImagePickerController().apply {
            this.sourceType = sourceType
            this.delegate = this@IosCameraPhotoLauncher.delegate
        }
        rootVc?.presentViewController(picker, animated = true, completion = null)
    }

    override fun launchCamera(onPhotoCaptured: (CapturedPhoto) -> Unit) {
        this.callback = onPhotoCaptured
        this._isScanning = true
        if (isCameraAvailable) {
            presentPicker(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)
        } else {
            presentPicker(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary)
        }
    }

    override fun launchGallery(onPhotoCaptured: (CapturedPhoto) -> Unit) {
        this.callback = onPhotoCaptured
        this._isScanning = true
        presentPicker(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary)
    }

    override fun cropAndScan(
        cropRect: CropRect,
        rotationDegrees: Int,
        onPhotoCaptured: (CapturedPhoto) -> Unit
    ) {
        // Fallback crop handler for iOS
        _isScanning = true
        val lastPhoto = lastCapturedPhoto
        if (lastPhoto != null) {
            _isScanning = false
            onPhotoCaptured(lastPhoto.copy(isCropped = true))
        } else {
            _isScanning = false
        }
    }
}

@Composable
actual fun rememberCameraPhotoLauncher(): CameraPhotoLauncher {
    return remember {
        IosCameraPhotoLauncher()
    }
}

