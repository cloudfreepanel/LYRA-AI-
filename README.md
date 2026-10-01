# LYRA AI — "Your Intelligent Mobile Companion"

LYRA is a futuristic female AI mobile assistant built natively for Android using Kotlin and Jetpack Compose. She communicates naturally through voice and text, understands multiple languages, analyzes images, executes permitted Android device actions, works offline for core features, and uses Gemini 3.5 Flash for advanced online tasks.

---

## Key Features & Architecture

### 1. App Identity & Futuristic UI
- **Name:** LYRA AI ("Your Intelligent Mobile Companion")
- **Wake Phrase:** “Hey Lyra” (and localized equivalents)
- **Personality:** Female AI assistant: warm, calm, intelligent, respectful, emotion-aware, concise for quick commands, and detailed for explanations. Never fabricates actions.
- **Visuals:** Obsidian dark cosmic theme, neon cyan and electric violet accents, glassmorphic cards, and an interactive **Animated Glowing AI Orb** with real-time audio reactivity.
- **States:**
  - ✦ Idle Breathing Pulse
  - ◉ Real-Time Audio-Reactive Listening Wave
  - ✺ Swirling Processing Vortex
  - ✦ Luminous Speaking Wave

---

### 2. Intelligent AI Router
```
User Input
   ↓
Language Detection (English, Hindi, Bengali, Assamese, Urdu)
   ↓
Wake Word Check ("Hey Lyra")
   ↓
Intent Normalization
   ↓
Is it a device command? (Torch, Volume, Camera, Battery, Settings, Media, Timer, Alarm)
   ├── YES → Android Command Engine
   │
   └── NO
        ↓
Is it an image request?
   ├── YES → Vision Engine (Gemini Multimodal)
   │
   └── NO
        ↓
Is network available & Gemini API configured?
   ├── YES → Online Gemini AI (Streaming response)
   │
   └── NO  → Local On-Device AI
```

---

### 3. Modular Engine System

- **Intent Classifier (`engine/intent`):** Normalized cross-language intent extraction for:
  - `TORCH_ON` / `TORCH_OFF`
  - `CALL_CONTACT`
  - `WHATSAPP_CONTACT`
  - `OPEN_APP`
  - `OPEN_SETTINGS`
  - `CAMERA`
  - `VOLUME_UP` / `VOLUME_DOWN` / `VOLUME_MUTE`
  - `BRIGHTNESS_SET`
  - `MEDIA_PLAY` / `MEDIA_PAUSE` / `MEDIA_NEXT` / `MEDIA_PREV`
  - `SET_TIMER` / `SET_ALARM`
  - `BATTERY_STATUS` / `DEVICE_STATUS`
  - `IMAGE_ANALYSIS` / `GENERAL_CHAT`

- **Multilingual Support (`engine/language`):** Automatic script and vocabulary detection for:
  - English
  - Hindi (हिन्दी)
  - Bengali (বাংলা)
  - Assamese (অসমীয়া)
  - Urdu (اردو)

- **Voice & Speech (`engine/voice`):**
  - Android `SpeechRecognizer` with real-time RMS dB tracking
  - Android `TextToSpeech` with female voice filtering, customizable pitch, speed, and immediate interrupt capability
  - `WakeWordEngine` for privacy-first, on-device trigger detection

- **Device Control (`engine/device`):**
  - Flashlight control via `CameraManager`
  - Volume manipulation via `AudioManager`
  - Battery telemetry and power saver state via `BatteryManager`
  - Settings shortcuts (`Settings.ACTION_*`)
  - Alarms and Timers via Android `AlarmClock`

- **Contacts & Communications (`engine/contact` & `engine/communication`):**
  - Disambiguation: if multiple contacts match (e.g. "Which Rahul? Rahul Kumar or Rahul Das?"), asks user before proceeding.
  - Calling: Initiates direct call when `CALL_PHONE` permission is granted and confirmed, or opens the system dialer.
  - WhatsApp: Prepares and opens WhatsApp conversation with prefilled text, honestly communicating that manual tap to send is required by WhatsApp's security model.

- **Emotion Engine (`engine/emotion`):**
  - Identifies conversational cues (Happy, Sad, Angry, Worried, Excited, Confused, Tired, Neutral)
  - Adapts tone and empathy gracefully (can be disabled in Settings)

- **Memory & Storage (`data/local` & `data/preferences`):**
  - Local SQLite database using Room with Kotlin Symbol Processing (KSP)
  - Chat history with search, message editing, deletion, and conversation switching
  - On-device User Memory for personalized preferences
  - DataStore Preferences for all AI, voice, and confirmation toggles

- **Privacy Center (`ui/screens/PrivacyCenterScreen`):**
  - Live inspection of Microphone, Camera, Contacts, Calling, and Notification permissions
  - Shortcuts to Android System Settings
  - Quick action to "Clear Assistant Memory" and "Delete All Local Data"

---

## Gemini API Configuration
The app uses `gemini-3.5-flash` for advanced text reasoning and multimodal vision analysis:
1. Open the **Secrets panel in Google AI Studio**.
2. Add your key as `GEMINI_API_KEY`.
3. The Secrets Gradle Plugin automatically injects it into `BuildConfig.GEMINI_API_KEY` via `.env`.
