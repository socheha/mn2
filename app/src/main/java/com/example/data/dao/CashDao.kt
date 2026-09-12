package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.CashAccountEntity
import com.example.data.entity.CashMutationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CashDao {
    @Query("SELECT * FROM cash_accounts")
    fun getAllAccounts(): Flow<List<CashAccountEntity>>

    @Query("SELECT * FROM cash_accounts")
    suspend fun getAllAccountsList(): List<CashAccountEntity>

    @Query("SELECT * FROM cash_accounts WHERE accountType = :accountType")
    suspend fun getAccountDirect(accountType: String): CashAccountEntity?

    @Query("SELECT COALESCE(SUM(saldo), 0.0) FROM cash_accounts")
    fun getTotalCashBalance(): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAccount(account: CashAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMutation(mutation: CashMutationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMutationDirect(mutation: CashMutationEntity)

    @Query("DELETE FROM cash_accounts WHERE accountType = :accountType")
    suspend fun deleteAccount(accountType: String)

    @Query("DELETE FROM cash_accounts WHERE accountType != 'TUNAI'")
    suspend fun deleteAllBankAccounts()

    @Query("SELECT * FROM cash_mutations ORDER BY tanggal DESC, timestamp DESC, id DESC")
    fun getAllMutations(): Flow<List<CashMutationEntity>>

    @Query("SELECT * FROM cash_mutations ORDER BY tanggal DESC, timestamp DESC, id DESC")
    suspend fun getAllMutationsList(): List<CashMutationEntity>

    @Query("SELECT * FROM cash_mutations WHERE accountType = :accountType ORDER BY tanggal DESC, timestamp DESC, id DESC")
    fun getMutationsByAccount(accountType: String): Flow<List<CashMutationEntity>>

    @Query("DELETE FROM cash_accounts")
    suspend fun deleteAllCashAccounts()

    @Query("DELETE FROM cash_mutations")
    suspend fun deleteAllCashMutations()

    @Query("SELECT * FROM cash_mutations WHERE id = :id")
    suspend fun getMutationById(id: Long): CashMutationEntity?

    @Query("DELETE FROM cash_mutations WHERE accountType = 'TUNAI'")
    suspend fun deleteAllTunaiMutations()

    @Query("DELETE FROM cash_mutations WHERE accountType != 'TUNAI' OR kategori LIKE '%Transfer%' OR keterangan LIKE '%Transfer%'")
    suspend fun deleteAllBankAndTransferMutations()

    @Query("DELETE FROM cash_mutations WHERE kategori LIKE '%Transfer%' OR keterangan LIKE '%Transfer%'")
    suspend fun deleteAllTransferOnlyMutations()

    @Query("DELETE FROM cash_mutations WHERE accountType = :accountType")
    suspend fun deleteMutationsByAccount(accountType: String)

    @Query("DELETE FROM cash_mutations WHERE id = :id")
    suspend fun deleteMutation(id: Long)
}
