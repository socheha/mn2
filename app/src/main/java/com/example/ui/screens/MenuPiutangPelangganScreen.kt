package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.CustomerReceivableEntity
import com.example.ui.MainViewModel
import com.example.ui.components.DatePickerField
import com.example.util.Formatters

@Composable
fun MenuPiutangPelangganScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val receivablesList by viewModel.allReceivables.collectAsStateWithLifecycle()
    val totalUnpaid by viewModel.totalUnpaidReceivables.collectAsStateWithLifecycle()

    val allCashAccounts by viewModel.allCashAccounts.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Belum Lunas") }
    var showAddDialog by remember { mutableStateOf(false) }
    var receivableForPayment by remember { mutableStateOf<CustomerReceivableEntity?>(null) }
    var receivableForConfirmLunas by remember { mutableStateOf<CustomerReceivableEntity?>(null) }
    var receivableForHistory by remember { mutableStateOf<CustomerReceivableEntity?>(null) }
    var receivableToCancel by remember { mutableStateOf<CustomerReceivableEntity?>(null) }

    val totalPiutangTerdaftar = remember(receivablesList) { receivablesList.sumOf { it.nominalAwal } }
    val totalPiutangBelumLunas = remember(receivablesList) { receivablesList.sumOf { it.nominalSisa } }
    val totalPiutangSudahTertagih = remember(receivablesList) { receivablesList.sumOf { (it.nominalAwal - it.nominalSisa).coerceAtLeast(0.0) } }

    val searchMatchingAll = remember(receivablesList, searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else receivablesList.filter { item ->
            item.namaPelanggan.contains(searchQuery, ignoreCase = true) ||
                    item.nomorHp.contains(searchQuery, ignoreCase = true) ||
                    item.catatan.contains(searchQuery, ignoreCase = true)
        }
    }
    val searchTotalSisa = remember(searchMatchingAll) { searchMatchingAll.sumOf { it.nominalSisa } }
    val searchTotalAwal = remember(searchMatchingAll) { searchMatchingAll.sumOf { it.nominalAwal } }
    val searchTotalDibayar = remember(searchMatchingAll) { (searchTotalAwal - searchTotalSisa).coerceAtLeast(0.0) }
    val searchBelumLunasCount = remember(searchMatchingAll) { searchMatchingAll.count { it.nominalSisa > 0 && it.status != "Lunas" } }
    val searchTotalNotaCount = remember(searchMatchingAll) { searchMatchingAll.size }
    val distinctCustomerNames = remember(searchMatchingAll) { searchMatchingAll.map { it.namaPelanggan }.distinct() }

    val filteredReceivables = remember(receivablesList, searchQuery, selectedFilter) {
        receivablesList.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                    item.namaPelanggan.contains(searchQuery, ignoreCase = true) ||
                    item.nomorHp.contains(searchQuery, ignoreCase = true) ||
                    item.catatan.contains(searchQuery, ignoreCase = true)

            val matchesStatus = when (selectedFilter) {
                "Belum Lunas" -> item.status != "Lunas" && item.nominalSisa > 0
                "Nota Lunas" -> item.status == "Lunas" || item.nominalSisa <= 0
                else -> true
            }

            matchesSearch && matchesStatus
        }
    }

    Scaffold(
        modifier = Modifier.testTag("menu_piutang_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Tambah Piutang", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Total Piutang Pelanggan Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F7FA))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF00838F), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Total Piutang Toko Terdaftar",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF006064)
                                    )
                                    Text(
                                        text = "${receivablesList.size} Transaksi Piutang Tercatat",
                                        fontSize = 11.sp,
                                        color = Color(0xFF00838F).copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Large Total Value
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Nilai Piutang:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF37474F)
                                )
                                Text(
                                    text = Formatters.formatRupiah(totalPiutangTerdaftar),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF006064)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Sisa Belum Lunas",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFD32F2F)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = Formatters.formatRupiah(totalPiutangBelumLunas),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC62828)
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "Sudah Tertagih",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF2E7D32)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = Formatters.formatRupiah(totalPiutangSudahTertagih),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B5E20)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column {
                    Text(
                        text = "Daftar Piutang Pelanggan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Cari nama pelanggan, no HP, catatan...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Cari")
                        },
                        trailingIcon = if (searchQuery.isNotEmpty()) {
                            {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Hapus")
                                }
                            }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val filters = listOf(
                            "Belum Lunas" to "Belum Lunas",
                            "Nota Lunas" to "Riwayat Nota Lunas",
                            "Semua" to "Semua Piutang"
                        )
                        items(filters) { (key, label) ->
                            val isSelected = selectedFilter == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedFilter = key },
                                label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (key == "Nota Lunas") Color(0xFFE8F5E9) else MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = if (key == "Nota Lunas") Color(0xFF2E7D32) else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // Highlight Card: Sisa Hutang Pelanggan Hasil Pencarian
            if (searchQuery.isNotBlank() && searchMatchingAll.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (searchTotalSisa > 0) Color(0xFFFFF8E1) else Color(0xFFE8F5E9)
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (searchTotalSisa > 0) Color(0xFFFFB74D) else Color(0xFFA5D6A7)
                        )
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
                                            .size(34.dp)
                                            .background(
                                                if (searchTotalSisa > 0) Color(0xFFE65100) else Color(0xFF2E7D32),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (searchTotalSisa > 0) Icons.Default.Person else Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (distinctCustomerNames.size == 1) "Pelanggan: ${distinctCustomerNames.first()}" else "Hasil Pencarian: \"$searchQuery\"",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (searchTotalSisa > 0) Color(0xFFBF360C) else Color(0xFF1B5E20)
                                        )
                                        Text(
                                            text = "$searchTotalNotaCount Nota Piutang Tercatat",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (searchTotalSisa > 0) Color(0xFFFFE0B2) else Color(0xFFC8E6C9)
                                ) {
                                    Text(
                                        text = if (searchTotalSisa > 0) "$searchBelumLunasCount Belum Lunas" else "Semua Lunas",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (searchTotalSisa > 0) Color(0xFFE65100) else Color(0xFF2E7D32),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Highlighted Box for Sisa Hutang yang Belum Dibayar
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White,
                                shadowElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "SISA HUTANG / PIUTANG BELUM DIBAYAR",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (searchTotalSisa > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = Formatters.formatRupiah(searchTotalSisa),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (searchTotalSisa > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Breakdown details row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.85f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Total Piutang Awal", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(Formatters.formatRupiah(searchTotalAwal), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.85f)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Sudah Dibayar/Tertagih", fontSize = 10.sp, color = Color(0xFF2E7D32))
                                        Text(Formatters.formatRupiah(searchTotalDibayar), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    }
                                }
                            }

                            // If multiple distinct customers match, show clickable chips for each customer
                            if (distinctCustomerNames.size > 1) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Rincian Pelanggan yang Cocok:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF424242)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(distinctCustomerNames) { cName ->
                                        val custItems = searchMatchingAll.filter { it.namaPelanggan.equals(cName, ignoreCase = true) }
                                        val custSisa = custItems.sumOf { it.nominalSisa }
                                        Surface(
                                            onClick = { searchQuery = cName },
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(cName, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    Text(
                                                        text = "Sisa: ${Formatters.formatRupiah(custSisa)}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (custSisa > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (filteredReceivables.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "Tidak ada data piutang pelanggan yang cocok pada tab '$selectedFilter'." else "Belum ada catatan piutang pelanggan.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (searchQuery.isNotBlank() && searchMatchingAll.isNotEmpty() && selectedFilter != "Semua") {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { selectedFilter = "Semua" },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Lihat Semua Status ($searchTotalNotaCount Nota Tersedia)", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                items(filteredReceivables, key = { it.id }) { receivable ->
                    val customerNotes = remember(receivablesList, receivable.namaPelanggan) {
                        receivablesList.filter { it.namaPelanggan.equals(receivable.namaPelanggan, ignoreCase = true) }
                    }
                    val customerTotalRemaining = remember(customerNotes) { customerNotes.sumOf { it.nominalSisa } }
                    val customerNotesCount = remember(customerNotes) { customerNotes.size }

                    ReceivableCard(
                        receivable = receivable,
                        customerTotalRemaining = customerTotalRemaining,
                        customerNotesCount = customerNotesCount,
                        onPayment = { receivableForPayment = receivable },
                        onDirectLunas = { receivableForConfirmLunas = receivable },
                        onViewHistory = { receivableForHistory = receivable },
                        onCancel = { receivableToCancel = receivable }
                    )
                }
            }
        }
    }

    // Cancellation Dialog
    receivableToCancel?.let { receivable ->
        AlertDialog(
            onDismissRequest = { receivableToCancel = null },
            title = { Text("Batalkan Catatan Piutang?", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F)) },
            text = {
                Text(
                    "Apakah Anda yakin ingin membatalkan dan menghapus catatan piutang '${receivable.namaPelanggan}' sebesar ${Formatters.formatRupiah(receivable.nominalAwal)}? Seluruh riwayat pembayarannya juga akan dihapus."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelCustomerReceivable(receivable.id) {
                            receivableToCancel = null
                            Toast.makeText(context, "Catatan piutang berhasil dibatalkan!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Ya, Batalkan Piutang", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { receivableToCancel = null }) {
                    Text("Tidak")
                }
            }
        )
    }

    // Add Receivable Dialog
    if (showAddDialog) {
        AddReceivableDialog(
            existingReceivables = receivablesList,
            onDismiss = { showAddDialog = false },
            onConfirm = { nama, hp, nominal, tgl, cat, jt ->
                viewModel.addReceivable(
                    namaPelanggan = nama,
                    nomorHp = hp,
                    nominal = nominal,
                    tanggal = tgl,
                    catatan = cat,
                    jatuhTempo = jt
                )
                showAddDialog = false
            }
        )
    }

    // Confirmation Dialog for Pelunasan (Ya / Tidak)
    receivableForConfirmLunas?.let { receivable ->
        var selectedAccountCode by remember { mutableStateOf("TUNAI") }
        AlertDialog(
            onDismissRequest = { receivableForConfirmLunas = null },
            title = { Text("Konfirmasi Pelunasan Piutang", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Apakah Anda yakin ingin melunasi seluruh sisa piutang dari ${receivable.namaPelanggan} sebesar ${Formatters.formatRupiah(receivable.nominalSisa)}?",
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Pilih Rekening / Kas Penerima Pembayaran:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedAccountCode == "TUNAI",
                            onClick = { selectedAccountCode = "TUNAI" },
                            label = { Text("Kas Tunai", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE8F5E9),
                                selectedLabelColor = Color(0xFF2E7D32)
                            )
                        )
                        allCashAccounts.filter { it.accountType != "TUNAI" }.forEach { acc ->
                            FilterChip(
                                selected = selectedAccountCode == acc.accountType,
                                onClick = { selectedAccountCode = acc.accountType },
                                label = { Text(acc.accountName, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE3F2FD),
                                    selectedLabelColor = Color(0xFF1565C0)
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markCustomerReceivableLunas(
                            piutangId = receivable.id,
                            tanggal = Formatters.getCurrentDateFormatted(),
                            metodePembayaran = selectedAccountCode
                        )
                        receivableForConfirmLunas = null
                        Toast.makeText(context, "Piutang '${receivable.namaPelanggan}' berhasil dilunasi!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Ya, Pelunasan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { receivableForConfirmLunas = null }) {
                    Text("Tidak / Batal")
                }
            }
        )
    }

    // Payment Dialog (Bayar Sebagian with Bank Accounts)
    receivableForPayment?.let { receivable ->
        PaymentDialog(
            title = "Pembayaran Piutang - ${receivable.namaPelanggan}",
            sisaTagihan = receivable.nominalSisa,
            allAccounts = allCashAccounts,
            onDismiss = { receivableForPayment = null },
            onConfirm = { nominal, tgl, cat, metode ->
                viewModel.addCustomerPayment(
                    piutangId = receivable.id,
                    nominalBayar = nominal,
                    tanggal = tgl,
                    catatan = cat,
                    metodePembayaran = metode
                )
                receivableForPayment = null
                Toast.makeText(context, "Pembayaran piutang ($metode) berhasil dicatat!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Payment History & Cancellation Dialog
    receivableForHistory?.let { receivable ->
        val paymentsList by viewModel.getCustomerPayments(receivable.id).collectAsStateWithLifecycle(emptyList())
        var paymentToCancel by remember { mutableStateOf<com.example.data.entity.CustomerPaymentEntity?>(null) }

        AlertDialog(
            onDismissRequest = { receivableForHistory = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Riwayat Pelunasan - ${receivable.namaPelanggan}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (paymentsList.isEmpty()) {
                        Text(
                            text = "Belum ada riwayat pembayaran untuk piutang ini.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(paymentsList, key = { it.id }) { payment ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp)
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
                                                text = Formatters.formatRupiah(payment.nominalBayar),
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2E7D32),
                                                fontSize = 14.sp
                                            )
                                            val payTime = Formatters.formatTimeOnly(payment.timestamp)
                                            val payDateTime = if (payTime.isNotBlank() && payTime != "00:00") "${Formatters.formatDateToIndonesian(payment.tanggal)} $payTime" else Formatters.formatDateToIndonesian(payment.tanggal)
                                            Text(
                                                text = "Tgl: $payDateTime | Akun: ${payment.metodePembayaran}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (payment.catatan.isNotBlank()) {
                                                Text(
                                                    text = "Catatan: ${payment.catatan}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        TextButton(
                                            onClick = { paymentToCancel = payment },
                                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F))
                                        ) {
                                            Text("Batalkan Pelunasan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { receivableForHistory = null }) {
                    Text("Tutup")
                }
            }
        )

        paymentToCancel?.let { payment ->
            AlertDialog(
                onDismissRequest = { paymentToCancel = null },
                title = { Text("Batalkan Pelunasan?", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F)) },
                text = {
                    Text("Apakah Anda yakin ingin membatalkan pembayaran sebesar ${Formatters.formatRupiah(payment.nominalBayar)}? Sisa piutang akan bertambah kembali dan kas akan disesuaikan.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.cancelCustomerPayment(payment.id) {
                                paymentToCancel = null
                                Toast.makeText(context, "Pelunasan berhasil dibatalkan!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Text("Ya, Batalkan Pelunasan", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { paymentToCancel = null }) {
                        Text("Tidak")
                    }
                }
            )
        }
    }
}

@Composable
fun ReceivableCard(
    receivable: CustomerReceivableEntity,
    customerTotalRemaining: Double = 0.0,
    customerNotesCount: Int = 1,
    onPayment: () -> Unit,
    onDirectLunas: () -> Unit,
    onViewHistory: () -> Unit,
    onCancel: () -> Unit
) {
    val isLunas = receivable.status == "Lunas"
    val isOverdue = !isLunas && Formatters.isOverdue(receivable.jatuhTempo)

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = receivable.namaPelanggan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    if (receivable.nomorHp.isNotBlank()) {
                        Text(
                            text = "HP: ${receivable.nomorHp}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val recTime = Formatters.formatTimeOnly(receivable.timestamp)
                    val recDateTime = if (recTime.isNotBlank() && recTime != "00:00") "${Formatters.formatDateToIndonesian(receivable.tanggal)} $recTime" else Formatters.formatDateToIndonesian(receivable.tanggal)
                    Text(
                        text = "Tgl Transaksi: $recDateTime",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (receivable.jatuhTempo.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = if (isOverdue) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 4.dp).height(12.dp).width(12.dp)
                            )
                            Text(
                                text = "Jatuh Tempo: ${Formatters.formatDateToIndonesian(receivable.jatuhTempo)}",
                                fontSize = 11.sp,
                                fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Medium,
                                color = if (isOverdue) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isLunas) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    ) {
                        Text(
                            text = receivable.status,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isLunas) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    if (isOverdue) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFB71C1C)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.height(10.dp).width(10.dp).padding(end = 2.dp)
                                )
                                Text(
                                    text = "TERLAMBAT",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Nominal Awal Nota:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = Formatters.formatRupiah(receivable.nominalAwal), fontSize = 13.sp)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Sisa Nota Ini:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = Formatters.formatRupiah(receivable.nominalSisa),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLunas) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                    )
                }
            }

            // If customer has multiple notes or active cumulative debt, display banner
            if (customerNotesCount > 1) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (customerTotalRemaining > 0) Color(0xFFFFF3E0) else Color(0xFFE8F5E9),
                    border = BorderStroke(1.dp, if (customerTotalRemaining > 0) Color(0xFFFFCC80) else Color(0xFFA5D6A7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = if (customerTotalRemaining > 0) Color(0xFFE65100) else Color(0xFF2E7D32),
                                modifier = Modifier.size(14.dp).padding(end = 4.dp)
                            )
                            Text(
                                text = "Total Sisa Hutang Pelanggan ($customerNotesCount nota):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (customerTotalRemaining > 0) Color(0xFFBF360C) else Color(0xFF1B5E20)
                            )
                        }
                        Text(
                            text = Formatters.formatRupiah(customerTotalRemaining),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (customerTotalRemaining > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                        )
                    }
                }
            }

            if (receivable.catatan.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Catatan: ${receivable.catatan}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(
                    onClick = onViewHistory,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp).padding(end = 2.dp))
                    Text("Riwayat", fontSize = 10.sp)
                }

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F)),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                ) {
                    Text("Batalkan", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                if (!isLunas) {
                    OutlinedButton(
                        onClick = onPayment,
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp).padding(end = 2.dp))
                        Text("Bayar", fontSize = 10.sp)
                    }

                    Button(
                        onClick = onDirectLunas,
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp).padding(end = 2.dp))
                        Text("Pelunasan", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AddReceivableDialog(
    existingReceivables: List<CustomerReceivableEntity> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (nama: String, hp: String, nominal: Double, tgl: String, cat: String, jatuhTempo: String) -> Unit
) {
    var nama by remember { mutableStateOf("") }
    var hp by remember { mutableStateOf("") }
    var nominalStr by remember { mutableStateOf("") }
    var tanggal by remember { mutableStateOf(Formatters.getCurrentDateFormatted()) }
    var jatuhTempo by remember { mutableStateOf(Formatters.getAddDaysDate(Formatters.getCurrentDateFormatted(), 14)) }
    var catatan by remember { mutableStateOf("") }

    val distinctCustomers = remember(existingReceivables) {
        existingReceivables.map { it.namaPelanggan }.distinct()
    }
    val matchedCustomerNotes = remember(existingReceivables, nama) {
        if (nama.isBlank()) emptyList()
        else existingReceivables.filter { it.namaPelanggan.equals(nama.trim(), ignoreCase = true) }
    }
    val matchedCustomerSisa = remember(matchedCustomerNotes) {
        matchedCustomerNotes.sumOf { it.nominalSisa }
    }
    val matchedCustomerUnpaidCount = remember(matchedCustomerNotes) {
        matchedCustomerNotes.count { it.nominalSisa > 0 && it.status != "Lunas" }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Piutang Pelanggan", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Quick selection of existing customers
                if (distinctCustomers.isNotEmpty() && nama.isBlank()) {
                    Column {
                        Text(
                            text = "Pilih Pelanggan Tersedia:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(distinctCustomers.take(5)) { custName ->
                                val custItems = existingReceivables.filter { it.namaPelanggan.equals(custName, ignoreCase = true) }
                                val custSisa = custItems.sumOf { it.nominalSisa }
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        nama = custName
                                        val latestHp = custItems.firstOrNull { it.nomorHp.isNotBlank() }?.nomorHp.orEmpty()
                                        if (latestHp.isNotBlank()) hp = latestHp
                                    },
                                    label = {
                                        Text(
                                            text = if (custSisa > 0) "$custName (Sisa: ${Formatters.formatRupiah(custSisa)})" else custName,
                                            fontSize = 11.sp
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text("Nama Pelanggan *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Info banner if current customer has outstanding unpaid debt
                if (matchedCustomerNotes.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (matchedCustomerSisa > 0) Color(0xFFFFF3E0) else Color(0xFFE8F5E9),
                        border = BorderStroke(1.dp, if (matchedCustomerSisa > 0) Color(0xFFFFCC80) else Color(0xFFA5D6A7)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = if (matchedCustomerSisa > 0)
                                    "⚠️ Pelanggan memiliki sisa hutang berjalan: ${Formatters.formatRupiah(matchedCustomerSisa)} ($matchedCustomerUnpaidCount nota belum lunas)"
                                else
                                    "✓ Semua piutang sebelumnya dari pelanggan ini telah lunas.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (matchedCustomerSisa > 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = hp,
                    onValueChange = { hp = it },
                    label = { Text("Nomor HP / WhatsApp (Opsional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = nominalStr,
                    onValueChange = { nominalStr = it },
                    label = { Text("Nominal Piutang (Rp) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                DatePickerField(
                    value = tanggal,
                    onDateSelected = { selectedDate ->
                        tanggal = selectedDate
                        jatuhTempo = Formatters.getAddDaysDate(selectedDate, 14)
                    },
                    label = "Tanggal Piutang",
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Text(
                        text = "Tanggal Jatuh Tempo:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = jatuhTempo == Formatters.getAddDaysDate(tanggal, 7),
                            onClick = { jatuhTempo = Formatters.getAddDaysDate(tanggal, 7) },
                            label = { Text("+7 Hari", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = jatuhTempo == Formatters.getAddDaysDate(tanggal, 14),
                            onClick = { jatuhTempo = Formatters.getAddDaysDate(tanggal, 14) },
                            label = { Text("+14 Hari", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = jatuhTempo == Formatters.getAddDaysDate(tanggal, 30),
                            onClick = { jatuhTempo = Formatters.getAddDaysDate(tanggal, 30) },
                            label = { Text("+30 Hari", fontSize = 11.sp) }
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = jatuhTempo,
                        onValueChange = { jatuhTempo = it },
                        label = { Text("Tanggal Jatuh Tempo (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = catatan,
                    onValueChange = { catatan = it },
                    label = { Text("Catatan / Rincian Barang") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val nom = nominalStr.toDoubleOrNull() ?: 0.0
                    if (nama.isNotBlank() && nom > 0) {
                        onConfirm(nama, hp, nom, tanggal, catatan, jatuhTempo)
                    }
                }
            ) {
                Text("Simpan Piutang")
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
fun PaymentDialog(
    title: String,
    sisaTagihan: Double,
    allAccounts: List<com.example.data.entity.CashAccountEntity> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (nominal: Double, tgl: String, cat: String, metode: String) -> Unit
) {
    var nominalStr by remember { mutableStateOf("") }
    var tanggal by remember { mutableStateOf(Formatters.getCurrentDateFormatted()) }
    var catatan by remember { mutableStateOf("") }
    var metodePembayaran by remember { mutableStateOf("TUNAI") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Sisa Tagihan: ${Formatters.formatRupiah(sisaTagihan)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Pilih Rekening / Kas Penerima:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val cashAccounts = allAccounts.filter { com.example.data.entity.CashAccountDefaults.getAccountCategory(it.accountType) == "TUNAI" }
                    if (cashAccounts.isEmpty()) {
                        item {
                            FilterChip(
                                selected = metodePembayaran == "TUNAI" || metodePembayaran == "Tunai",
                                onClick = { metodePembayaran = "TUNAI" },
                                label = { Text("Kas Tunai", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE8F5E9),
                                    selectedLabelColor = Color(0xFF2E7D32)
                                )
                            )
                        }
                    } else {
                        items(cashAccounts, key = { it.accountType }) { acc ->
                            FilterChip(
                                selected = metodePembayaran == acc.accountType,
                                onClick = { metodePembayaran = acc.accountType },
                                label = { Text("${acc.accountName} (${Formatters.formatRupiah(acc.saldo)})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE8F5E9),
                                    selectedLabelColor = Color(0xFF2E7D32)
                                )
                            )
                        }
                    }
                    val transferAccounts = allAccounts.filter { com.example.data.entity.CashAccountDefaults.getAccountCategory(it.accountType) != "TUNAI" }
                    items(transferAccounts, key = { it.accountType }) { acc ->
                        FilterChip(
                            selected = metodePembayaran == acc.accountType,
                            onClick = { metodePembayaran = acc.accountType },
                            label = { Text("${acc.accountName} (${Formatters.formatRupiah(acc.saldo)})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE3F2FD),
                                selectedLabelColor = Color(0xFF1565C0)
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = nominalStr,
                    onValueChange = { nominalStr = it },
                    label = { Text("Nominal Pembayaran (Rp)") },
                    placeholder = { Text(sisaTagihan.toInt().toString()) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                DatePickerField(
                    value = tanggal,
                    onDateSelected = { tanggal = it },
                    label = "Tanggal Bayar",
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = catatan,
                    onValueChange = { catatan = it },
                    label = { Text("Catatan Pembayaran") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val nom = nominalStr.toDoubleOrNull() ?: sisaTagihan
                    if (nom > 0) {
                        onConfirm(nom, tanggal, catatan, metodePembayaran)
                    }
                }
            ) {
                Text("Simpan Pembayaran")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
