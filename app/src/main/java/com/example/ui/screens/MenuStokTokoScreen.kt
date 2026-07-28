package com.example.ui.screens

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

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Semua") } // "Semua", "Utama", "Cabang", "Menipis"

    // Dialog States
    var showTransferDialog by remember { mutableStateOf(false) }
    var selectedItemForTransfer by remember { mutableStateOf<ItemEntity?>(null) }

    var showInputStokLainDialog by remember { mutableStateOf(false) }
    var selectedItemForInputLain by remember { mutableStateOf<ItemEntity?>(null) }

    var showAdjustDialog by remember { mutableStateOf(false) }
    var selectedItemForAdjust by remember { mutableStateOf<ItemEntity?>(null) }

    var showHistoryDialog by remember { mutableStateOf(false) }
    var selectedItemForHistory by remember { mutableStateOf<ItemEntity?>(null) }

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
                "Menipis" -> item.totalStokCombined < 10
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Transfer Stok", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        selectedItemForInputLain = null
                        showInputStokLainDialog = true
                    },
                    modifier = Modifier.weight(1f).testTag("btn_input_stok_lain"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Stok Toko Lain", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                    "Menipis" to "Stok Menipis (<10)"
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

    // --- DIALOG 2: Input Stok Toko Lain / Direct Add ---
    if (showInputStokLainDialog) {
        InputStokTokoLainDialog(
            allItems = allItems,
            initialItem = selectedItemForInputLain,
            onDismiss = {
                showInputStokLainDialog = false
                selectedItemForInputLain = null
            },
            onConfirm = { item, targetStore, qty, note ->
                viewModel.addStockToStore(
                    itemId = item.id,
                    targetStore = targetStore,
                    qtyToAdd = qty,
                    note = note
                )
                showInputStokLainDialog = false
                selectedItemForInputLain = null
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
        if (itemSearchQuery.isBlank()) allItems
        else allItems.filter {
            it.namaBarang.contains(itemSearchQuery, ignoreCase = true) ||
                    it.kodeBarang.contains(itemSearchQuery, ignoreCase = true)
        }
    }

    var fromStore by remember { mutableStateOf("Gudang") }
    var toStore by remember { mutableStateOf("Stok Toko") }
    var qtyText by remember { mutableStateOf("1") }
    var noteText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transfer Stok Antar Toko", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column {
                // Pilih Barang & Search
                Text("Cari & Pilih Barang Ditransfer:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = itemSearchQuery,
                    onValueChange = {
                        itemSearchQuery = it
                        itemExpanded = true
                    },
                    placeholder = { Text("Ketik nama atau kode barang...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (itemSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { itemSearchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("search_transfer_item_field"),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = itemExpanded,
                    onExpandedChange = { itemExpanded = !itemExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedItem?.let { "${it.namaBarang} (${it.kodeBarang})" } ?: "Pilih Barang Dari Daftar",
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
                                            Text("Gudang: ${item.actualStokUtama} | Stok Toko: ${item.actualStokCabang}", fontSize = 11.sp, color = Color.Gray)
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

                Spacer(modifier = Modifier.height(10.dp))

                // Asal -> Tujuan Store Selector
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dari:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row {
                            FilterChip(
                                selected = fromStore == "Gudang" || fromStore == "Toko Utama",
                                onClick = {
                                    fromStore = "Gudang"
                                    toStore = "Stok Toko"
                                },
                                label = { Text("Gudang", fontSize = 11.sp) }
                            )
                        }
                    }

                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = Color.Gray)

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("Ke:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row {
                            FilterChip(
                                selected = toStore == "Stok Toko" || toStore == "Toko Cabang",
                                onClick = {
                                    fromStore = "Gudang"
                                    toStore = "Stok Toko"
                                },
                                label = { Text("Stok Toko", fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Swap Direction Button
                TextButton(
                    onClick = {
                        val temp = fromStore
                        fromStore = toStore
                        toStore = temp
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tukar Arah ($fromStore ➔ $toStore)", fontSize = 11.sp)
                }

                selectedItem?.let { item ->
                    val isFromGudangOrUtama = fromStore == "Gudang" || fromStore == "Toko Utama" || fromStore.contains("Gudang", ignoreCase = true) || fromStore.contains("Utama", ignoreCase = true)
                    val availableInFrom = if (isFromGudangOrUtama) item.actualStokUtama else item.actualStokCabang
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Stok Tersedia di $fromStore: $availableInFrom Pcs",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(8.dp)
                        )
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
                    label = { Text("Jumlah Transfer (Pcs)") },
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
                    val isFromGudangOrUtama = fromStore == "Gudang" || fromStore == "Toko Utama" || fromStore.contains("Gudang", ignoreCase = true) || fromStore.contains("Utama", ignoreCase = true)
                    val available = if (isFromGudangOrUtama) item.actualStokUtama else item.actualStokCabang
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

// --- Dialog Input Stok Toko Lain ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputStokTokoLainDialog(
    allItems: List<ItemEntity>,
    initialItem: ItemEntity?,
    onDismiss: () -> Unit,
    onConfirm: (item: ItemEntity, targetStore: String, qty: Int, note: String) -> Unit
) {
    var selectedItem by remember { mutableStateOf(initialItem ?: allItems.firstOrNull()) }
    var itemExpanded by remember { mutableStateOf(false) }

    var targetStore by remember { mutableStateOf("Stok Toko") }
    var qtyText by remember { mutableStateOf("1") }
    var noteText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("+ Input Stok Toko Lain ke Master", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column {
                Text(
                    text = "Fitur ini menambahkan stok dari Toko Lain/Cabang langsung terinput ke Stok Master Barang.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text("Pilih Barang:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))

                ExposedDropdownMenuBox(
                    expanded = itemExpanded,
                    onExpandedChange = { itemExpanded = !itemExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedItem?.namaBarang ?: "Pilih Barang",
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
                        allItems.forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(item.namaBarang, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Master Stok: ${item.totalStokCombined} pcs", fontSize = 11.sp, color = Color.Gray)
                                    }
                                },
                                onClick = {
                                    selectedItem = item
                                    itemExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Lokasi Toko Tujuan:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = targetStore == "Stok Toko" || targetStore == "Toko Cabang",
                        onClick = { targetStore = "Stok Toko" },
                        label = { Text("Stok Toko", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = targetStore == "Gudang" || targetStore == "Toko Utama",
                        onClick = { targetStore = "Gudang" },
                        label = { Text("Gudang", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = qtyText,
                    onValueChange = {
                        qtyText = it.filter { char -> char.isDigit() }
                        errorMessage = ""
                    },
                    label = { Text("Jumlah Tambahan Stok (Pcs)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Keterangan Sumber Stok (Opsional)") },
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

                    onConfirm(item, targetStore, qty, noteText)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("+ Tambah ke Stok")
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
