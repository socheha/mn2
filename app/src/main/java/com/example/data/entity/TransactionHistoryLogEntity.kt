package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transaction_history_logs")
data class TransactionHistoryLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val tanggal: String, // YYYY-MM-DD
    val transactionType: String, // "Barang Masuk", "Penjualan", "Hutang Supplier", "Piutang Pelanggan", "Kas & Bank", "Pelunasan"
    val transactionId: Long,
    val referenceNumber: String = "", // e.g. "BM-#12", "PJ-#45", "Faktur-101"
    val actionType: String, // "CREATED", "STATUS_CHANGE", "UPDATED", "CANCELLED", "SYNC_BALANCE"
    val previousStatus: String = "", // "Pending", "Belum Lunas", "Tunai", "Aktif", "Draft", "-"
    val newStatus: String = "", // "Completed", "Lunas", "Dibatalkan", "Diperbarui", "Sinkron"
    val nominal: Double = 0.0,
    val accountType: String = "", // "TUNAI", "BANK", "BCA", etc.
    val balanceBefore: Double = 0.0,
    val balanceAfter: Double = 0.0,
    val keterangan: String = "",
    val syncStatus: String = "SYNCED" // "SYNCED", "PENDING", "REVERTED"
)
