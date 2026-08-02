package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.entity.ItemEntity
import com.example.util.Formatters
import com.example.util.GeminiReceiptScanner
import com.example.util.ItemMatcher
import com.example.util.OfflineReceiptScanner
import com.example.util.ScannedReceiptItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanReceiptDialog(
    title: String = "Scan Foto Nota (Online & Offline)",
    targetLabel: String = "Impor ke Penjualan",
    databaseItems: List<ItemEntity> = emptyList(),
    onDismiss: () -> Unit,
    onConfirmImport: (supplier: String, noFaktur: String, tanggal: String, items: List<ScannedReceiptItem>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isOnlineMode by remember { mutableStateOf(true) } // true = Online AI Gemini, false = Offline MLKit OCR
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    var supplierInput by remember { mutableStateOf("") }
    var noFakturInput by remember { mutableStateOf("") }
    var tanggalInput by remember { mutableStateOf(Formatters.getCurrentDateFormatted()) }

    var searchingItemIndex by remember { mutableStateOf<Int?>(null) }
    var showCameraXPreview by remember { mutableStateOf(false) }

    val scannedItems = remember { mutableStateListOf<ScannedReceiptItem>() }

    fun processUri(uri: Uri) {
        selectedImageUri = uri
        selectedBitmap = null
        errorMessage = ""
        isAnalyzing = true
        scope.launch {
            val result = if (isOnlineMode) {
                GeminiReceiptScanner.scanReceiptUri(context, uri, databaseItems)
            } else {
                OfflineReceiptScanner.scanReceiptUriOffline(context, uri, databaseItems)
            }
            isAnalyzing = false
            result.onSuccess { scannedData ->
                if (scannedData.namaSupplier.isNotBlank()) supplierInput = scannedData.namaSupplier
                if (scannedData.nomorFaktur.isNotBlank()) noFakturInput = scannedData.nomorFaktur
                if (scannedData.tanggal.isNotBlank()) tanggalInput = scannedData.tanggal
                scannedItems.clear()
                scannedItems.addAll(scannedData.items)
                if (scannedData.items.isEmpty()) {
                    errorMessage = "Tidak ada item terdeteksi dari foto. Pastikan tulisan/cetakan nota terang & jelas."
                }
            }.onFailure { err ->
                errorMessage = err.message ?: "Gagal memproses gambar nota."
            }
        }
    }

    fun processBitmap(bitmap: Bitmap) {
        selectedBitmap = bitmap
        selectedImageUri = null
        errorMessage = ""
        isAnalyzing = true
        scope.launch {
            val result = if (isOnlineMode) {
                GeminiReceiptScanner.scanReceiptBitmap(bitmap, databaseItems)
            } else {
                OfflineReceiptScanner.scanReceiptBitmapOffline(bitmap, databaseItems)
            }
            isAnalyzing = false
            result.onSuccess { scannedData ->
                if (scannedData.namaSupplier.isNotBlank()) supplierInput = scannedData.namaSupplier
                if (scannedData.nomorFaktur.isNotBlank()) noFakturInput = scannedData.nomorFaktur
                if (scannedData.tanggal.isNotBlank()) tanggalInput = scannedData.tanggal
                scannedItems.clear()
                scannedItems.addAll(scannedData.items)
                if (scannedData.items.isEmpty()) {
                    errorMessage = "Tidak ada item terdeteksi dari foto. Pastikan tulisan/cetakan nota terang & jelas."
                }
            }.onFailure { err ->
                errorMessage = err.message ?: "Gagal memproses foto kamera."
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) processUri(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) processBitmap(bitmap)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isOnlineMode) MaterialTheme.colorScheme.primaryContainer else Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isOnlineMode) Icons.Default.AutoAwesome else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (isOnlineMode) MaterialTheme.colorScheme.primary else Color(0xFF2E7D32),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = if (isOnlineMode) "Mode AI Online (Membaca Tulisan Tangan & Cetakan)" else "Mode Offline MLKit (Tanpa Internet)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Mode Selector Filter Chips (Online vs Offline)
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Pilih Metode Scan Nota:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    FilterChip(
                                        selected = isOnlineMode,
                                        onClick = {
                                            isOnlineMode = true
                                            if (selectedImageUri != null) processUri(selectedImageUri!!)
                                            else if (selectedBitmap != null) processBitmap(selectedBitmap!!)
                                        },
                                        label = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Online (Gemini AI)", fontSize = 11.sp)
                                            }
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    FilterChip(
                                        selected = !isOnlineMode,
                                        onClick = {
                                            isOnlineMode = false
                                            if (selectedImageUri != null) processUri(selectedImageUri!!)
                                            else if (selectedBitmap != null) processBitmap(selectedBitmap!!)
                                        },
                                        label = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Offline (MLKit OCR)", fontSize = 11.sp)
                                            }
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFFC8E6C9),
                                            selectedLabelColor = Color(0xFF1B5E20)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // --- CAMERA & PHOTO SCANNER SECTION (PROMINENT AT TOP) ---
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Ambil / Pilih Foto Nota Penjualan",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Image Preview Frame
                                if (selectedImageUri != null || selectedBitmap != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Black.copy(alpha = 0.05f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (selectedBitmap != null) {
                                            Image(
                                                bitmap = selectedBitmap!!.asImageBitmap(),
                                                contentDescription = "Foto Kamera Nota",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else if (selectedImageUri != null) {
                                            AsyncImage(
                                                model = selectedImageUri,
                                                contentDescription = "Foto Galeri Nota",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        Surface(
                                            color = Color.Black.copy(alpha = 0.65f),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                text = "Foto Terpilih",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                // Buttons for Camera / Gallery
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Buka Kamera Card
                                    Card(
                                        onClick = { showCameraXPreview = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("btn_buka_kamera_nota"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color.White.copy(alpha = 0.25f),
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.CameraAlt,
                                                        contentDescription = "Kamera",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = if (selectedBitmap == null && selectedImageUri == null) "Buka Kamera" else "Foto Ulang",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "Scan Real-time",
                                                    fontSize = 10.sp,
                                                    color = Color.White.copy(alpha = 0.85f)
                                                )
                                            }
                                        }
                                    }

                                    // Pilih Galeri Card
                                    Card(
                                        onClick = { photoPickerLauncher.launch("image/*") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("btn_pilih_galeri_nota"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Photo,
                                                        contentDescription = "Galeri",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "Pilih Galeri",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Text(
                                                    text = "Upload Foto",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Metadata fields (Supplier, No Faktur)
                    if (selectedImageUri != null || selectedBitmap != null) {
                        item {
                            Text(
                                text = "Informasi Nota Terdeteksi:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = supplierInput,
                                    onValueChange = { supplierInput = it },
                                    label = { Text("Toko / Pelanggan", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = noFakturInput,
                                    onValueChange = { noFakturInput = it },
                                    label = { Text("No. Nota", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            DatePickerField(
                                value = tanggalInput,
                                onDateSelected = { tanggalInput = it },
                                label = "Tanggal Nota",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Scanned Items Title & Add Button
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Hasil Deteksi Barang (${scannedItems.size} Item):",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Dicocokkan otomatis dengan Database Barang",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                TextButton(
                                    onClick = {
                                        scannedItems.add(
                                            ScannedReceiptItem(
                                                namaBarang = "Barang Baru",
                                                jumlah = 1,
                                                hargaSatuan = 0.0,
                                                totalHarga = 0.0
                                            )
                                        )
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ Item", fontSize = 12.sp)
                                }
                            }
                        }

                        if (scannedItems.isEmpty() && !isAnalyzing) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Belum ada item terdeteksi. Silakan ambil foto ulang atau pilih gambar yang lebih jelas.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(scannedItems) { index, item ->
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        // Database Matching Badge & Action Button
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (item.isMatchedWithDatabase) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                                modifier = Modifier.clickable { searchingItemIndex = index }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = if (item.isMatchedWithDatabase) Icons.Default.CheckCircle else Icons.Default.Warning,
                                                        contentDescription = null,
                                                        tint = if (item.isMatchedWithDatabase) Color(0xFF2E7D32) else Color(0xFFE65100),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (item.isMatchedWithDatabase) "✓ DB: ${item.matchedKodeBarang ?: item.matchedNamaBarang ?: ""}" else "⚠ Belum Link Database (Klik Cari)",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (item.isMatchedWithDatabase) Color(0xFF1B5E20) else Color(0xFFBF360C)
                                                    )
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                TextButton(
                                                    onClick = { searchingItemIndex = index },
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp).testTag("btn_cari_db_item_$index")
                                                ) {
                                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text("Cari & Pilih DB", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }

                                                IconButton(
                                                    onClick = { scannedItems.removeAt(index) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Hapus",
                                                        tint = Color(0xFFD32F2F),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        OutlinedTextField(
                                            value = item.namaBarang,
                                            onValueChange = { newNama ->
                                                val matched = ItemMatcher.findBestMatch(newNama, databaseItems)
                                                scannedItems[index] = item.copy(
                                                    namaBarang = newNama,
                                                    matchedItemId = matched?.id,
                                                    matchedKodeBarang = matched?.kodeBarang,
                                                    matchedNamaBarang = matched?.namaBarang,
                                                    isMatchedWithDatabase = matched != null
                                                )
                                            },
                                            label = { Text("Nama / Kata Kunci Barang (Draft Nota)", fontSize = 10.sp) },
                                            trailingIcon = {
                                                IconButton(onClick = { searchingItemIndex = index }) {
                                                    Icon(Icons.Default.Inventory, contentDescription = "Pilih dari DB", modifier = Modifier.size(16.dp))
                                                }
                                            },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedTextField(
                                                value = if (item.jumlah == 0) "" else item.jumlah.toString(),
                                                onValueChange = { newJmlStr ->
                                                    val filtered = newJmlStr.filter { char -> char.isDigit() }
                                                    val newJml = if (filtered.isBlank()) 0 else (filtered.toIntOrNull() ?: 0)
                                                    val newTot = newJml * item.hargaSatuan
                                                    scannedItems[index] = item.copy(jumlah = newJml, totalHarga = newTot)
                                                },
                                                label = { Text("Jumlah", fontSize = 10.sp) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            OutlinedTextField(
                                                value = if (item.hargaSatuan > 0) item.hargaSatuan.toInt().toString() else "",
                                                onValueChange = { newHrgStr ->
                                                    val filtered = newHrgStr.filter { char -> char.isDigit() }
                                                    val newHrg = filtered.toDoubleOrNull() ?: 0.0
                                                    val newTot = item.jumlah * newHrg
                                                    scannedItems[index] = item.copy(hargaSatuan = newHrg, totalHarga = newTot)
                                                },
                                                label = { Text("Harga Satuan", fontSize = 10.sp) },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                modifier = Modifier.weight(1.2f)
                                            )
                                            OutlinedTextField(
                                                value = Formatters.formatRupiah(item.totalHarga),
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Total", fontSize = 10.sp) },
                                                singleLine = true,
                                                modifier = Modifier.weight(1.3f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Loading State
                    if (isAnalyzing) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = if (isOnlineMode) MaterialTheme.colorScheme.primary else Color(0xFF2E7D32),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = if (isOnlineMode) "Menganalisis foto nota dengan AI Gemini 3.5..." else "Memproses OCR MLKit Offline...",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Mencocokkan kata kunci & kode dengan Database Barang",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Error Message
                    if (errorMessage.isNotBlank() && !isAnalyzing) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFFEBEE),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFC62828),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = errorMessage,
                                        color = Color(0xFFC62828),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (scannedItems.isNotEmpty()) {
                        onConfirmImport(supplierInput, noFakturInput, tanggalInput, scannedItems.toList())
                        Toast.makeText(context, "Berhasil mengimpor ${scannedItems.size} barang dari nota!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                },
                enabled = scannedItems.isNotEmpty() && !isAnalyzing,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_konfirmasi_impor_nota")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(targetLabel, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )

    searchingItemIndex?.let { index ->
        if (index in scannedItems.indices) {
            val currentItem = scannedItems[index]
            val initialQuery = if (!currentItem.matchedNamaBarang.isNullOrBlank()) {
                currentItem.matchedNamaBarang!!
            } else {
                currentItem.namaBarang
            }

            SearchItemDatabaseDialog(
                initialQueryText = initialQuery,
                databaseItems = databaseItems,
                onDismiss = { searchingItemIndex = null },
                onSelectItem = { dbItem ->
                    val finalPrice = if (currentItem.hargaSatuan > 0) currentItem.hargaSatuan else dbItem.hargaModal
                    scannedItems[index] = currentItem.copy(
                        namaBarang = "${dbItem.namaBarang} (${dbItem.kodeBarang})",
                        hargaSatuan = finalPrice,
                        totalHarga = finalPrice * currentItem.jumlah,
                        matchedItemId = dbItem.id,
                        matchedKodeBarang = dbItem.kodeBarang,
                        matchedNamaBarang = dbItem.namaBarang,
                        isMatchedWithDatabase = true
                    )
                    searchingItemIndex = null
                },
                onUnlinkItem = {
                    scannedItems[index] = currentItem.copy(
                        matchedItemId = null,
                        matchedKodeBarang = null,
                        matchedNamaBarang = null,
                        isMatchedWithDatabase = false
                    )
                    searchingItemIndex = null
                }
            )
        }
    }

    if (showCameraXPreview) {
        CameraXPreviewDialog(
            databaseItems = databaseItems,
            onDismiss = { showCameraXPreview = false },
            onImageCaptured = { capturedBitmap ->
                showCameraXPreview = false
                processBitmap(capturedBitmap)
            },
            onRealTimePopulateDraft = { scannedResult ->
                showCameraXPreview = false
                if (scannedResult.namaSupplier.isNotBlank()) supplierInput = scannedResult.namaSupplier
                if (scannedResult.nomorFaktur.isNotBlank()) noFakturInput = scannedResult.nomorFaktur
                scannedItems.clear()
                scannedItems.addAll(scannedResult.items)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchItemDatabaseDialog(
    initialQueryText: String,
    databaseItems: List<ItemEntity>,
    onDismiss: () -> Unit,
    onSelectItem: (ItemEntity) -> Unit,
    onUnlinkItem: () -> Unit
) {
    val cleanedInitial = remember(initialQueryText) { ItemMatcher.cleanOcrQuery(initialQueryText) }
    var searchQuery by remember { mutableStateOf(cleanedInitial) }

    val filteredItems = remember(searchQuery, databaseItems) {
        ItemMatcher.searchItems(searchQuery, databaseItems)
    }

    val displayList = remember(filteredItems, databaseItems) {
        if (filteredItems.isNotEmpty()) filteredItems else databaseItems
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cari & Pilih Barang Database", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Clear, contentDescription = "Tutup", modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "📦 Database Stok: ${databaseItems.size} Barang Tersedia",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (searchQuery.isNotBlank()) {
                            TextButton(
                                onClick = { searchQuery = "" },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Tampilkan Semua", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Ketik Nama / Kode Barang", fontSize = 11.sp) },
                    placeholder = { Text("Misal: kabel, totolink, 609...", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Hapus Pencarian")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("search_db_dialog_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (filteredItems.isNotEmpty()) {
                            "Ditemukan ${filteredItems.size} barang cocok"
                        } else {
                            "Menampilkan seluruh ${databaseItems.size} barang database"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (filteredItems.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )

                    TextButton(
                        onClick = onUnlinkItem,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text("Gunakan Draft Saja (Unlink)", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (displayList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada barang terdaftar di Database Stok.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayList, key = { it.id }) { dbItem ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectItem(dbItem) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (dbItem.kodeBarang.isNotBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    modifier = Modifier.padding(end = 6.dp)
                                                ) {
                                                    Text(
                                                        text = dbItem.kodeBarang,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = dbItem.namaBarang,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Harga Modal: ${Formatters.formatRupiah(dbItem.hargaModal)} | Stok: ${dbItem.stok}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Button(
                                        onClick = { onSelectItem(dbItem) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Pilih", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal / Tutup")
            }
        }
    )
}
