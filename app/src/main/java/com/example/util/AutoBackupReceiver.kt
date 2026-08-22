package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutoBackupReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val action = intent.action
                if (action == Intent.ACTION_MY_PACKAGE_REPLACED) {
                    val db = AppDatabase.getDatabase(context)
                    val items = db.itemDao().getAllItemsList()
                    if (items.isEmpty()) {
                        // After update, if database was empty, auto-recover from snapshot
                        AppBackupUtils.restoreFromLatestAutoSnapshot(context, db)
                    } else {
                        // After update, immediately refresh safe snapshot
                        AppBackupUtils.saveContinuousSnapshot(context, db)
                    }
                    AutoBackupManager.scheduleDailyMidnightAlarm(context)
                } else if (action == AutoBackupManager.ACTION_AUTO_BACKUP ||
                    action == Intent.ACTION_BOOT_COMPLETED
                ) {
                    val db = AppDatabase.getDatabase(context)
                    if (AutoBackupManager.isAutoBackupEnabled(context)) {
                        AutoBackupManager.performAutoBackup(context, db)
                    }
                    // Re-arm midnight alarm for next 24 hour cycle
                    AutoBackupManager.scheduleDailyMidnightAlarm(context)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
