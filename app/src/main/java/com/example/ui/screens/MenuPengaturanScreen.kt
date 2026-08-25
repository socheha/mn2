package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.ui.MainViewModel
import com.example.util.AppBackupUtils
import com.example.util.AutoBackupManager
import com.example.util.StorageLocationManager
import com.example.util.ThemePreferenceManager
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MenuPengaturanScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val themeMode by viewModel.themeMode.collectAsState()
    val isAutoBackupEnabled by viewModel.isAutoBackupEnabled.collectAsState()
    val autoBackupFrequency by viewModel.autoBackupFrequency.collectAsState()
    val lastAutoBackupTime by viewModel.lastAutoBackupTime.collectAsState()
    val lastAutoBackupStatus by viewModel.lastAutoBackupStatus.collectAsState()
    val nextScheduledBackupTime by viewModel.nextScheduledBackupTime.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val isOnlineStatus by viewModel.isOnlineStatus.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val allSyncQueue by viewModel.allSyncQueue.collectAsState()

    val isPinEnabled by viewModel.isPinEnabled.collectAsState()
    val pinTimeoutMinutes by viewModel.pinTimeoutMinutes.collectAsState()

    val storageLocationType by viewModel.storageLocationType.collectAsState()
    val customStorageFolderName by viewModel.customStorageFolderName.collectAsState()
    val customStorageTreeUri by viewModel.customStorageTreeUri.collectAsState()
    val isDualCopyDownloads by viewModel.isDualCopyDownloads.collectAsState()
    val freeStorageSpace by viewModel.freeStorageSpace.collectAsState()

    // Activity launcher for Storage Access Framework (SAF) folder picker
    val openDocumentTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                // Some document providers might not support persistable permissions
            }
            val folderName = StorageLocationManager.getFolderNameFromUri(context, uri)
            viewModel.setCustomStorageFolder(uri, folderName)
            Toast.makeText(context, "Folder penyimpanan diatur ke: $folderName", Toast.LENGTH_LONG).show()
        }
    }

    var showSetPinDialog by remember { mutableStateOf(false) }
    var inputPin by remember { mutableStateOf("") }
    var inputConfirmPin by remember { mutableStateOf("") }
    var inputQuestion by remember { mutableStateOf("Siapa nama toko Anda?") }
    var inputAnswer by remember { mutableStateOf("") }
    var pinDialogErrorMsg by remember { mutableStateOf<String?>(null) }

    var showDisablePinDialog by remember { mutableStateOf(false) }
    var disablePinInput by remember { mutableStateOf("") }
    var disablePinErrorMsg by remember { mutableStateOf<String?>(null) }

    var showRestoreTextDialog by remember { mutableStateOf(false) }
    var pastedJsonText by remember { mutableStateOf("") }

    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showConfirmPreUpdateRestoreDialog by remember { mutableStateOf(false) }
    var resetConfirmationInput by remember { mutableStateOf("") }
    var resetPinInput by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    var showAutoBackupListDialog by remember { mutableStateOf(false) }
    var showSyncQueueListDialog by remember { mutableStateOf(false) }

    // File Picker for Restore JSON
    val restoreFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                val jsonContent = reader.readText()
                reader.close()

                if (jsonContent.isNotBlank()) {
                    isProcessing = true
                    viewModel.restoreFromBackupJson(jsonContent) { success, msg ->
                        isProcessing = false
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "File kosong atau tidak valid.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membaca file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(14.dp)
            .testTag("menu_pengaturan_screen")
    ) {
        // --- Header Banner ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Pengaturan Aplikasi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Kelola tema, keamanan, cadangan data otomatis, & sinkronisasi",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // =========================================================================
        // 1. TEMA & TAMPILAN APLIKASI
        // =========================================================================
        SettingsSectionCard(
            icon = Icons.Default.Palette,
            title = "Tema & Tampilan",
            subtitle = "Pilih mode visual untuk kenyamanan penggunaan"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = themeMode == ThemePreferenceManager.MODE_LIGHT,
                    onClick = { viewModel.setThemeMode(ThemePreferenceManager.MODE_LIGHT) },
                    label = { Text("Terang", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = themeMode == ThemePreferenceManager.MODE_DARK,
                    onClick = { viewModel.setThemeMode(ThemePreferenceManager.MODE_DARK) },
                    label = { Text("Gelap", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = themeMode == ThemePreferenceManager.MODE_SYSTEM,
                    onClick = { viewModel.setThemeMode(ThemePreferenceManager.MODE_SYSTEM) },
                    label = { Text("Sistem", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // =========================================================================
        // 2. KEAMANAN & KUNCI PIN APLIKASI
        // =========================================================================
        SettingsSectionCard(
            icon = Icons.Default.Security,
            title = "Keamanan & Kunci PIN",
            subtitle = "Lindungi akses ke data stok dan laporan keuangan",
            headerAction = {
                Switch(
                    checked = isPinEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            showSetPinDialog = true
                        } else {
                            showDisablePinDialog = true
                        }
                    },
                    modifier = Modifier.testTag("switch_kunci_pin")
                )
            }
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isPinEnabled) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isPinEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = if (isPinEnabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPinEnabled) "Kunci PIN Aktif (Enkripsi SHA-256 Digest)" else "Kunci PIN Tidak Aktif",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPinEnabled) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isPinEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Durasi Sesi PIN (Tetap Terbuka):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = pinTimeoutMinutes == 5,
                        onClick = { viewModel.setPinTimeoutMinutes(5) },
                        label = { Text("5 Menit", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = pinTimeoutMinutes == 10,
                        onClick = { viewModel.setPinTimeoutMinutes(10) },
                        label = { Text("10 Menit", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = pinTimeoutMinutes == 0,
                        onClick = { viewModel.setPinTimeoutMinutes(0) },
                        label = { Text("Selalu Minta", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showSetPinDialog = true },
                        modifier = Modifier.weight(1f).testTag("btn_ubah_pin"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ubah PIN", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.lockApp()
                            Toast.makeText(context, "Aplikasi Berhasil Dikunci", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).testTag("btn_kunci_sekarang"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kunci Sekarang", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // =========================================================================
        // 3. LOKASI PENYIMPANAN FILE CADANGAN
        // =========================================================================
        SettingsSectionCard(
            icon = Icons.Default.FolderSpecial,
            title = "Lokasi Penyimpanan File",
            subtitle = "Folder target cadangan otomatis & ekspor data",
            headerAction = {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = freeStorageSpace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        ) {
            val (activeTitle, activePath) = when (storageLocationType) {
                StorageLocationManager.STORAGE_DOWNLOADS -> Pair("Folder Download Publik", "/Download/SmartStock_AutoBackup/")
                StorageLocationManager.STORAGE_DOCUMENTS -> Pair("Folder Dokumen Publik", "/Documents/SmartStock_Backups/")
                StorageLocationManager.STORAGE_CUSTOM_SAF -> Pair("Folder Khusus (SAF / SD Card / USB)", customStorageFolderName.ifBlank { "Folder Pilihan SAF" })
                else -> Pair("Memori Internal Aplikasi", "Android/data/${context.packageName}/files/auto_backups/")
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (storageLocationType) {
                            StorageLocationManager.STORAGE_CUSTOM_SAF -> Icons.Default.SdStorage
                            StorageLocationManager.STORAGE_DOWNLOADS -> Icons.Default.FileDownload
                            StorageLocationManager.STORAGE_DOCUMENTS -> Icons.Default.Folder
                            else -> Icons.Default.Storage
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lokasi Aktif: $activeTitle",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = activePath,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Option 1: Downloads Folder (Recommended)
            StorageOptionItem(
                title = "Folder Download Publik (Direkomendasikan)",
                subtitle = "/Download/SmartStock_AutoBackup/ (Mudah diakses via File Manager / WA / PC).",
                isSelected = storageLocationType == StorageLocationManager.STORAGE_DOWNLOADS,
                onClick = { viewModel.setStorageLocationType(StorageLocationManager.STORAGE_DOWNLOADS) }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Option 2: Documents Folder
            StorageOptionItem(
                title = "Folder Dokumen Publik",
                subtitle = "/Documents/SmartStock_Backups/ (Standar folder dokumen perangkat).",
                isSelected = storageLocationType == StorageLocationManager.STORAGE_DOCUMENTS,
                onClick = { viewModel.setStorageLocationType(StorageLocationManager.STORAGE_DOCUMENTS) }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Option 3: Internal Storage
            StorageOptionItem(
                title = "Memori Internal Aplikasi (Sandbox Terisolasi)",
                subtitle = "Tersimpan di sandbox aplikasi. Sangat aman dan terisolasi dari aplikasi lain.",
                isSelected = storageLocationType == StorageLocationManager.STORAGE_INTERNAL,
                onClick = { viewModel.setStorageLocationType(StorageLocationManager.STORAGE_INTERNAL) }
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Option 4: Custom SAF / SD Card / USB OTG
            StorageOptionItem(
                title = "Folder Khusus / Kartu SD / Flashdisk OTG (SAF)",
                subtitle = if (storageLocationType == StorageLocationManager.STORAGE_CUSTOM_SAF && customStorageFolderName.isNotBlank()) {
                    "Folder terpilih: $customStorageFolderName"
                } else {
                    "Pilih folder eksternal di Kartu SD atau Flashdisk via Storage Access Framework."
                },
                isSelected = storageLocationType == StorageLocationManager.STORAGE_CUSTOM_SAF,
                onClick = {
                    viewModel.setStorageLocationType(StorageLocationManager.STORAGE_CUSTOM_SAF)
                    if (customStorageTreeUri.isNullOrBlank()) {
                        openDocumentTreeLauncher.launch(null)
                    }
                }
            )

            if (storageLocationType == StorageLocationManager.STORAGE_CUSTOM_SAF) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { openDocumentTreeLauncher.launch(null) },
                    modifier = Modifier.fillMaxWidth().testTag("btn_pilih_folder_saf"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (customStorageFolderName.isNotBlank()) "Ubah / Pilih Ulang Folder (SAF)" else "Pilih Folder di Kartu SD / Memori (SAF)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Switch Dual-Copy to Downloads
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Simpan Salinan Ganda ke Folder Download",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Menyimpan salinan ganda ekstra agar data cadangan selalu terlindungi berlapis.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isDualCopyDownloads,
                    onCheckedChange = { viewModel.setDualCopyDownloads(it) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        isProcessing = true
                        viewModel.testStorageLocation { success, msg ->
                            isProcessing = false
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.weight(1f).testTag("btn_uji_coba_penyimpanan"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Uji Izin Tulis", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        var opened = false
                        try {
                            // First attempt: Storage Access Framework / DocumentsUI
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                val downloadsUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload")
                                setDataAndType(downloadsUri, "resource/folder")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                            opened = true
                        } catch (e: Exception) {
                            opened = false
                        }

                        if (!opened) {
                            try {
                                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                    type = "*/*"
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(Intent.createChooser(intent, "Buka File Manager"))
                                opened = true
                            } catch (e: Exception) {
                                opened = false
                            }
                        }

                        if (!opened) {
                            Toast.makeText(context, "Lokasi file cadangan: $activePath", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Buka Folder", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // =========================================================================
        // 4. CADANGAN OTOMATIS & SNAPSHOT (AUTO BACKUP) - TUNGGAL & LENGKAP
        // =========================================================================
        SettingsSectionCard(
            icon = Icons.Default.Schedule,
            title = "Cadangan Otomatis (Auto Backup)",
            subtitle = "Pencadangan berkala database tanpa perlu diingat",
            headerAction = {
                Switch(
                    checked = isAutoBackupEnabled,
                    onCheckedChange = { viewModel.setAutoBackupEnabled(it) }
                )
            }
        ) {
            // Status Box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Status Cadangan:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (isAutoBackupEnabled) "Aktif Berjalan" else "Nonaktif",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAutoBackupEnabled) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Jadwal Berikutnya:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = nextScheduledBackupTime,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Terakhir Dicadangkan:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (lastAutoBackupTime > 0) SimpleDateFormat("dd MMM yyyy HH:mm 'WIB'", Locale("id", "ID")).format(Date(lastAutoBackupTime)) else "Belum pernah",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Frequency Interval
            Text(
                text = "Interval Waktu Cadangan Otomatis:",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = autoBackupFrequency == AutoBackupManager.FREQ_24_HOURS,
                    onClick = { viewModel.setAutoBackupFrequency(AutoBackupManager.FREQ_24_HOURS) },
                    label = { Text("24 Jam (Malam)", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = autoBackupFrequency == AutoBackupManager.FREQ_12_HOURS,
                    onClick = { viewModel.setAutoBackupFrequency(AutoBackupManager.FREQ_12_HOURS) },
                    label = { Text("12 Jam", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = autoBackupFrequency == AutoBackupManager.FREQ_WEEKLY,
                    onClick = { viewModel.setAutoBackupFrequency(AutoBackupManager.FREQ_WEEKLY) },
                    label = { Text("Mingguan", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Button(
                onClick = {
                    isProcessing = true
                    viewModel.runAutoBackupNow { msg ->
                        isProcessing = false
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("btn_run_auto_backup_now"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isProcessing) "Memproses Cadangan..." else "Jalankan Auto Backup Sekarang",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showAutoBackupListDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Daftar File Snapshot", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { showConfirmPreUpdateRestoreDialog = true },
                    modifier = Modifier.weight(1f).testTag("btn_pulihkan_data_sebelum_update"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE65100))
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pulihkan Snapshot", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // =========================================================================
        // 5. SINKRONISASI DATA LURING (OFFLINE SYNC)
        // =========================================================================
        SettingsSectionCard(
            icon = Icons.Default.Sync,
            title = "Sinkronisasi Data Luring",
            subtitle = "Penyelarasan transaksi lokal saat kembali online",
            headerAction = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isOnlineStatus) "Online" else "Offline",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOnlineStatus) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = isOnlineStatus,
                        onCheckedChange = { viewModel.setOnlineMode(it) }
                    )
                }
            }
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (pendingSyncCount > 0) Color(0xFFFFF3E0) else Color(0xFFE8F5E9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isOnlineStatus) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = if (pendingSyncCount > 0) Color(0xFFE65100) else Color(0xFF2E7D32),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (pendingSyncCount > 0) "$pendingSyncCount transaksi pending menantikan sinkronisasi" else "Semua data lokal telah tersinkronisasi.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (pendingSyncCount > 0) Color(0xFFE65100) else Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.triggerSyncNow { count, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = !isSyncing,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isSyncing) "Menyinkronkan..." else "Sinkronkan Sekarang", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { showSyncQueueListDialog = true },
                    modifier = Modifier.weight(0.8f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log (${allSyncQueue.size})", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // =========================================================================
        // 6. CADANGAN MANUAL & PEMULIHAN (BACKUP & RESTORE DATA) - TUNGGAL & RAPI
        // =========================================================================
        SettingsSectionCard(
            icon = Icons.Default.Backup,
            title = "Cadangan & Pemulihan Manual",
            subtitle = "Ekspor berkas JSON atau impor cadangan dari luar"
        ) {
            // Sub-Section: Ekspor / Cadangkan
            Text(
                text = "1. Ekspor Cadangan Data (.JSON)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = {
                    isProcessing = true
                    viewModel.exportFullBackupJson { jsonStr ->
                        isProcessing = false
                        coroutineScope.launch {
                            val resultMessage = AppBackupUtils.saveJsonBackupToPreferredStorage(context, jsonStr)
                            Toast.makeText(context, resultMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("btn_simpan_backup_manual"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simpan File Backup JSON ke Memori", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.exportFullBackupJson { jsonStr ->
                            AppBackupUtils.shareJsonBackupFile(context, jsonStr)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bagikan (WA/Drive)", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = {
                        viewModel.exportFullBackupJson { jsonStr ->
                            AppBackupUtils.copyToClipboard(context, jsonStr, "Backup Database SmartStock")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Salin Teks JSON", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(14.dp))

            // Sub-Section: Impor / Pulihkan
            Text(
                text = "2. Pemulihan (Restore) Database",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { restoreFilePickerLauncher.launch("application/json") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pilih File JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { showRestoreTextDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Tempel Teks JSON", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // =========================================================================
        // 7. ZONA BAHAYA: RESET DATABASE
        // =========================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Zona Bahaya: Reset Database",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Gunakan dengan sangat hati-hati untuk mengosongkan riwayat transaksi atau membersihkan seluruh data toko.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        resetConfirmationInput = ""
                        resetPinInput = ""
                        showResetConfirmDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset Seluruh Data Aplikasi", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // --- DIALOG AUTO BACKUP & SNAPSHOT FILES LIST ---
    if (showAutoBackupListDialog) {
        val files = remember {
            (viewModel.getAllRecoveryFiles() + viewModel.getLocalAutoBackupFiles())
                .distinctBy { it.absolutePath }
                .sortedByDescending { it.lastModified() }
        }
        AlertDialog(
            onDismissRequest = { showAutoBackupListDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Daftar File Cadangan & Snapshot (${files.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                if (files.isEmpty()) {
                    Text("Belum ada file cadangan yang tersimpan.", fontSize = 13.sp)
                } else {
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(files) { file ->
                            val fileSizeKb = (file.length() / 1024.0).let { if (it < 1.0) "1 KB" else "%.1f KB".format(it) }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(file.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                        Text(
                                            "${SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")).format(Date(file.lastModified()))} • $fileSizeKb",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row {
                                        IconButton(
                                            onClick = {
                                                try {
                                                    val content = file.readText(Charsets.UTF_8)
                                                    AppBackupUtils.shareJsonBackupFile(context, content)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Gagal membagikan: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = "Bagikan", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        }

                                        TextButton(
                                            onClick = {
                                                try {
                                                    val content = file.readText(Charsets.UTF_8)
                                                    viewModel.restoreFromBackupJson(content) { success, msg ->
                                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                        showAutoBackupListDialog = false
                                                    }
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Gagal memulihkan: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Text("Restore", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAutoBackupListDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    // --- DIALOG SYNC QUEUE LIST ---
    if (showSyncQueueListDialog) {
        AlertDialog(
            onDismissRequest = { showSyncQueueListDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Antrean Sinkronisasi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                if (allSyncQueue.isEmpty()) {
                    Text("Tidak ada log antrean transaksi.", fontSize = 13.sp)
                } else {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Record: ${allSyncQueue.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            TextButton(
                                onClick = {
                                    viewModel.clearSyncedQueue()
                                    Toast.makeText(context, "Log tersinkron dibersihkan.", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("Bersihkan Log Lunas", fontSize = 11.sp)
                            }
                        }
                        LazyColumn(modifier = Modifier.height(260.dp)) {
                            items(allSyncQueue) { item ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (item.status == "PENDING") Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "[${item.transactionType}] ${item.payloadSummary}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = SimpleDateFormat("dd/MM/yy HH:mm:ss", Locale("id", "ID")).format(Date(item.createdAt)),
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (item.status == "PENDING") Color(0xFFE65100) else Color(0xFF2E7D32)
                                        ) {
                                            Text(
                                                text = item.status,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSyncQueueListDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    // --- DIALOG RESTORE JSON MANUAL ---
    if (showRestoreTextDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreTextDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tempel Teks Backup JSON", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Tempelkan seluruh teks JSON hasil backup ke kotak di bawah ini:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pastedJsonText,
                        onValueChange = { pastedJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        placeholder = { Text("{\n  \"version\": 6,\n  \"items\": [...]\n}", fontSize = 11.sp) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pastedJsonText.isNotBlank()) {
                            showRestoreTextDialog = false
                            viewModel.restoreFromBackupJson(pastedJsonText) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Teks JSON masih kosong.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Jalankan Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreTextDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // --- DIALOG CONFIRM RESTORE PRE-UPDATE SNAPSHOT ---
    if (showConfirmPreUpdateRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmPreUpdateRestoreDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                    tint = Color(0xFFE65100),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Pulihkan Snapshot Cadangan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFFE65100)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Apakah Anda ingin memulihkan seluruh data toko dari snapshot cadangan otomatis terakhir?",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFF3E0),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "ℹ️ Seluruh data barang, stok gudang & cabang, kas tunai/bank, transaksi, piutang, dan hutang akan dikembalikan ke kondisi snapshot terakhir.",
                            fontSize = 11.sp,
                            color = Color(0xFFBF360C),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmPreUpdateRestoreDialog = false
                        isProcessing = true
                        viewModel.restoreFromLatestAutoSnapshot { success, msg ->
                            isProcessing = false
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                    modifier = Modifier.testTag("btn_confirm_pulihkan_snapshot_sebelum_update")
                ) {
                    Text("Ya, Pulihkan Sekarang", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmPreUpdateRestoreDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // --- DIALOG CONFIRM RESET DATABASE ---
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFC62828))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Konfirmasi Reset Total", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                }
            },
            text = {
                Column {
                    Text(
                        text = "Tindakan ini TIDAK DAPAT DIBATALKAN. Seluruh data barang dan histori keuangan akan terhapus permanen.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFB71C1C)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Ketik 'RESET' di bawah untuk mengonfirmasi:",
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = resetConfirmationInput,
                        onValueChange = { resetConfirmationInput = it },
                        placeholder = { Text("Ketik RESET") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_confirm_reset")
                    )

                    if (isPinEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Masukkan PIN Keamanan Toko:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = resetPinInput,
                            onValueChange = { resetPinInput = it.filter { char -> char.isDigit() } },
                            placeholder = { Text("Masukkan PIN") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_pin_confirm_reset")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!resetConfirmationInput.trim().equals("RESET", ignoreCase = false)) {
                            Toast.makeText(context, "Silakan ketik 'RESET' dengan huruf kapital.", Toast.LENGTH_SHORT).show()
                        } else if (isPinEnabled && !viewModel.verifyPinForAction(resetPinInput)) {
                            Toast.makeText(context, "PIN Keamanan Salah!", Toast.LENGTH_SHORT).show()
                        } else {
                            showResetConfirmDialog = false
                            viewModel.resetAllAppData {
                                Toast.makeText(context, "Semua data berhasil di-reset total.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    modifier = Modifier.testTag("btn_konfirmasi_reset_total")
                ) {
                    Text("Hapus & Reset Total")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // --- DIALOG SET / CHANGE PIN ---
    if (showSetPinDialog) {
        AlertDialog(
            onDismissRequest = { showSetPinDialog = false },
            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(if (isPinEnabled) "Ubah PIN Keamanan" else "Pasang Kunci PIN", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Masukkan PIN 4-6 angka dan pertanyaan pemulihan jika lupa PIN:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = inputPin,
                        onValueChange = {
                            inputPin = it.filter { char -> char.isDigit() }
                            pinDialogErrorMsg = null
                        },
                        label = { Text("PIN Keamanan (Min 4 Angka)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_pin_keamanan")
                    )

                    OutlinedTextField(
                        value = inputConfirmPin,
                        onValueChange = {
                            inputConfirmPin = it.filter { char -> char.isDigit() }
                            pinDialogErrorMsg = null
                        },
                        label = { Text("Konfirmasi PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_konfirmasi_pin")
                    )

                    OutlinedTextField(
                        value = inputQuestion,
                        onValueChange = {
                            inputQuestion = it
                            pinDialogErrorMsg = null
                        },
                        label = { Text("Pertanyaan Pemulihan") },
                        placeholder = { Text("misal: Siapa nama toko / pemilik Anda?") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_pertanyaan_pemulihan")
                    )

                    OutlinedTextField(
                        value = inputAnswer,
                        onValueChange = {
                            inputAnswer = it
                            pinDialogErrorMsg = null
                        },
                        label = { Text("Jawaban Pemulihan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_jawaban_pemulihan_set")
                    )

                    if (pinDialogErrorMsg != null) {
                        Text(
                            text = pinDialogErrorMsg ?: "",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputPin.length < 4) {
                            pinDialogErrorMsg = "PIN minimal 4 angka."
                        } else if (inputPin != inputConfirmPin) {
                            pinDialogErrorMsg = "Konfirmasi PIN tidak cocok."
                        } else if (inputAnswer.isBlank()) {
                            pinDialogErrorMsg = "Jawaban pemulihan wajib diisi."
                        } else {
                            val success = viewModel.setupPin(inputPin, inputQuestion, inputAnswer)
                            if (success) {
                                Toast.makeText(context, "Kunci PIN Berhasil Disimpan & Diaktifkan!", Toast.LENGTH_SHORT).show()
                                showSetPinDialog = false
                                inputPin = ""
                                inputConfirmPin = ""
                                inputAnswer = ""
                            }
                        }
                    },
                    modifier = Modifier.testTag("btn_simpan_pin")
                ) {
                    Text("Simpan PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetPinDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // --- DIALOG DISABLE PIN ---
    if (showDisablePinDialog) {
        AlertDialog(
            onDismissRequest = { showDisablePinDialog = false },
            icon = { Icon(Icons.Default.LockOpen, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Nonaktifkan Kunci PIN", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Masukkan PIN Anda saat ini untuk mengonfirmasi penonaktifan kunci aplikasi:",
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = disablePinInput,
                        onValueChange = {
                            disablePinInput = it.filter { char -> char.isDigit() }
                            disablePinErrorMsg = null
                        },
                        label = { Text("PIN Saat Ini") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_pin_nonaktifkan")
                    )

                    if (disablePinErrorMsg != null) {
                        Text(
                            text = disablePinErrorMsg ?: "",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (disablePinInput.isBlank()) {
                            disablePinErrorMsg = "PIN wajib diisi."
                        } else {
                            val success = viewModel.disablePin(disablePinInput)
                            if (success) {
                                Toast.makeText(context, "Kunci PIN Berhasil Dinonaktifkan", Toast.LENGTH_SHORT).show()
                                showDisablePinDialog = false
                                disablePinInput = ""
                            } else {
                                disablePinErrorMsg = "PIN Salah! Gagal menonaktifkan."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_konfirmasi_nonaktifkan_pin")
                ) {
                    Text("Nonaktifkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisablePinDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    headerAction: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (headerAction != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    headerAction()
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun StorageOptionItem(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
