package com.example.engine.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.preferences.AppSettings
import com.example.engine.contact.ContactEngine
import com.example.engine.contact.ContactMatch
import com.example.engine.contact.ContactSearchResult
import com.example.engine.device.DeviceControlEngine
import com.example.engine.emotion.EmotionCue
import com.example.engine.emotion.EmotionEngine
import com.example.engine.intent.ConfirmationRequirement
import com.example.engine.intent.IntentClassifier
import com.example.engine.intent.IntentType
import com.example.engine.intent.LyraIntent
import com.example.engine.language.LanguageEngine

sealed class RouterOutcome {
    data class ImmediateResult(
        val text: String,
        val intentType: IntentType,
        val emotionCue: EmotionCue
    ) : RouterOutcome()

    data class NeedsConfirmation(
        val title: String,
        val description: String,
        val targetNumber: String? = null,
        val contactName: String? = null,
        val intent: LyraIntent
    ) : RouterOutcome()

    data class DisambiguateContact(
        val nameQuery: String,
        val candidates: List<ContactMatch>,
        val originalIntent: LyraIntent
    ) : RouterOutcome()

    data class CallOnlineAi(
        val cleanPrompt: String,
        val emotionCue: EmotionCue,
        val imageBitmap: Bitmap? = null
    ) : RouterOutcome()
}

