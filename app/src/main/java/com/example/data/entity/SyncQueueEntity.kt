package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionType: String, // "SALES", "INCOMING", "CASH", "RECEIVABLE", "PAYABLE", "STOCK_UPDATE"
    val payloadSummary: String, // e.g., "Penjualan #102 - Rp 150.000"
    val payloadJson: String,    // Detailed JSON payload
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // "PENDING", "SYNCED", "FAILED"
    val syncedAt: Long? = null,
    val errorMessage: String? = null
)
