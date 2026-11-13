package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSBundle

@Composable
actual fun rememberAppVersion(): String {
    return remember {
        try {
            (NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String) ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }
}

