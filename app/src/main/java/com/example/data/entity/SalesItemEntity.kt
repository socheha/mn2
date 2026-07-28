package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sales_items")
data class SalesItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: Long,
    val itemId: Long,
    val kodeBarang: String = "",
    val namaBarang: String,
    val jumlahTerjual: Int,
    val hargaSatuan: Double = 0.0,
    val totalHarga: Double = 0.0
)
