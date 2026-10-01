package com.example.engine.emotion

enum class EmotionCue(val label: String, val emoji: String) {
    HAPPY("Happy", "😄"),
    SAD("Sad", "🫂"),
    ANGRY("Frustrated", "🕊️"),
    WORRIED("Concerned", "🌱"),
    EXCITED("Excited", "✨"),
    CONFUSED("Puzzled", "💡"),
    TIRED("Exhausted", "🌙"),
    NEUTRAL("Neutral", "✦")
}

object EmotionEngine {
    /**
     * Detects conversational emotional cues from text based on sentiment indicators.
     * Disclaimer: This is an estimated conversational style cue, not a medical or clinical diagnosis.
     */
    fun detectCue(text: String): EmotionCue {
        val lower = text.lowercase()

        // Confused cues
        if (lower.contains("don't understand") || lower.contains("confused") || lower.contains("what do you mean") ||
            lower.contains("samajh nahi") || lower.contains("bujhte parchhina") || lower.contains("bujhi nai") ||
            lower.contains("kya bol rahe ho") || lower.contains("how does") || lower.contains("explain again")
        ) {
            return EmotionCue.CONFUSED
        }

        // Angry / Frustrated cues
        if (lower.contains("angry") || lower.contains("stupid") || lower.contains("annoying") || lower.contains("hate") ||
            lower.contains("gussa") || lower.contains("bekaar") || lower.contains("khub rag") || lower.contains("faltu") ||
            lower.contains("shut up") || lower.contains("irritating")
        ) {
            return EmotionCue.ANGRY
        }

        // Sad / Down cues
        if (lower.contains("sad") || lower.contains("depressed") || lower.contains("crying") || lower.contains("lonely") ||
            lower.contains("hurt") || lower.contains("udas") || lower.contains("dukhi") || lower.contains("mon kharap") ||
            lower.contains("heartbroken") || lower.contains("unhappy")
        ) {
            return EmotionCue.SAD
        }

        // Worried / Anxious cues
        if (lower.contains("worried") || lower.contains("scared") || lower.contains("nervous") || lower.contains("stress") ||
            lower.contains("chinta") || lower.contains("dar lag raha") || lower.contains("bhoy lagche") || lower.contains("anxious")
        ) {
            return EmotionCue.WORRIED
        }

        // Excited cues
        if (lower.contains("amazing") || lower.contains("wow") || lower.contains("can't wait") || lower.contains("yay") ||
            lower.contains("awesome") || lower.contains("zabardast") || lower.contains("osadharon") || lower.contains("darun") ||
            lower.contains("hurray") || lower.contains("thrilled")
        ) {
            return EmotionCue.EXCITED
        }

        // Happy cues
        if (lower.contains("happy") || lower.contains("great") || lower.contains("good") || lower.contains("love it") ||
            lower.contains("khush") || lower.contains("bhalo") || lower.contains("thanks") || lower.contains("thank you") ||
            lower.contains("dhanyawad") || lower.contains("shukriya")
        ) {
            return EmotionCue.HAPPY
        }

        // Tired cues
        if (lower.contains("tired") || lower.contains("sleepy") || lower.contains("exhausted") || lower.contains("thak gaya") ||
            lower.contains("klanto") || lower.contains("need rest") || lower.contains("sleeping")
        ) {
            return EmotionCue.TIRED
        }

        return EmotionCue.NEUTRAL
    }

    /**
     * Adapts response prefix or empathy tone based on detected cue.
     */
    fun getAdaptiveTonePrefix(cue: EmotionCue, isMultilingual: Boolean = false): String? {
        return when (cue) {
            EmotionCue.HAPPY -> "Awesome! 😄 "
            EmotionCue.EXCITED -> "That sounds fantastic! ✨ "
            EmotionCue.SAD -> "I’m right here with you. 🫂 "
            EmotionCue.ANGRY -> "I understand your frustration. Let's take it step by step calmly. "
            EmotionCue.WORRIED -> "Take a breath, I'm here to help you work through this. 🌱 "
            EmotionCue.CONFUSED -> "No problem! Let me explain this step by step. 💡 "
            EmotionCue.TIRED -> "Rest is important. Let's keep things easy. 🌙 "
            EmotionCue.NEUTRAL -> null
        }
    }
}
