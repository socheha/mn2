package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customer_payments")
data class CustomerPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val piutangId: Long,
    val nominalBayar: Double,
    val tanggal: String, // YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    val catatan: String = "",
    val metodePembayaran: String = "Tunai"
)
