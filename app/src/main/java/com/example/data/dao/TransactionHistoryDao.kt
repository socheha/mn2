package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.TransactionHistoryLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionHistoryDao {
    @Query("SELECT * FROM transaction_history_logs ORDER BY tanggal DESC, timestamp DESC, id DESC")
    fun getAllLogs(): Flow<List<TransactionHistoryLogEntity>>

    @Query("SELECT * FROM transaction_history_logs ORDER BY tanggal DESC, timestamp DESC, id DESC")
    suspend fun getAllLogsList(): List<TransactionHistoryLogEntity>

    @Query("SELECT * FROM transaction_history_logs WHERE transactionType = :type AND transactionId = :txId ORDER BY tanggal DESC, timestamp DESC, id DESC")
    fun getLogsForTransaction(type: String, txId: Long): Flow<List<TransactionHistoryLogEntity>>

    @Query("SELECT * FROM transaction_history_logs WHERE tanggal >= :startDate AND tanggal <= :endDate ORDER BY tanggal DESC, timestamp DESC, id DESC")
    fun getLogsBetweenDates(startDate: String, endDate: String): Flow<List<TransactionHistoryLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TransactionHistoryLogEntity): Long

    @Query("DELETE FROM transaction_history_logs WHERE id = :id")
    suspend fun deleteLog(id: Long)

    @Query("DELETE FROM transaction_history_logs WHERE transactionType = :type AND transactionId = :txId")
    suspend fun deleteLogsByTransaction(type: String, txId: Long)

    @Query("DELETE FROM transaction_history_logs")
    suspend fun deleteAllLogs()
}
