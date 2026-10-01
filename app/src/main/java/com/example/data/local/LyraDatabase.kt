package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.ConversationEntity
import com.example.model.MemoryEntity
import com.example.model.MessageEntity

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        MemoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LyraDatabase : RoomDatabase() {
    abstract fun lyraDao(): LyraDao

    companion object {
        @Volatile
        private var INSTANCE: LyraDatabase? = null

        fun getInstance(context: Context): LyraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LyraDatabase::class.java,
                    "lyra_ai_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
