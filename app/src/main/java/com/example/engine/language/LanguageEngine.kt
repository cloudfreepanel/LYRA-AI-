package com.example.engine.language

data class SupportedLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val wakePhrase: String
)

object LanguageEngine {
    val SUPPORTED_LANGUAGES = listOf(
        SupportedLanguage("en", "English", "English", "Hey Lyra"),
        SupportedLanguage("hi", "Hindi", "हिन्दी", "हे लायरा"),
        SupportedLanguage("bn", "Bengali", "বাংলা", "হেই লায়রা"),
        SupportedLanguage("as", "Assamese", "অসমীয়া", "হেই লায়ৰা"),
        SupportedLanguage("ur", "Urdu", "اردو", "ارے لائرا")
    )

    /**
     * Automatic language detection based on Unicode script ranges and characteristic words.
     */
    fun detectLanguage(text: String): String {
        var devanagariCount = 0
        var bengaliAssameseCount = 0
        var arabicUrduCount = 0
        var latinCount = 0

        for (ch in text) {
            val code = ch.code
            when {
                code in 0x0900..0x097F -> devanagariCount++ // Hindi
                code in 0x0980..0x09FF -> bengaliAssameseCount++ // Bengali & Assamese
                code in 0x0600..0x06FF || code in 0x0750..0x077F || code in 0xFB50..0xFDFF || code in 0xFE70..0xFEFF -> arabicUrduCount++ // Urdu
                code in 0x0041..0x005A || code in 0x0061..0x007A -> latinCount++
            }
        }

        if (arabicUrduCount > 2) return "ur"
        if (devanagariCount > 2) return "hi"

        // Distinguish Assamese (অসমীয়া) from Bengali (বাংলা) by characteristic letters: ৰ (0x09F0) and ৱ (0x09F1)
        if (bengaliAssameseCount > 0) {
            if (text.contains("ৰ") || text.contains("ৱ") || text.contains("কৰা") || text.contains("কৰি")) {
                return "as"
            }
            return "bn"
        }

        // Transliterated or Latin text detection
        val lower = text.lowercase()
        if (lower.contains("karo") || lower.contains("kijiye") || lower.contains("hai") || lower.contains("bhejo") || lower.contains("batao")) {
            return "hi"
        }
        if (lower.contains("koro") || lower.contains("korun") || lower.contains("aache") || lower.contains("pathao") || lower.contains("bolo")) {
            return "bn"
        }

        return "en"
    }

    fun getLanguageByCode(code: String): SupportedLanguage {
        return SUPPORTED_LANGUAGES.find { it.code == code } ?: SUPPORTED_LANGUAGES[0]
    }
}
