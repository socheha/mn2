package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.StockHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockHistoryDao {
    @Query("SELECT * FROM stock_history WHERE itemId = :itemId ORDER BY timestamp DESC")
    fun getHistoryByItem(itemId: Long): Flow<List<StockHistoryEntity>>

    @Query("SELECT * FROM stock_history ORDER BY timestamp DESC LIMIT 1000")
    fun getAllHistory(): Flow<List<StockHistoryEntity>>

    @Query("SELECT * FROM stock_history ORDER BY timestamp DESC")
    suspend fun getAllHistoryList(): List<StockHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: StockHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertHistoryDirect(history: StockHistoryEntity)

    @Query("DELETE FROM stock_history")
    suspend fun deleteAllHistory()
}
