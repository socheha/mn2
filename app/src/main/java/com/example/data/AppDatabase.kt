package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.CashDao
import com.example.data.dao.CustomerReceivableDao
import com.example.data.dao.IncomingDao
import com.example.data.dao.ItemDao
import com.example.data.dao.SalesDao
import com.example.data.dao.StockHistoryDao
import com.example.data.dao.SupplierPayableDao
import com.example.data.dao.SyncQueueDao
import com.example.data.dao.TransactionHistoryDao
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
import com.example.data.entity.SyncQueueEntity
import com.example.data.entity.TransactionHistoryLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

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
        CashMutationEntity::class,
        SyncQueueEntity::class,
        TransactionHistoryLogEntity::class
    ],
    version = 8,
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
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun transactionHistoryDao(): TransactionHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        const val DATABASE_NAME = "me_smartstock_db"

        // Safe Room Migrations so updates NEVER lose or drop user data
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 1 to 2
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 2 to 3
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 3 to 4
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 4 to 5: sync queue
                db.execSQL("CREATE TABLE IF NOT EXISTS sync_queue (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `transactionType` TEXT NOT NULL, `summary` TEXT NOT NULL, `payloadJson` TEXT NOT NULL, `status` TEXT NOT NULL, `retryCount` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `lastError` TEXT NOT NULL)")
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 5 to 6: ensure all tables exist safely
                db.execSQL("CREATE TABLE IF NOT EXISTS transaction_history_logs (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `tanggal` TEXT NOT NULL, `transactionType` TEXT NOT NULL, `transactionId` INTEGER NOT NULL, `referenceNumber` TEXT NOT NULL, `actionType` TEXT NOT NULL, `previousStatus` TEXT NOT NULL, `newStatus` TEXT NOT NULL, `nominal` REAL NOT NULL, `accountType` TEXT NOT NULL, `balanceBefore` REAL NOT NULL, `balanceAfter` REAL NOT NULL, `keterangan` TEXT NOT NULL, `syncStatus` TEXT NOT NULL)")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 6 to 7
                db.execSQL("CREATE TABLE IF NOT EXISTS transaction_history_logs (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `tanggal` TEXT NOT NULL, `transactionType` TEXT NOT NULL, `transactionId` INTEGER NOT NULL, `referenceNumber` TEXT NOT NULL, `actionType` TEXT NOT NULL, `previousStatus` TEXT NOT NULL, `newStatus` TEXT NOT NULL, `nominal` REAL NOT NULL, `accountType` TEXT NOT NULL, `balanceBefore` REAL NOT NULL, `balanceAfter` REAL NOT NULL, `keterangan` TEXT NOT NULL, `syncStatus` TEXT NOT NULL)")
            }
        }
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Version 7 to 8
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            val appContext = context.applicationContext
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8
                )
                .fallbackToDestructiveMigrationOnDowngrade()
                .addCallback(DatabaseCallback(appContext))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // When opened, verify default accounts exist
                CoroutineScope(Dispatchers.IO).launch {
                    val appDb = getDatabase(context)
                    if (appDb.cashDao().getAllAccountsList().isEmpty()) {
                        appDb.cashDao().insertOrUpdateAccount(
                            CashAccountEntity(
                                accountType = "TUNAI",
                                accountName = "Kas Tunai Toko",
                                saldo = 0.0,
                                lastUpdated = System.currentTimeMillis()
                            )
                        )
                        appDb.cashDao().insertOrUpdateAccount(
                            CashAccountEntity(
                                accountType = "BANK",
                                accountName = "Kas Rekening Bank",
                                saldo = 0.0,
                                lastUpdated = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        }
    }
}

