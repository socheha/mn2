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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.CashMutationEntity
import com.example.data.entity.IncomingItemEntity
import com.example.data.entity.IncomingTransactionEntity
import com.example.data.entity.SalesItemEntity
import com.example.data.entity.SalesTransactionEntity
import com.example.data.entity.StockHistoryEntity
import com.example.ui.MainViewModel
import com.example.ui.components.DatePickerField
import com.example.util.Formatters
import com.example.util.PrinterUtils

@Composable
fun MenuRekapScreen(
    viewModel: MainViewModel
) {
    val startDate by viewModel.rekapStartDate.collectAsStateWithLifecycle()
    val endDate by viewModel.rekapEndDate.collectAsStateWithLifecycle()
    val transactions by viewModel.rekapTransactions.collectAsStateWithLifecycle()
    val soldItemsSummary by viewModel.rekapSoldItems.collectAsStateWithLifecycle()
    val incomingTransactions by viewModel.allIncomingTransactions.collectAsStateWithLifecycle()
    val stockHistoryList by viewModel.allStockHistory.collectAsStateWithLifecycle()
    val cashMutationsList by viewModel.allCashMutations.collectAsStateWithLifecycle()

    var selectedCategoryFilter by remember { mutableStateOf("Semua") } // "Semua", "Penjualan", "Perpindahan Stok", "Kas", "Barang Masuk"
    var activePreset by remember { mutableStateOf("Harian") } // "Harian", "Mingguan", "Bulanan", "Tahunan", "Kustom"
    var selectedTabIndex by remember { mutableStateOf(0) } // 0: Nota Penjualan, 1: Barang Terjual, 2: Perpindahan Stok, 3: Kas & Mutasi, 4: Barang Masuk
    var searchQuery by remember { mutableStateOf("") }

    var selectedTransactionForDetail by remember { mutableStateOf<SalesTransactionEntity?>(null) }
    var selectedIncomingForDetail by remember { mutableStateOf<IncomingTransactionEntity?>(null) }

    val totalOmzet = remember(transactions) { transactions.sumOf { it.totalUangPenjualan } }
    val totalProfit = remember(transactions) { transactions.sumOf { it.keuntungan } }
    val totalItemsSold = remember(soldItemsSummary) { soldItemsSummary.sumOf { it.totalJumlahTerjual } }

    val filteredTransactions = remember(transactions, searchQuery) {
        if (searchQuery.isBlank()) transactions
        else transactions.filter { tx ->
            tx.id.toString().contains(searchQuery) ||
                    tx.catatan.contains(searchQuery, ignoreCase = true) ||
                    tx.metodePembayaran.contains(searchQuery, ignoreCase = true) ||
                    tx.tanggal.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredSoldItems = remember(soldItemsSummary, searchQuery) {
        if (searchQuery.isBlank()) soldItemsSummary
        else soldItemsSummary.filter { item ->
            item.namaBarang.contains(searchQuery, ignoreCase = true) ||
                    item.kodeBarang.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredIncomingTransactions = remember(incomingTransactions, searchQuery) {
        if (searchQuery.isBlank()) incomingTransactions
        else incomingTransactions.filter { inc ->
            inc.namaSupplier.contains(searchQuery, ignoreCase = true) ||
                    inc.nomorFaktur.contains(searchQuery, ignoreCase = true) ||
                    inc.catatan.contains(searchQuery, ignoreCase = true) ||
                    inc.tanggal.contains(searchQuery, ignoreCase = true)
        }
    }

    val filteredStockHistory = remember(stockHistoryList, startDate, endDate, searchQuery) {
        stockHistoryList.filter { history ->
            val dateStr = Formatters.formatTimestampToDateString(history.timestamp)
            val inRange = dateStr >= startDate && dateStr <= endDate
            val matchesSearch = searchQuery.isBlank() ||
                    history.namaBarang.contains(searchQuery, ignoreCase = true) ||
                    history.kodeBarang.contains(searchQuery, ignoreCase = true) ||
                    history.jenis.contains(searchQuery, ignoreCase = true) ||
                    history.keterangan.contains(searchQuery, ignoreCase = true) ||
                    history.namaToko.contains(searchQuery, ignoreCase = true)
            inRange && matchesSearch
        }
    }

    val filteredCashMutations = remember(cashMutationsList, startDate, endDate, searchQuery) {
        cashMutationsList.filter { mutation ->
            val inRange = mutation.tanggal >= startDate && mutation.tanggal <= endDate
            val matchesSearch = searchQuery.isBlank() ||
                    mutation.kategori.contains(searchQuery, ignoreCase = true) ||
                    mutation.keterangan.contains(searchQuery, ignoreCase = true) ||
                    mutation.accountType.contains(searchQuery, ignoreCase = true) ||
                    mutation.jenis.contains(searchQuery, ignoreCase = true)
            inRange && matchesSearch
        }
    }

    Scaffold(
        modifier = Modifier.testTag("menu_rekap_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Filter Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Assessment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Laporan & Rekapan Toko",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Pilih periode atau tanggal laporan",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        // Preset Filter Chips (Scrollable Row)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = activePreset == "Harian",
                                    onClick = {
                                        activePreset = "Harian"
                                        viewModel.setRekapPreset("Harian")
                                    },
                                    label = { Text("Hari Ini") },
                                    leadingIcon = if (activePreset == "Harian") {
                                        { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                            item {
                                FilterChip(
                                    selected = activePreset == "Mingguan",
                                    onClick = {
                                        activePreset = "Mingguan"
                                        viewModel.setRekapPreset("Mingguan")
                                    },
                                    label = { Text("7 Hari Terakhir") },
                                    leadingIcon = if (activePreset == "Mingguan") {
                                        { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                            item {
                                FilterChip(
                                    selected = activePreset == "Bulanan",
                                    onClick = {
                                        activePreset = "Bulanan"
                                        viewModel.setRekapPreset("Bulanan")
                                    },
                                    label = { Text("Bulan Ini") },
                                    leadingIcon = if (activePreset == "Bulanan") {
                                        { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                            item {
                                FilterChip(
                                    selected = activePreset == "Tahunan",
                                    onClick = {
                                        activePreset = "Tahunan"
                                        viewModel.setRekapPreset("Tahunan")
                                    },
                                    label = { Text("1 Tahun Ini") },
                                    leadingIcon = if (activePreset == "Tahunan") {
                                        { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Date Range Inputs
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            DatePickerField(
                                value = startDate,
                                onDateSelected = { newDate ->
                                    activePreset = "Kustom"
                                    viewModel.setRekapDateRange(newDate, endDate)
                                },
                                label = "Dari Tanggal",
                                modifier = Modifier.weight(1f)
                            )
                            DatePickerField(
                                value = endDate,
                                onDateSelected = { newDate ->
                                    activePreset = "Kustom"
                                    viewModel.setRekapDateRange(startDate, newDate)
                                },
                                label = "Sampai Tanggal",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Summary Totals
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFC8E6C9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AttachMoney,
                                        contentDescription = null,
                                        tint = Color(0xFF1B5E20),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Total Omzet",
                                        fontSize = 11.sp,
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = Formatters.formatRupiah(totalOmzet),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B5E20)
                                    )
                                }
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF86EFAC)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AttachMoney,
                                        contentDescription = null,
                                        tint = Color(0xFF14532D),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Income/Laba",
                                        fontSize = 11.sp,
                                        color = Color(0xFF166534),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = Formatters.formatRupiah(totalProfit),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF14532D)
                                    )
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFBBDEFB)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingBag,
                                        contentDescription = null,
                                        tint = Color(0xFF0D47A1),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Volume Barang Terjual",
                                    fontSize = 12.sp,
                                    color = Color(0xFF1565C0),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "$totalItemsSold Unit",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0D47A1)
                            )
                        }
                    }
                }
            }

            // Search Bar for Rekapan
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari nota, barang, pelanggan, supplier, atau faktur...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_rekap_field")
                )
            }

            // Category Filter Section (Penjualan, Perpindahan Stok, Kas, etc.)
            item {
                Column {
                    Text(
                        text = "Filter Kategori Riwayat:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val categories = listOf(
                            "Semua" to "Semua Riwayat",
                            "Penjualan" to "Penjualan",
                            "Perpindahan Stok" to "Perpindahan Stok",
                            "Kas" to "Kas & Mutasi",
                            "Barang Masuk" to "Barang Masuk"
                        )
                        items(categories) { (key, label) ->
                            val isSelected = selectedCategoryFilter == key
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCategoryFilter = key
                                    when (key) {
                                        "Penjualan" -> selectedTabIndex = 0
                                        "Perpindahan Stok" -> selectedTabIndex = 2
                                        "Kas" -> selectedTabIndex = 3
                                        "Barang Masuk" -> selectedTabIndex = 4
                                        else -> selectedTabIndex = 0
                                    }
                                },
                                label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = {
                                    val icon = when (key) {
                                        "Penjualan" -> Icons.Default.PointOfSale
                                        "Perpindahan Stok" -> Icons.Default.SwapHoriz
                                        "Kas" -> Icons.Default.AccountBalanceWallet
                                        "Barang Masuk" -> Icons.Default.Inventory
                                        else -> Icons.Default.Assessment
                                    }
                                    Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // Tab Selector Section
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val visibleTabs = when (selectedCategoryFilter) {
                        "Penjualan" -> listOf(0 to "Nota Penjualan (${filteredTransactions.size})", 1 to "Barang Terjual (${filteredSoldItems.size})")
                        "Perpindahan Stok" -> listOf(2 to "Perpindahan Stok (${filteredStockHistory.size})")
                        "Kas" -> listOf(3 to "Kas & Mutasi (${filteredCashMutations.size})")
                        "Barang Masuk" -> listOf(4 to "Barang Masuk (${filteredIncomingTransactions.size})")
                        else -> listOf(
                            0 to "Nota (${filteredTransactions.size})",
                            1 to "Terjual (${filteredSoldItems.size})",
                            2 to "Stok (${filteredStockHistory.size})",
                            3 to "Kas (${filteredCashMutations.size})",
                            4 to "Masuk (${filteredIncomingTransactions.size})"
                        )
                    }

                    items(visibleTabs) { (index, title) ->
                        val isSelected = selectedTabIndex == index
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { selectedTabIndex = index }
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            if (selectedTabIndex == 0) {
                // Rincian Penjualan Tersimpan (Nota)
                if (filteredTransactions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = "Tidak ada nota transaksi penjualan ditemukan.",
                                modifier = Modifier.padding(16.dp),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedTransactionForDetail = tx },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.padding(end = 6.dp)
                                        ) {
                                            Text(
                                                text = "Nota #${tx.id}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = Formatters.formatDateToIndonesian(tx.tanggal),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${tx.totalItemTerjual} items • Pembayaran: ${tx.metodePembayaran}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (tx.catatan.isNotBlank()) {
                                        Text(
                                            text = "Catatan: ${tx.catatan}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Klik untuk cek rincian barang >",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = Formatters.formatRupiah(tx.totalUangPenjualan),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { selectedTransactionForDetail = tx },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Print,
                                                contentDescription = "Cetak Nota",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (selectedTabIndex == 1) {
                // Ringkasan Items Terjual
                if (filteredSoldItems.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = "Tidak ada barang terjual ditemukan.",
                                modifier = Modifier.padding(16.dp),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredSoldItems, key = { it.itemId }) { itemSummary ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (itemSummary.kodeBarang.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                modifier = Modifier.padding(end = 6.dp)
                                            ) {
                                                Text(
                                                    text = itemSummary.kodeBarang,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = itemSummary.namaBarang,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Total Nilai: ${Formatters.formatRupiah(itemSummary.totalNilai)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "Terjual: ${itemSummary.totalJumlahTerjual}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (selectedTabIndex == 2) {
                // Tab 2: Perpindahan & Transfer Stok
                if (filteredStockHistory.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = "Tidak ada riwayat perpindahan stok ditemukan pada rentang tanggal ini.",
                                modifier = Modifier.padding(16.dp),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredStockHistory, key = { it.id }) { history ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            modifier = Modifier.padding(end = 6.dp)
                                        ) {
                                            Text(
                                                text = history.jenis,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = history.namaBarang,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Toko: ${history.namaToko} • Waktu: ${Formatters.formatTimestamp(history.timestamp)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Perubahan: ${history.stokAwal} → ${history.stokAkhir} unit",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (history.keterangan.isNotBlank()) {
                                        Text(
                                            text = "Ket: ${history.keterangan}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (history.jumlahPerubahan >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                ) {
                                    Text(
                                        text = if (history.jumlahPerubahan >= 0) "+${history.jumlahPerubahan}" else "${history.jumlahPerubahan}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (history.jumlahPerubahan >= 0) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (selectedTabIndex == 3) {
                // Tab 3: Kas & Mutasi
                if (filteredCashMutations.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = "Tidak ada mutasi kas ditemukan pada rentang tanggal ini.",
                                modifier = Modifier.padding(16.dp),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredCashMutations, key = { it.id }) { mutation ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (mutation.jenis == "MASUK") Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                            modifier = Modifier.padding(end = 6.dp)
                                        ) {
                                            Text(
                                                text = "${mutation.accountType} • ${mutation.jenis}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (mutation.jenis == "MASUK") Color(0xFF2E7D32) else Color(0xFFD32F2F),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = mutation.kategori,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Tanggal: ${Formatters.formatDateToIndonesian(mutation.tanggal)} • Saldo: ${Formatters.formatRupiah(mutation.saldoSesudah)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (mutation.keterangan.isNotBlank()) {
                                        Text(
                                            text = "Ket: ${mutation.keterangan}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                Text(
                                    text = if (mutation.jenis == "MASUK") "+${Formatters.formatRupiah(mutation.nominal)}" else "-${Formatters.formatRupiah(mutation.nominal)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (mutation.jenis == "MASUK") Color(0xFF2E7D32) else Color(0xFFD32F2F)
                                )
                            }
                        }
                    }
                }
            } else {
                // Tab 4: Rekapan Barang Masuk (Faktur Supplier)
                if (filteredIncomingTransactions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = "Tidak ada riwayat barang masuk tersimpan pada rentang tanggal ini.",
                                modifier = Modifier.padding(16.dp),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filteredIncomingTransactions, key = { it.id }) { inc ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedIncomingForDetail = inc },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFE8F5E9),
                                            modifier = Modifier.padding(end = 6.dp)
                                        ) {
                                            Text(
                                                text = "Faktur: ${inc.nomorFaktur.ifBlank { "-" }}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2E7D32),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = inc.namaSupplier.ifBlank { "Supplier Umum" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${inc.tanggal} • Status Bayar: ${inc.statusPembayaran}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (inc.catatan.isNotBlank()) {
                                        Text(
                                            text = "Catatan: ${inc.catatan}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Klik untuk cek rincian barang masuk >",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = Formatters.formatRupiah(inc.totalNilai),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF2E7D32)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Transaction Dialog (Penjualan)
    selectedTransactionForDetail?.let { transaction ->
        TransactionDetailDialog(
            transaction = transaction,
            viewModel = viewModel,
            onDismiss = { selectedTransactionForDetail = null }
        )
    }

    // Detail Transaction Dialog (Barang Masuk)
    selectedIncomingForDetail?.let { incoming ->
        IncomingDetailDialog(
            transaction = incoming,
            viewModel = viewModel,
            onDismiss = { selectedIncomingForDetail = null }
        )
    }
}

@Composable
fun TransactionDetailDialog(
    transaction: SalesTransactionEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<SalesItemEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(transaction.id) {
        items = viewModel.getTransactionItems(transaction.id)
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Rincian Transaksi #${transaction.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${Formatters.formatDateToIndonesian(transaction.tanggal)} • Pembayaran: ${transaction.metodePembayaran}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (items.isEmpty()) {
                Text(
                    text = "Tidak ada item tercatat dalam nota ini.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (transaction.catatan.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Catatan: ${transaction.catatan}",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider()
                    Text(
                        text = "Barang-barang yang terjual:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(items) { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.namaBarang,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${item.jumlahTerjual} pcs @ ${Formatters.formatRupiah(item.hargaSatuan)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = Formatters.formatRupiah(item.totalHarga),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total (${transaction.totalItemTerjual} Item)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = Formatters.formatRupiah(transaction.totalUangPenjualan),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isLoading && items.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            val text = PrinterUtils.generateReceiptTextForTransaction(transaction, items)
                            PrinterUtils.shareReceiptText(context, text, "Nota Penjualan #${transaction.id}")
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bagikan", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val html = PrinterUtils.generateReceiptHtmlForTransaction(transaction, items)
                            PrinterUtils.printReceiptHtml(context, "Nota_${transaction.id}", html)
                        }
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cetak Nota", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(onClick = onDismiss) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

@Composable
fun IncomingDetailDialog(
    transaction: IncomingTransactionEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var items by remember { mutableStateOf<List<IncomingItemEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(transaction.id) {
        items = viewModel.getIncomingTransactionItems(transaction.id)
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Detail Transaksi Barang Masuk",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Supplier: ${transaction.namaSupplier.ifBlank { "Umum" }} • Faktur: ${transaction.nomorFaktur.ifBlank { "-" }}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tanggal Masuk:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(transaction.tanggal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Status Pembayaran:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(transaction.statusPembayaran, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }
                    if (transaction.catatan.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Catatan: ${transaction.catatan}",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider()
                    Text(
                        text = "Daftar Barang Masuk:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    if (items.isEmpty()) {
                        Text("Tidak ada rincian item.", fontSize = 12.sp, color = Color.Gray)
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(items) { item ->
                                val subtotal = item.jumlahMasuk * item.hargaModal
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.namaBarang,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "${item.jumlahMasuk} pcs @ ${Formatters.formatRupiah(item.hargaModal)}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = Formatters.formatRupiah(subtotal),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Pembelian",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = Formatters.formatRupiah(transaction.totalNilai),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup", fontWeight = FontWeight.Bold)
            }
        }
    )
}
