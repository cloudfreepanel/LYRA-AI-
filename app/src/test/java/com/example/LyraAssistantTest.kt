package com.example

import com.example.engine.emotion.EmotionCue
import com.example.engine.emotion.EmotionEngine
import com.example.engine.intent.IntentClassifier
import com.example.engine.intent.IntentType
import com.example.engine.language.LanguageEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyraAssistantTest {

    @Test
    fun testIntentClassification_TorchCommands() {
        // English
        val englishOn = IntentClassifier.classify("Hey Lyra, turn on the torch")
        assertEquals(IntentType.TORCH_ON, englishOn.type)

        val englishOff = IntentClassifier.classify("Lyra, torch off")
        assertEquals(IntentType.TORCH_OFF, englishOff.type)

        // Hindi
        val hindiOn = IntentClassifier.classify("Hey Lyra, torch on karo")
        assertEquals(IntentType.TORCH_ON, hindiOn.type)

        val hindiOff = IntentClassifier.classify("torch band karo")
        assertEquals(IntentType.TORCH_OFF, hindiOff.type)

        // Bengali
        val bengaliOn = IntentClassifier.classify("Hey Lyra, টর্চ অন করো")
        assertEquals(IntentType.TORCH_ON, bengaliOn.type)

        val bengaliOff = IntentClassifier.classify("টর্চ বন্ধ করো")
        assertEquals(IntentType.TORCH_OFF, bengaliOff.type)

        // Assamese
        val assameseOn = IntentClassifier.classify("Lyra, টৰ্চ অন কৰা")
        assertEquals(IntentType.TORCH_ON, assameseOn.type)

        // Urdu
        val urduOn = IntentClassifier.classify("ارے لائرا، ٹارچ آن کرو")
        assertEquals(IntentType.TORCH_ON, urduOn.type)
    }

    @Test
    fun testIntentClassification_CallingCommands() {
        // Section 34 explicit test specification:
        // Input: "Hey Lyra, Rahul ko call karo." -> CALL_CONTACT, Parameter: Rahul
        val hindiCall = IntentClassifier.classify("Hey Lyra, Rahul ko call karo.")
        assertEquals(IntentType.CALL_CONTACT, hindiCall.type)
        assertEquals("Rahul", hindiCall.parameters["contactName"])

        val englishCall = IntentClassifier.classify("Lyra, call Mom")
        assertEquals(IntentType.CALL_CONTACT, englishCall.type)
        assertEquals("Mom", englishCall.parameters["contactName"])

        val bengaliCall = IntentClassifier.classify("Rahul-কে ফোন করো")
        assertEquals(IntentType.CALL_CONTACT, bengaliCall.type)
        assertEquals("Rahul", bengaliCall.parameters["contactName"])
    }

    @Test
    fun testIntentClassification_WhatsAppCommands() {
        val englishWhatsApp = IntentClassifier.classify("Hey Lyra, WhatsApp Rahul and say I'll call you later")
        assertEquals(IntentType.WHATSAPP_CONTACT, englishWhatsApp.type)
        assertEquals("Rahul", englishWhatsApp.parameters["contactName"])
        assertEquals("I'll call you later", englishWhatsApp.parameters["messageText"])

        val sendPattern = IntentClassifier.classify("Hey Lyra, send Jiya a WhatsApp message saying Hi, where are you?")
        assertEquals(IntentType.WHATSAPP_CONTACT, sendPattern.type)
        assertEquals("Jiya", sendPattern.parameters["contactName"])
        assertTrue(sendPattern.parameters["messageText"]?.contains("Hi") == true)
    }

    @Test
    fun testIntentClassification_DeviceFeatures() {
        val camera = IntentClassifier.classify("open camera")
        assertEquals(IntentType.CAMERA, camera.type)

        val battery = IntentClassifier.classify("Lyra, battery percentage?")
        assertEquals(IntentType.BATTERY_STATUS, battery.type)

        val timer = IntentClassifier.classify("set a timer for 10 minutes")
        assertEquals(IntentType.SET_TIMER, timer.type)
        assertEquals("600", timer.parameters["seconds"])

        val volume = IntentClassifier.classify("increase volume")
        assertEquals(IntentType.VOLUME_UP, volume.type)
    }

    @Test
    fun testLanguageDetection() {
        assertEquals("hi", LanguageEngine.detectLanguage("नमस्ते आप कैसे हैं"))
        assertEquals("bn", LanguageEngine.detectLanguage("কেমন আছেন আপনি"))
        assertEquals("as", LanguageEngine.detectLanguage("আপুনি কেনেকৈ কৰা আছে"))
        assertEquals("ur", LanguageEngine.detectLanguage("آپ کیسے ہیں"))
        assertEquals("en", LanguageEngine.detectLanguage("Hello how are you today"))
    }

    @Test
    fun testEmotionDetection() {
        assertEquals(EmotionCue.HAPPY, EmotionEngine.detectCue("I am so happy and thankful!"))
        assertEquals(EmotionCue.CONFUSED, EmotionEngine.detectCue("I don't understand, what do you mean?"))
        assertEquals(EmotionCue.SAD, EmotionEngine.detectCue("I'm feeling so sad and lonely today"))
        assertEquals(EmotionCue.EXCITED, EmotionEngine.detectCue("This is awesome and amazing!"))
        assertEquals(EmotionCue.ANGRY, EmotionEngine.detectCue("This is so annoying and irritating"))
    }
}
