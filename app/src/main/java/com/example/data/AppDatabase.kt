package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.CashDao
import com.example.data.dao.CustomerReceivableDao
import com.example.data.dao.IncomingDao
import com.example.data.dao.ItemDao
import com.example.data.dao.SalesDao
import com.example.data.dao.StockHistoryDao
import com.example.data.dao.SupplierPayableDao
import com.example.data.entity.CashAccountEntity
import com.example.data.entity.CashMutationEntity
import com.example.data.entity.CustomerPaymentEntity
import com.example.data.entity.CustomerReceivableEntity
import com.example.data.entity.IncomingItemEntity
import com.example.data.entity.IncomingTransactionEntity
import com.example.data.entity.ItemEntity
import com.example.data.entity.SalesItemEntity
import com.example.data.entity.SalesTransactionEntity
import com.example.data.entity.StockHistoryEntity
import com.example.data.entity.SupplierPayableEntity
import com.example.data.entity.SupplierPaymentEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ItemEntity::class,
        StockHistoryEntity::class,
        IncomingTransactionEntity::class,
        IncomingItemEntity::class,
        SalesTransactionEntity::class,
        SalesItemEntity::class,
        CustomerReceivableEntity::class,
        CustomerPaymentEntity::class,
        SupplierPayableEntity::class,
        SupplierPaymentEntity::class,
        CashAccountEntity::class,
        CashMutationEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao
    abstract fun stockHistoryDao(): StockHistoryDao
    abstract fun incomingDao(): IncomingDao
    abstract fun salesDao(): SalesDao
    abstract fun customerReceivableDao(): CustomerReceivableDao
    abstract fun supplierPayableDao(): SupplierPayableDao
    abstract fun cashDao(): CashDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            val appContext = context.applicationContext
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    "me_smartstock_db"
                )
                .addCallback(DatabaseCallback())
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Database dikosongkan sebelum install sesuai permintaan pengguna
            }
        }
    }
}
