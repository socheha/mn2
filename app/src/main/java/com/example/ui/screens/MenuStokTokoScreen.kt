package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ItemEntity
import com.example.data.entity.StockHistoryEntity
import com.example.ui.MainViewModel
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuStokTokoScreen(viewModel: MainViewModel) {
    val allItems by viewModel.allItems.collectAsState()
    val totalStockCount by viewModel.totalStockCount.collectAsState()
    val totalStockUtama by viewModel.totalStockUtama.collectAsState()
    val totalStockCabang by viewModel.totalStockCabang.collectAsState()

    val allStockHistories by viewModel.allStockHistory.collectAsState(initial = emptyList())
    val transferHistoriesCount = remember(allStockHistories) {
        allStockHistories.count { h ->
            h.jenis.contains("Transfer", ignoreCase = true) ||
                    h.keterangan.contains("Transfer", ignoreCase = true)
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Semua") } // "Semua", "Utama", "Cabang", "Menipis"

    // Dialog States
    var showTransferDialog by remember { mutableStateOf(false) }
    var selectedItemForTransfer by remember { mutableStateOf<ItemEntity?>(null) }

    var showAdjustDialog by remember { mutableStateOf(false) }
    var selectedItemForAdjust by remember { mutableStateOf<ItemEntity?>(null) }

    var showHistoryDialog by remember { mutableStateOf(false) }
    var selectedItemForHistory by remember { mutableStateOf<ItemEntity?>(null) }
    var showTransferHistoryDialog by remember { mutableStateOf(false) }

    // Calculate Asset values
    val totalValueUtama = allItems.sumOf { it.actualStokUtama * it.hargaModal }
    val totalValueCabang = allItems.sumOf { it.actualStokCabang * it.hargaModal }
    val totalValueMaster = allItems.sumOf { it.totalStokCombined * it.hargaModal }

    val filteredItems = remember(allItems, searchQuery, selectedFilter) {
        allItems.filter { item ->
            val matchQuery = searchQuery.isBlank() ||
                    item.namaBarang.contains(searchQuery, ignoreCase = true) ||
                    item.kodeBarang.contains(searchQuery, ignoreCase = true)

            val matchFilter = when (selectedFilter) {
                "Utama" -> item.actualStokUtama > 0
                "Cabang" -> item.actualStokCabang > 0
                "Menipis" -> item.actualStokCabang < 5
                else -> true
            }
            matchQuery && matchFilter
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
            .testTag("menu_stok_toko_screen"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- Header Banner ---
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Manajemen Stok Multi-Toko",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Kelola & Transfer Stok antara Gudang dan Stok Toko",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // --- Store Stock Cards Row ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Card Gudang
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1976D2))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Gudang", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$totalStockUtama Unit",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = Formatters.formatRupiah(totalValueUtama),
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        // Card Stok Toko
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2E7D32))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Stok Toko", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$totalStockCabang Unit",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    text = Formatters.formatRupiah(totalValueCabang),
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        // Card Master Stok Total
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFED6C02))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Stok Master", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$totalStockCount Unit",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFED6C02)
                                )
                                Text(
                                    text = Formatters.formatRupiah(totalValueMaster),
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Main Quick Action Buttons ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = {
                        selectedItemForTransfer = null
                        showTransferDialog = true
                    },
                    modifier = Modifier.weight(1f).testTag("btn_transfer_stok"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Transfer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { showTransferHistoryDialog = true },
                    modifier = Modifier.weight(1f).testTag("btn_riwayat_transfer"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (transferHistoriesCount > 0) "Riwayat Transfer ($transferHistoriesCount)" else "Riwayat Transfer",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- Search Field ---
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_stok_toko_field"),
                placeholder = { Text("Cari barang atau kode SKU...", fontSize = 13.sp) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // --- Filter Chips ---
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val filters = listOf(
                    "Semua" to "Semua Barang",
                    "Utama" to "Ada di Gudang",
                    "Cabang" to "Ada di Stok Toko",
                    "Menipis" to "Stok Toko Menipis (<5)"
                )
                items(filters) { (key, label) ->
                    val isSelected = selectedFilter == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = key },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        // --- Items List ---
        if (filteredItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tidak ada barang ditemukan", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
        } else {
            items(filteredItems, key = { it.id }) { item ->
                ItemStoreStockCard(
                    item = item,
                    onTransferClick = {
                        selectedItemForTransfer = item
                        showTransferDialog = true
                    },
                    onAdjustClick = {
                        selectedItemForAdjust = item
                        showAdjustDialog = true
                    },
                    onHistoryClick = {
                        selectedItemForHistory = item
                        showHistoryDialog = true
                    }
                )
            }
        }
    }

    // --- DIALOG 1: Transfer Stok Antar Toko ---
    if (showTransferDialog) {
        TransferStokDialog(
            allItems = allItems,
            initialItem = selectedItemForTransfer,
            onDismiss = {
                showTransferDialog = false
                selectedItemForTransfer = null
            },
            onConfirm = { item, fromStore, toStore, qty, note ->
                viewModel.transferStockBetweenStores(
                    itemId = item.id,
                    fromStore = fromStore,
                    toStore = toStore,
                    qty = qty,
                    note = note
                )
                showTransferDialog = false
                selectedItemForTransfer = null
            }
        )
    }

    // --- DIALOG 3: Edit / Adjust Stok Direct ---
    if (showAdjustDialog && selectedItemForAdjust != null) {
        AdjustStokDirectDialog(
            item = selectedItemForAdjust!!,
            onDismiss = {
                showAdjustDialog = false
                selectedItemForAdjust = null
            },
            onConfirm = { newUtama, newCabang, note ->
                viewModel.updateStoreStockDirect(
                    itemId = selectedItemForAdjust!!.id,
                    newStokUtama = newUtama,
                    newStokCabang = newCabang,
                    note = note
                )
                showAdjustDialog = false
                selectedItemForAdjust = null
            }
        )
    }

    // --- DIALOG 4: Riwayat Mutasi Stok ---
    if (showHistoryDialog && selectedItemForHistory != null) {
        RiwayatStokItemDialog(
            item = selectedItemForHistory!!,
            viewModel = viewModel,
            onDismiss = {
                showHistoryDialog = false
                selectedItemForHistory = null
            }
        )
    }

    // --- DIALOG 5: Riwayat Transfer Stok Gudang -> Toko ---
    if (showTransferHistoryDialog) {
        RiwayatTransferStokDialog(
            viewModel = viewModel,
            onDismiss = { showTransferHistoryDialog = false }
        )
    }
}

@Composable
fun ItemStoreStockCard(
    item: ItemEntity,
    onTransferClick: () -> Unit,
    onAdjustClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    val sUtama = item.actualStokUtama
    val sCabang = item.actualStokCabang
    val sTotal = item.totalStokCombined

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.namaBarang,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Kode SKU: ${item.kodeBarang.ifBlank { "-" }} • Modal: ${Formatters.formatRupiah(item.hargaModal)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = if (sTotal < 5) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "Total Master: $sTotal Unit",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (sTotal < 5) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Breakdown Per Toko
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Gudang", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    Text(
                        text = "$sUtama Pcs",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (sUtama == 0) Color.Red else MaterialTheme.colorScheme.primary
                    )
                }

                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .width(1.dp)
                        .background(Color.LightGray)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Stok Toko", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    Text(
                        text = "$sCabang Pcs",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (sCabang == 0) Color.Red else Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onHistoryClick, modifier = Modifier.size(32.dp)) {
                    Icon(imageVector = Icons.Default.History, contentDescription = "Riwayat", tint = Color.Gray, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(4.dp))

                OutlinedButton(
                    onClick = onAdjustClick,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Stok", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = onTransferClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Transfer", fontSize = 11.sp)
                }
            }
        }
    }
}

