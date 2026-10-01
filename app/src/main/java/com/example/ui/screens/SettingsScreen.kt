package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.language.LanguageEngine
import com.example.ui.LyraViewModel
import com.example.ui.Screen
import com.example.ui.components.GlassmorphicCard
import com.example.ui.theme.DarkCosmicBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: LyraViewModel,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val settings by viewModel.settings.collectAsState()

    BackHandler {
        viewModel.navigateTo(Screen.MAIN)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkCosmicBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(Screen.MAIN) },
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = NeonCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkCosmicBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. AI Engine Settings
            SettingsSectionHeader(title = "AI ENGINE")
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = DarkSurface
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingToggleRow(
                        title = "Online AI (Gemini)",
                        subtitle = "Use cloud intelligence for multimodal and deep reasoning",
                        checked = settings.onlineAiEnabled,
                        onCheckedChange = { scope.launch { viewModel.repository.userPreferences.updateOnlineAi(it) } }
                    )

                    SettingToggleRow(
                        title = "Offline AI Fallback",
                        subtitle = "Enable local device intelligence when network is unavailable",
                        checked = settings.offlineAiEnabled,
                        onCheckedChange = { scope.launch { viewModel.repository.userPreferences.updateOfflineAi(it) } }
                    )

                    // Response length selector
                    Text(
                        text = "Response Style: ${settings.responseLength}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Concise", "Balanced", "Detailed").forEach { length ->
                            val selected = settings.responseLength == length
                            androidx.compose.material3.FilterChip(
                                selected = selected,
                                onClick = { scope.launch { viewModel.repository.userPreferences.updateResponseLength(length) } },
                                label = { Text(length) },
                                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan.copy(alpha = 0.3f),
                                    selectedLabelColor = NeonCyan
                                )
                            )
                        }
                    }
                }
            }

            // 2. Voice & Multilingual Settings
            SettingsSectionHeader(title = "VOICE & MULTILINGUAL")
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = DarkSurface
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingToggleRow(
                        title = "Wake Phrase (\"Hey Lyra\")",
                        subtitle = "Activate assistant on speech recognition with wake phrase",
                        checked = settings.wakeWordEnabled,
                        onCheckedChange = { scope.launch { viewModel.repository.userPreferences.updateWakeWord(it) } }
                    )

                    SettingToggleRow(
                        title = "Auto Language Detection",
                        subtitle = "Detect English, Hindi, Bengali, Assamese, or Urdu automatically",
                        checked = settings.autoLanguageDetection,
                        onCheckedChange = { scope.launch { viewModel.repository.userPreferences.updateAutoLanguageDetection(it) } }
                    )

                    // Manual Language selection
                    var langMenuExpanded by remember { mutableStateOf(false) }
                    val currentLangName = LanguageEngine.SUPPORTED_LANGUAGES.find { it.code == settings.languageCode }?.displayName ?: "Auto"

                    ExposedDropdownMenuBox(
                        expanded = langMenuExpanded,
                        onExpandedChange = { langMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = "Language: $currentLangName",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langMenuExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = NeonCyan.copy(alpha = 0.3f)
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = langMenuExpanded,
                            onDismissRequest = { langMenuExpanded = false }
                        ) {
                            LanguageEngine.SUPPORTED_LANGUAGES.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text("${lang.displayName} (${lang.nativeName})") },
                                    onClick = {
                                        scope.launch { viewModel.repository.userPreferences.updateLanguage(lang.code) }
                                        langMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Speech Rate Slider
                    Column {
                        Text(
                            text = "Speech Speed: ${"%.2f".format(settings.speechSpeed)}x",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Slider(
                            value = settings.speechSpeed,
                            onValueChange = { scope.launch { viewModel.repository.userPreferences.updateSpeechSpeed(it) } },
                            valueRange = 0.75f..1.5f,
                            steps = 3,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                        )
                    }

                    // Speech Pitch Slider
                    Column {
                        Text(
                            text = "Speech Pitch: ${"%.2f".format(settings.speechPitch)}x",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Slider(
                            value = settings.speechPitch,
                            onValueChange = { scope.launch { viewModel.repository.userPreferences.updateSpeechPitch(it) } },
                            valueRange = 0.8f..1.4f,
                            steps = 3,
                            colors = SliderDefaults.colors(thumbColor = NeonViolet, activeTrackColor = NeonViolet)
                        )
                    }
                }
            }

            // 3. Assistant & Confirmation Settings
            SettingsSectionHeader(title = "ASSISTANT BEHAVIOR")
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = DarkSurface
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SettingToggleRow(
                        title = "Emotion-Aware Cues",
                        subtitle = "Adapt tone and empathy when detecting conversational feelings",
                        checked = settings.emotionAwareEnabled,
                        onCheckedChange = { scope.launch { viewModel.repository.userPreferences.updateEmotionAware(it) } }
                    )

                    // Confirmation behavior
                    Text(
                        text = "Confirmation Requirement",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "SENSITIVE" to "Confirm sensitive actions (Calls, WhatsApp)",
                            "ALWAYS" to "Confirm every device command",
                            "SAFE_ONLY" to "Execute immediately without asking"
                        ).forEach { (level, desc) ->
                            val isSelected = settings.confirmationLevel == level
                            androidx.compose.material3.Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp)),
                                color = if (isSelected) NeonCyan.copy(alpha = 0.15f) else DarkCosmicBg,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) NeonCyan else Color.Transparent
                                ),
                                onClick = { scope.launch { viewModel.repository.userPreferences.updateConfirmationLevel(level) } }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    androidx.compose.material3.RadioButton(
                                        selected = isSelected,
                                        onClick = { scope.launch { viewModel.repository.userPreferences.updateConfirmationLevel(level) } },
                                        colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = NeonCyan)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = NeonCyan,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = DarkCosmicBg,
                checkedTrackColor = NeonCyan
            )
        )
    }
}
