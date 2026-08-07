package com.example.util

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.DocumentsContract
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StorageLocationManager {

    private const val PREFS_NAME = "smartstock_storage_prefs"

    const val STORAGE_INTERNAL = "INTERNAL"
    const val STORAGE_DOWNLOADS = "DOWNLOADS"
    const val STORAGE_DOCUMENTS = "DOCUMENTS"
    const val STORAGE_CUSTOM_SAF = "CUSTOM_SAF"

    private const val KEY_STORAGE_TYPE = "storage_location_type"
    private const val KEY_CUSTOM_TREE_URI = "custom_tree_uri"
    private const val KEY_CUSTOM_FOLDER_NAME = "custom_folder_name"
    private const val KEY_DUAL_COPY_DOWNLOADS = "dual_copy_downloads"
    private const val KEY_ALWAYS_PROMPT_SAVE_AS = "always_prompt_save_as"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getStorageType(context: Context): String {
        return getPrefs(context).getString(KEY_STORAGE_TYPE, STORAGE_INTERNAL) ?: STORAGE_INTERNAL
    }

    fun setStorageType(context: Context, type: String) {
        getPrefs(context).edit().putString(KEY_STORAGE_TYPE, type).apply()
    }

    fun getCustomTreeUri(context: Context): String? {
        return getPrefs(context).getString(KEY_CUSTOM_TREE_URI, null)
    }

    fun getCustomFolderName(context: Context): String {
        val savedName = getPrefs(context).getString(KEY_CUSTOM_FOLDER_NAME, null)
        if (!savedName.isNullOrBlank()) return savedName

        val uriStr = getCustomTreeUri(context)
        return if (!uriStr.isNullOrBlank()) {
            try {
                getFolderNameFromUri(context, Uri.parse(uriStr))
            } catch (e: Exception) {
                "Folder Khusus (Pilihan Pengguna)"
            }
        } else {
            "Belum Dipilih (Klik tombol untuk memilih)"
        }
    }

    fun setCustomFolder(context: Context, uriString: String?, folderName: String?) {
        getPrefs(context).edit()
            .putString(KEY_CUSTOM_TREE_URI, uriString)
            .putString(KEY_CUSTOM_FOLDER_NAME, folderName)
            .apply()
    }

    fun isDualCopyDownloads(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DUAL_COPY_DOWNLOADS, true)
    }

    fun setDualCopyDownloads(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DUAL_COPY_DOWNLOADS, enabled).apply()
    }

    fun isAlwaysPromptSaveAs(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ALWAYS_PROMPT_SAVE_AS, false)
    }

    fun setAlwaysPromptSaveAs(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ALWAYS_PROMPT_SAVE_AS, enabled).apply()
    }

    /**
     * Get a user-friendly summary of the active storage target
     */
    fun getActiveStorageSummary(context: Context): Pair<String, String> {
        return when (getStorageType(context)) {
            STORAGE_DOWNLOADS -> Pair(
                "Folder Download Publik",
                "/Download/SmartStock_AutoBackup/"
            )
            STORAGE_DOCUMENTS -> Pair(
                "Folder Dokumen Publik",
                "/Documents/SmartStock_Backups/"
            )
            STORAGE_CUSTOM_SAF -> {
                val customName = getCustomFolderName(context)
                Pair("Folder Khusus (SAF / Kartu SD / USB)", customName)
            }
            else -> Pair(
                "Memori Internal Aplikasi",
                "Android/data/${context.packageName}/files/auto_backups/"
            )
        }
    }

    /**
     * Parse human-readable folder name from Storage Access Framework tree Uri
     */
    fun getFolderNameFromUri(context: Context, uri: Uri): String {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(uri)
            val parts = docId.split(":")
            if (parts.size >= 2) {
                val root = if (parts[0].equals("primary", ignoreCase = true)) "Penyimpanan Utama" else parts[0]
                val subPath = parts[1]
                if (subPath.isBlank()) root else "$root > $subPath"
            } else {
                docId
            }
        } catch (e: Exception) {
            uri.lastPathSegment ?: "Folder Pilihan"
        }
    }

    /**
     * Calculate available free space on the internal / external storage
     */
    fun getFreeSpaceFormatted(context: Context): String {
        return try {
            val path = context.filesDir
            val stat = StatFs(path.path)
            val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
            val gb = bytesAvailable / (1024.0 * 1024.0 * 1024.0)
            if (gb >= 1.0) {
                String.format(Locale("id", "ID"), "%.2f GB Bebas", gb)
            } else {
                val mb = bytesAvailable / (1024.0 * 1024.0)
                String.format(Locale("id", "ID"), "%.1f MB Bebas", mb)
            }
        } catch (e: Exception) {
            "Ruang penyimpanan mencukupi"
        }
    }

    /**
     * Writes raw bytes to the user's preferred storage location.
     * Also handles dual-copy to Downloads if configured.
     */
    suspend fun writeBytesToPreferredStorage(
        context: Context,
        fileName: String,
        mimeType: String,
        dataBytes: ByteArray
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val storageType = getStorageType(context)
        val messages = mutableListOf<String>()
        var primarySuccess = false

        when (storageType) {
            STORAGE_CUSTOM_SAF -> {
                val customTreeUriStr = getCustomTreeUri(context)
                if (!customTreeUriStr.isNullOrBlank()) {
                    val safResult = writeToSafTreeUri(context, Uri.parse(customTreeUriStr), fileName, mimeType, dataBytes)
                    if (safResult.first) {
                        primarySuccess = true
                        messages.add(safResult.second)
                    } else {
                        messages.add("Gagal simpan ke folder khusus (${safResult.second}), dialihkan ke memori internal.")
                        val internalRes = writeToInternalStorage(context, fileName, dataBytes)
                        primarySuccess = internalRes.first
                        messages.add(internalRes.second)
                    }
                } else {
                    messages.add("Folder khusus belum dipilih, menyimpan ke memori internal.")
                    val internalRes = writeToInternalStorage(context, fileName, dataBytes)
                    primarySuccess = internalRes.first
                    messages.add(internalRes.second)
                }
            }
            STORAGE_DOCUMENTS -> {
                val docRes = writeToDocumentsFolder(context, fileName, mimeType, dataBytes)
                primarySuccess = docRes.first
                messages.add(docRes.second)
            }
            STORAGE_DOWNLOADS -> {
                val dlRes = writeToDownloadsFolder(context, fileName, mimeType, dataBytes)
                primarySuccess = dlRes.first
                messages.add(dlRes.second)
            }
            else -> {
                // Internal
                val internalRes = writeToInternalStorage(context, fileName, dataBytes)
                primarySuccess = internalRes.first
                messages.add(internalRes.second)
            }
        }

        // Dual copy to Downloads if enabled and primary is not already Downloads
        if (isDualCopyDownloads(context) && storageType != STORAGE_DOWNLOADS) {
            try {
                val dualRes = writeToDownloadsFolder(context, fileName, mimeType, dataBytes)
                if (dualRes.first) {
                    messages.add("Salinan ganda tersimpan di Download")
                }
            } catch (e: Exception) {
                // Ignore failure of non-critical secondary copy
            }
        }

        Pair(primarySuccess, messages.joinToString(" | "))
    }

    /**
     * Write file into internal storage auto_backups folder
     */
    fun writeToInternalStorage(context: Context, fileName: String, dataBytes: ByteArray): Pair<Boolean, String> {
        return try {
            val dir = AutoBackupManager.getAutoBackupDirectory(context)
            val file = File(dir, fileName)
            FileOutputStream(file).use { it.write(dataBytes) }
            Pair(true, "Tersimpan di Memori Internal (${file.name})")
        } catch (e: Exception) {
            Pair(false, "Gagal menulis ke memori internal: ${e.message}")
        }
    }

    /**
     * Write file into public Downloads folder
     */
    fun writeToDownloadsFolder(context: Context, fileName: String, mimeType: String, dataBytes: ByteArray): Pair<Boolean, String> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver: ContentResolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SmartStock_AutoBackup")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        os.write(dataBytes)
                    }
                    Pair(true, "Tersimpan di Folder Download ($fileName)")
                } else {
                    Pair(false, "Gagal membuat file di folder Downloads")
                }
            } else {
                val downloadsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "SmartStock_AutoBackup")
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { it.write(dataBytes) }
                Pair(true, "Tersimpan di Folder Download (${file.name})")
            }
        } catch (e: Exception) {
            Pair(false, "Gagal simpan ke Download: ${e.message}")
        }
    }

    /**
     * Write file into public Documents folder
     */
    fun writeToDocumentsFolder(context: Context, fileName: String, mimeType: String, dataBytes: ByteArray): Pair<Boolean, String> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver: ContentResolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/SmartStock_Backups")
                }
                val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                    ?: resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        os.write(dataBytes)
                    }
                    Pair(true, "Tersimpan di Folder Dokumen ($fileName)")
                } else {
                    Pair(false, "Gagal membuat file di folder Dokumen")
                }
            } else {
                val docsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "SmartStock_Backups")
                if (!docsDir.exists()) {
                    docsDir.mkdirs()
                }
                val file = File(docsDir, fileName)
                FileOutputStream(file).use { it.write(dataBytes) }
                Pair(true, "Tersimpan di Folder Dokumen (${file.name})")
            }
        } catch (e: Exception) {
            Pair(false, "Gagal simpan ke Dokumen: ${e.message}")
        }
    }

    /**
     * Write file into Storage Access Framework Document Tree URI
     */
    fun writeToSafTreeUri(
        context: Context,
        treeUri: Uri,
        fileName: String,
        mimeType: String,
        dataBytes: ByteArray
    ): Pair<Boolean, String> {
        return try {
            val treeDocUri = DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri)
            )
            val docUri = DocumentsContract.createDocument(
                context.contentResolver,
                treeDocUri,
                mimeType,
                fileName
            )
            if (docUri != null) {
                context.contentResolver.openOutputStream(docUri)?.use { os ->
                    os.write(dataBytes)
                }
                val folderName = getFolderNameFromUri(context, treeUri)
                Pair(true, "Tersimpan di $folderName ($fileName)")
            } else {
                Pair(false, "Sistem tidak dapat membuat file di folder pilihan")
            }
        } catch (e: Exception) {
            Pair(false, "Izin akses folder kadaluarsa atau ditolak: ${e.message}")
        }
    }

    /**
     * Test write operation to verify whether the active storage location has working write permissions
     */
    suspend fun testWriteToStorageLocation(context: Context): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val testFileName = "Uji_Coba_Penyimpanan_SmartStock_$timeStamp.txt"
        val testContent = "Tes Penulisan File SmartStock POS\nWaktu: ${Date()}\nStatus: Sukses Terverifikasi\n"
        val bytes = testContent.toByteArray(Charsets.UTF_8)

        val result = writeBytesToPreferredStorage(context, testFileName, "text/plain", bytes)
        if (result.first) {
            Pair(true, "Uji coba berhasil! ${result.second}")
        } else {
            Pair(false, "Uji coba gagal: ${result.second}")
        }
    }
}
