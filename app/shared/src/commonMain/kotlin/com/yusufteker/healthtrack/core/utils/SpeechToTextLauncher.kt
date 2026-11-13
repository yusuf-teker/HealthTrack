package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import com.yusufteker.healthtrack.core.i18n.trEn

/**
 * Platform-agnostic speech-to-text contract for AI text fields.
 */
interface SpeechToTextLauncher {
    val isAvailable: Boolean
    fun launch(prompt: String = trEn("Sesinizle söyleyin...", "Speak now..."), onResult: (String) -> Unit)
}

/**
 * Creates and remembers a [SpeechToTextLauncher] instance for the current platform.
 */
@Composable
expect fun rememberSpeechToTextLauncher(): SpeechToTextLauncher
