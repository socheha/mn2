package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.util.AppBackupUtils
import com.example.util.ThemePreferenceManager
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MenuPengaturanScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val themeMode by viewModel.themeMode.collectAsState()
    val isAutoBackupEnabled by viewModel.isAutoBackupEnabled.collectAsState()
    val lastAutoBackupTime by viewModel.lastAutoBackupTime.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val isOnlineStatus by viewModel.isOnlineStatus.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val allSyncQueue by viewModel.allSyncQueue.collectAsState()

    val isPinEnabled by viewModel.isPinEnabled.collectAsState()
    val pinTimeoutMinutes by viewModel.pinTimeoutMinutes.collectAsState()
    val securityQuestion by viewModel.securityQuestion.collectAsState()

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
                    Toast.makeText(context, "File kosong / tidak valid.", Toast.LENGTH_SHORT).show()
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
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Pengaturan & Pemeliharaan Data",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tema tampilan, backup otomatis, antrean sinkronisasi, dan manajemen data",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 0. CARD PERLINDUNGAN & PEMULIHAN DATA SEBELUM UPDATE ---
        Card(
            modifier = Modifier.fillMaxWidth().testTag("card_pemulihan_data_update"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Pemulihan Data & Anti Hilang Saat Update",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Status: Terlindungi (Room Migration & Auto Snapshot Aktif)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Jika data Anda sebelumnya sempat tereset saat memperbarui aplikasi, Anda dapat memulihkannya kembali secara otomatis dalam 1 kali klik dari snapshot cadangan sistem.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tombol Pulihkan Data Sebelum Update
                Button(
                    onClick = {
                        isProcessing = true
                        viewModel.restoreFromLatestAutoSnapshot { success, msg ->
                            isProcessing = false
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("btn_restore_pre_update"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isProcessing) "Memulihkan Data..." else "Kembalikan Data Sebelum Update (Auto Recovery)",
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
                        Text("Pilih Cadangan", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.loadStandardSampleStoreData { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Data Standar Toko", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 1. DARK MODE & TEMA CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DarkMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tema & Tampilan Aplikasi (Dark Mode)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Pilih tema tampilan untuk kenyamanan visual pengoperasian stok di malam hari.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

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
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- KEAMANAN & KUNCI PIN CARD ---
        Card(
            modifier = Modifier.fillMaxWidth().testTag("card_keamanan_aplikasi"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Keamanan & Kunci PIN Aplikasi",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

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

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isPinEnabled) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPinEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (isPinEnabled) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPinEnabled) "Sistem Kunci PIN Aktif (Enkripsi SHA-256)" else "Kunci PIN Belum Diaktifkan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPinEnabled) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Lindungi data transaksi kas, stok barang, dan laporan keuangan toko dari akses tanpa izin.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔒", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enkripsi PIN: Salted SHA-256 Digest", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🛡️", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Anti Brute-Force: Kunci otomatis jika 5x percobaan salah", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⚠️", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Proteksi Reset Total: Wajib verifikasi PIN & konfirmasi", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🧼", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sanitasi Data: Pencegahan injeksi script & karakter berbahaya", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                if (isPinEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Durasi Sesi PIN (Tetap Terbuka Tanpa Close App):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
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
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 2. PENJADWALAN BACKUP OTOMATIS MINGGUAN ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Backup Otomatis Mingguan",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Switch(
                        checked = isAutoBackupEnabled,
                        onCheckedChange = { viewModel.setAutoBackupEnabled(it) }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Aplikasi akan secara otomatis menyimpan cadangan data seluruh toko ke penyimpanan lokal ponsel setiap minggu.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))
                val lastDateText = if (lastAutoBackupTime > 0) {
                    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(lastAutoBackupTime))
                } else "Belum Pernah"

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "📅 Terakhir Backup Otomatis: $lastDateText",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.runAutoBackupNow { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Jalankan Backup Otomatis Sekarang", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showAutoBackupListDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lihat Riwayat File Backup Otomatis Lokal", fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 3. ANTREAN SINKRONISASI DATA LURING (OFFLINE SYNC QUEUE) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Antrean Sinkronisasi Data Luring",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isOnlineStatus) "Online" else "Offline",
                            fontSize = 12.sp,
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

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Mencatat transaksi secara lokal saat tidak ada internet dan otomatis sinkron saat online.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

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

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.triggerSyncNow { count, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSyncing) "Sedang Men-sinkronkan..." else "Sinkronkan Antrean Sekarang", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showSyncQueueListDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Lihat Rincian Log Antrean (${allSyncQueue.size})", fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 4. BACKUP DATA CARD MANUAL ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Backup Manual Database",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Ekspor data barang, stok, transaksi penjualan, piutang, hutang, dan kas ke file .json atau clipboard.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        viewModel.exportFullBackupJson { jsonStr ->
                            val resultMessage = AppBackupUtils.saveJsonBackupToDownloads(context, jsonStr)
                            Toast.makeText(context, resultMessage, Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simpan ke Folder Download", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.exportFullBackupJson { jsonStr ->
                            AppBackupUtils.shareJsonBackupFile(context, jsonStr)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bagikan ke Aplikasi Lain (WA/Drive)", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        viewModel.exportFullBackupJson { jsonStr ->
                            AppBackupUtils.copyToClipboard(context, jsonStr, "Backup Database SmartStock")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salin Teks Backup ke Clipboard", fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 5. RESTORE DATA CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pemulihan (Restore) Data",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Pulihkan seluruh data aplikasi dari file backup JSON atau tempel teks backup.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { restoreFilePickerLauncher.launch("application/json") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pilih File Backup JSON", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showRestoreTextDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tempel Teks Backup JSON Manual", fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 6. RESET DATA CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                        text = "Reset Data Database",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Gunakan fitur ini dengan sangat hati-hati untuk mengosongkan histori transaksi atau membersihkan seluruh data aplikasi.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        resetConfirmationInput = ""
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

    // --- DIALOG AUTO BACKUP FILES LIST ---
    if (showAutoBackupListDialog) {
        val files = remember { viewModel.getAllRecoveryFiles() }
        AlertDialog(
            onDismissRequest = { showAutoBackupListDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cadangan & Snapshot Sistem", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                if (files.isEmpty()) {
                    Text("Belum ada file cadangan yang tersimpan di memori internal.", fontSize = 13.sp)
                } else {
                    LazyColumn(modifier = Modifier.height(260.dp)) {
                        items(files) { file ->
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
                                        Text(file.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(
                                            SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")).format(Date(file.lastModified())),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
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
                                        Text("Restore", fontSize = 12.sp)
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
                    Text("Rincian Log Antrean Sinkronisasi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                if (allSyncQueue.isEmpty()) {
                    Text("Tidak ada log antrean transaksi.", fontSize = 13.sp)
                } else {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
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
