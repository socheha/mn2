package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.SupplierPayableEntity
import com.example.data.entity.SupplierPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierPayableDao {
    @Query("SELECT * FROM supplier_payables ORDER BY tanggal DESC, timestamp DESC, id DESC")
    fun getAllPayables(): Flow<List<SupplierPayableEntity>>

    @Query("SELECT * FROM supplier_payables ORDER BY tanggal DESC, timestamp DESC, id DESC")
    suspend fun getAllPayablesList(): List<SupplierPayableEntity>

    @Query("SELECT * FROM supplier_payments WHERE hutangId = :hutangId ORDER BY tanggal DESC, timestamp DESC, id DESC")
    suspend fun getPaymentsByPayableList(hutangId: Long): List<SupplierPaymentEntity>

    @Query("SELECT * FROM supplier_payments ORDER BY tanggal DESC, timestamp DESC, id DESC")
    suspend fun getAllSupplierPaymentsList(): List<SupplierPaymentEntity>

    @Query("SELECT * FROM supplier_payables WHERE status = 'Belum Lunas' ORDER BY tanggal DESC, timestamp DESC, id DESC")
    fun getUnpaidPayables(): Flow<List<SupplierPayableEntity>>

    @Query("SELECT COALESCE(SUM(nominalSisa), 0.0) FROM supplier_payables WHERE status = 'Belum Lunas'")
    fun getTotalUnpaidPayables(): Flow<Double>

    @Query("SELECT * FROM supplier_payables WHERE id = :id")
    suspend fun getPayableById(id: Long): SupplierPayableEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayable(payable: SupplierPayableEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPayableDirect(payable: SupplierPayableEntity)

    @Update
    suspend fun updatePayable(payable: SupplierPayableEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: SupplierPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPaymentDirect(payment: SupplierPaymentEntity)

    @Query("SELECT * FROM supplier_payments WHERE hutangId = :hutangId ORDER BY tanggal DESC, timestamp DESC, id DESC")
    fun getPaymentsByPayable(hutangId: Long): Flow<List<SupplierPaymentEntity>>

    @Query("SELECT * FROM supplier_payments WHERE id = :paymentId")
    suspend fun getPaymentById(paymentId: Long): SupplierPaymentEntity?

    @Query("DELETE FROM supplier_payments WHERE id = :paymentId")
    suspend fun deletePayment(paymentId: Long)

    @Query("DELETE FROM supplier_payables WHERE id = :id")
    suspend fun deletePayable(id: Long)

    @Query("DELETE FROM supplier_payments WHERE hutangId = :hutangId")
    suspend fun deletePaymentsByPayable(hutangId: Long)
}
