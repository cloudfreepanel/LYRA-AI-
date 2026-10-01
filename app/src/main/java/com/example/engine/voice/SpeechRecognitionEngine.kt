package com.example.engine.voice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

class SpeechRecognitionEngine(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main)
    private var simulatedRmsJob: Job? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _recognizedText = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val recognizedText: SharedFlow<String> = _recognizedText.asSharedFlow()

    private val _requestSystemDialog = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
    val requestSystemDialog: SharedFlow<Intent> = _requestSystemDialog.asSharedFlow()

    private val _errorMessage = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun createRecognizeIntent(languageCode: String): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

            val tag = when (languageCode) {
                "hi" -> "hi-IN"
                "bn" -> "bn-IN"
                "as" -> "as-IN"
                "ur" -> "ur-PK"
                else -> Locale.getDefault().toLanguageTag()
            }
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to LYRA AI...")
        }
    }

    fun startListening(languageCode: String = "en-US") {
        mainHandler.post {
            stopListening()

            val hasRecognitionService = isAvailable()

            if (!hasRecognitionService) {
                // Device or emulator does not have an in-process recognition service
                // Launch the system voice intent as direct fallback
                val intent = createRecognizeIntent(languageCode)
                _requestSystemDialog.tryEmit(intent)
                _errorMessage.tryEmit("Using system voice input dialog...")
                return@post
            }

            try {
                // Prefer on-device speech recognizer if supported on Android 12+ (API 31+)
                val recognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                } else {
                    SpeechRecognizer.createSpeechRecognizer(context)
                }

                speechRecognizer = recognizer

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        _partialText.value = ""
                        startSimulatedRmsLoop()
                    }

                    override fun onBeginningOfSpeech() {
                        _isListening.value = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        _rmsLevel.value = maxOf(_rmsLevel.value, normalized)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                        stopSimulatedRmsLoop()
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        stopSimulatedRmsLoop()

                        // If client error or service unavailable in emulator, trigger system voice dialog fallback
                        if (error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_SERVER) {
                            val intent = createRecognizeIntent(languageCode)
                            _requestSystemDialog.tryEmit(intent)
                            return
                        }

                        val msg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Tap mic to try again."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out."
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording issue."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network error during speech recognition."
                            else -> "Could not hear clearly. Tap mic to retry."
                        }
                        _errorMessage.tryEmit(msg)
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        stopSimulatedRmsLoop()
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val text = matches[0]
                            _partialText.value = ""
                            _recognizedText.tryEmit(text)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            _partialText.value = matches[0]
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = createRecognizeIntent(languageCode)
                recognizer.startListening(intent)
                _isListening.value = true
                startSimulatedRmsLoop()
            } catch (e: Exception) {
                _isListening.value = false
                stopSimulatedRmsLoop()
                // Fallback to system voice dialog
                val fallbackIntent = createRecognizeIntent(languageCode)
                _requestSystemDialog.tryEmit(fallbackIntent)
            }
        }
    }

    private fun startSimulatedRmsLoop() {
        simulatedRmsJob?.cancel()
        simulatedRmsJob = scope.launch {
            while (isActive && _isListening.value) {
                // Subtle organic pulsation while waiting for words
                val base = 0.15f + (Random.nextFloat() * 0.35f)
                _rmsLevel.value = base
                delay(120)
            }
            _rmsLevel.value = 0f
        }
    }

    private fun stopSimulatedRmsLoop() {
        simulatedRmsJob?.cancel()
        simulatedRmsJob = null
        _rmsLevel.value = 0f
    }

    fun onExternalSpeechResult(text: String) {
        _isListening.value = false
        stopSimulatedRmsLoop()
        if (text.isNotBlank()) {
            _partialText.value = ""
            _recognizedText.tryEmit(text.trim())
        }
    }

    fun stopListening() {
        mainHandler.post {
            stopSimulatedRmsLoop()
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
            _isListening.value = false
            _rmsLevel.value = 0f
        }
    }
}