class AiRouter(
    private val context: Context,
    private val deviceEngine: DeviceControlEngine,
    private val contactEngine: ContactEngine,
    private val offlineAiEngine: OfflineAiEngine,
    private val onlineAiEngine: OnlineAiEngine
) {

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = cm?.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun route(
        input: String,
        imageBitmap: Bitmap? = null,
        settings: AppSettings
    ): RouterOutcome {
        val detectedLang = if (settings.autoLanguageDetection) {
            LanguageEngine.detectLanguage(input)
        } else {
            settings.languageCode
        }

        val cue = if (settings.emotionAwareEnabled) {
            EmotionEngine.detectCue(input)
        } else {
            EmotionCue.NEUTRAL
        }

        // 1. If image is provided, route directly to Vision Engine / Online AI if internet is available
        if (imageBitmap != null) {
            return if (isOnline() && settings.onlineAiEnabled && onlineAiEngine.hasApiKey()) {
                RouterOutcome.CallOnlineAi(
                    cleanPrompt = input.ifBlank { "What is in this image? Please describe and explain in detail." },
                    emotionCue = cue,
                    imageBitmap = imageBitmap
                )
            } else {
                RouterOutcome.ImmediateResult(
                    text = "Image analysis requires an active internet connection and configured Gemini API key.",
                    intentType = IntentType.IMAGE_ANALYSIS,
                    emotionCue = cue
                )
            }
        }

        // 2. Classify intent
        val intent = IntentClassifier.classify(input)

        // 3. Process Device Commands
        when (intent.type) {
            IntentType.TORCH_ON -> {
                val res = deviceEngine.setTorch(true)
                return RouterOutcome.ImmediateResult(res.message, intent.type, cue)
            }
            IntentType.TORCH_OFF -> {
                val res = deviceEngine.setTorch(false)
                return RouterOutcome.ImmediateResult(res.message, intent.type, cue)
            }
            IntentType.BATTERY_STATUS -> {
                val summary = deviceEngine.getBatterySummary()
                return RouterOutcome.ImmediateResult(summary, intent.type, cue)
            }
            IntentType.DEVICE_STATUS -> {
                val summary = deviceEngine.getDeviceStatusSummary()
                return RouterOutcome.ImmediateResult(summary, intent.type, cue)
            }
            IntentType.CAMERA -> {
                val res = deviceEngine.openCamera()
                return RouterOutcome.ImmediateResult(res.message, intent.type, cue)
            }
            IntentType.OPEN_SETTINGS -> {
                val type = intent.parameters["settingType"] ?: "general"
                val res = deviceEngine.openSettings(type)
                return RouterOutcome.ImmediateResult(res.message, intent.type, cue)
            }
            IntentType.OPEN_APP -> {
                val app = intent.parameters["appName"] ?: "App"
                val pkg = intent.parameters["packageName"]
                val res = deviceEngine.openApp(app, pkg)
                return RouterOutcome.ImmediateResult(res.message, intent.type, cue)
            }
            IntentType.VOLUME_UP -> {
                val res = deviceEngine.adjustVolume(1)
                return RouterOutcome.ImmediateResult(res.message, intent.type, cue)
            }
            IntentType.VOLUME_DOWN -> {
                val res = deviceEngine.adjustVolume(-1)
                return RouterOutcome.ImmediateResult(res.message, intent.type, cue)
            }
            IntentType.VOLUME_MUTE -> {
                val res = deviceEngine.muteVolume()
                return RouterOutcome.ImmediateResult(res.message, intent.type, cue)
            }
            IntentType.MEDIA_PLAY -> {
                val res = deviceEngine.controlMedia("play")
                return RouterOutcome.ImmediateResult(res.message, intent.type, cue)
            }
            IntentType.MEDIA_PAUSE -> {
                val res = deviceEngine.controlMedia("pause")
                return RouterOutcome.ImmediateResult(res.message, intent.type, cue)
            }
            IntentType.MEDIA_NEXT -> {
                val res = deviceEngine.controlMedia("next")
                return RouterOutcome.ImmediateResult(res.message, intent.type, cue)
            }
            IntentType.MEDIA_PREVIOUS -> {
                val res = deviceEngine.controlMedia("prev")
                return RouterOutcome.ImmediateResult(res.message, intent.type, cue)
            }
            IntentType.SET_TIMER -> {
                val seconds = intent.parameters["seconds"]?.toIntOrNull() ?: 300
                val label = intent.parameters["label"] ?: "Lyra Timer"
                val res = deviceEngine.setTimer(seconds, label)
                return RouterOutcome.ImmediateResult(res.message, intent.type, cue)
            }
            IntentType.SET_ALARM -> {
                val hour = intent.parameters["hour"]?.toIntOrNull() ?: 7
                val min = intent.parameters["minute"]?.toIntOrNull() ?: 0
                val amPm = intent.parameters["amPm"] ?: ""
                val adjustedHour = if (amPm.equals("pm", true) && hour < 12) hour + 12 else if (amPm.equals("am", true) && hour == 12) 0 else hour
                val res = deviceEngine.setAlarm(adjustedHour, min)
                return RouterOutcome.ImmediateResult(res.message, intent.type, cue)
            }
            IntentType.CALL_CONTACT -> {
                val contactTarget = intent.parameters["contactName"] ?: ""
                val searchResult = contactEngine.findContact(contactTarget)

                return when (searchResult) {
                    is ContactSearchResult.PermissionRequired -> {
                        RouterOutcome.ImmediateResult(
                            "I need Android Contacts permission to look up ${contactTarget}.",
                            intent.type,
                            cue
                        )
                    }
                    is ContactSearchResult.NotFound -> {
                        RouterOutcome.ImmediateResult(
                            "I couldn't find any contact named \"$contactTarget\".",
                            intent.type,
                            cue
                        )
                    }
                    is ContactSearchResult.Multiple -> {
                        RouterOutcome.DisambiguateContact(
                            nameQuery = contactTarget,
                            candidates = searchResult.matches,
                            originalIntent = intent
                        )
                    }
                    is ContactSearchResult.Single -> {
                        val match = searchResult.contact
                        val shouldConfirm = settings.confirmationLevel != "SAFE_ONLY"
                        if (shouldConfirm) {
                            RouterOutcome.NeedsConfirmation(
                                title = "Call ${match.name}?",
                                description = "Dial ${match.phoneNumber} on mobile network",
                                targetNumber = match.phoneNumber,
                                contactName = match.name,
                                intent = intent
                            )
                        } else {
                            // Direct call if confirmation disabled
                            val commEngine = com.example.engine.communication.CommunicationEngine(context)
                            val callRes = commEngine.initiateCall(match.phoneNumber, match.name)
                            val message = when (callRes) {
                                is com.example.engine.communication.CommunicationResult.Success -> callRes.message
                                is com.example.engine.communication.CommunicationResult.Failure -> callRes.reason
                                is com.example.engine.communication.CommunicationResult.PermissionRequired -> callRes.message
                            }
                            RouterOutcome.ImmediateResult(message, intent.type, cue)
                        }
                    }
                }
            }
            IntentType.WHATSAPP_CONTACT -> {
                val contactTarget = intent.parameters["contactName"] ?: ""
                val messageText = intent.parameters["messageText"] ?: ""
                val searchResult = contactEngine.findContact(contactTarget)

                val (resolvedNumber, resolvedName) = when (searchResult) {
                    is ContactSearchResult.Single -> Pair(searchResult.contact.phoneNumber, searchResult.contact.name)
                    is ContactSearchResult.Multiple -> {
                        return RouterOutcome.DisambiguateContact(
                            nameQuery = contactTarget,
                            candidates = searchResult.matches,
                            originalIntent = intent
                        )
                    }
                    else -> Pair(null, contactTarget)
                }

                return RouterOutcome.NeedsConfirmation(
                    title = "Send WhatsApp message to $resolvedName?",
                    description = if (messageText.isNotEmpty()) "\"$messageText\"" else "Open WhatsApp chat",
                    targetNumber = resolvedNumber,
                    contactName = resolvedName,
                    intent = intent
                )
            }
            else -> {
                // Not a device action. Route between Offline AI and Online AI
                val online = isOnline()
                if (online && settings.onlineAiEnabled && onlineAiEngine.hasApiKey()) {
                    return RouterOutcome.CallOnlineAi(
                        cleanPrompt = input,
                        emotionCue = cue
                    )
                } else {
                    val offlineText = offlineAiEngine.getOfflineResponse(input, detectedLang)
                    val tonePrefix = if (settings.emotionAwareEnabled) EmotionEngine.getAdaptiveTonePrefix(cue) ?: "" else ""
                    return RouterOutcome.ImmediateResult(
                        text = tonePrefix + offlineText,
                        intentType = IntentType.GENERAL_CHAT,
                        emotionCue = cue
                    )
                }
            }
        }
    }
}
