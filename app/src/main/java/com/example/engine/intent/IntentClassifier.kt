package com.example.engine.intent

import android.Manifest
import java.util.regex.Pattern

object IntentClassifier {

    /**
     * Strips wake words and greeting prefixes like "Hey Lyra", "Lyra", "হেই লায়রা", etc.
     */
    fun cleanQuery(raw: String): String {
        var clean = raw.trim()
        val wakePrefixes = listOf(
            "hey lyra,", "hey lyra", "ok lyra,", "ok lyra", "lyra,", "lyra",
            "हे लायरा,", "हे लायरा", "लायरा,", "लायरा",
            "হেই লায়রা,", "হেই লায়রা", "লায়রা,", "লায়রা",
            "হেই লায়ৰা,", "হেই লায়ৰা", "লায়ৰা,", "লায়ৰা",
            "ارے لائرا,", "ارے لائرا", "لائرا,", "لائرا",
            "please", "can you", "could you"
        )
        for (prefix in wakePrefixes) {
            if (clean.lowercase().startsWith(prefix.lowercase())) {
                clean = clean.substring(prefix.length).trim().removePrefix(",").trim()
            }
        }
        return clean
    }

    /**
     * Classifies user input into normalized LyraIntent.
     */
    fun classify(raw: String): LyraIntent {
        val query = cleanQuery(raw)
        val lower = query.lowercase()

        // 1. Torch ON
        if (matchesTorchOn(lower)) {
            return LyraIntent(
                type = IntentType.TORCH_ON,
                rawQuery = raw,
                confirmationRequired = ConfirmationRequirement.NONE,
                description = "Turn on device flashlight"
            )
        }

        // 2. Torch OFF
        if (matchesTorchOff(lower)) {
            return LyraIntent(
                type = IntentType.TORCH_OFF,
                rawQuery = raw,
                confirmationRequired = ConfirmationRequirement.NONE,
                description = "Turn off device flashlight"
            )
        }

        // 3. Battery Status
        if (matchesBattery(lower)) {
            return LyraIntent(
                type = IntentType.BATTERY_STATUS,
                rawQuery = raw,
                confirmationRequired = ConfirmationRequirement.NONE,
                description = "Check battery level and charging state"
            )
        }

        // 4. WhatsApp Message
        val whatsappIntent = parseWhatsAppIntent(query, lower)
        if (whatsappIntent != null) {
            return whatsappIntent
        }

        // 5. Call Contact
        val callIntent = parseCallIntent(query, lower)
        if (callIntent != null) {
            return callIntent
        }

        // 6. Camera
        if (matchesCamera(lower)) {
            return LyraIntent(
                type = IntentType.CAMERA,
                rawQuery = raw,
                confirmationRequired = ConfirmationRequirement.NONE,
                description = "Open device camera"
            )
        }

        // 7. Settings
        val settingsIntent = parseSettingsIntent(raw, lower)
        if (settingsIntent != null) {
            return settingsIntent
        }

        // 8. Open Apps
        val openAppIntent = parseOpenAppIntent(raw, lower)
        if (openAppIntent != null) {
            return openAppIntent
        }

        // 9. Volume Controls
        if (lower.contains("volume up") || lower.contains("increase volume") || lower.contains("awaz badhao") || lower.contains("awaj barao") || lower.contains("আওয়াজ বাড়াও")) {
            return LyraIntent(type = IntentType.VOLUME_UP, rawQuery = raw, description = "Increase volume")
        }
        if (lower.contains("volume down") || lower.contains("decrease volume") || lower.contains("awaz kam karo") || lower.contains("awaj komao") || lower.contains("আওয়াজ কমাও")) {
            return LyraIntent(type = IntentType.VOLUME_DOWN, rawQuery = raw, description = "Decrease volume")
        }
        if (lower.contains("mute") || lower.contains("mute media") || lower.contains("silent") || lower.contains("chup karo")) {
            return LyraIntent(type = IntentType.VOLUME_MUTE, rawQuery = raw, description = "Mute media volume")
        }

        // 10. Brightness
        val brightnessPattern = Pattern.compile("(?:brightness|ব্রাইটনেস)\\s+(?:to\\s+)?(\\d{1,3})\\s*%?")
        val brightnessMatcher = brightnessPattern.matcher(lower)
        if (brightnessMatcher.find()) {
            val percent = brightnessMatcher.group(1) ?: "50"
            return LyraIntent(
                type = IntentType.BRIGHTNESS_SET,
                rawQuery = raw,
                parameters = mapOf("percent" to percent),
                description = "Set screen brightness to $percent%"
            )
        }

        // 11. Media controls
        if (lower == "play" || lower.contains("play music") || lower.contains("gana bajao") || lower.contains("গান বাজাও")) {
            return LyraIntent(type = IntentType.MEDIA_PLAY, rawQuery = raw, description = "Resume media playback")
        }
        if (lower == "pause" || lower.contains("pause music") || lower.contains("gana roko") || lower.contains("গান থামাও")) {
            return LyraIntent(type = IntentType.MEDIA_PAUSE, rawQuery = raw, description = "Pause media playback")
        }
        if (lower.contains("next song") || lower.contains("agla gana") || lower.contains("পরের গান")) {
            return LyraIntent(type = IntentType.MEDIA_NEXT, rawQuery = raw, description = "Skip to next track")
        }
        if (lower.contains("previous song") || lower.contains("pichhla gana") || lower.contains("আগের গান")) {
            return LyraIntent(type = IntentType.MEDIA_PREVIOUS, rawQuery = raw, description = "Skip to previous track")
        }

        // 12. Timer
        val timerPattern = Pattern.compile("(?:set\\s+(?:a\\s+)?timer|timer|টাইমার)\\s+(?:for\\s+)?(\\d+)\\s*(minute|minutes|min|second|seconds|sec|মিনিট|সেকেন্ড)?", Pattern.CASE_INSENSITIVE)
        val timerMatcher = timerPattern.matcher(query)
        if (timerMatcher.find()) {
            val amount = timerMatcher.group(1) ?: "5"
            val unit = timerMatcher.group(2)?.lowercase() ?: "minute"
            val seconds = if (unit.startsWith("sec") || unit.contains("সেকেন্ড")) {
                amount.toIntOrNull() ?: 60
            } else {
                (amount.toIntOrNull() ?: 5) * 60
            }
            return LyraIntent(
                type = IntentType.SET_TIMER,
                rawQuery = raw,
                parameters = mapOf("seconds" to seconds.toString(), "label" to "Lyra Timer ($amount $unit)"),
                description = "Set a timer for $amount $unit"
            )
        }

        // 13. Alarm
        val alarmPattern = Pattern.compile("(?:set\\s+(?:an\\s+)?alarm|alarm|অ্যালার্ম)\\s+(?:for\\s+|at\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", Pattern.CASE_INSENSITIVE)
        val alarmMatcher = alarmPattern.matcher(query)
        if (alarmMatcher.find()) {
            val hour = alarmMatcher.group(1) ?: "7"
            val minute = alarmMatcher.group(2) ?: "0"
            val amPm = alarmMatcher.group(3) ?: ""
            return LyraIntent(
                type = IntentType.SET_ALARM,
                rawQuery = raw,
                parameters = mapOf("hour" to hour, "minute" to minute, "amPm" to amPm),
                description = "Set an alarm for $hour:$minute $amPm"
            )
        }

        // 14. Device status
        if (lower.contains("device status") || lower.contains("phone status") || lower.contains("system status")) {
            return LyraIntent(type = IntentType.DEVICE_STATUS, rawQuery = raw, description = "Get device hardware summary")
        }

        // Default to General Chat
        return LyraIntent(
            type = IntentType.GENERAL_CHAT,
            rawQuery = raw,
            description = "Natural conversation query"
        )
    }

