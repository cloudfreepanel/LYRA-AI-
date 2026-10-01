package com.example.engine.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TextToSpeechEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingSpeechText: String? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var currentPitch: Float = 1.1f
    private var currentSpeed: Float = 1.0f
    private var currentVoiceType: String = "Female 1"
    private var currentLanguageCode: String = "en"

    init {
        mainHandler.post {
            try {
                tts = TextToSpeech(context.applicationContext, this)
            } catch (e: Exception) {
                isInitialized = false
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.language = Locale.US
            configureVoice()
            setupProgressListener()

            // Play any pending speech queued before initialization completed
            pendingSpeechText?.let { text ->
                pendingSpeechText = null
                speak(text)
            }
        } else {
            isInitialized = false
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
            }

            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
            }
        })
    }

    private fun configureVoice() {
        val engine = tts ?: return
        engine.setPitch(currentPitch)
        engine.setSpeechRate(currentSpeed)

        try {
            val locale = when (currentLanguageCode) {
                "hi" -> Locale.forLanguageTag("hi-IN")
                "bn" -> Locale.forLanguageTag("bn-IN")
                "as" -> Locale.forLanguageTag("as-IN")
                "ur" -> Locale.forLanguageTag("ur-PK")
                else -> Locale.US
            }
            engine.language = locale

            // Attempt to select a female voice profile
            val voices = engine.voices
            if (!voices.isNullOrEmpty()) {
                val femaleVoice = voices.firstOrNull { voice ->
                    val name = voice.name.lowercase()
                    (name.contains("female") || name.contains("woman") || name.contains("f0") || name.contains("#female")) &&
                            !voice.isNetworkConnectionRequired
                } ?: voices.firstOrNull { !it.isNetworkConnectionRequired }

                if (femaleVoice != null && currentVoiceType != "System Voice") {
                    engine.voice = femaleVoice
                }
            }
        } catch (_: Exception) {
            // Fallback to default engine configuration
        }
    }

    fun setSpeechParameters(pitch: Float, speed: Float, voiceType: String, languageCode: String = "en") {
        currentPitch = pitch
        currentSpeed = speed
        currentVoiceType = voiceType
        currentLanguageCode = languageCode

        if (isInitialized) {
            configureVoice()
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        val cleanedText = text
            .replace(Regex("[#*`_\\[\\]()]"), " ")
            .replace(Regex("https?://\\S+"), "link")
            .trim()

        if (cleanedText.isEmpty()) {
            onDone?.invoke()
            return
        }

        if (!isInitialized || tts == null) {
            pendingSpeechText = cleanedText
            return
        }

        mainHandler.post {
            try {
                stop()
                val utteranceId = "lyra_${System.currentTimeMillis()}"
                tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            } catch (e: Exception) {
                _isSpeaking.value = false
                onDone?.invoke()
            }
        }
    }

    fun stop() {
        mainHandler.post {
            try {
                if (isInitialized) {
                    tts?.stop()
                }
            } catch (_: Exception) {}
            _isSpeaking.value = false
        }
    }

    fun shutdown() {
        stop()
        mainHandler.post {
            try {
                tts?.shutdown()
            } catch (_: Exception) {}
            tts = null
            isInitialized = false
        }
    }
}
