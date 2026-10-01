package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LyraDatabase
import com.example.data.preferences.AppSettings
import com.example.data.preferences.UserPreferences
import com.example.data.repository.LyraRepository
import com.example.engine.ai.AiRouter
import com.example.engine.ai.OfflineAiEngine
import com.example.engine.ai.OnlineAiEngine
import com.example.engine.ai.RouterOutcome
import com.example.engine.communication.CommunicationEngine
import com.example.engine.contact.ContactEngine
import com.example.engine.contact.ContactMatch
import com.example.engine.device.DeviceControlEngine
import com.example.engine.emotion.EmotionCue
import com.example.engine.intent.IntentClassifier
import com.example.engine.intent.IntentType
import com.example.engine.intent.LyraIntent
import com.example.engine.voice.SpeechRecognitionEngine
import com.example.engine.voice.TextToSpeechEngine
import com.example.engine.voice.WakeWordEngine
import com.example.model.ConversationEntity
import com.example.model.MemoryEntity
import com.example.model.MessageEntity
import com.example.ui.components.OrbState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class Screen {
    MAIN,
    HISTORY,
    PRIVACY,
    SETTINGS,
    MEMORY
}

data class PendingActionConfirmation(
    val title: String,
    val description: String,
    val targetNumber: String? = null,
    val contactName: String? = null,
    val intent: LyraIntent
)

data class PendingDisambiguation(
    val nameQuery: String,
    val candidates: List<ContactMatch>,
    val originalIntent: LyraIntent
)

class LyraViewModel(application: Application) : AndroidViewModel(application) {

    // Database & Repository
    private val database = LyraDatabase.getInstance(application)
    private val userPreferences = UserPreferences(application)
    val repository = LyraRepository(database.lyraDao(), userPreferences)

    // Engines
    val deviceEngine = DeviceControlEngine(application)
    val contactEngine = ContactEngine(application)
    val communicationEngine = CommunicationEngine(application)
    val ttsEngine = TextToSpeechEngine(application)
    val speechEngine = SpeechRecognitionEngine(application)
    val wakeWordEngine = WakeWordEngine()
    val offlineAiEngine = OfflineAiEngine(application)
    val onlineAiEngine = OnlineAiEngine()
    val aiRouter = AiRouter(application, deviceEngine, contactEngine, offlineAiEngine, onlineAiEngine)

    // App Navigation State
    private val _currentScreen = MutableStateFlow(Screen.MAIN)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Current Conversation State
    private val _currentConversationId = MutableStateFlow(UUID.randomUUID().toString())
    val currentConversationId: StateFlow<String> = _currentConversationId.asStateFlow()

