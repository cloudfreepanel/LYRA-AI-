package com.example.data.repository

import com.example.data.local.LyraDao
import com.example.data.preferences.AppSettings
import com.example.data.preferences.UserPreferences
import com.example.model.ConversationEntity
import com.example.model.MemoryEntity
import com.example.model.MessageEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class LyraRepository(
    private val dao: LyraDao,
    private val preferences: UserPreferences
) {
    // Preferences Flow
    val settings: Flow<AppSettings> = preferences.settingsFlow
    val userPreferences: UserPreferences = preferences

    // Conversations
    val allConversations: Flow<List<ConversationEntity>> = dao.getAllConversations()

    suspend fun getOrCreateCurrentConversationId(): String {
        return UUID.randomUUID().toString()
    }

    suspend fun insertConversation(title: String): String {
        val id = UUID.randomUUID().toString()
        val conv = ConversationEntity(
            id = id,
            title = title
        )
        dao.insertConversation(conv)
        return id
    }

    suspend fun ensureConversationExists(id: String, defaultTitle: String = "Chat with LYRA") {
        val existing = dao.getConversationById(id)
        if (existing == null) {
            dao.insertConversation(
                ConversationEntity(
                    id = id,
                    title = defaultTitle
                )
            )
        }
    }

    suspend fun updateConversationTitle(id: String, title: String) {
        dao.updateConversationTitle(id, title)
    }

    suspend fun deleteConversation(id: String) {
        dao.deleteConversationById(id)
    }

    suspend fun deleteAllConversations() {
        dao.deleteAllConversations()
    }

    // Messages
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>> {
        return dao.getMessagesForConversation(conversationId)
    }

    suspend fun getRecentMessages(conversationId: String): List<MessageEntity> {
        return dao.getMessagesList(conversationId)
    }

    suspend fun saveMessage(
        conversationId: String,
        role: String,
        content: String,
        imageUri: String? = null,
        intentType: String? = null,
        emotionCue: String? = null
    ): MessageEntity {
        val message = MessageEntity(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            role = role,
            content = content,
            imageUri = imageUri,
            intentType = intentType,
            emotionCue = emotionCue
        )
        dao.insertMessage(message)
        // Also update conversation timestamp
        dao.updateConversationTitle(conversationId, content.take(30) + if (content.length > 30) "..." else "")
        return message
    }

    suspend fun updateMessage(message: MessageEntity) {
        dao.updateMessage(message)
    }

    suspend fun deleteMessage(messageId: String) {
        dao.deleteMessageById(messageId)
    }

    fun searchMessages(query: String): Flow<List<MessageEntity>> {
        return dao.searchMessages(query)
    }

    // Memory
    val allMemories: Flow<List<MemoryEntity>> = dao.getAllMemories()

    suspend fun getMemoriesList(): List<MemoryEntity> = dao.getMemoriesList()

    suspend fun addMemory(key: String, value: String, category: String = "preference") {
        val existing = dao.getMemoryByKey(key)
        val memory = MemoryEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            key = key,
            value = value,
            category = category
        )
        dao.insertMemory(memory)
    }

    suspend fun deleteMemory(id: String) {
        dao.deleteMemoryById(id)
    }

    suspend fun clearAllMemories() {
        dao.deleteAllMemories()
    }

    suspend fun clearAllLocalData() {
        dao.deleteAllConversations()
        dao.deleteAllMemories()
    }
}
