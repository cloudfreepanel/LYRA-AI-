package com.example.engine.communication

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import java.net.URLEncoder

sealed class CommunicationResult {
    data class Success(val message: String, val requiresUserAction: Boolean = false) : CommunicationResult()
    data class Failure(val reason: String) : CommunicationResult()
    data class PermissionRequired(val permission: String, val message: String) : CommunicationResult()
}

class CommunicationEngine(private val context: Context) {

    fun hasCallPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Initiates a phone call. If CALL_PHONE permission is present, performs direct call;
     * otherwise falls back to opening dialer with prefilled number.
     */
    fun initiateCall(phoneNumber: String, contactName: String): CommunicationResult {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (cleanNumber.isEmpty()) {
            return CommunicationResult.Failure("Invalid phone number.")
        }

        return if (hasCallPermission()) {
            try {
                val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanNumber")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(callIntent)
                CommunicationResult.Success("Calling $contactName ($cleanNumber)...")
            } catch (e: Exception) {
                // Fallback to dialer
                openDialer(cleanNumber, contactName)
            }
        } else {
            // Open dialer so user can tap call without risking permission failure
            openDialer(cleanNumber, contactName)
        }
    }

    fun openDialer(phoneNumber: String, contactName: String): CommunicationResult {
        return try {
            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(dialIntent)
            CommunicationResult.Success("Opening dialer with $contactName ($phoneNumber).", requiresUserAction = true)
        } catch (e: Exception) {
            CommunicationResult.Failure("Could not open phone dialer: ${e.localizedMessage}")
        }
    }

    /**
     * Prepares and opens WhatsApp conversation with prefilled message.
     * Honestly reports that user must tap send inside WhatsApp.
     */
    fun prepareWhatsAppMessage(phoneNumber: String?, contactName: String, messageText: String): CommunicationResult {
        val isWhatsAppInstalled = isPackageInstalled("com.whatsapp")
        if (!isWhatsAppInstalled) {
            return CommunicationResult.Failure("WhatsApp is not installed on this device.")
        }

        return try {
            val encodedMessage = URLEncoder.encode(messageText, "UTF-8")
            val cleanPhone = phoneNumber?.replace(Regex("[^0-9]"), "")

            val intent = if (!cleanPhone.isNullOrEmpty()) {
                Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage")
                    setPackage("com.whatsapp")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            } else {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, messageText)
                    setPackage("com.whatsapp")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            context.startActivity(intent)
            CommunicationResult.Success(
                message = "I opened WhatsApp for $contactName with your message ready. Please tap send to deliver it.",
                requiresUserAction = true
            )
        } catch (e: Exception) {
            CommunicationResult.Failure("Failed to open WhatsApp: ${e.localizedMessage}")
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
