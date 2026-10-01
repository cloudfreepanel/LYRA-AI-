package com.example.engine.ai

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class OfflineAiEngine(private val context: Context) {

    data class DeviceResources(
        val totalRamMb: Long,
        val availableRamMb: Long,
        val availableStorageMb: Long,
        val isLowRamDevice: Boolean
    )

    fun getDeviceResources(): DeviceResources {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memoryInfo)

        val stat = StatFs(Environment.getDataDirectory().path)
        val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
        val storageMb = bytesAvailable / (1024 * 1024)

        return DeviceResources(
            totalRamMb = memoryInfo.totalMem / (1024 * 1024),
            availableRamMb = memoryInfo.availMem / (1024 * 1024),
            availableStorageMb = storageMb,
            isLowRamDevice = memoryInfo.lowMemory || actManager.isLowRamDevice
        )
    }

    /**
     * Responds to conversational prompts using on-device pattern intelligence and local knowledge.
     */
    fun getOfflineResponse(prompt: String, languageCode: String = "en"): String {
        val lower = prompt.lowercase().trim()

        // Time / Date queries
        if (lower.contains("what time") || lower.contains("time is it") || lower.contains("samay kya") || lower.contains("কটা বাজে") || lower.contains("সময় কি")) {
            val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            return when (languageCode) {
                "hi" -> "अभी $time बज रहे हैं।"
                "bn" -> "এখন সময় $time।"
                "as" -> "এতিয়া সময় $time।"
                "ur" -> "اس وقت $time ہے۔"
                else -> "The current time is $time."
            }
        }

        if (lower.contains("what date") || lower.contains("today's date") || lower.contains("aaj ki tarikh") || lower.contains("আজকের তারিখ")) {
            val date = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
            return when (languageCode) {
                "hi" -> "आज $date है।"
                "bn" -> "আজকের তারিখ হলো $date।"
                "as" -> "আজিৰ তাৰিখ হ'ল $date।"
                "ur" -> "آج کی تاریخ $date ہے۔"
                else -> "Today is $date."
            }
        }

        // Identity queries
        if (lower.contains("who are you") || lower.contains("what is your name") || lower.contains("tum kaun ho") || lower.contains("apnar naam ki") || lower.contains("তুমি কে")) {
            return "I am LYRA, your intelligent female mobile AI companion. I can assist you with device control, phone calls, WhatsApp messages, reminders, voice commands, and questions both online and offline."
        }

        if (lower.contains("how are you") || lower.contains("kaise ho") || lower.contains("kemon acho") || lower.contains("কেমন আছো") || lower.contains("আপুনি কেনে আছে")) {
            return when (languageCode) {
                "hi" -> "मैं बिल्कुल ठीक हूँ! आपकी क्या मदद कर सकती हूँ?"
                "bn" -> "আমি খুব ভালো আছি! আপনাকে কীভাবে সাহায্য করতে পারি?"
                "as" -> "মই ভালে আছো! আপোনাক কেনেকৈ সহায় কৰিব পাৰোঁ?"
                "ur" -> "میں بالکل ٹھیک ہوں۔ میں آپ کی کیا مدد کر سکتی ہوں؟"
                else -> "I'm functioning at full capacity and ready to assist you! How can I help you today?"
            }
        }

        // Math calculations
        val mathResult = tryEvaluateSimpleMath(lower)
        if (mathResult != null) {
            return "The answer is $mathResult."
        }

        // Capabilities query
        if (lower.contains("what can you do") || lower.contains("help") || lower.contains("features") || lower.contains("kya kar sakti ho") || lower.contains("কি করতে পারো")) {
            return """Here is what I can do for you:
• 🔦 Torch control ("Turn on torch", "Torch off")
• 📞 Phone calling ("Call Rahul", "Dial 9876543210")
• 💬 WhatsApp ("WhatsApp Jiya saying Hello")
• 📷 Camera & Vision ("Open camera", analyze images)
• 🔊 Volume & brightness controls
• ⏰ Alarms & Timers ("Set timer for 10 minutes")
• 🔋 Battery status & system telemetry
• 🌐 Multilingual voice in English, Hindi, Bengali, Assamese, and Urdu
• 🛡️ Privacy Center & on-device memory management"""
        }

        // Greetings
        if (lower == "hello" || lower == "hi" || lower == "hey" || lower == "namaste" || lower == "nomoshkar" || lower == "সালাম") {
            return "Hello! I am LYRA. How can I help you right now?"
        }

        // Generic fallback for offline chat
        return "I am currently in Offline Mode. I can control your device settings, flashlight, volume, battery status, timers, phone calls, and contacts. Connect to the internet for deep AI discussions and image understanding."
    }

    private fun tryEvaluateSimpleMath(text: String): String? {
        val regex = Regex("(\\d+(?:\\.\\d+)?)\\s*([+\\-*/x])\\s*(\\d+(?:\\.\\d+)?)")
        val match = regex.find(text) ?: return null
        val num1 = match.groupValues[1].toDoubleOrNull() ?: return null
        val op = match.groupValues[2]
        val num2 = match.groupValues[3].toDoubleOrNull() ?: return null

        val res = when (op) {
            "+" -> num1 + num2
            "-" -> num1 - num2
            "*", "x" -> num1 * num2
            "/" -> if (num2 != 0.0) num1 / num2 else return "undefined (division by zero)"
            else -> return null
        }
        return if (res % 1.0 == 0.0) res.toLong().toString() else "%.2f".format(res)
    }
}