    private fun matchesTorchOn(lower: String): Boolean {
        return lower.contains("torch on") || lower.contains("turn on torch") || lower.contains("turn on the torch") ||
                lower.contains("flashlight on") || lower.contains("turn on flashlight") ||
                lower.contains("torch jalao") || lower.contains("torch chalu karo") ||
                lower.contains("টর্চ অন করো") || lower.contains("টর্চ জ্বালাও") || lower.contains("টর্চ চালু করো") ||
                lower.contains("টৰ্চ অন কৰা") || lower.contains("টৰ্চ জ্বলোৱা") ||
                lower.contains("ٹارچ آن کرو") || lower.contains("ٹارچ جلاؤ")
    }

    private fun matchesTorchOff(lower: String): Boolean {
        return lower.contains("torch off") || lower.contains("turn off torch") || lower.contains("turn off the torch") ||
                lower.contains("flashlight off") || lower.contains("turn off flashlight") ||
                lower.contains("torch band karo") || lower.contains("torch bujhao") ||
                lower.contains("টর্চ অফ করো") || lower.contains("টর্চ বন্ধ করো") || lower.contains("টর্চ নিভাও") ||
                lower.contains("টৰ্চ বন্ধ কৰা") ||
                lower.contains("ٹارچ بند کرو")
    }

