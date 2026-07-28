package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_history")
data class StockHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemId: Long,
    val kodeBarang: String = "",
    val namaBarang: String,
    val jumlahPerubahan: Int, // e.g., +10 or -5
    val stokAwal: Int,
    val stokAkhir: Int,
    val jenis: String, // "Awal", "Barang Masuk", "Penjualan Harian", "Edit Stok", "Transfer Toko"
    val keterangan: String = "",
    val namaToko: String = "Toko Utama",
    val timestamp: Long = System.currentTimeMillis()
)
