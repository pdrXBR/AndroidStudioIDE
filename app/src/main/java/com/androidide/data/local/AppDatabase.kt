package com.androidide.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.androidide.data.local.dao.ChatHistoryDao
import com.androidide.data.local.dao.RecentFileDao
import com.androidide.data.local.entity.ChatHistoryEntity
import com.androidide.data.local.entity.RecentFileEntity

@Database(
    entities = [RecentFileEntity::class, ChatHistoryEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recentFileDao(): RecentFileDao
    abstract fun chatHistoryDao(): ChatHistoryDao
}
