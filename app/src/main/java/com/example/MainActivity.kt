package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.LyraViewModel
import com.example.ui.Screen
import com.example.ui.screens.ChatHistoryScreen
import com.example.ui.screens.MainScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.PrivacyCenterScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DarkCosmicBg
import com.example.ui.theme.LyraTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: LyraViewModel = viewModel()
            val settings by viewModel.settings.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()

            LaunchedEffect(Unit) {
                viewModel.errorMessage.collectLatest { error ->
                    Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                }
            }

            LyraTheme(darkTheme = settings.darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkCosmicBg
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "screen_transition"
                    ) { screen ->
                        when (screen) {
                            Screen.MAIN -> MainScreen(viewModel = viewModel)
                            Screen.HISTORY -> ChatHistoryScreen(viewModel = viewModel)
                            Screen.PRIVACY -> PrivacyCenterScreen(viewModel = viewModel)
                            Screen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                            Screen.MEMORY -> MemoryScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
