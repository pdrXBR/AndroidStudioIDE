package com.androidide.data.local.dao

import androidx.room.*
import com.androidide.data.local.entity.RecentFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentFileDao {
    @Query("SELECT * FROM recent_files ORDER BY lastOpenedAt DESC LIMIT 20")
    fun getRecentFiles(): Flow<List<RecentFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(file: RecentFileEntity)

    @Query("DELETE FROM recent_files WHERE path = :path")
    suspend fun delete(path: String)

    @Query("DELETE FROM recent_files WHERE lastOpenedAt < :before")
    suspend fun deleteOlderThan(before: Long)
}
