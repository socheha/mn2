package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val kodeBarang: String = "",
    val namaBarang: String,
    val stok: Int = 0,
    val stokTokoUtama: Int = 0,
    val stokTokoCabang: Int = 0,
    val hargaModal: Double = 0.0,
    val keterangan: String = "",
    val updatedAt: Long = System.currentTimeMillis()
) {
    val actualStokUtama: Int
        get() = if (stokTokoUtama == 0 && stokTokoCabang == 0 && stok > 0) stok else stokTokoUtama

    val actualStokCabang: Int
        get() = stokTokoCabang

    val totalStokCombined: Int
        get() = if (stokTokoUtama == 0 && stokTokoCabang == 0 && stok > 0) stok else (stokTokoUtama + stokTokoCabang)
}
