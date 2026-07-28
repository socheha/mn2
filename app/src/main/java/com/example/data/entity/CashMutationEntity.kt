package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cash_mutations")
data class CashMutationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tanggal: String, // YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    val accountType: String, // "TUNAI" or "BANK"
    val jenis: String, // "MASUK", "KELUAR", "PENYESUAIAN"
    val nominal: Double,
    val saldoSesudah: Double = 0.0,
    val kategori: String, // "Penjualan Toko", "Pembelian Barang", "Bayar Piutang", "Bayar Hutang", "Kas Keluar", "Penyesuaian Saldo"
    val keterangan: String = ""
)
