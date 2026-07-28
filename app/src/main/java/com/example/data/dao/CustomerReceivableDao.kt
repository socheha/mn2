package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.CustomerPaymentEntity
import com.example.data.entity.CustomerReceivableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerReceivableDao {
    @Query("SELECT * FROM customer_receivables ORDER BY timestamp DESC")
    fun getAllReceivables(): Flow<List<CustomerReceivableEntity>>

    @Query("SELECT * FROM customer_receivables ORDER BY timestamp DESC")
    suspend fun getAllReceivablesList(): List<CustomerReceivableEntity>

    @Query("SELECT * FROM customer_payments WHERE piutangId = :piutangId ORDER BY timestamp DESC")
    suspend fun getPaymentsByReceivableList(piutangId: Long): List<CustomerPaymentEntity>

    @Query("SELECT * FROM customer_receivables WHERE status = 'Belum Lunas' ORDER BY timestamp DESC")
    fun getUnpaidReceivables(): Flow<List<CustomerReceivableEntity>>

    @Query("SELECT COALESCE(SUM(nominalSisa), 0.0) FROM customer_receivables WHERE status = 'Belum Lunas'")
    fun getTotalUnpaidReceivables(): Flow<Double>

    @Query("SELECT * FROM customer_receivables WHERE id = :id")
    suspend fun getReceivableById(id: Long): CustomerReceivableEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceivable(receivable: CustomerReceivableEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertReceivableDirect(receivable: CustomerReceivableEntity)

    @Update
    suspend fun updateReceivable(receivable: CustomerReceivableEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: CustomerPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPaymentDirect(payment: CustomerPaymentEntity)

    @Query("SELECT * FROM customer_payments WHERE piutangId = :piutangId ORDER BY timestamp DESC")
    fun getPaymentsByReceivable(piutangId: Long): Flow<List<CustomerPaymentEntity>>
}
