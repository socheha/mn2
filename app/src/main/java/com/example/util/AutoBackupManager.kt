package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import com.example.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AutoBackupManager {

    private const val PREFS_NAME = "smartstock_autobackup_prefs"
    private const val KEY_ENABLED = "auto_backup_enabled"
    private const val KEY_FREQUENCY = "auto_backup_frequency"
    private const val KEY_HOUR = "auto_backup_hour" // Default 0 (12 malam / midnight)
    private const val KEY_MINUTE = "auto_backup_minute" // Default 0
    private const val KEY_LAST_BACKUP = "last_auto_backup_timestamp"
    private const val KEY_LAST_STATUS = "last_auto_backup_status"

    const val FREQ_24_HOURS = "24_HOURS" // Setiap 24 Jam (Jam 12 Malam / 00:00)
    const val FREQ_12_HOURS = "12_HOURS" // Setiap 12 Jam
    const val FREQ_6_HOURS = "6_HOURS"   // Setiap 6 Jam
    const val FREQ_WEEKLY = "WEEKLY"     // Setiap Minggu

    const val ACTION_AUTO_BACKUP = "com.example.smartstock.ACTION_AUTO_BACKUP_MIDNIGHT"
    const val ALARM_REQUEST_CODE = 998822

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isAutoBackupEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ENABLED, true)
    }

    fun setAutoBackupEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) {
            scheduleDailyMidnightAlarm(context)
        } else {
            cancelAlarm(context)
        }
    }

    fun getAutoBackupFrequency(context: Context): String {
        return getPrefs(context).getString(KEY_FREQUENCY, FREQ_24_HOURS) ?: FREQ_24_HOURS
    }

    fun setAutoBackupFrequency(context: Context, frequency: String) {
        getPrefs(context).edit().putString(KEY_FREQUENCY, frequency).apply()
        scheduleDailyMidnightAlarm(context)
    }

    fun getAutoBackupHour(context: Context): Int {
        return getPrefs(context).getInt(KEY_HOUR, 0) // Default 0 = 12 malam
    }

    fun setAutoBackupHour(context: Context, hour: Int) {
        getPrefs(context).edit().putInt(KEY_HOUR, hour).apply()
        scheduleDailyMidnightAlarm(context)
    }

    fun getLastBackupTimestamp(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_BACKUP, 0L)
    }

    fun getLastBackupStatus(context: Context): String {
        return getPrefs(context).getString(KEY_LAST_STATUS, "Belum pernah dijalankan") ?: "Belum pernah dijalankan"
    }

    private fun setLastBackupResult(context: Context, time: Long, status: String) {
        getPrefs(context).edit()
            .putLong(KEY_LAST_BACKUP, time)
            .putString(KEY_LAST_STATUS, status)
            .apply()
    }

    private val backupMutex = kotlinx.coroutines.sync.Mutex()

    /**
     * Compute next scheduled midnight / 24-hour backup time in milliseconds
     */
    fun getNextScheduledBackupTimeMillis(context: Context): Long {
        val targetHour = getAutoBackupHour(context)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = System.currentTimeMillis()
        val freq = getAutoBackupFrequency(context)
        while (calendar.timeInMillis <= now) {
            when (freq) {
                FREQ_12_HOURS -> calendar.add(Calendar.HOUR_OF_DAY, 12)
                FREQ_6_HOURS -> calendar.add(Calendar.HOUR_OF_DAY, 6)
                FREQ_WEEKLY -> calendar.add(Calendar.DAY_OF_YEAR, 7)
                else -> calendar.add(Calendar.DAY_OF_YEAR, 1) // 24 Jam (Besok Jam 12 Malam)
            }
        }
        return calendar.timeInMillis
    }

    fun getNextScheduledBackupFormatted(context: Context): String {
        if (!isAutoBackupEnabled(context)) return "Nonaktif"
        val nextTime = getNextScheduledBackupTimeMillis(context)
        val sdf = SimpleDateFormat("EEEE, dd MMM yyyy 'pukul' HH:mm 'WIB'", Locale("id", "ID"))
        return sdf.format(Date(nextTime))
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
     * Schedule Android AlarmManager for 24-hour midnight execution
     */
    fun scheduleDailyMidnightAlarm(context: Context) {
        if (!isAutoBackupEnabled(context)) {
            cancelAlarm(context)
            return
        }

        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, AutoBackupReceiver::class.java).apply {
                action = ACTION_AUTO_BACKUP
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE, intent, flags)

            val nextTrigger = getNextScheduledBackupTimeMillis(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, nextTrigger, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelAlarm(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, AutoBackupReceiver::class.java).apply {
                action = ACTION_AUTO_BACKUP
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_NO_CREATE
            }
            val pendingIntent = PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE, intent, flags)
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Checks if 24 hours or scheduled interval has passed since last backup. If enabled and due, performs automatic backup.
     */
    suspend fun checkAndPerformWeeklyAutoBackup(context: Context, db: AppDatabase): String? = withContext(Dispatchers.IO) {
        if (!isAutoBackupEnabled(context)) return@withContext null

        val lastBackup = getLastBackupTimestamp(context)
        val now = System.currentTimeMillis()
        val freq = getAutoBackupFrequency(context)
        val intervalMs = when (freq) {
            FREQ_12_HOURS -> 12 * 60 * 60 * 1000L
            FREQ_6_HOURS -> 6 * 60 * 60 * 1000L
            FREQ_WEEKLY -> 7 * 24 * 60 * 60 * 1000L
            else -> 24 * 60 * 60 * 1000L // 24 jam
        }

        if (now - lastBackup >= intervalMs || lastBackup == 0L) {
            return@withContext performAutoBackup(context, db)
        }
        return@withContext null
    }

    /**
     * Immediately triggers an automatic backup execution and saves to the configured storage location.
     */
    suspend fun performAutoBackup(context: Context, db: AppDatabase): String = withContext(Dispatchers.IO) {
        if (!backupMutex.tryLock()) {
            return@withContext "Proses cadangan otomatis sedang berlangsung..."
        }
        try {
            val jsonStr = AppBackupUtils.exportDatabaseToJson(db)
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "AutoBackup_SmartStock_24Jam_$dateStr.json"
            val jsonBytes = jsonStr.toByteArray(Charsets.UTF_8)

            // Save to app local storage directory for continuous auto-recovery
            val autoBackupDir = getAutoBackupDirectory(context)
            val backupFile = File(autoBackupDir, fileName)
            FileOutputStream(backupFile).use { os ->
                os.write(jsonBytes)
            }

            // Also keep continuous pre_update_snapshot and auto_backup_latest with already-exported json
            AppBackupUtils.saveContinuousSnapshot(context, db, precomputedJson = jsonStr)

            // Save to preferred storage location (Custom Folder / SAF, Documents, Downloads, etc.)
            val preferredResult = StorageLocationManager.writeBytesToPreferredStorage(
                context = context,
                fileName = fileName,
                mimeType = "application/json",
                dataBytes = jsonBytes
            )

            val successMsg = "Auto backup berhasil disimpan: ${preferredResult.second}"
            setLastBackupResult(context, System.currentTimeMillis(), successMsg)
            cleanOldBackups(context)
            scheduleDailyMidnightAlarm(context)

            successMsg
        } catch (e: Exception) {
            e.printStackTrace()
            val errMsg = "Gagal melakukan auto backup: ${e.message}"
            setLastBackupResult(context, System.currentTimeMillis(), errMsg)
            errMsg
        } finally {
            backupMutex.unlock()
        }
    }

    /**
     * Keeps the list of local auto-backups managed, deleting files older than the last 20 backups.
     */
    private fun cleanOldBackups(context: Context) {
        val dir = getAutoBackupDirectory(context)
        val files = dir.listFiles { _, name -> name.startsWith("AutoBackup_") && name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() } ?: return

        if (files.size > 20) {
            for (i in 20 until files.size) {
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