    private fun matchesBattery(lower: String): Boolean {
        return lower.contains("battery percentage") || lower.contains("battery percent") || lower.contains("battery level") ||
                lower.contains("is my phone charging") || lower.contains("charging state") || lower.contains("battery status") ||
                lower.contains("battery saver") || lower.contains("battery kitni") || lower.contains("charge kitna") ||
                lower.contains("ব্যাটারি কত") || lower.contains("চার্জ কত") || lower.contains("ব্যাটাৰী") ||
                lower.contains("بیٹری")
    }

    private fun matchesCamera(lower: String): Boolean {
        return lower == "camera" || lower == "open camera" || lower.contains("open the camera") ||
                lower.contains("camera kholo") || lower.contains("ক্যামেরা খোলো") || lower.contains("ক্যামেৰা খোলা") ||
                lower.contains("کیمرہ کھولو")
    }

    private fun parseCallIntent(query: String, lower: String): LyraIntent? {
        // Regex patterns for call in various languages
        val patterns = listOf(
            Pattern.compile("(?:call|phone|dial)\\s+([a-zA-Z0-9\\+\\s]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([a-zA-Z0-9\\+\\s]+?)\\s+(?:ko\\s+call\\s+karo|ko\\s+phone\\s+lagao|ko\\s+call\\s+lagao)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([a-zA-Z0-9\\+\\s\u0980-\u09FF]+?)(?:-কে|\\s+ke)?\\s+(?:phone\\s+koro|call\\s+koro|ফোন\\s+করো|কল\\s+করো)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([a-zA-Z0-9\\+\\s\u0980-\u09FF]+?)(?:ক|\\s+ok)?\\s+(?:ফোন\\s+কৰা|কল\\s+কৰা)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([a-zA-Z0-9\\+\\s\u0600-\u06FF]+?)\\s+(?:کو\\s+کال\\s+کرو|کو\\s+فون\\s+کرو)", Pattern.CASE_INSENSITIVE)
        )

        for (pattern in patterns) {
            val matcher = pattern.matcher(query)
            if (matcher.find()) {
                val target = matcher.group(1)?.trim() ?: continue
                if (target.isNotEmpty() && !target.equals("me", ignoreCase = true) && !target.equals("back", ignoreCase = true)) {
                    return LyraIntent(
                        type = IntentType.CALL_CONTACT,
                        rawQuery = query,
                        parameters = mapOf("contactName" to target),
                        requiredPermission = Manifest.permission.CALL_PHONE,
                        confirmationRequired = ConfirmationRequirement.SENSITIVE,
                        description = "Initiate voice call to $target"
                    )
                }
            }
        }
        return null
    }

