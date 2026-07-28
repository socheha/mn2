package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales_transactions")
data class SalesTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tanggal: String, // YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    val totalUangPenjualan: Double = 0.0,
    val totalModal: Double = 0.0,
    val keuntungan: Double = 0.0,
    val totalItemTerjual: Int = 0,
    val catatan: String = "",
    val metodePembayaran: String = "Tunai",
    val namaToko: String = "Toko Utama"
)
