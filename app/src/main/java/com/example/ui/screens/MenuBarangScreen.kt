package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material3.Checkbox
import androidx.compose.material.icons.filled.CameraAlt
import com.example.ui.components.ScanReceiptDialog
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import kotlinx.coroutines.launch
import com.example.util.ExcelImportUtils
import com.example.util.ImportedItemRaw
import com.example.data.entity.ItemEntity
import com.example.data.entity.StockHistoryEntity
import com.example.ui.MainViewModel
import com.example.util.Formatters

@Composable
fun MenuBarangScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val itemsList by viewModel.allItems.collectAsStateWithLifecycle()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val allCashAccounts by viewModel.allCashAccounts.collectAsStateWithLifecycle()

    val filteredItemsList = remember(itemsList, searchQuery) {
        if (searchQuery.isBlank()) itemsList
        else com.example.util.ItemMatcher.searchItems(searchQuery, itemsList)
    }

    val totalNilaiModal = remember(itemsList) {
        itemsList.sumOf { (it.totalStokCombined.toDouble()) * it.hargaModal }
    }
    val totalNilaiGudang = remember(itemsList) {
        itemsList.sumOf { (it.actualStokUtama.toDouble()) * it.hargaModal }
    }
    val totalNilaiToko = remember(itemsList) {
        itemsList.sumOf { (it.actualStokCabang.toDouble()) * it.hargaModal }
    }
    val totalUnitStok = remember(itemsList) {
        itemsList.sumOf { it.totalStokCombined }
    }
    val totalUnitGudang = remember(itemsList) {
        itemsList.sumOf { it.actualStokUtama }
    }
    val totalUnitToko = remember(itemsList) {
        itemsList.sumOf { it.actualStokCabang }
    }

    val availableAccounts = remember(allCashAccounts) {
        val combined = mutableListOf<com.example.data.entity.AccountTypeInfo>()

        val tunaiInDb = allCashAccounts.find { it.accountType == "TUNAI" }
        combined.add(
            com.example.data.entity.AccountTypeInfo(
                type = "TUNAI",
                name = tunaiInDb?.accountName ?: "Kas Tunai Toko",
                category = "TUNAI"
            )
        )

        allCashAccounts.filter { it.accountType != "TUNAI" }.forEach { acc ->
            combined.add(
                com.example.data.entity.AccountTypeInfo(
                    type = acc.accountType,
                    name = acc.accountName,
                    category = com.example.data.entity.CashAccountDefaults.getAccountCategory(acc.accountType)
                )
            )
        }

        combined
    }

    val coroutineScope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var showExcelImportDialog by remember { mutableStateOf(false) }
    var showScanReceiptDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ItemEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<ItemEntity?>(null) }
    var itemForHistory by remember { mutableStateOf<ItemEntity?>(null) }
    var itemToReturn by remember { mutableStateOf<ItemEntity?>(null) }

    Scaffold(
        modifier = Modifier.testTag("menu_barang_screen"),
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { showExcelImportDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E7E34),
                    contentColor = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier.testTag("import_excel_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Excel",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier.testTag("add_item_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Barang",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Summary Card: Total Nilai Seluruh Produk (2 Layer Rapi)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Total Nilai Seluruh Produk",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "${itemsList.size} Jenis Produk • $totalUnitStok Unit Total Stok",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // LAYER 1: Total Nilai Modal Keseluruhan (Aset Toko)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.95f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column(modifier = Modifier.weight(1f, fill = false)) {
                                    Text(
                                        text = "Total Nilai Modal Produk (Aset Toko):",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF37474F)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Total Aset Seluruh Stok Barang",
                                        fontSize = 10.sp,
                                        color = Color(0xFF78909C)
                                    )
                                }
                                Text(
                                    text = Formatters.formatRupiah(totalNilaiModal),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 1.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // LAYER 2: 2 Kolom Rincian Nilai Stok (Gudang & Toko)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE).copy(alpha = 0.85f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Nilai Stok Gudang",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD32F2F)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = Formatters.formatRupiah(totalNilaiGudang),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE53935)
                                    )
                                    Text(
                                        text = "$totalUnitGudang Unit",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFE53935)
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE).copy(alpha = 0.85f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Nilai Stok Toko",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD32F2F)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = Formatters.formatRupiah(totalNilaiToko),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE53935)
                                    )
                                    Text(
                                        text = "$totalUnitToko Unit",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFE53935)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar & Import Action
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Data Stok Barang",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Cari barang (ketik misal: 609, 663, router)...", fontSize = 13.sp) },
                            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = null)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Card(
                                onClick = { showScanReceiptDialog = true },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Scan AI",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Card(
                                onClick = { showExcelImportDialog = true },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.UploadFile,
                                        contentDescription = null,
                                        tint = Color(0xFF1B5E20),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Import Excel",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B5E20)
                                    )
                                }
                            }

                            Card(
                                onClick = {
                                    coroutineScope.launch {
                                        val resultMsg = com.example.util.AppBackupUtils.saveCsvToPreferredStorage(context, itemsList)
                                        android.widget.Toast.makeText(context, resultMsg, android.widget.Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = Color(0xFF1565C0),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Backup Excel",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1565C0)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // List of Items
            if (filteredItemsList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "Belum ada data barang. Tekan tombol + untuk menambahkan."
                            else "Tidak ditemukan barang cocok dengan '$searchQuery'",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(filteredItemsList, key = { it.id }) { item ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)) {
                        ItemCard(
                            item = item,
                            onEdit = { itemToEdit = item },
                            onDelete = { itemToDelete = item },
                            onReturn = { itemToReturn = item },
                            onViewHistory = { itemForHistory = item }
                        )
                    }
                }
            }
        }
    }

    // Add Item Dialog
    if (showAddDialog) {
        ItemFormDialog(
            title = "Tambah Barang Baru",
            initialKode = "",
            initialNama = "",
            initialStok = "0",
            initialHargaModal = "",
            initialKeterangan = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { kode, nama, stok, harga, ket ->
                viewModel.addItem(
                    kodeBarang = kode,
                    namaBarang = nama,
                    stok = stok.toIntOrNull() ?: 0,
                    hargaModal = harga.toDoubleOrNull() ?: 0.0,
                    keterangan = ket
                )
                showAddDialog = false
            }
        )
    }

    // Excel Import Dialog
    if (showExcelImportDialog) {
        ExcelImportDialog(
            viewModel = viewModel,
            onDismiss = { showExcelImportDialog = false }
        )
    }

    // Scan Receipt AI Dialog
    if (showScanReceiptDialog) {
        val context = androidx.compose.ui.platform.LocalContext.current
        ScanReceiptDialog(
            title = "Scan Foto Nota Barang (Online & Offline)",
            targetLabel = "Impor ke Data Stok Barang",
            databaseItems = itemsList,
            onDismiss = { showScanReceiptDialog = false },
            onConfirmImport = { _, _, _, items ->
                val rawList = items.map {
                    ImportedItemRaw(
                        kodeBarang = "SKU-${System.currentTimeMillis().toString().takeLast(6)}",
                        namaBarang = it.namaBarang,
                        stok = it.jumlah,
                        hargaModal = it.hargaSatuan,
                        keterangan = "Hasil Scan Foto Nota AI"
                    )
                }
                viewModel.importItemsBulk(rawList) { count ->
                    android.widget.Toast.makeText(context, "Berhasil mengimpor $count barang dari foto nota!", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // Edit Item Dialog
    itemToEdit?.let { item ->
        ItemFormDialog(
            title = "Edit Barang",
            initialKode = item.kodeBarang,
            initialNama = item.namaBarang,
            initialStok = item.stok.toString(),
            initialHargaModal = if (item.hargaModal > 0) item.hargaModal.toInt().toString() else "",
            initialKeterangan = item.keterangan,
            onDismiss = { itemToEdit = null },
            onConfirm = { kode, nama, stok, harga, ket ->
                viewModel.updateItem(
                    item = item,
                    newStok = stok.toIntOrNull() ?: item.stok,
                    newHargaModal = harga.toDoubleOrNull() ?: item.hargaModal,
                    newKode = kode,
                    newNama = nama,
                    newKet = ket
                )
                itemToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Hapus Barang", fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin menghapus '${item.namaBarang}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteItem(item)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Stock History Dialog
    itemForHistory?.let { item ->
        StockHistoryDialog(
            item = item,
            viewModel = viewModel,
            onDismiss = { itemForHistory = null }
        )
    }

    // Return Stock Dialog
    itemToReturn?.let { item ->
        ReturnStockDialog(
            item = item,
            viewModel = viewModel,
            availableAccounts = availableAccounts,
            allAccounts = allCashAccounts,
            onDismiss = { itemToReturn = null }
        )
    }
}

@Composable
fun ItemCard(
    item: ItemEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReturn: () -> Unit,
    onViewHistory: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (item.kodeBarang.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Text(
                                        text = item.kodeBarang,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = item.namaBarang,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        if (item.keterangan.isNotBlank()) {
                            Text(
                                text = item.keterangan,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = "Gudang: ${item.actualStokUtama}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Text(
                                    text = "Stok Toko: ${item.actualStokCabang}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Stock Badge
                val totalStok = item.totalStokCombined
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (totalStok < 10) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
                ) {
                    Text(
                        text = "Total Stok: $totalStok",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (totalStok < 10) Color(0xFFD32F2F) else Color(0xFF2E7D32),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2-LAYER NILAI BARANG
            val totalStokCombined = item.totalStokCombined
            val nilaiTotalBarang = totalStokCombined.toDouble() * item.hargaModal
            val nilaiStokGudang = item.actualStokUtama.toDouble() * item.hargaModal
            val nilaiStokToko = item.actualStokCabang.toDouble() * item.hargaModal

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    // LAYER 1: Harga Modal Satuan & Total Nilai Barang
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Harga Modal:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${Formatters.formatRupiah(item.hargaModal)} / unit",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Total Nilai:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = Formatters.formatRupiah(nilaiTotalBarang),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // LAYER 2: Rincian Nilai per Lokasi (Gudang & Toko)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFEBEE)
                        ) {
                            Text(
                                text = "Nilai Gudang: ${Formatters.formatRupiah(nilaiStokGudang)} (${item.actualStokUtama} unit)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE53935),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFEBEE)
                        ) {
                            Text(
                                text = "Nilai Toko: ${Formatters.formatRupiah(nilaiStokToko)} (${item.actualStokCabang} unit)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE53935),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aksi Barang",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onViewHistory, modifier = Modifier.size(34.dp)) {
                        Icon(imageVector = Icons.Default.History, contentDescription = "Riwayat", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onReturn, modifier = Modifier.size(34.dp)) {
                        Icon(imageVector = Icons.Default.AssignmentReturn, contentDescription = "Return Stok", tint = Color(0xFFE65100), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ItemFormDialog(
    title: String,
    initialKode: String,
    initialNama: String,
    initialStok: String,
    initialHargaModal: String,
    initialKeterangan: String,
    onDismiss: () -> Unit,
    onConfirm: (kode: String, nama: String, stok: String, harga: String, ket: String) -> Unit
) {
    var kode by remember { mutableStateOf(initialKode) }
    var nama by remember { mutableStateOf(initialNama) }
    var stok by remember { mutableStateOf(initialStok.filter { it.isDigit() }) }
    var harga by remember { mutableStateOf(initialHargaModal.filter { it.isDigit() }) }
    var keterangan by remember { mutableStateOf(initialKeterangan) }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = nama,
                    onValueChange = {
                        nama = it
                        isError = false
                        errorMessage = null
                    },
                    label = { Text("Nama Barang *") },
                    isError = isError && nama.isBlank(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_nama_barang")
                )

                OutlinedTextField(
                    value = kode,
                    onValueChange = { kode = it },
                    label = { Text("Kode Barang (Opsional, misal: F609)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_kode_barang")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stok,
                        onValueChange = { input ->
                            stok = input.filter { it.isDigit() }
                            errorMessage = null
                        },
                        label = { Text("Stok Initial") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = errorMessage?.contains("Stok", ignoreCase = true) == true,
                        modifier = Modifier.weight(1f).testTag("input_stok_barang")
                    )

                    OutlinedTextField(
                        value = harga,
                        onValueChange = { input ->
                            harga = input.filter { it.isDigit() }
                            errorMessage = null
                        },
                        label = { Text("Harga Modal (Rp)") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = errorMessage?.contains("Harga", ignoreCase = true) == true,
                        modifier = Modifier.weight(1f).testTag("input_harga_barang")
                    )
                }

                OutlinedTextField(
                    value = keterangan,
                    onValueChange = { keterangan = it },
                    label = { Text("Keterangan (Opsional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val stokVal = if (stok.isBlank()) 0 else stok.toIntOrNull()
                    val hargaVal = if (harga.isBlank()) 0.0 else harga.toDoubleOrNull()

                    when {
                        nama.isBlank() -> {
                            isError = true
                            errorMessage = "Nama barang wajib diisi"
                        }
                        stokVal == null -> {
                            isError = true
                            errorMessage = "Stok harus berupa angka bulat yang valid"
                        }
                        hargaVal == null -> {
                            isError = true
                            errorMessage = "Harga modal harus berupa angka yang valid"
                        }
                        else -> {
                            onConfirm(kode.trim(), nama.trim(), stokVal.toString(), hargaVal.toInt().toString(), keterangan.trim())
                        }
                    }
                },
                modifier = Modifier.testTag("btn_simpan_barang")
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun StockHistoryDialog(
    item: ItemEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val historyList by viewModel.getStockHistoryForItem(item.id).collectAsStateWithLifecycle(emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = "Riwayat Stok Barang", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = item.namaBarang, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            if (historyList.isEmpty()) {
                Text("Belum ada riwayat stok.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(280.dp)
                ) {
                    items(historyList, key = { it.id }) { history ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                                        text = "${history.jenis} • ${history.keterangan}",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = Formatters.formatTimestamp(history.timestamp),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (history.jumlahPerubahan >= 0) "+${history.jumlahPerubahan}" else "${history.jumlahPerubahan}",
                                        fontWeight = FontWeight.Bold,
                                        color = if (history.jumlahPerubahan >= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                                    )
                                    Text(
                                        text = "${history.stokAwal} ➔ ${history.stokAkhir}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

@Composable
fun ExcelImportDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var importMode by remember { mutableStateOf(0) } // 0 = File, 1 = Tempel Teks
    var parsedItems by remember { mutableStateOf<List<ImportedItemRaw>>(emptyList()) }
    var pastedText by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    var isExpandedPreview by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                selectedFileName = uri.lastPathSegment ?: "File Excel"
                val items = ExcelImportUtils.parseItemsFromUri(context, uri)
                parsedItems = items
                if (items.isEmpty()) {
                    Toast.makeText(context, "Tidak ada data barang valid ditemukan dalam file.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Berhasil membaca ${items.size} barang dari file", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membaca file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
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
                        imageVector = Icons.Default.TableChart,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(text = "Import Data Excel / CSV", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup", modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 400.dp, max = if (isExpandedPreview) 600.dp else 480.dp)
            ) {
                // If not in expanded preview mode, show input controls
                if (!isExpandedPreview) {
                    // Mode Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = importMode == 0,
                            onClick = { importMode = 0 },
                            label = { Text("Pilih File (.xlsx/.csv)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE8F5E9),
                                selectedLabelColor = Color(0xFF1B5E20)
                            )
                        )
                        FilterChip(
                            selected = importMode == 1,
                            onClick = { importMode = 1 },
                            label = { Text("Tempel Teks (Paste)", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE8F5E9),
                                selectedLabelColor = Color(0xFF1B5E20)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (importMode == 0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Pilih file Excel (.xlsx), CSV (.csv) atau TXT dari HP / Google Drive Anda.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = { filePickerLauncher.launch("*/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("btn_pilih_file_excel")
                                ) {
                                    Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pilih File Excel / CSV", fontSize = 12.sp)
                                }
                                if (selectedFileName.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Terpilih: $selectedFileName", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    } else {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Salin dari Excel/Sheets & tempel di bawah:", fontSize = 11.sp)
                                IconButton(
                                    onClick = {
                                        val template = ExcelImportUtils.getSampleExcelTemplate()
                                        clipboardManager.setText(AnnotatedString(template))
                                        Toast.makeText(context, "Contoh format disalin ke clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Salin Contoh Format",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = pastedText,
                                onValueChange = {
                                    pastedText = it
                                    parsedItems = ExcelImportUtils.parseItemsFromPastedText(it)
                                },
                                placeholder = { Text("Paste tabel baris dari Excel di sini...\nFormat: Kode | Nama | Stok | Harga | Ket", fontSize = 11.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(95.dp)
                                    .testTag("input_paste_excel_text")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Format Guide Card
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFF8E1),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Text(
                                text = "📌 Format Urutan Kolom Excel:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF57F17)
                            )
                            Text(
                                text = "Kolom 1: Kode Barang • Kolom 2: Nama Barang • Kolom 3: Stok Awal • Kolom 4: Harga Modal • Kolom 5: Keterangan",
                                fontSize = 10.sp,
                                color = Color(0xFF5D4037)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Preview Table Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Preview Data (${parsedItems.size} Barang Ditemukan):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )

                    if (parsedItems.isNotEmpty()) {
                        TextButton(
                            onClick = { isExpandedPreview = !isExpandedPreview },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isExpandedPreview) "Collapse Layout" else "↕️ Perluas Preview",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (parsedItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada data barang dibaca.\nPilih file Excel/CSV atau tempel teks di atas.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(parsedItems) { item ->
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = item.namaBarang, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        if (item.kodeBarang.isNotBlank()) {
                                            Text(text = "Kode: ${item.kodeBarang}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                        }
                                        if (item.keterangan.isNotBlank()) {
                                            Text(text = "Ket: ${item.keterangan}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFE8F5E9)
                                        ) {
                                            Text(
                                                text = "Stok: ${item.stok}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1B5E20),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = Formatters.formatRupiah(item.hargaModal), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
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
                    if (parsedItems.isNotEmpty() && !isImporting) {
                        isImporting = true
                        viewModel.importItemsBulk(parsedItems) { count ->
                            isImporting = false
                            Toast.makeText(context, "Berhasil mengimpor $count barang ke data stok!", Toast.LENGTH_LONG).show()
                            onDismiss()
                        }
                    }
                },
                enabled = parsedItems.isNotEmpty() && !isImporting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_konfirmasi_impor_excel")
            ) {
                Text(if (isImporting) "Memproses..." else "Impor ${parsedItems.size} Barang", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
fun ReturnStockDialog(
    item: ItemEntity,
    viewModel: MainViewModel,
    availableAccounts: List<com.example.data.entity.AccountTypeInfo>,
    allAccounts: List<com.example.data.entity.CashAccountEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var qtyStr by remember { mutableStateOf("1") }
    var alasan by remember { mutableStateOf("") }
    
    val hasCabangStock = item.actualStokCabang > 0
    var selectedLokasi by remember { mutableStateOf(if (item.actualStokUtama > 0) "Gudang" else "Toko Cabang") }

    var isRefundEnabled by remember { mutableStateOf(false) }
    val initialRefund = remember(item.hargaModal) {
        if (item.hargaModal > 0) item.hargaModal.toLong().toString() else "0"
    }
    var refundAmountStr by remember { mutableStateOf(initialRefund) }
    var targetAccountCode by remember { mutableStateOf("TUNAI") }

    val qtyInt = qtyStr.toIntOrNull() ?: 0
    val maxAvailable = if (selectedLokasi == "Toko Cabang") item.actualStokCabang else item.actualStokUtama
    val isQtyExceed = qtyInt > maxAvailable

    androidx.compose.runtime.LaunchedEffect(qtyInt) {
        if (qtyInt > 0 && item.hargaModal > 0) {
            refundAmountStr = (qtyInt * item.hargaModal).toLong().toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AssignmentReturn,
                    contentDescription = null,
                    tint = Color(0xFFE65100),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Return / Retur Stok Barang",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (item.kodeBarang.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Text(
                                        text = item.kodeBarang,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = item.namaBarang,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Stok Gudang: ${item.actualStokUtama} unit | Stok Toko: ${item.actualStokCabang} unit (Total: ${item.totalStokCombined})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (hasCabangStock && item.actualStokUtama > 0) {
                    Text(text = "Pilih Lokasi Stok yang Di-return:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedLokasi == "Gudang",
                            onClick = { selectedLokasi = "Gudang" },
                            label = { Text("Gudang (${item.actualStokUtama})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedLokasi == "Toko Cabang",
                            onClick = { selectedLokasi = "Toko Cabang" },
                            label = { Text("Toko Cabang (${item.actualStokCabang})", fontSize = 11.sp) }
                        )
                    }
                }

                Text(text = "Jumlah Barang yang Di-return (Unit):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = { input ->
                        qtyStr = input.filter { it.isDigit() }
                    },
                    label = { Text("Jumlah Return (Unit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = isQtyExceed || qtyInt <= 0,
                    modifier = Modifier.fillMaxWidth().testTag("input_qty_return")
                )

                if (isQtyExceed) {
                    Text(
                        text = "⚠️ Jumlah melebihi stok tersedia di $selectedLokasi (Maksimal: $maxAvailable unit)!",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedTextField(
                    value = alasan,
                    onValueChange = { alasan = it },
                    label = { Text("Alasan / Keterangan Return") },
                    placeholder = { Text("misal: Barang Rusak / Cacat Pabrik / Retur Supplier") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_alasan_return")
                )

                Spacer(modifier = Modifier.height(2.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().clickable { isRefundEnabled = !isRefundEnabled }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = isRefundEnabled,
                                    onCheckedChange = { isRefundEnabled = it }
                                )
                                Text(
                                    text = "Ada Pengembalian Dana (Uang Masuk)?",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (isRefundEnabled) {
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = refundAmountStr,
                                onValueChange = { input ->
                                    refundAmountStr = input.filter { it.isDigit() }
                                },
                                label = { Text("Nominal Uang Kembali (Rp)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("input_nominal_refund")
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tujuan Kas / Bank Penerima Pengembalian:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                items(availableAccounts, key = { it.type }) { acc ->
                                    val accSaldo = allAccounts.find { it.accountType == acc.type }?.saldo ?: 0.0
                                    FilterChip(
                                        selected = targetAccountCode == acc.type,
                                        onClick = { targetAccountCode = acc.type },
                                        label = { Text("${acc.name} (${Formatters.formatRupiah(accSaldo)})", fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = if (acc.type == "TUNAI") Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
                                            selectedLabelColor = if (acc.type == "TUNAI") Color(0xFF2E7D32) else Color(0xFF1565C0)
                                        )
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
                    val refundVal = if (isRefundEnabled) (refundAmountStr.toDoubleOrNull() ?: 0.0) else 0.0
                    viewModel.processReturnItemStock(
                        item = item,
                        qtyReturn = qtyInt,
                        alasan = alasan.ifBlank { "Return Stok Barang" },
                        lokasiStok = selectedLokasi,
                        refundAmount = refundVal,
                        targetAccountCode = if (isRefundEnabled) targetAccountCode else null,
                        onSuccess = { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            onDismiss()
                        }
                    )
                },
                enabled = qtyInt > 0 && !isQtyExceed,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                modifier = Modifier.testTag("btn_konfirmasi_return")
            ) {
                Text("Proses Return Stok", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
