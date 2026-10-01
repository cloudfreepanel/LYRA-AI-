package com.example.ui.components

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.DarkCosmicBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LiveVoiceOverlay(
    isOpen: Boolean,
    orbState: OrbState,
    rmsLevel: Float,
    isListening: Boolean,
    isSpeaking: Boolean,
    partialText: String,
    lastResponse: String,
    onToggleMic: () -> Unit,
    onStopSpeech: () -> Unit,
    onClose: () -> Unit,
    onQuickPrompt: (String) -> Unit
) {
    if (!isOpen) return

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("live_voice_overlay"),
            color = DarkCosmicBg.copy(alpha = 0.96f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Bar with Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isListening) NeonCyan else NeonMagenta)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LIVE VOICE ASSISTANT",
                            style = MaterialTheme.typography.labelSmall,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("close_live_voice_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Live Voice",
                            tint = TextSecondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Center: Glowing AI Orb & Dynamic Visual Feedback
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LyraOrb(
                        state = orbState,
                        rmsLevel = rmsLevel,
                        size = 200.dp,
                        onClick = onToggleMic
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Status Indicator
                    val statusText = when {
                        isListening -> "◉ LYRA IS LISTENING"
                        isSpeaking -> "✦ LYRA IS SPEAKING"
                        orbState == OrbState.THINKING -> "✺ LYRA IS THINKING..."
                        else -> "Tap the orb or mic to speak"
                    }

                    val statusColor = when {
                        isListening -> NeonCyan
                        isSpeaking -> NeonMagenta
                        orbState == OrbState.THINKING -> NeonViolet
                        else -> TextSecondary
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Live Speech Transcription / Response display
                    if (partialText.isNotBlank()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .clip(RoundedCornerShape(16.dp)),
                            color = DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "“$partialText”",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else if (lastResponse.isNotBlank()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .clip(RoundedCornerShape(16.dp)),
                            color = DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonViolet.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = lastResponse,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = 5,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "Say “Hey Lyra” or tap mic below\n(Torch, Calls, Time, Alarms, Multilingual)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                }

                // Bottom: Controls & Quick Voice Test Prompts
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Quick Voice Test Chips (vital for testing live voice even if browser mic is muted)
                    Text(
                        text = "QUICK VOICE TEST PROMPTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VoicePromptChip("What time is it?") { onQuickPrompt("What time is it?") }
                        VoicePromptChip("Turn on the torch") { onQuickPrompt("Hey Lyra, turn on the torch") }
                        VoicePromptChip("Torch band karo (Hindi)") { onQuickPrompt("Torch band karo") }
                        VoicePromptChip("Battery percentage?") { onQuickPrompt("Battery percentage?") }
                        VoicePromptChip("Call Rahul") { onQuickPrompt("Hey Lyra, call Rahul") }
                        VoicePromptChip("টর্চ অন করো (Bengali)") { onQuickPrompt("টর্চ অন করো") }
                        VoicePromptChip("Set a timer for 5 minutes") { onQuickPrompt("Set a timer for 5 minutes") }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Mic and Stop Controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Stop speech button (visible if speaking)
                        AnimatedVisibility(visible = isSpeaking) {
                            Surface(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .clickable { onStopSpeech() },
                                shape = CircleShape,
                                color = DarkSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonMagenta)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop Speech",
                                        tint = NeonMagenta,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }

                        // Big Central Mic Button
                        Surface(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .clickable { onToggleMic() }
                                .testTag("live_voice_main_mic_button"),
                            shape = CircleShape,
                            color = if (isListening) NeonCyan else NeonCyan.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(2.dp, NeonCyan)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = if (isListening) "Stop Listening" else "Start Listening",
                                    tint = if (isListening) DarkCosmicBg else NeonCyan,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isListening) "Tap to pause listening" else "Tap to start voice recognition",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun VoicePromptChip(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