    private fun parseWhatsAppIntent(query: String, lower: String): LyraIntent? {
        if (!lower.contains("whatsapp") && !lower.contains("হোৱাটছএপ") && !lower.contains("হোয়াটসঅ্যাপ") && !lower.contains("واٹس ایپ")) {
            return null
        }

        // Match: "whatsapp Rahul and say I'll call you later" or "whatsapp Jiya saying hi"
        val sayPattern = Pattern.compile("(?:whatsapp|message)\\s+([a-zA-Z0-9\\s]+?)\\s+(?:and\\s+say|saying|say)\\s+(.+)", Pattern.CASE_INSENSITIVE)
        val sayMatcher = sayPattern.matcher(query)
        if (sayMatcher.find()) {
            val contact = sayMatcher.group(1)?.trim() ?: ""
            val message = sayMatcher.group(2)?.trim() ?: ""
            return LyraIntent(
                type = IntentType.WHATSAPP_CONTACT,
                rawQuery = query,
                parameters = mapOf("contactName" to contact, "messageText" to message),
                confirmationRequired = ConfirmationRequirement.SENSITIVE,
                description = "Send WhatsApp message to $contact"
            )
        }

        // Match: "send Jiya a WhatsApp message saying Hi, where are you?"
        val sendPattern = Pattern.compile("send\\s+([a-zA-Z0-9\\s]+?)\\s+(?:a\\s+)?whatsapp\\s+(?:message\\s+)?(?:saying|say)?\\s*(.*)", Pattern.CASE_INSENSITIVE)
        val sendMatcher = sendPattern.matcher(query)
        if (sendMatcher.find()) {
            val contact = sendMatcher.group(1)?.trim() ?: ""
            val message = sendMatcher.group(2)?.trim() ?: ""
            return LyraIntent(
                type = IntentType.WHATSAPP_CONTACT,
                rawQuery = query,
                parameters = mapOf("contactName" to contact, "messageText" to message),
                confirmationRequired = ConfirmationRequirement.SENSITIVE,
                description = "Send WhatsApp message to $contact"
            )
        }

        // Hindi: "Rahul ko WhatsApp karo ki ... / Rahul ko WhatsApp message bhejo"
        val hindiPattern = Pattern.compile("([a-zA-Z0-9\\s]+?)\\s+ko\\s+whatsapp(?:\\s+message)?\\s+(?:bhejo|karo)(?:\\s+ki\\s+(.+))?", Pattern.CASE_INSENSITIVE)
        val hindiMatcher = hindiPattern.matcher(query)
        if (hindiMatcher.find()) {
            val contact = hindiMatcher.group(1)?.trim() ?: ""
            val message = hindiMatcher.group(2)?.trim() ?: ""
            return LyraIntent(
                type = IntentType.WHATSAPP_CONTACT,
                rawQuery = query,
                parameters = mapOf("contactName" to contact, "messageText" to message),
                confirmationRequired = ConfirmationRequirement.SENSITIVE,
                description = "Send WhatsApp message to $contact"
            )
        }

        // Just open WhatsApp or simple WhatsApp contact without prefilled message
        val simplePattern = Pattern.compile("(?:whatsapp)\\s+([a-zA-Z0-9\\s]+)", Pattern.CASE_INSENSITIVE)
        val simpleMatcher = simplePattern.matcher(query)
        if (simpleMatcher.find()) {
            val contact = simpleMatcher.group(1)?.trim() ?: ""
            if (contact.isNotEmpty() && !contact.equals("app", ignoreCase = true)) {
                return LyraIntent(
                    type = IntentType.WHATSAPP_CONTACT,
                    rawQuery = query,
                    parameters = mapOf("contactName" to contact, "messageText" to ""),
                    confirmationRequired = ConfirmationRequirement.SENSITIVE,
                    description = "Open WhatsApp chat with $contact"
                )
            }
        }

        // General open WhatsApp
        return LyraIntent(
            type = IntentType.OPEN_APP,
            rawQuery = query,
            parameters = mapOf("appName" to "WhatsApp", "packageName" to "com.whatsapp"),
            description = "Open WhatsApp application"
        )
    }

    private fun parseSettingsIntent(raw: String, lower: String): LyraIntent? {
        if (!lower.contains("settings") && !lower.contains("setting")) return null

        val settingType = when {
            lower.contains("wi-fi") || lower.contains("wifi") -> "wifi"
            lower.contains("bluetooth") -> "bluetooth"
            lower.contains("display") || lower.contains("screen") -> "display"
            lower.contains("sound") || lower.contains("volume") || lower.contains("audio") -> "sound"
            else -> "general"
        }

        return LyraIntent(
            type = IntentType.OPEN_SETTINGS,
            rawQuery = raw,
            parameters = mapOf("settingType" to settingType),
            description = "Open $settingType system settings"
        )
    }

    private fun parseOpenAppIntent(raw: String, lower: String): LyraIntent? {
        if (!lower.startsWith("open ") && !lower.contains("kholo") && !lower.contains("খোলো") && !lower.contains("کھولو")) {
            return null
        }

        val appName = when {
            lower.contains("youtube") -> "YouTube"
            lower.contains("instagram") || lower.contains("insta") -> "Instagram"
            lower.contains("whatsapp") -> "WhatsApp"
            lower.contains("camera") -> return LyraIntent(type = IntentType.CAMERA, rawQuery = raw, description = "Open Camera")
            lower.contains("gallery") || lower.contains("photos") -> "Gallery"
            lower.contains("browser") || lower.contains("chrome") -> "Browser"
            lower.contains("settings") -> return parseSettingsIntent(raw, lower)
            else -> {
                val match = Pattern.compile("open\\s+([a-zA-Z0-9\\s]+)", Pattern.CASE_INSENSITIVE).matcher(lower)
                if (match.find()) match.group(1)?.trim()?.replaceFirstChar { it.uppercase() } ?: "" else ""
            }
        }

        if (appName.isNotEmpty()) {
            return LyraIntent(
                type = IntentType.OPEN_APP,
                rawQuery = raw,
                parameters = mapOf("appName" to appName),
                description = "Open $appName"
            )
        }
        return null
    }
}