    val settings: StateFlow<AppSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings()
    )

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

    val conversations: StateFlow<List<ConversationEntity>> = repository.allConversations.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val memories: StateFlow<List<MemoryEntity>> = repository.allMemories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Orb & Voice State
    private val _orbState = MutableStateFlow(OrbState.IDLE)
    val orbState: StateFlow<OrbState> = _orbState.asStateFlow()

    val rmsLevel: StateFlow<Float> = speechEngine.rmsLevel
    val isListening: StateFlow<Boolean> = speechEngine.isListening
    val isSpeaking: StateFlow<Boolean> = ttsEngine.isSpeaking
    val partialText: StateFlow<String> = speechEngine.partialText
    val requestSystemSpeechDialog = speechEngine.requestSystemDialog

    private val _isLiveVoiceOpen = MutableStateFlow(false)
    val isLiveVoiceOpen: StateFlow<Boolean> = _isLiveVoiceOpen.asStateFlow()

    private val _lastAssistantResponse = MutableStateFlow("")
    val lastAssistantResponse: StateFlow<String> = _lastAssistantResponse.asStateFlow()

    // Input & Image attachments
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _selectedImageBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedImageBitmap: StateFlow<Bitmap?> = _selectedImageBitmap.asStateFlow()

    // Confirmation & Disambiguation Dialogs
    private val _pendingConfirmation = MutableStateFlow<PendingActionConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingActionConfirmation?> = _pendingConfirmation.asStateFlow()

    private val _pendingDisambiguation = MutableStateFlow<PendingDisambiguation?>(null)
    val pendingDisambiguation: StateFlow<PendingDisambiguation?> = _pendingDisambiguation.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    private var activeGenerationJob: Job? = null

    init {
        // Observe conversation messages
        viewModelScope.launch {
            _currentConversationId.collectLatest { convId ->
                repository.ensureConversationExists(convId)
                repository.getMessagesForConversation(convId).collectLatest { list ->
                    _messages.value = list
                }
            }
        }

        // Apply TTS preferences
        viewModelScope.launch {
            settings.collectLatest { s ->
                ttsEngine.setSpeechParameters(
                    pitch = s.speechPitch,
                    speed = s.speechSpeed,
                    voiceType = s.voiceType,
                    languageCode = s.languageCode
                )
            }
        }

        // Listen for speech recognizer results
        viewModelScope.launch {
            speechEngine.recognizedText.collectLatest { spoken ->
                if (spoken.isNotBlank()) {
                    // Check wake phrase if enabled
                    val textToProcess = if (settings.value.wakeWordEnabled && wakeWordEngine.checkWakeWord(spoken)) {
                        wakeWordEngine.extractQueryAfterWakeWord(spoken).ifEmpty { spoken }
                    } else {
                        spoken
                    }
                    submitUserMessage(textToProcess, speakResponse = true)
                }
            }
        }

        // Sync Orb states with speech & TTS
        viewModelScope.launch {
            isListening.collectLatest { listening ->
                if (listening) {
                    _orbState.value = OrbState.LISTENING
                } else if (!isSpeaking.value && activeGenerationJob?.isActive != true) {
                    _orbState.value = OrbState.IDLE
                }
            }
        }

        viewModelScope.launch {
            isSpeaking.collectLatest { speaking ->
                if (speaking) {
                    _orbState.value = OrbState.SPEAKING
                } else if (!isListening.value && activeGenerationJob?.isActive != true) {
                    _orbState.value = OrbState.IDLE
                }
            }
        }

        // Listen for speech recognizer errors
        viewModelScope.launch {
            speechEngine.errorMessage.collectLatest { err ->
                _errorMessage.tryEmit(err)
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setInputText(text: String) {
        _inputText.value = text
    }

    fun attachImage(uri: Uri?) {
        _selectedImageUri.value = uri
        if (uri != null) {
            try {
                val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                _selectedImageBitmap.value = bitmap
            } catch (e: Exception) {
                _errorMessage.tryEmit("Failed to load image: ${e.localizedMessage}")
            }
        } else {
            _selectedImageBitmap.value = null
        }
    }

    fun clearAttachedImage() {
        _selectedImageUri.value = null
        _selectedImageBitmap.value = null
    }

    fun openLiveVoice() {
        _isLiveVoiceOpen.value = true
        if (!isListening.value) {
            toggleVoiceListening()
        }
    }

    fun closeLiveVoice() {
        _isLiveVoiceOpen.value = false
        speechEngine.stopListening()
        ttsEngine.stop()
        _orbState.value = OrbState.IDLE
    }

    fun onExternalSpeechResult(text: String) {
        speechEngine.onExternalSpeechResult(text)
    }

    fun toggleVoiceListening() {
        if (isListening.value) {
            speechEngine.stopListening()
            _orbState.value = OrbState.IDLE
        } else {
            ttsEngine.stop()
            val lang = settings.value.languageCode
            speechEngine.startListening(lang)
            _orbState.value = OrbState.LISTENING
        }
    }

    fun submitUserMessage(queryText: String, speakResponse: Boolean = false) {
        val query = queryText.trim()
        val imageBitmap = _selectedImageBitmap.value
        val imageUriString = _selectedImageUri.value?.toString()

        if (query.isEmpty() && imageBitmap == null) return

        _inputText.value = ""
        clearAttachedImage()

        viewModelScope.launch {
            val convId = _currentConversationId.value
            // 1. Save User Message
            repository.saveMessage(
                conversationId = convId,
                role = "user",
                content = query.ifEmpty { "Analyze attached image" },
                imageUri = imageUriString
            )

            _orbState.value = OrbState.THINKING

            // 2. Route via AI Router
            val outcome = aiRouter.route(query, imageBitmap, settings.value)

            when (outcome) {
                is RouterOutcome.ImmediateResult -> {
                    _lastAssistantResponse.value = outcome.text
                    val saved = repository.saveMessage(
                        conversationId = convId,
                        role = "assistant",
                        content = outcome.text,
                        intentType = outcome.intentType.name,
                        emotionCue = outcome.emotionCue.name
                    )
                    _orbState.value = OrbState.IDLE
                    if (speakResponse) {
                        ttsEngine.speak(outcome.text)
                    }
                }

                is RouterOutcome.NeedsConfirmation -> {
                    _orbState.value = OrbState.IDLE
                    _lastAssistantResponse.value = "${outcome.title}\n${outcome.description}"
                    _pendingConfirmation.value = PendingActionConfirmation(
                        title = outcome.title,
                        description = outcome.description,
                        targetNumber = outcome.targetNumber,
                        contactName = outcome.contactName,
                        intent = outcome.intent
                    )
                    // Inform the user
                    repository.saveMessage(
                        conversationId = convId,
                        role = "assistant",
                        content = "${outcome.title}\n${outcome.description}",
                        intentType = outcome.intent.type.name
                    )
                    if (speakResponse) {
                        ttsEngine.speak(outcome.title)
                    }
                }

                is RouterOutcome.DisambiguateContact -> {
                    _orbState.value = OrbState.IDLE
                    _pendingDisambiguation.value = PendingDisambiguation(
                        nameQuery = outcome.nameQuery,
                        candidates = outcome.candidates,
                        originalIntent = outcome.originalIntent
                    )
                    val candidateNames = outcome.candidates.joinToString(" or ") { it.name }
                    val promptText = "I found multiple contacts for \"${outcome.nameQuery}\": $candidateNames. Which one would you like?"
                    _lastAssistantResponse.value = promptText
                    repository.saveMessage(
                        conversationId = convId,
                        role = "assistant",
                        content = promptText,
                        intentType = outcome.originalIntent.type.name
                    )
                    if (speakResponse) {
                        ttsEngine.speak(promptText)
                    }
                }

                is RouterOutcome.CallOnlineAi -> {
                    executeOnlineAiGeneration(outcome.cleanPrompt, outcome.emotionCue, outcome.imageBitmap, speakResponse)
                }
            }
        }
    }

    private fun executeOnlineAiGeneration(
        cleanPrompt: String,
        emotionCue: EmotionCue,
        imageBitmap: Bitmap?,
        speakResponse: Boolean
    ) {
        activeGenerationJob?.cancel()
        activeGenerationJob = viewModelScope.launch {
            val convId = _currentConversationId.value
            _orbState.value = OrbState.THINKING

            // Create temporary streaming message
            val tempMessage = MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = convId,
                role = "assistant",
                content = "",
                emotionCue = emotionCue.name,
                isStreaming = true
            )
            repository.insertMessageDirect(tempMessage)

            val fullResponseBuilder = StringBuilder()
            val recentTurns = repository.getRecentMessages(convId).dropLast(1)

            val tonePrefix = if (settings.value.emotionAwareEnabled) {
                com.example.engine.emotion.EmotionEngine.getAdaptiveTonePrefix(emotionCue)
            } else null

            val result = onlineAiEngine.generateResponse(
                prompt = cleanPrompt,
                recentMessages = recentTurns,
                imageBitmap = imageBitmap,
                emotionPrefix = tonePrefix
            )

            result.onSuccess { responseText ->
                _lastAssistantResponse.value = responseText
                repository.updateMessage(
                    tempMessage.copy(
                        content = responseText,
                        isStreaming = false
                    )
                )
                _orbState.value = OrbState.IDLE
                if (speakResponse) {
                    ttsEngine.speak(responseText)
                }
            }.onFailure { err ->
                // Fallback to offline AI on error
                val fallbackText = offlineAiEngine.getOfflineResponse(cleanPrompt, settings.value.languageCode)
                val failureMessage = "(${err.localizedMessage ?: "Network issue"})\n\n$fallbackText"
                _lastAssistantResponse.value = fallbackText
                repository.updateMessage(
                    tempMessage.copy(
                        content = failureMessage,
                        isStreaming = false
                    )
                )
                _orbState.value = OrbState.IDLE
                if (speakResponse) {
                    ttsEngine.speak(fallbackText)
                }
            }
        }
    }

    fun confirmPendingAction() {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null

        viewModelScope.launch {
            val convId = _currentConversationId.value
            when (pending.intent.type) {
                IntentType.CALL_CONTACT -> {
                    val phone = pending.targetNumber ?: pending.intent.parameters["contactName"] ?: ""
                    val name = pending.contactName ?: phone
                    val res = communicationEngine.initiateCall(phone, name)
                    val text = when (res) {
                        is com.example.engine.communication.CommunicationResult.Success -> res.message
                        is com.example.engine.communication.CommunicationResult.Failure -> res.reason
                        is com.example.engine.communication.CommunicationResult.PermissionRequired -> res.message
                    }
                    repository.saveMessage(convId, "assistant", text, intentType = IntentType.CALL_CONTACT.name)
                    ttsEngine.speak(text)
                }
                IntentType.WHATSAPP_CONTACT -> {
                    val contact = pending.contactName ?: pending.intent.parameters["contactName"] ?: ""
                    val message = pending.intent.parameters["messageText"] ?: ""
                    val res = communicationEngine.prepareWhatsAppMessage(pending.targetNumber, contact, message)
                    val text = when (res) {
                        is com.example.engine.communication.CommunicationResult.Success -> res.message
                        is com.example.engine.communication.CommunicationResult.Failure -> res.reason
                        is com.example.engine.communication.CommunicationResult.PermissionRequired -> res.message
                    }
                    repository.saveMessage(convId, "assistant", text, intentType = IntentType.WHATSAPP_CONTACT.name)
                    ttsEngine.speak(text)
                }
                else -> {}
            }
        }
    }

    fun dismissPendingAction() {
        _pendingConfirmation.value = null
    }

    fun selectDisambiguatedContact(contact: ContactMatch) {
        val pending = _pendingDisambiguation.value ?: return
        _pendingDisambiguation.value = null

        viewModelScope.launch {
            if (pending.originalIntent.type == IntentType.CALL_CONTACT) {
                _pendingConfirmation.value = PendingActionConfirmation(
                    title = "Call ${contact.name}?",
                    description = "Dial ${contact.phoneNumber}",
                    targetNumber = contact.phoneNumber,
                    contactName = contact.name,
                    intent = pending.originalIntent
                )
            } else if (pending.originalIntent.type == IntentType.WHATSAPP_CONTACT) {
                val messageText = pending.originalIntent.parameters["messageText"] ?: ""
                _pendingConfirmation.value = PendingActionConfirmation(
                    title = "Send WhatsApp message to ${contact.name}?",
                    description = if (messageText.isNotEmpty()) "\"$messageText\"" else "Open WhatsApp chat",
                    targetNumber = contact.phoneNumber,
                    contactName = contact.name,
                    intent = pending.originalIntent
                )
            }
        }
    }

    fun dismissDisambiguation() {
        _pendingDisambiguation.value = null
    }

    fun stopSpeaking() {
        ttsEngine.stop()
    }

    fun speakText(text: String) {
        ttsEngine.speak(text)
    }

    fun startNewConversation() {
        activeGenerationJob?.cancel()
        ttsEngine.stop()
        speechEngine.stopListening()
        val newId = UUID.randomUUID().toString()
        _currentConversationId.value = newId
    }

    fun selectConversation(id: String) {
        activeGenerationJob?.cancel()
        ttsEngine.stop()
        _currentConversationId.value = id
        _currentScreen.value = Screen.MAIN
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            repository.deleteConversation(id)
            if (_currentConversationId.value == id) {
                startNewConversation()
            }
        }
    }

    fun deleteMessage(id: String) {
        viewModelScope.launch {
            repository.deleteMessage(id)
        }
    }

    fun regenerateLastMessage() {
        val msgs = _messages.value
        val lastUserMessage = msgs.lastOrNull { it.role == "user" } ?: return
        submitUserMessage(lastUserMessage.content)
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllLocalData()
            startNewConversation()
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            repository.clearAllMemories()
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechEngine.stopListening()
        ttsEngine.shutdown()
        activeGenerationJob?.cancel()
    }
}

// Extension helper on LyraRepository to insert direct message
suspend fun LyraRepository.insertMessageDirect(message: MessageEntity) {
    saveMessage(
        conversationId = message.conversationId,
        role = message.role,
        content = message.content,
        imageUri = message.imageUri,
        intentType = message.intentType,
        emotionCue = message.emotionCue
    )
}
