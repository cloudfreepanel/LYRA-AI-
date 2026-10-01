package com.example.engine.ai

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.model.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class OnlineAiEngine {

    companion object {
        const val SYSTEM_PROMPT = """You are LYRA, a futuristic female AI mobile companion.
Personality:
- Friendly, warm, natural, calm, intelligent, and respectful.
- Emotion-aware: respond with empathy when the user expresses feelings.
- Keep simple answers concise and clear; provide deep, well-structured explanations when asked.
- NEVER falsely claim an Android action was completed if you didn't execute it.
- You can communicate fluently in English, Hindi, Bengali, Assamese, and Urdu."""
    }

    fun hasApiKey(): Boolean {
        return BuildConfig.GEMINI_API_KEY.isNotEmpty() &&
                BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
    }

    suspend fun generateResponse(
        prompt: String,
        recentMessages: List<MessageEntity> = emptyList(),
        imageBitmap: Bitmap? = null,
        userPreferencesSummary: String = "",
        emotionPrefix: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!hasApiKey()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please add GEMINI_API_KEY in AI Studio Secrets panel.")
            )
        }

        try {
            val contents = mutableListOf<GeminiContent>()

            // Add previous recent messages for conversation context (up to 6 turns)
            val history = recentMessages.takeLast(6)
            for (msg in history) {
                val role = if (msg.role == "user") "user" else "model"
                contents.add(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = msg.content)),
                        role = role
                    )
                )
            }

            // Current turn parts
            val currentParts = mutableListOf<GeminiPart>()
            if (imageBitmap != null) {
                val base64Data = bitmapToBase64(imageBitmap)
                currentParts.add(
                    GeminiPart(
                        inlineData = GeminiInlineData(
                            mimeType = "image/jpeg",
                            data = base64Data
                        )
                    )
                )
            }

            val finalPromptText = buildString {
                if (!emotionPrefix.isNullOrBlank()) {
                    append(emotionPrefix)
                }
                append(prompt)
            }
            currentParts.add(GeminiPart(text = finalPromptText))

            contents.add(GeminiContent(parts = currentParts, role = "user"))

            val systemInstruction = GeminiContent(
                parts = listOf(
                    GeminiPart(
                        text = SYSTEM_PROMPT + if (userPreferencesSummary.isNotEmpty()) "\nUser Preferences: $userPreferencesSummary" else ""
                    )
                )
            )

            val request = GeminiRequest(
                contents = contents,
                systemInstruction = systemInstruction,
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.7f,
                    maxOutputTokens = 1024
                )
            )

            val response = GeminiClient.apiService.generateContent(apiKey, request)
            val responseText = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text

            if (responseText != null) {
                Result.success(responseText.trim())
            } else {
                Result.failure(Exception("LYRA received an empty response from server."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun streamResponse(
        prompt: String,
        recentMessages: List<MessageEntity> = emptyList(),
        imageBitmap: Bitmap? = null,
        userPreferencesSummary: String = ""
    ): Flow<String> = flow {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (!hasApiKey()) {
            emit("API key is not configured. Falling back to offline intelligence.")
            return@flow
        }

        val contents = mutableListOf<GeminiContent>()
        val history = recentMessages.takeLast(4)
        for (msg in history) {
            val role = if (msg.role == "user") "user" else "model"
            contents.add(
                GeminiContent(
                    parts = listOf(GeminiPart(text = msg.content)),
                    role = role
                )
            )
        }

        val currentParts = mutableListOf<GeminiPart>()
        if (imageBitmap != null) {
            currentParts.add(
                GeminiPart(
                    inlineData = GeminiInlineData(
                        mimeType = "image/jpeg",
                        data = bitmapToBase64(imageBitmap)
                    )
                )
            )
        }
        currentParts.add(GeminiPart(text = prompt))
        contents.add(GeminiContent(parts = currentParts, role = "user"))

        val request = GeminiRequest(
            contents = contents,
            systemInstruction = GeminiContent(
                parts = listOf(
                    GeminiPart(
                        text = SYSTEM_PROMPT + if (userPreferencesSummary.isNotEmpty()) "\nUser preferences: $userPreferencesSummary" else ""
                    )
                )
            ),
            generationConfig = GeminiGenerationConfig(temperature = 0.7f)
        )

        try {
            val body = GeminiClient.apiService.generateContentStream(apiKey, request)
            body.byteStream().bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val rawLine = line?.trim() ?: continue
                    if (!rawLine.startsWith("data:")) continue
                    val jsonPayload = rawLine.removePrefix("data:").trim()
                    if (jsonPayload.isEmpty() || jsonPayload == "[DONE]") continue

                    try {
                        val json = JSONObject(jsonPayload)
                        val candidates = json.optJSONArray("candidates")
                        val firstCand = candidates?.optJSONObject(0)
                        val content = firstCand?.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        val text = parts?.optJSONObject(0)?.optString("text")
                        if (!text.isNullOrEmpty()) {
                            emit(text)
                        }
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            emit(" [Connection interrupted: ${e.localizedMessage ?: "Network issue"}]")
        }
    }.flowOn(Dispatchers.IO)

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Resize if too large
        val maxDim = 1024
        val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val scale = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else {
            bitmap
        }
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
