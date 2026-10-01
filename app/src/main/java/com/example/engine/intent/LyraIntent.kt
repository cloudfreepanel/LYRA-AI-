package com.example.engine.intent

enum class IntentType {
    TORCH_ON,
    TORCH_OFF,
    CALL_CONTACT,
    WHATSAPP_CONTACT,
    SEND_MESSAGE,
    OPEN_APP,
    OPEN_SETTINGS,
    CAMERA,
    VOLUME_UP,
    VOLUME_DOWN,
    VOLUME_MUTE,
    BRIGHTNESS_SET,
    MEDIA_PLAY,
    MEDIA_PAUSE,
    MEDIA_NEXT,
    MEDIA_PREVIOUS,
    SET_TIMER,
    SET_ALARM,
    BATTERY_STATUS,
    DEVICE_STATUS,
    IMAGE_ANALYSIS,
    GENERAL_CHAT,
    UNKNOWN
}

enum class ConfirmationRequirement {
    NONE,       // Safe actions (torch, battery, volume)
    SENSITIVE,  // Calling, sending WhatsApp, deleting data
    ALWAYS      // When configured in strict mode
}

data class LyraIntent(
    val type: IntentType,
    val rawQuery: String,
    val parameters: Map<String, String> = emptyMap(),
    val requiredPermission: String? = null,
    val confirmationRequired: ConfirmationRequirement = ConfirmationRequirement.NONE,
    val description: String = ""
) {
    fun isSensitive(): Boolean = confirmationRequired != ConfirmationRequirement.NONE
}
