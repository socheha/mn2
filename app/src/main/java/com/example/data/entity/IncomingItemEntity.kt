package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incoming_items")
data class IncomingItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transactionId: Long,
    val itemId: Long,
    val kodeBarang: String = "",
    val namaBarang: String,
    val jumlahMasuk: Int,
    val hargaModal: Double
)
