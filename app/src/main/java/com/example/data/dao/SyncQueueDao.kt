package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue ORDER BY id DESC")
    fun getAllSyncQueue(): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' ORDER BY id ASC")
    suspend fun getPendingSyncQueueList(): List<SyncQueueEntity>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
    fun getPendingCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncQueue(item: SyncQueueEntity): Long

    @Query("UPDATE sync_queue SET status = :status, syncedAt = :syncedAt, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: String, syncedAt: Long?, errorMessage: String?)

    @Query("DELETE FROM sync_queue WHERE status = 'SYNCED'")
    suspend fun clearSyncedQueue()

    @Query("DELETE FROM sync_queue")
    suspend fun clearAllQueue()
}
