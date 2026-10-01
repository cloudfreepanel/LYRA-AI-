package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey
    val id: String,
    val key: String,
    val value: String,
    val category: String = "general", // "preference", "identity", "fact", "custom"
    val timestamp: Long = System.currentTimeMillis()
)
