package com.example.engine.contact

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

data class ContactMatch(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val photoUri: String? = null
)

sealed class ContactSearchResult {
    data class Single(val contact: ContactMatch) : ContactSearchResult()
    data class Multiple(val nameQuery: String, val matches: List<ContactMatch>) : ContactSearchResult()
    data class NotFound(val nameQuery: String) : ContactSearchResult()
    object PermissionRequired : ContactSearchResult()
}

class ContactEngine(private val context: Context) {

    fun hasContactPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Resolves a contact query string against device contacts.
     */
    fun findContact(nameQuery: String): ContactSearchResult {
        if (!hasContactPermission()) {
            return ContactSearchResult.PermissionRequired
        }

        val cleanQuery = nameQuery.trim()
        if (cleanQuery.isEmpty()) {
            return ContactSearchResult.NotFound(cleanQuery)
        }

        // If the query is already an explicit phone number, return it immediately as a Single match
        val digitsOnly = cleanQuery.replace(Regex("[^0-9+]"), "")
        if (digitsOnly.length >= 7 && (digitsOnly.startsWith("+") || cleanQuery.first().isDigit())) {
            return ContactSearchResult.Single(
                ContactMatch(
                    id = "direct_number",
                    name = cleanQuery,
                    phoneNumber = digitsOnly
                )
            )
        }

        val matches = mutableListOf<ContactMatch>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )

        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$cleanQuery%")

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.let {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                val seenNumbers = mutableSetOf<String>()

                while (it.moveToNext()) {
                    val id = if (idIndex >= 0) it.getString(idIndex) else ""
                    val name = if (nameIndex >= 0) it.getString(nameIndex) else "Unknown"
                    val number = if (numberIndex >= 0) it.getString(numberIndex) else ""
                    val photo = if (photoIndex >= 0) it.getString(photoIndex) else null

                    val normalizedNum = number.replace(Regex("[^0-9+]"), "")
                    if (normalizedNum.isNotEmpty() && !seenNumbers.contains(normalizedNum)) {
                        seenNumbers.add(normalizedNum)
                        matches.add(ContactMatch(id, name, number, photo))
                    }
                }
            }
        } catch (e: Exception) {
            return ContactSearchResult.NotFound(cleanQuery)
        } finally {
            cursor?.close()
        }

        return when {
            matches.isEmpty() -> ContactSearchResult.NotFound(cleanQuery)
            matches.size == 1 -> ContactSearchResult.Single(matches[0])
            else -> {
                // If there's an exact case-insensitive match, check if it's uniquely exact
                val exactMatches = matches.filter { it.name.equals(cleanQuery, ignoreCase = true) }
                if (exactMatches.size == 1) {
                    ContactSearchResult.Single(exactMatches[0])
                } else {
                    ContactSearchResult.Multiple(cleanQuery, matches)
                }
            }
        }
    }
}
