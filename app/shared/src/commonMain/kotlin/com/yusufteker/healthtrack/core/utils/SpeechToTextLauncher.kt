package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable

/**
 * Platform-agnostic speech-to-text contract for AI text fields.
 */
interface SpeechToTextLauncher {
    val isAvailable: Boolean
    fun launch(prompt: String = "Sesinizle söyleyin...", onResult: (String) -> Unit)
}

/**
 * Creates and remembers a [SpeechToTextLauncher] instance for the current platform.
 */
@Composable
expect fun rememberSpeechToTextLauncher(): SpeechToTextLauncher
