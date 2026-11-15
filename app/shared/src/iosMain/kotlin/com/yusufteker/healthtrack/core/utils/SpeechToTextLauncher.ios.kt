package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryOptionDuckOthers
import platform.AVFAudio.AVAudioSessionCategoryRecord
import platform.AVFAudio.AVAudioSessionModeMeasurement
import platform.AVFAudio.setActive
import platform.Foundation.NSLocale
import platform.Foundation.NSTimer
import platform.Foundation.currentLocale
import platform.Speech.SFSpeechAudioBufferRecognitionRequest
import platform.Speech.SFSpeechRecognitionTask
import platform.Speech.SFSpeechRecognizer
import platform.Speech.SFSpeechRecognizerAuthorizationStatus
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@OptIn(ExperimentalForeignApi::class)
private class IosSpeechToTextLauncher : SpeechToTextLauncher {
    private val recognizer: SFSpeechRecognizer? =
        SFSpeechRecognizer(locale = NSLocale.currentLocale).takeIf { it.isAvailable() }
            ?: SFSpeechRecognizer()

    private var audioEngine: AVAudioEngine? = null
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest? = null
    private var recognitionTask: SFSpeechRecognitionTask? = null
    private var silenceTimer: NSTimer? = null
    private var isListening = false
    private var latestTranscript = ""
    private var pendingCallback: ((String) -> Unit)? = null

    override val isAvailable: Boolean
        get() = recognizer != null

    override fun launch(prompt: String, onResult: (String) -> Unit) {
        if (isListening) {
            stopAndDeliver()
            return
        }

        val currentRecognizer = recognizer ?: return
        SFSpeechRecognizer.requestAuthorization { authStatus ->
            dispatch_async(dispatch_get_main_queue()) {
                if (authStatus != SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized) {
                    return@dispatch_async
                }
                AVAudioSession.sharedInstance().requestRecordPermission { granted ->
                    dispatch_async(dispatch_get_main_queue()) {
                        if (granted) {
                            startRecording(currentRecognizer, onResult)
                        }
                    }
                }
            }
        }
    }

    private fun startRecording(
        speechRecognizer: SFSpeechRecognizer,
        onResult: (String) -> Unit
    ) {
        cleanupSession()
        latestTranscript = ""
        pendingCallback = onResult

        val session = AVAudioSession.sharedInstance()
        try {
            session.setCategory(
                AVAudioSessionCategoryRecord,
                mode = AVAudioSessionModeMeasurement,
                options = AVAudioSessionCategoryOptionDuckOthers,
                error = null
            )
            session.setActive(true, error = null)
        } catch (_: Throwable) {
            cleanupSession()
            return
        }

        val request = SFSpeechAudioBufferRecognitionRequest().apply {
            shouldReportPartialResults = true
        }
        val engine = AVAudioEngine()
        val inputNode = engine.inputNode
        val recordingFormat = inputNode.outputFormatForBus(0u)

        inputNode.installTapOnBus(
            bus = 0u,
            bufferSize = 1024u,
            format = recordingFormat
        ) { buffer, _ ->
            if (buffer != null) {
                request.appendAudioPCMBuffer(buffer)
            }
        }

        engine.prepare()
        val started = try {
            engine.startAndReturnError(null)
        } catch (_: Throwable) {
            false
        }
        if (!started) {
            inputNode.removeTapOnBus(0u)
            cleanupSession()
            return
        }

        audioEngine = engine
        recognitionRequest = request
        isListening = true

        // Auto-stop after 8 seconds of max capture or 2.2s after last partial speech update
        resetSilenceTimer(timeoutSeconds = 8.0)

        recognitionTask = speechRecognizer.recognitionTaskWithRequest(request) { result, error ->
            dispatch_async(dispatch_get_main_queue()) {
                if (result != null) {
                    val text = result.bestTranscription.formattedString.trim()
                    if (text.isNotEmpty()) {
                        latestTranscript = text
                        resetSilenceTimer(timeoutSeconds = 2.0)
                    }
                    if (result.isFinal()) {
                        stopAndDeliver()
                        return@dispatch_async
                    }
                }
                if (error != null && isListening) {
                    stopAndDeliver()
                }
            }
        }
    }

    private fun resetSilenceTimer(timeoutSeconds: Double) {
        silenceTimer?.invalidate()
        silenceTimer = NSTimer.scheduledTimerWithTimeInterval(
            interval = timeoutSeconds,
            repeats = false
        ) { _ ->
            if (isListening) {
                stopAndDeliver()
            }
        }
    }

    private fun stopAndDeliver() {
        val text = latestTranscript.trim()
        val callback = pendingCallback
        cleanupSession()
        if (text.isNotEmpty()) {
            callback?.invoke(text)
        }
    }

    fun cleanupSession() {
        isListening = false
        silenceTimer?.invalidate()
        silenceTimer = null
        audioEngine?.let { engine ->
            if (engine.isRunning()) {
                engine.stop()
            }
            engine.inputNode.removeTapOnBus(0u)
        }
        recognitionRequest?.endAudio()
        recognitionTask?.cancel()
        audioEngine = null
        recognitionRequest = null
        recognitionTask = null
        pendingCallback = null
        try {
            AVAudioSession.sharedInstance().setActive(false, error = null)
        } catch (_: Throwable) {
        }
    }
}

@Composable
actual fun rememberSpeechToTextLauncher(): SpeechToTextLauncher {
    val launcher = remember { IosSpeechToTextLauncher() }
    DisposableEffect(launcher) {
        onDispose {
            launcher.cleanupSession()
        }
    }
    return launcher
}
