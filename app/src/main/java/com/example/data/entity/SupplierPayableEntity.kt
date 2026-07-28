package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "supplier_payables")
data class SupplierPayableEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val namaSupplier: String,
    val nominalAwal: Double,
    val nominalSisa: Double,
    val tanggal: String, // YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    val catatan: String = "",
    val status: String = "Belum Lunas", // "Belum Lunas" or "Lunas"
    val incomingTransactionId: Long? = null
)
