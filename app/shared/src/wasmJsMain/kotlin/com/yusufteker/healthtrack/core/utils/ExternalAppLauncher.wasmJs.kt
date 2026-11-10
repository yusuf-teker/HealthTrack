package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberExternalAppLauncher(): ExternalAppLauncher {
    return remember {
        object : ExternalAppLauncher {
            override fun isAppInstalled(app: ExternalApp): Boolean = false
            override fun openApp(app: ExternalApp): Boolean = false
            override fun shareToWhatsApp(text: String, phoneNumber: String?): Boolean = false
            override fun sendToPlanoraTodo(title: String, items: List<String>): Boolean = false
        }
    }
}
