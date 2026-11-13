package com.yusufteker.healthtrack.core.utils

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.yusufteker.healthtrack.core.i18n.LocalizationManager

@Composable
actual fun rememberSpeechToTextLauncher(): SpeechToTextLauncher {
    val context = LocalContext.current
    var onSpeechResultCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenTextList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenTextList?.firstOrNull()?.trim()
            if (!spokenText.isNullOrBlank()) {
                onSpeechResultCallback?.invoke(spokenText)
            }
        }
    }

    return remember(launcher, context) {
        object : SpeechToTextLauncher {
            override val isAvailable: Boolean
                get() {
                    return try {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                        intent.resolveActivity(context.packageManager) != null
                    } catch (_: Exception) {
                        false
                    }
                }

            override fun launch(prompt: String, onResult: (String) -> Unit) {
                onSpeechResultCallback = onResult
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, LocalizationManager.currentLanguage.bcp47Tag)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
                }
                try {
                    launcher.launch(intent)
                } catch (_: Exception) {
                    // Fallback if launcher cannot open
                }
            }
        }
    }
}
