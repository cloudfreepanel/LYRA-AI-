package com.example.engine.voice

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class WakeWordEngine {

    private val _wakeWordTriggered = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val wakeWordTriggered: SharedFlow<String> = _wakeWordTriggered.asSharedFlow()

    private val wakePhrases = listOf(
        "hey lyra", "ok lyra", "lyra", "hi lyra",
        "हे लायरा", "हाय लायरा",
        "হেই লায়রা", "হে লায়রা",
        "হেই লায়ৰা", "হে লায়ৰা",
        "ارے لائرا", "ہائے لائرا"
    )

    /**
     * Inspects a recognized audio phrase or hotword buffer for wake phrases.
     */
    fun checkWakeWord(input: String): Boolean {
        val lower = input.lowercase().trim()
        val match = wakePhrases.any { phrase ->
            lower.startsWith(phrase) || lower.contains(phrase)
        }
        if (match) {
            _wakeWordTriggered.tryEmit(input)
        }
        return match
    }

    /**
     * Extracts the query following the wake phrase.
     */
    fun extractQueryAfterWakeWord(input: String): String {
        var result = input.trim()
        for (phrase in wakePhrases) {
            val idx = result.lowercase().indexOf(phrase)
            if (idx >= 0) {
                result = result.substring(idx + phrase.length).trim()
                if (result.startsWith(",")) {
                    result = result.substring(1).trim()
                }
            }
        }
        return result
    }
}
