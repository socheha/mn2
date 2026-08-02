package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AutoBackupManager {

    private const val PREFS_NAME = "smartstock_autobackup_prefs"
    private const val KEY_ENABLED = "auto_backup_enabled"
    private const val KEY_LAST_BACKUP = "last_auto_backup_timestamp"
    private const val ONE_WEEK_MS = 7 * 24 * 60 * 60 * 1000L

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isAutoBackupEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ENABLED, true)
    }

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getLastBackupTimestamp(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_BACKUP, 0L)
    }

    private fun setLastBackupTimestamp(context: Context, time: Long) {
        getPrefs(context).edit().putLong(KEY_LAST_BACKUP, time).apply()
    }

    /**
     * Directory on local device storage where automatic backups are saved.
     */
    fun getAutoBackupDirectory(context: Context): File {
        val dir = File(context.filesDir, "auto_backups")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Checks if a week has passed since last backup. If enabled and due, performs automatic backup.
     */
    suspend fun checkAndPerformWeeklyAutoBackup(context: Context, db: AppDatabase): String? = withContext(Dispatchers.IO) {
        if (!isAutoBackupEnabled(context)) return@withContext null

        val lastBackup = getLastBackupTimestamp(context)
        val now = System.currentTimeMillis()

        if (now - lastBackup >= ONE_WEEK_MS || lastBackup == 0L) {
            return@withContext performAutoBackup(context, db)
        }
        return@withContext null
    }

    /**
     * Immediately triggers an automatic backup execution and saves locally on the phone.
     */
    suspend fun performAutoBackup(context: Context, db: AppDatabase): String = withContext(Dispatchers.IO) {
        try {
            val jsonStr = AppBackupUtils.exportDatabaseToJson(db)
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "AutoBackup_SmartStock_$dateStr.json"

            // Save to app local storage directory
            val autoBackupDir = getAutoBackupDirectory(context)
            val backupFile = File(autoBackupDir, fileName)

            FileOutputStream(backupFile).use { os ->
                os.write(jsonStr.toByteArray(Charsets.UTF_8))
            }

            // Also try to save a copy in public Downloads/SmartStock_AutoBackup
            try {
                AppBackupUtils.saveJsonBackupToDownloads(context, jsonStr)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            setLastBackupTimestamp(context, System.currentTimeMillis())
            cleanOldBackups(context)

            "Backup otomatis mingguan berhasil disimpan: $fileName"
        } catch (e: Exception) {
            e.printStackTrace()
            "Gagal melakukan backup otomatis mingguan: ${e.message}"
        }
    }

    /**
     * Keeps the list of local auto-backups managed, deleting files older than the last 10 backups.
     */
    private fun cleanOldBackups(context: Context) {
        val dir = getAutoBackupDirectory(context)
        val files = dir.listFiles { _, name -> name.startsWith("AutoBackup_") && name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() } ?: return

        if (files.size > 10) {
            for (i in 10 until files.size) {
                files[i].delete()
            }
        }
    }

    /**
     * Get list of locally stored auto-backup files.
     */
    fun getLocalAutoBackupFiles(context: Context): List<File> {
        val dir = getAutoBackupDirectory(context)
        return dir.listFiles { _, name -> name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}
