package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY namaBarang ASC")
    fun getAllItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items ORDER BY namaBarang ASC")
    suspend fun getAllItemsList(): List<ItemEntity>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemById(id: Long): ItemEntity?

    @Query("SELECT * FROM items WHERE stok < 10 ORDER BY stok ASC")
    fun getLowStockItems(): Flow<List<ItemEntity>>

    @Query("SELECT COUNT(*) FROM items")
    fun getTotalItemTypes(): Flow<Int>

    @Query("SELECT COALESCE(SUM(stok), 0) FROM items")
    fun getTotalStockCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(CASE WHEN stokTokoUtama = 0 AND stokTokoCabang = 0 AND stok > 0 THEN stok ELSE stokTokoUtama END), 0) FROM items")
    fun getTotalStockUtama(): Flow<Int>

    @Query("SELECT COALESCE(SUM(stokTokoCabang), 0) FROM items")
    fun getTotalStockCabang(): Flow<Int>

    @Query("SELECT * FROM items WHERE kodeBarang LIKE '%' || :query || '%' OR namaBarang LIKE '%' || :query || '%' ORDER BY namaBarang ASC")
    fun searchItems(query: String): Flow<List<ItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertItemDirect(item: ItemEntity)

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Query("UPDATE items SET stok = stok + :delta, hargaModal = CASE WHEN :newHargaModal > 0 THEN :newHargaModal ELSE hargaModal END, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStockAndPrice(id: Long, delta: Int, newHargaModal: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE items SET stokTokoUtama = :stokUtama, stokTokoCabang = :stokCabang, stok = :totalStok, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStoreStocks(id: Long, stokUtama: Int, stokCabang: Int, totalStok: Int, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteItem(item: ItemEntity)

    @Query("DELETE FROM items")
    suspend fun deleteAllItems()

    @Query("SELECT COUNT(*) FROM items")
    suspend fun getItemCountDirect(): Int
}
