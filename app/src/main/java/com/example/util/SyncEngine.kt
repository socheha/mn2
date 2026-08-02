package com.example.util

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.dao.SyncQueueDao
import com.example.data.entity.SyncQueueEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

object SyncEngine {

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun setOnlineStatus(online: Boolean) {
        _isOnline.value = online
    }

    /**
     * Enqueue transaction payload for offline sync
     */
    suspend fun enqueueTransaction(
        syncQueueDao: SyncQueueDao,
        transactionType: String,
        summary: String,
        jsonPayload: String
    ): Long = withContext(Dispatchers.IO) {
        val entity = SyncQueueEntity(
            transactionType = transactionType,
            payloadSummary = summary,
            payloadJson = jsonPayload,
            createdAt = System.currentTimeMillis(),
            status = "PENDING"
        )
        syncQueueDao.insertSyncQueue(entity)
    }

    /**
     * Process pending offline sync items if online
     */
    suspend fun processPendingQueue(syncQueueDao: SyncQueueDao): Pair<Int, String> = withContext(Dispatchers.IO) {
        if (!_isOnline.value) {
            return@withContext Pair(0, "Aplikasi dalam mode Offline. Hubungkan internet untuk sinkronisasi.")
        }

        _isSyncing.value = true
        try {
            val pendingList = syncQueueDao.getPendingSyncQueueList()
            if (pendingList.isEmpty()) {
                _isSyncing.value = false
                return@withContext Pair(0, "Antrean sinkronisasi sudah bersih.")
            }

            var successCount = 0
            val now = System.currentTimeMillis()

            for (item in pendingList) {
                // Simulate/execute cloud/central database sync commit
                syncQueueDao.updateSyncStatus(
                    id = item.id,
                    status = "SYNCED",
                    syncedAt = now,
                    errorMessage = null
                )
                successCount++
            }

            _isSyncing.value = false
            Pair(successCount, "Berhasil men-sinkronkan $successCount transaksi ke database.")
        } catch (e: Exception) {
            _isSyncing.value = false
            Pair(0, "Gagal melakukan sinkronisasi: ${e.message}")
        }
    }
}
