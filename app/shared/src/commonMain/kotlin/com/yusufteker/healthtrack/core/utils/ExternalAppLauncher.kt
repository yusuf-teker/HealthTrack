package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable

enum class ExternalApp(
    val packageName: String,
    val scheme: String,
    val appName: String
) {
    PLANORA(
        packageName = "com.yusufteker.planora",
        scheme = "planora://",
        appName = "Planora"
    ),
    HEALTH_TRACK(
        packageName = "com.yusufteker.healthtrack",
        scheme = "healthtrack://",
        appName = "HealthTrack"
    ),
    WHATSAPP(
        packageName = "com.whatsapp",
        scheme = "whatsapp://",
        appName = "WhatsApp"
    )
}

interface ExternalAppLauncher {
    /**
     * Checks whether the target application is installed on the device.
     */
    fun isAppInstalled(app: ExternalApp): Boolean

    /**
     * Launches the target application if installed.
     * Returns true if successfully launched, false otherwise.
     */
    fun openApp(app: ExternalApp): Boolean

    /**
     * Sends a formatted report text via WhatsApp (to a specific [phoneNumber] if provided, or contact chooser)
     * with fallback to the system share sheet or wa.me link.
     */
    fun shareToWhatsApp(text: String, phoneNumber: String? = null): Boolean
}

/**
 * Creates and remembers an [ExternalAppLauncher] instance for the current platform.
 */
@Composable
expect fun rememberExternalAppLauncher(): ExternalAppLauncher
