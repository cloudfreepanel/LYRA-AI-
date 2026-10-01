package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lyra_preferences")

data class AppSettings(
    val onlineAiEnabled: Boolean = true,
    val offlineAiEnabled: Boolean = true,
    val modelName: String = "gemini-3.5-flash",
    val responseLength: String = "Balanced", // Concise, Balanced, Detailed
    val voiceType: String = "Female 1", // Female 1, Female 2, System Voice
    val languageCode: String = "auto", // auto, en, hi, bn, as, ur
    val autoLanguageDetection: Boolean = true,
    val speechSpeed: Float = 1.0f,
    val speechPitch: Float = 1.1f, // default slightly higher for clear female AI presence
    val wakeWordEnabled: Boolean = true,
    val emotionAwareEnabled: Boolean = true,
    val confirmationLevel: String = "SENSITIVE", // ALWAYS, SENSITIVE, SAFE_ONLY
    val memoryEnabled: Boolean = true,
    val darkTheme: Boolean = true,
    val neonIntensity: Float = 1.0f
)

class UserPreferences(private val context: Context) {
    companion object {
        val KEY_ONLINE_AI = booleanPreferencesKey("online_ai_enabled")
        val KEY_OFFLINE_AI = booleanPreferencesKey("offline_ai_enabled")
        val KEY_MODEL_NAME = stringPreferencesKey("model_name")
        val KEY_RESPONSE_LENGTH = stringPreferencesKey("response_length")
        val KEY_VOICE_TYPE = stringPreferencesKey("voice_type")
        val KEY_LANGUAGE_CODE = stringPreferencesKey("language_code")
        val KEY_AUTO_LANG = booleanPreferencesKey("auto_lang_detection")
        val KEY_SPEECH_SPEED = floatPreferencesKey("speech_speed")
        val KEY_SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        val KEY_WAKE_WORD = booleanPreferencesKey("wake_word_enabled")
        val KEY_EMOTION_AWARE = booleanPreferencesKey("emotion_aware_enabled")
        val KEY_CONFIRMATION_LEVEL = stringPreferencesKey("confirmation_level")
        val KEY_MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
        val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        val KEY_NEON_INTENSITY = floatPreferencesKey("neon_intensity")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { pref ->
        AppSettings(
            onlineAiEnabled = pref[KEY_ONLINE_AI] ?: true,
            offlineAiEnabled = pref[KEY_OFFLINE_AI] ?: true,
            modelName = pref[KEY_MODEL_NAME] ?: "gemini-3.5-flash",
            responseLength = pref[KEY_RESPONSE_LENGTH] ?: "Balanced",
            voiceType = pref[KEY_VOICE_TYPE] ?: "Female 1",
            languageCode = pref[KEY_LANGUAGE_CODE] ?: "auto",
            autoLanguageDetection = pref[KEY_AUTO_LANG] ?: true,
            speechSpeed = pref[KEY_SPEECH_SPEED] ?: 1.0f,
            speechPitch = pref[KEY_SPEECH_PITCH] ?: 1.1f,
            wakeWordEnabled = pref[KEY_WAKE_WORD] ?: true,
            emotionAwareEnabled = pref[KEY_EMOTION_AWARE] ?: true,
            confirmationLevel = pref[KEY_CONFIRMATION_LEVEL] ?: "SENSITIVE",
            memoryEnabled = pref[KEY_MEMORY_ENABLED] ?: true,
            darkTheme = pref[KEY_DARK_THEME] ?: true,
            neonIntensity = pref[KEY_NEON_INTENSITY] ?: 1.0f
        )
    }

    suspend fun updateOnlineAi(enabled: Boolean) = context.dataStore.edit { it[KEY_ONLINE_AI] = enabled }
    suspend fun updateOfflineAi(enabled: Boolean) = context.dataStore.edit { it[KEY_OFFLINE_AI] = enabled }
    suspend fun updateModelName(model: String) = context.dataStore.edit { it[KEY_MODEL_NAME] = model }
    suspend fun updateResponseLength(length: String) = context.dataStore.edit { it[KEY_RESPONSE_LENGTH] = length }
    suspend fun updateVoiceType(type: String) = context.dataStore.edit { it[KEY_VOICE_TYPE] = type }
    suspend fun updateLanguage(code: String) = context.dataStore.edit { it[KEY_LANGUAGE_CODE] = code }
    suspend fun updateAutoLanguageDetection(enabled: Boolean) = context.dataStore.edit { it[KEY_AUTO_LANG] = enabled }
    suspend fun updateSpeechSpeed(speed: Float) = context.dataStore.edit { it[KEY_SPEECH_SPEED] = speed }
    suspend fun updateSpeechPitch(pitch: Float) = context.dataStore.edit { it[KEY_SPEECH_PITCH] = pitch }
    suspend fun updateWakeWord(enabled: Boolean) = context.dataStore.edit { it[KEY_WAKE_WORD] = enabled }
    suspend fun updateEmotionAware(enabled: Boolean) = context.dataStore.edit { it[KEY_EMOTION_AWARE] = enabled }
    suspend fun updateConfirmationLevel(level: String) = context.dataStore.edit { it[KEY_CONFIRMATION_LEVEL] = level }
    suspend fun updateMemoryEnabled(enabled: Boolean) = context.dataStore.edit { it[KEY_MEMORY_ENABLED] = enabled }
    suspend fun updateDarkTheme(dark: Boolean) = context.dataStore.edit { it[KEY_DARK_THEME] = dark }
    suspend fun updateNeonIntensity(intensity: Float) = context.dataStore.edit { it[KEY_NEON_INTENSITY] = intensity }
}
