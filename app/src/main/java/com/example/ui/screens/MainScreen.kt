package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.ui.LyraViewModel
import com.example.ui.Screen
import com.example.ui.components.ActionConfirmationDialog
import com.example.ui.components.ChatMessageItem
import com.example.ui.components.DisambiguationDialog
import com.example.ui.components.GlassmorphicCard
import com.example.ui.components.LiveVoiceOverlay
import com.example.ui.components.LyraOrb
import com.example.ui.components.OrbState
import com.example.ui.theme.DarkCosmicBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: LyraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val messages by viewModel.messages.collectAsState()
    val orbState by viewModel.orbState.collectAsState()
    val rmsLevel by viewModel.rmsLevel.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val isLiveVoiceOpen by viewModel.isLiveVoiceOpen.collectAsState()
    val partialText by viewModel.partialText.collectAsState()
    val lastAssistantResponse by viewModel.lastAssistantResponse.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val selectedImageUri by viewModel.selectedImageUri.collectAsState()
    val pendingConfirmation by viewModel.pendingConfirmation.collectAsState()
    val pendingDisambiguation by viewModel.pendingDisambiguation.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val isOnline = remember(viewModel) { viewModel.aiRouter.isOnline() }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Photo picker launcher (Zero storage permission per Play Policy)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.attachImage(uri)
        }
    }

    // System Speech Recognition Activity Result fallback
    val systemSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenList = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            if (!spokenList.isNullOrEmpty()) {
                viewModel.onExternalSpeechResult(spokenList[0])
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.requestSystemSpeechDialog.collectLatest { intent ->
            try {
                systemSpeechLauncher.launch(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "No speech recognition app installed on this device.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Microphone permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.openLiveVoice()
        } else {
            Toast.makeText(context, "Microphone permission is required for live voice.", Toast.LENGTH_SHORT).show()
        }
    }

    fun startLiveVoiceFlow() {
        val hasAudioPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasAudioPerm) {
            viewModel.openLiveVoice()
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Handle Confirmation Dialog
    pendingConfirmation?.let { conf ->
        ActionConfirmationDialog(
            title = conf.title,
            description = conf.description,
            intentType = conf.intent.type,
            onConfirm = { viewModel.confirmPendingAction() },
            onDismiss = { viewModel.dismissPendingAction() }
        )
    }

    // Handle Contact Disambiguation Dialog
    pendingDisambiguation?.let { dis ->
        DisambiguationDialog(
            nameQuery = dis.nameQuery,
            candidates = dis.candidates,
            onSelectContact = { contact -> viewModel.selectDisambiguatedContact(contact) },
            onDismiss = { viewModel.dismissDisambiguation() }
        )
    }

    // Dedicated Full-Screen Live Voice Assistant Overlay
    LiveVoiceOverlay(
        isOpen = isLiveVoiceOpen,
        orbState = orbState,
        rmsLevel = rmsLevel,
        isListening = isListening,
        isSpeaking = isSpeaking,
        partialText = partialText,
        lastResponse = lastAssistantResponse,
        onToggleMic = { viewModel.toggleVoiceListening() },
        onStopSpeech = { viewModel.stopSpeaking() },
        onClose = { viewModel.closeLiveVoice() },
        onQuickPrompt = { prompt -> viewModel.submitUserMessage(prompt, speakResponse = true) }
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCosmicBg)
            .imePadding(),
        containerColor = DarkCosmicBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "LYRA AI",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // Online/Offline status pill
                                Surface(
                                    shape = CircleShape,
                                    color = if (isOnline) NeonGreen.copy(alpha = 0.2f) else NeonAmber.copy(alpha = 0.2f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isOnline) NeonGreen.copy(alpha = 0.5f) else NeonAmber.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (isOnline) NeonGreen else NeonAmber)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isOnline) "ONLINE" else "OFFLINE",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = if (isOnline) NeonGreen else NeonAmber,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Your Intelligent Mobile Companion",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    // Dedicated Live Voice Button
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { startLiveVoiceFlow() }
                            .testTag("open_live_voice_bar_button"),
                        shape = RoundedCornerShape(12.dp),
                        color = NeonCyan.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Live Voice",
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Voice",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // New Chat
                    IconButton(
                        onClick = { viewModel.startNewConversation() },
                        modifier = Modifier.testTag("new_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Conversation",
                            tint = NeonCyan
                        )
                    }

                    // Chat History
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.HISTORY) },
                        modifier = Modifier.testTag("nav_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Chat History",
                            tint = TextSecondary
                        )
                    }

                    // Privacy Center
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.PRIVACY) },
                        modifier = Modifier.testTag("nav_privacy_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Privacy Center",
                            tint = TextSecondary
                        )
                    }

                    // Settings
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.SETTINGS) },
                        modifier = Modifier.testTag("nav_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkCosmicBg.copy(alpha = 0.95f)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Animated LYRA Orb & Status Zone
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = if (messages.isEmpty()) 28.dp else 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LyraOrb(
                        state = orbState,
                        rmsLevel = rmsLevel,
                        size = if (messages.isEmpty()) 160.dp else 100.dp,
                        onClick = { startLiveVoiceFlow() }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Status Text Indicator
                    when {
                        isListening -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NeonCyan))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LYRA IS LISTENING",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                        isSpeaking -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NeonMagenta))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "LYRA IS SPEAKING",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = NeonMagenta,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { viewModel.stopSpeaking() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop",
                                        tint = NeonMagenta,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        orbState == OrbState.THINKING -> {
                            Text(
                                text = "LYRA IS PROCESSING...",
                                style = MaterialTheme.typography.labelLarge,
                                color = NeonViolet,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        messages.isEmpty() -> {
                            Text(
                                text = "“How can I help you?”",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextSecondary,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Quick suggestion chips when few or no messages
            if (messages.size <= 2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionChip(label = "🔦 Turn on torch") { viewModel.submitUserMessage("Turn on the torch") }
                    SuggestionChip(label = "🔋 Battery status") { viewModel.submitUserMessage("Battery status") }
                    SuggestionChip(label = "📞 Call Mom") { viewModel.submitUserMessage("Call Mom") }
                    SuggestionChip(label = "💬 WhatsApp") { viewModel.submitUserMessage("WhatsApp Rahul saying Hello") }
                    SuggestionChip(label = "⏰ 10 min timer") { viewModel.submitUserMessage("Set a timer for 10 minutes") }
                    SuggestionChip(label = "📷 Open camera") { viewModel.submitUserMessage("Open camera") }
                    SuggestionChip(label = "🔊 Volume up") { viewModel.submitUserMessage("Volume up") }
                }
            }

            // Conversation Messages Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatMessageItem(
                        message = msg,
                        isSpeaking = isSpeaking,
                        onSpeak = { text -> viewModel.speakText(text) },
                        onStopSpeak = { viewModel.stopSpeaking() },
                        onRegenerate = { viewModel.regenerateLastMessage() },
                        onDelete = { viewModel.deleteMessage(msg.id) },
                        onEdit = { text -> viewModel.setInputText(text) }
                    )
                }
            }

            // Attached Image Preview
            AnimatedVisibility(visible = selectedImageUri != null) {
                selectedImageUri?.let { uri ->
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                            .size(70.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Attached image preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(22.dp)
                                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                .clickable { viewModel.clearAttachedImage() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove image",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Futuristic Bottom Input Bar
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                backgroundColor = DarkSurface,
                borderColor = NeonCyan.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Image attachment button (Photo Picker)
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.size(42.dp).testTag("image_picker_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Attach image",
                            tint = if (selectedImageUri != null) NeonCyan else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Text Input Field
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { viewModel.setInputText(it) },
                        placeholder = {
                            Text(
                                text = if (isListening) "Listening..." else "Type a message or command...",
                                color = TextTertiary,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        singleLine = false,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = NeonCyan
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank() || selectedImageUri != null) {
                                    viewModel.submitUserMessage(inputText)
                                }
                            }
                        )
                    )

                    // Voice / Mic Button
                    Surface(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .testTag("voice_mic_button")
                            .clickable { startLiveVoiceFlow() },
                        shape = CircleShape,
                        color = if (isListening) NeonCyan.copy(alpha = 0.25f) else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isListening) NeonCyan else Color.Transparent
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.Mic else Icons.Default.Mic,
                                contentDescription = if (isListening) "Listening" else "Voice input",
                                tint = if (isListening) NeonCyan else TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Send Button
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() || selectedImageUri != null) {
                                viewModel.submitUserMessage(inputText)
                            }
                        },
                        enabled = inputText.isNotBlank() || selectedImageUri != null,
                        modifier = Modifier.size(42.dp).testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank() || selectedImageUri != null) NeonCyan else TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestionChip(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("suggestion_chip_$label"),
        shape = RoundedCornerShape(14.dp),
        color = DarkSurfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
