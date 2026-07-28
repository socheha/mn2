package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incoming_transactions")
data class IncomingTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tanggal: String, // YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    val namaSupplier: String,
    val nomorFaktur: String = "",
    val catatan: String = "",
    val totalNilai: Double = 0.0,
    val totalItem: Int = 0,
    val statusPembayaran: String // "Tunai" or "Hutang"
)
