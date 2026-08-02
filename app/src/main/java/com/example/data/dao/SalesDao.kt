package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.SalesItemEntity
import com.example.data.entity.SalesTransactionEntity
import kotlinx.coroutines.flow.Flow

data class SoldItemSummary(
    val itemId: Long,
    val kodeBarang: String,
    val namaBarang: String,
    val totalJumlahTerjual: Int,
    val totalNilai: Double
)

@Dao
interface SalesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: SalesTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTransactionDirect(transaction: SalesTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<SalesItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertItemDirect(item: SalesItemEntity)

    @Query("SELECT * FROM sales_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<SalesTransactionEntity>>

    @Query("SELECT * FROM sales_transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsList(): List<SalesTransactionEntity>

    @Query("SELECT COALESCE(SUM(totalUangPenjualan), 0.0) FROM sales_transactions WHERE tanggal = :todayDate")
    fun getRevenueForDate(todayDate: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(keuntungan), 0.0) FROM sales_transactions WHERE tanggal = :todayDate")
    fun getProfitForDate(todayDate: String): Flow<Double>

    @Query("SELECT COUNT(*) FROM sales_transactions WHERE tanggal = :todayDate")
    fun getTransactionCountForDate(todayDate: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(totalUangPenjualan), 0.0) FROM sales_transactions WHERE tanggal >= :startDate AND tanggal <= :endDate")
    fun getRevenueBetweenDates(startDate: String, endDate: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(keuntungan), 0.0) FROM sales_transactions WHERE tanggal >= :startDate AND tanggal <= :endDate")
    fun getProfitBetweenDates(startDate: String, endDate: String): Flow<Double>

    @Query("SELECT * FROM sales_transactions WHERE tanggal >= :startDate AND tanggal <= :endDate ORDER BY timestamp DESC")
    fun getTransactionsBetweenDates(startDate: String, endDate: String): Flow<List<SalesTransactionEntity>>

    @Query("SELECT itemId, kodeBarang, namaBarang, SUM(jumlahTerjual) as totalJumlahTerjual, SUM(totalHarga) as totalNilai FROM sales_items WHERE transactionId IN (SELECT id FROM sales_transactions WHERE tanggal >= :startDate AND tanggal <= :endDate) GROUP BY itemId, kodeBarang, namaBarang ORDER BY totalJumlahTerjual DESC")
    fun getSoldItemsSummaryBetweenDates(startDate: String, endDate: String): Flow<List<SoldItemSummary>>

    @Query("SELECT * FROM sales_items WHERE transactionId = :transactionId")
    suspend fun getItemsForTransaction(transactionId: Long): List<SalesItemEntity>

    @Query("SELECT * FROM sales_transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): SalesTransactionEntity?

    @Query("DELETE FROM sales_transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("DELETE FROM sales_items WHERE transactionId = :transactionId")
    suspend fun deleteItemsForTransaction(transactionId: Long)
}
