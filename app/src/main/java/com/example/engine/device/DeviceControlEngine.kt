package com.example.engine.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.view.KeyEvent

data class DeviceActionResult(
    val success: Boolean,
    val message: String
)

data class BatteryInfo(
    val percentage: Int,
    val isCharging: Boolean,
    val isBatterySaverOn: Boolean
)

class DeviceControlEngine(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    private var isTorchOn = false

    fun setTorch(enabled: Boolean): DeviceActionResult {
        if (cameraManager == null) {
            return DeviceActionResult(false, "Camera hardware is unavailable on this device.")
        }

        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }

            if (cameraId == null) {
                return DeviceActionResult(false, "This device does not have a camera flash unit.")
            }

            cameraManager.setTorchMode(cameraId, enabled)
            isTorchOn = enabled
            val stateText = if (enabled) "on" else "off"
            return DeviceActionResult(true, "Done. Torch is $stateText.")
        } catch (e: CameraAccessException) {
            return DeviceActionResult(false, "Cannot access torch right now: ${e.localizedMessage}")
        } catch (e: Exception) {
            return DeviceActionResult(false, "Torch operation failed: ${e.localizedMessage}")
        }
    }

    fun adjustVolume(direction: Int): DeviceActionResult {
        if (audioManager == null) {
            return DeviceActionResult(false, "Audio service unavailable.")
        }
        val dir = if (direction > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir, AudioManager.FLAG_SHOW_UI)
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val percent = (currentVol * 100) / maxVol
        return DeviceActionResult(true, "Media volume adjusted to $percent%.")
    }

    fun muteVolume(): DeviceActionResult {
        if (audioManager == null) return DeviceActionResult(false, "Audio service unavailable.")
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
        return DeviceActionResult(true, "Media volume muted.")
    }

    fun getBatteryInfo(): BatteryInfo {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val percentage = if (level >= 0 && scale > 0) (level * 100) / scale else 0
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val isBatterySaverOn = powerManager?.isPowerSaveMode ?: false

        return BatteryInfo(percentage, isCharging, isBatterySaverOn)
    }

    fun getBatterySummary(): String {
        val info = getBatteryInfo()
        val chargingStatus = if (info.isCharging) "currently charging ⚡" else "not charging"
        val saverStatus = if (info.isBatterySaverOn) " Battery saver is enabled." else ""
        return "Battery is at ${info.percentage}%, and your phone is $chargingStatus.$saverStatus"
    }

    fun getDeviceStatusSummary(): String {
        val battery = getBatteryInfo()
        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val androidVersion = Build.VERSION.RELEASE
        return "Device: $manufacturer $model (Android $androidVersion)\nBattery: ${battery.percentage}% (${if (battery.isCharging) "Charging" else "On Battery"})\nBattery Saver: ${if (battery.isBatterySaverOn) "Active" else "Off"}"
    }

    fun openSettings(type: String): DeviceActionResult {
        val action = when (type.lowercase()) {
            "wifi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound" -> Settings.ACTION_SOUND_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }

        val intent = Intent(action).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            DeviceActionResult(true, "Opening $type settings.")
        } catch (e: Exception) {
            DeviceActionResult(false, "Could not open $type settings: ${e.localizedMessage}")
        }
    }

    fun openCamera(): DeviceActionResult {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            DeviceActionResult(true, "Opening camera.")
        } catch (e: Exception) {
            DeviceActionResult(false, "Failed to launch camera: ${e.localizedMessage}")
        }
    }

    fun openApp(appName: String, explicitPackage: String? = null): DeviceActionResult {
        val pm = context.packageManager
        val pkg = explicitPackage ?: when (appName.lowercase()) {
            "youtube" -> "com.google.android.youtube"
            "whatsapp" -> "com.whatsapp"
            "instagram" -> "com.instagram.android"
            else -> null
        }

        if (pkg != null) {
            val intent = pm.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return DeviceActionResult(true, "Opening $appName.")
            }
        }

        // Generic launch search
        val launchIntent = pm.getInstalledApplications(0).firstOrNull {
            it.packageName.contains(appName, ignoreCase = true) ||
                    pm.getApplicationLabel(it).toString().contains(appName, ignoreCase = true)
        }?.let { pm.getLaunchIntentForPackage(it.packageName) }

        if (launchIntent != null) {
            launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(launchIntent)
            return DeviceActionResult(true, "Opening $appName.")
        }

        return DeviceActionResult(false, "That app isn't installed on this device.")
    }

    fun setTimer(seconds: Int, label: String): DeviceActionResult {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            val minutes = seconds / 60
            val secRemaining = seconds % 60
            val durationText = if (minutes > 0) "$minutes minute" + if (minutes > 1) "s" else "" else "$secRemaining seconds"
            DeviceActionResult(true, "Timer set for $durationText.")
        } catch (e: Exception) {
            DeviceActionResult(false, "Timer could not be opened: ${e.localizedMessage}")
        }
    }

    fun setAlarm(hour: Int, minute: Int): DeviceActionResult {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Lyra Alarm")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return try {
            context.startActivity(intent)
            DeviceActionResult(true, "Alarm set for %02d:%02d.".format(hour, minute))
        } catch (e: Exception) {
            DeviceActionResult(false, "Could not set alarm: ${e.localizedMessage}")
        }
    }

    fun controlMedia(action: String): DeviceActionResult {
        if (audioManager == null) return DeviceActionResult(false, "Audio manager unavailable.")
        val keyCode = when (action.lowercase()) {
            "play" -> KeyEvent.KEYCODE_MEDIA_PLAY
            "pause" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "prev", "previous" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }

        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        return DeviceActionResult(true, "Media command sent.")
    }
}
