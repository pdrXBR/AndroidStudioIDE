package com.androidide.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_files")
data class RecentFileEntity(
    @PrimaryKey val path: String,
    val name: String,
    val lastOpenedAt: Long = System.currentTimeMillis(),
    val cursorLine: Int = 0,
    val cursorColumn: Int = 0,
    val isWorkspace: Boolean = false
)
