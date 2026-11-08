package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberSpeechToTextLauncher(): SpeechToTextLauncher {
    return remember {
        object : SpeechToTextLauncher {
            override val isAvailable: Boolean = false
            override fun launch(prompt: String, onResult: (String) -> Unit) {}
        }
    }
}
