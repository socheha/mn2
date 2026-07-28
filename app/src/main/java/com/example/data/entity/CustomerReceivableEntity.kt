package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customer_receivables")
data class CustomerReceivableEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val namaPelanggan: String,
    val nomorHp: String = "",
    val nominalAwal: Double,
    val nominalSisa: Double,
    val tanggal: String, // YYYY-MM-DD
    val jatuhTempo: String = "", // YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    val catatan: String = "",
    val status: String = "Belum Lunas" // "Belum Lunas" or "Lunas"
)
