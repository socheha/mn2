package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.IncomingItemEntity
import com.example.data.entity.IncomingTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: IncomingTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTransactionDirect(transaction: IncomingTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<IncomingItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertItemDirect(item: IncomingItemEntity)

    @Query("SELECT * FROM incoming_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<IncomingTransactionEntity>>

    @Query("SELECT * FROM incoming_transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactionsList(): List<IncomingTransactionEntity>

    @Query("SELECT * FROM incoming_items WHERE transactionId = :transactionId")
    suspend fun getItemsForTransaction(transactionId: Long): List<IncomingItemEntity>

    @Query("SELECT * FROM incoming_transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): IncomingTransactionEntity?

    @Query("DELETE FROM incoming_transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("DELETE FROM incoming_items WHERE transactionId = :transactionId")
    suspend fun deleteItemsForTransaction(transactionId: Long)
}