// --- Dialog Transfer Stok ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferStokDialog(
    allItems: List<ItemEntity>,
    initialItem: ItemEntity?,
    onDismiss: () -> Unit,
    onConfirm: (item: ItemEntity, fromStore: String, toStore: String, qty: Int, note: String) -> Unit
) {
    var rawSelectedItem by remember { mutableStateOf(initialItem ?: allItems.firstOrNull()) }
    val selectedItem = remember(rawSelectedItem, allItems) {
        allItems.find { it.id == rawSelectedItem?.id } ?: rawSelectedItem
    }

    var itemExpanded by remember { mutableStateOf(false) }
    var itemSearchQuery by remember { mutableStateOf("") }

    val filteredDropdownItems = remember(allItems, itemSearchQuery) {
        com.example.util.ItemMatcher.searchItems(itemSearchQuery, allItems)
    }

    var fromStore by remember { mutableStateOf("Gudang") }
    var toStore by remember { mutableStateOf("Stok Toko") }
    var qtyText by remember { mutableStateOf("1") }
    var noteText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Transfer Barang Multi-Lokasi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column {
                // Direction Selector Buttons
                Text("Arah Perpindahan Stok:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isGudangToToko = fromStore == "Gudang" || fromStore == "Toko Utama"
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                fromStore = "Gudang"
                                toStore = "Stok Toko"
                                errorMessage = ""
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isGudangToToko) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = if (isGudangToToko) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🏬 Gudang ➔ 🏪 Toko", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isGudangToToko) MaterialTheme.colorScheme.primary else Color.Gray)
                            Text("Kirim ke Toko", fontSize = 10.sp, color = if (isGudangToToko) MaterialTheme.colorScheme.primary else Color.Gray)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                fromStore = "Stok Toko"
                                toStore = "Gudang"
                                errorMessage = ""
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isGudangToToko) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = if (!isGudangToToko) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🏪 Toko ➔ 🏬 Gudang", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (!isGudangToToko) MaterialTheme.colorScheme.primary else Color.Gray)
                            Text("Tarik ke Gudang", fontSize = 10.sp, color = if (!isGudangToToko) MaterialTheme.colorScheme.primary else Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Pilih Barang & Search
                Text("Pilih Barang Ditransfer:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))

                ExposedDropdownMenuBox(
                    expanded = itemExpanded,
                    onExpandedChange = { itemExpanded = !itemExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedItem?.let { "${it.namaBarang} (${it.kodeBarang.ifBlank { "-" }})" } ?: "Pilih Barang Dari Daftar",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = itemExpanded,
                        onDismissRequest = { itemExpanded = false }
                    ) {
                        OutlinedTextField(
                            value = itemSearchQuery,
                            onValueChange = { itemSearchQuery = it },
                            placeholder = { Text("Cari nama/kode barang...", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            trailingIcon = {
                                if (itemSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { itemSearchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Default.Clear, contentDescription = null)
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        HorizontalDivider()

                        if (filteredDropdownItems.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Barang tidak ditemukan", fontSize = 12.sp, color = Color.Gray) },
                                onClick = {}
                            )
                        } else {
                            filteredDropdownItems.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(item.namaBarang, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("🏬 Gudang: ${item.actualStokUtama} | 🏪 Toko: ${item.actualStokCabang}", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    },
                                    onClick = {
                                        rawSelectedItem = item
                                        itemExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Live Stock Breakdown & Calculation
                selectedItem?.let { item ->
                    val isFromGudang = fromStore == "Gudang" || fromStore == "Toko Utama"
                    val availableInSource = if (isFromGudang) item.actualStokUtama else item.actualStokCabang
                    val targetCurrent = if (isFromGudang) item.actualStokCabang else item.actualStokUtama
                    val qty = qtyText.toIntOrNull() ?: 0

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Stok Asal ($fromStore):", fontSize = 11.sp, color = Color.Gray)
                                Text("$availableInSource Pcs", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (availableInSource > 0) MaterialTheme.colorScheme.primary else Color.Red)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Stok Tujuan ($toStore):", fontSize = 11.sp, color = Color.Gray)
                                Text("$targetCurrent Pcs", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }
                            if (qty > 0 && qty <= availableInSource) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Text(
                                    text = "Estimasi Baru ➔ $fromStore: ${availableInSource - qty} Pcs | $toStore: ${targetCurrent + qty} Pcs",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quantity
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = {
                        qtyText = it.filter { char -> char.isDigit() }
                        errorMessage = ""
                    },
                    label = { Text("Jumlah Transfer (Pcs / Unit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Note
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Catatan / Alasan Transfer (Opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                if (errorMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(errorMessage, color = Color.Red, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val item = selectedItem
                    val qty = qtyText.toIntOrNull() ?: 0
                    if (item == null) {
                        errorMessage = "Pilih barang terlebih dahulu"
                        return@Button
                    }
                    if (qty <= 0) {
                        errorMessage = "Masukkan jumlah yang valid (> 0)"
                        return@Button
                    }
                    val isFromGudang = fromStore == "Gudang" || fromStore == "Toko Utama"
                    val available = if (isFromGudang) item.actualStokUtama else item.actualStokCabang
                    if (qty > available) {
                        errorMessage = "Jumlah transfer melebihi stok $fromStore ($available pcs)"
                        return@Button
                    }

                    onConfirm(item, fromStore, toStore, qty, noteText)
                }
            ) {
                Text("Kirim Transfer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

// --- Dialog Adjust Direct ---
@Composable
fun AdjustStokDirectDialog(
    item: ItemEntity,
    onDismiss: () -> Unit,
    onConfirm: (newUtama: Int, newCabang: Int, note: String) -> Unit
) {
    var stokUtamaText by remember { mutableStateOf(item.actualStokUtama.toString()) }
    var stokCabangText by remember { mutableStateOf(item.actualStokCabang.toString()) }
    var noteText by remember { mutableStateOf("") }

    val uVal = stokUtamaText.toIntOrNull() ?: 0
    val cVal = stokCabangText.toIntOrNull() ?: 0
    val totalVal = uVal + cVal

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Stok Per Toko", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column {
                Text(item.namaBarang, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Text("SKU: ${item.kodeBarang.ifBlank { "-" }}", fontSize = 11.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = stokUtamaText,
                    onValueChange = { stokUtamaText = it.filter { char -> char.isDigit() } },
                    label = { Text("Stok Gudang (Pcs)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = stokCabangText,
                    onValueChange = { stokCabangText = it.filter { char -> char.isDigit() } },
                    label = { Text("Stok Toko (Pcs)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Total Master Stok Kombinasi: $totalVal Pcs",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Catatan Penyesuaian") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(uVal, cVal, noteText) }) {
                Text("Simpan Stok")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

// --- Dialog Riwayat Mutasi Stok ---
@Composable
fun RiwayatStokItemDialog(
    item: ItemEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val historyList by viewModel.getStockHistoryForItem(item.id).collectAsState(initial = emptyList())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Riwayat Stok Barang", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(item.namaBarang, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            if (historyList.isEmpty()) {
                Text("Belum ada riwayat mutasi stok untuk barang ini.", color = Color.Gray, fontSize = 12.sp)
            } else {
                LazyColumn(
                    modifier = Modifier.height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(historyList) { h ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(h.jenis, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        if (h.namaToko.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("(${h.namaToko})", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    if (h.keterangan.isNotBlank()) {
                                        Text(h.keterangan, fontSize = 11.sp, color = Color.Gray)
                                    }
                                    Text(Formatters.formatTimestamp(h.timestamp), fontSize = 10.sp, color = Color.Gray)
                                }

                                Text(
                                    text = if (h.jumlahPerubahan >= 0) "+${h.jumlahPerubahan}" else "${h.jumlahPerubahan}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (h.jumlahPerubahan >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}

// --- Dialog Riwayat Transfer Stok Gudang <-> Toko ---
@Composable
fun RiwayatTransferStokDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val allHistory by viewModel.allStockHistory.collectAsState(initial = emptyList())
    var selectedDirectionFilter by remember { mutableStateOf("SEMUA") } // "SEMUA", "GUDANG_TO_TOKO", "TOKO_TO_GUDANG"
    var searchQuery by remember { mutableStateOf("") }

    // Helper functions to identify transfer direction
    fun isGudangToTokoTransfer(h: StockHistoryEntity): Boolean {
        val jenis = h.jenis
        val namaToko = h.namaToko
        val ket = h.keterangan
        return (jenis.contains("Gudang ➔ Toko", ignoreCase = true) ||
                jenis.contains("Gudang -> Toko", ignoreCase = true) ||
                namaToko.contains("Gudang -> Toko", ignoreCase = true) ||
                namaToko.contains("Gudang ➔", ignoreCase = true) ||
                (ket.contains("dari Gudang", ignoreCase = true) && ket.contains("ke Toko", ignoreCase = true)) ||
                (ket.contains("Gudang ke Toko", ignoreCase = true)))
    }

    fun isTokoToGudangTransfer(h: StockHistoryEntity): Boolean {
        val jenis = h.jenis
        val namaToko = h.namaToko
        val ket = h.keterangan
        return (jenis.contains("Toko ➔ Gudang", ignoreCase = true) ||
                jenis.contains("Toko -> Gudang", ignoreCase = true) ||
                namaToko.contains("Toko -> Gudang", ignoreCase = true) ||
                namaToko.contains("Toko ➔ Gudang", ignoreCase = true) ||
                namaToko.contains("Stok Toko -> Gudang", ignoreCase = true) ||
                (ket.contains("dari Stok Toko", ignoreCase = true) && ket.contains("ke Gudang", ignoreCase = true)) ||
                (ket.contains("dari Toko", ignoreCase = true) && ket.contains("ke Gudang", ignoreCase = true)) ||
                (ket.contains("Toko ke Gudang", ignoreCase = true)))
    }

    val allTransferHistories = remember(allHistory) {
        allHistory.filter { h ->
            h.jenis.contains("Transfer", ignoreCase = true) ||
                    h.keterangan.contains("Transfer", ignoreCase = true) ||
                    h.namaToko.contains("->", ignoreCase = true) ||
                    h.namaToko.contains("➔", ignoreCase = true)
        }
    }

    val totalTransferCount = allTransferHistories.size
    val totalQtyToToko = remember(allTransferHistories) {
        allTransferHistories.filter { isGudangToTokoTransfer(it) }.sumOf { kotlin.math.abs(it.jumlahPerubahan) }
    }
    val totalQtyToGudang = remember(allTransferHistories) {
        allTransferHistories.filter { isTokoToGudangTransfer(it) }.sumOf { kotlin.math.abs(it.jumlahPerubahan) }
    }

    val filteredHistories = remember(allTransferHistories, selectedDirectionFilter, searchQuery) {
        allTransferHistories.filter { h ->
            val matchesDirection = when (selectedDirectionFilter) {
                "GUDANG_TO_TOKO" -> isGudangToTokoTransfer(h)
                "TOKO_TO_GUDANG" -> isTokoToGudangTransfer(h)
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                    h.namaBarang.contains(searchQuery, ignoreCase = true) ||
                    h.kodeBarang.contains(searchQuery, ignoreCase = true) ||
                    h.keterangan.contains(searchQuery, ignoreCase = true) ||
                    h.namaToko.contains(searchQuery, ignoreCase = true)

            matchesDirection && matchesQuery
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CompareArrows, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Riwayat Transfer Barang", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Gudang ⇄ Stok Toko", fontSize = 11.sp, color = Color.Gray)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Summary Metrics Banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Total Transfer", fontSize = 10.sp, color = Color.Gray)
                            Text("$totalTransferCount", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🏬 ➔ 🏪 Toko", fontSize = 10.sp, color = Color(0xFF1565C0), fontWeight = FontWeight.SemiBold)
                            Text("$totalQtyToToko Unit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🏪 ➔ 🏬 Gudang", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
                            Text("$totalQtyToGudang Unit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Direction Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedDirectionFilter == "SEMUA",
                            onClick = { selectedDirectionFilter = "SEMUA" },
                            label = { Text("Semua Arah", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedDirectionFilter == "GUDANG_TO_TOKO",
                            onClick = { selectedDirectionFilter = "GUDANG_TO_TOKO" },
                            label = { Text("🏬 Gudang ➔ Toko", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedDirectionFilter == "TOKO_TO_GUDANG",
                            onClick = { selectedDirectionFilter = "TOKO_TO_GUDANG" },
                            label = { Text("🏪 Toko ➔ Gudang", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari barang / catatan...", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(18.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // History List
                if (filteredHistories.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Default.CompareArrows, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Tidak ada riwayat transfer ditemukan", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredHistories) { h ->
                            val isGudangToToko = isGudangToTokoTransfer(h)
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp)
                                ) {
                                    // Row 1: Item Name and Transferred Qty
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = h.namaBarang,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = if (isGudangToToko) Color(0xFF1565C0) else Color(0xFF2E7D32),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "${kotlin.math.abs(h.jumlahPerubahan)} Unit",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Row 2: Direction Badge
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = if (isGudangToToko) Color(0xFFE3F2FD) else Color(0xFFE8F5E9),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (isGudangToToko) Icons.Default.ArrowForward else Icons.Default.ArrowBack,
                                                    contentDescription = null,
                                                    tint = if (isGudangToToko) Color(0xFF1565C0) else Color(0xFF2E7D32),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isGudangToToko) "Gudang ➔ Stok Toko" else "Stok Toko ➔ Gudang",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isGudangToToko) Color(0xFF1565C0) else Color(0xFF2E7D32)
                                                )
                                            }
                                        }

                                        if (h.kodeBarang.isNotBlank()) {
                                            Text("SKU: ${h.kodeBarang}", fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }

                                    // Row 3: Description / Note
                                    if (h.keterangan.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = h.keterangan,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Row 4: Timestamp & Initial/Final Stock
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = Formatters.formatTimestamp(h.timestamp),
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                        if (h.stokAwal > 0 || h.stokAkhir > 0) {
                                            Text(
                                                text = "Stok: ${h.stokAwal} ➔ ${h.stokAkhir}",
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}
