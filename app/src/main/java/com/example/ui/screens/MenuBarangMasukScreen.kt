package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.LaunchedEffect
import com.example.ui.components.CalculatorDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.example.ui.MainViewModel
import com.example.ui.components.DatePickerField
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.foundation.layout.size
import com.example.ui.components.ScanReceiptDialog
import com.example.ui.components.SearchableItemAutocomplete
import com.example.util.Formatters

@Composable
fun MenuBarangMasukScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val allItemsList by viewModel.allItems.collectAsStateWithLifecycle()
    val incomingCart by viewModel.incomingCart.collectAsStateWithLifecycle()
    val allCashAccounts by viewModel.allCashAccounts.collectAsStateWithLifecycle()
    val allIncomingTransactions by viewModel.allIncomingTransactions.collectAsStateWithLifecycle()

    var incomingTransactionToCancel by remember { mutableStateOf<com.example.data.entity.IncomingTransactionEntity?>(null) }
    var incomingTransactionToEdit by remember { mutableStateOf<com.example.data.entity.IncomingTransactionEntity?>(null) }

    val bankAccounts = remember(allCashAccounts) {
        allCashAccounts.filter { com.example.data.entity.CashAccountDefaults.getAccountCategory(it.accountType) != "TUNAI" }
    }

    val cashAccounts = remember(allCashAccounts) {
        val list = allCashAccounts.filter { com.example.data.entity.CashAccountDefaults.getAccountCategory(it.accountType) == "TUNAI" }
        if (list.isNotEmpty()) list else listOf(com.example.data.entity.CashAccountEntity(accountType = "TUNAI", accountName = "Kas Tunai Toko", saldo = 0.0))
    }

    var tanggal by remember { mutableStateOf(Formatters.getCurrentDateFormatted()) }
    var namaSupplier by remember { mutableStateOf("") }
    var nomorFaktur by remember { mutableStateOf("") }
    var catatan by remember { mutableStateOf("") }
    var statusPembayaran by remember { mutableStateOf("Tunai") } // "Tunai", "Transfer", "Tunai & Transfer", "Hutang"
    var selectedBankCode by remember { mutableStateOf("BANK") }
    var selectedCashCode by remember { mutableStateOf("TUNAI") }
    var nominalTunaiSplitInput by remember { mutableStateOf("") }
    var nominalTransferSplitInput by remember { mutableStateOf("") }
    var calcTargetField by remember { mutableStateOf<String?>(null) } // "split_tunai", "split_transfer", or cart item ID string
    var showScanReceiptDialog by remember { mutableStateOf(false) }
    var showCreateNewItemDialog by remember { mutableStateOf(false) }
    var newInitialItemName by remember { mutableStateOf("") }

    androidx.compose.runtime.LaunchedEffect(allCashAccounts) {
        if (bankAccounts.isNotEmpty() && (selectedBankCode == "BANK" || bankAccounts.none { it.accountType == selectedBankCode })) {
            selectedBankCode = bankAccounts.first().accountType
        } else if (allCashAccounts.isNotEmpty() && allCashAccounts.none { it.accountType == selectedBankCode }) {
            selectedBankCode = allCashAccounts.first().accountType
        }
        if (cashAccounts.isNotEmpty() && (selectedCashCode == "TUNAI" || cashAccounts.none { it.accountType == selectedCashCode })) {
            selectedCashCode = cashAccounts.first().accountType
        }
    }

    val totalNilai = remember(incomingCart) { incomingCart.sumOf { it.jumlahMasuk * it.hargaModal } }
    val totalItemCount = remember(incomingCart) { incomingCart.sumOf { it.jumlahMasuk } }

    val excludedIds = remember(incomingCart) { incomingCart.map { it.item.id } }

    Scaffold(
        modifier = Modifier.testTag("menu_barang_masuk_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalShipping,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Penerimaan Barang Masuk",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DatePickerField(
                                value = tanggal,
                                onDateSelected = { tanggal = it },
                                label = "Tanggal Masuk",
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = nomorFaktur,
                                onValueChange = { nomorFaktur = it },
                                label = { Text("No. Faktur (Opsional)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = namaSupplier,
                            onValueChange = { namaSupplier = it },
                            label = { Text("Nama Supplier / Toko Pemasok *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = catatan,
                            onValueChange = { catatan = it },
                            label = { Text("Catatan Masuk (Opsional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Searchable Autocomplete Bar for Adding Items
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tambah Barang Masuk",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedButton(
                                onClick = {
                                    newInitialItemName = ""
                                    showCreateNewItemDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Barang Baru", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ketik nama/kode barang untuk langsung memilih:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val draftQuantitiesMap = remember(incomingCart) {
                            incomingCart.associate { it.item.id to it.jumlahMasuk }
                        }

                        SearchableItemAutocomplete(
                            itemsList = allItemsList,
                            draftItemQuantities = draftQuantitiesMap,
                            isPenjualanMode = false,
                            onItemSelectedWithQtyAndPrice = { selectedItem, qty, customCost ->
                                val currentQty = draftQuantitiesMap[selectedItem.id] ?: 0
                                viewModel.addIncomingCartItem(selectedItem, qty, customCost)
                                val newQty = currentQty + qty
                                val priceFormatted = if (customCost != null && customCost > 0.0) " @ ${Formatters.formatRupiah(customCost)}" else ""
                                val toastMsg = if (currentQty > 0) {
                                    "'${selectedItem.namaBarang}' ditambah (+$qty) di draf$priceFormatted (Total: $newQty)"
                                } else {
                                    "'${selectedItem.namaBarang}' x$qty dimasukkan ke draf$priceFormatted"
                                }
                                Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                            },
                            onAddNewItemClick = { queryName ->
                                newInitialItemName = queryName
                                showCreateNewItemDialog = true
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            onClick = { showScanReceiptDialog = true },
                            modifier = Modifier.fillMaxWidth().testTag("btn_scan_nota_masuk"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Scan Nota Pembelian",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "📷 Scan Foto Nota Pembelian",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Deteksi Otomatis AI Gemini & MLKit Offline + Cocokkan Database",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Button(
                                    onClick = { showScanReceiptDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Scan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // List of Selected Items
            item {
                Text(
                    text = "Daftar Barang Masuk (${incomingCart.size} Jenis)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (incomingCart.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = "Belum ada barang dipilih. Gunakan pencarian di atas untuk menambah barang.",
                            modifier = Modifier.padding(16.dp),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(incomingCart, key = { it.item.id }) { cartItem ->
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (cartItem.item.kodeBarang.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                modifier = Modifier.padding(end = 6.dp)
                                            ) {
                                                Text(
                                                    text = cartItem.item.kodeBarang,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = cartItem.item.namaBarang,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Stok Saat Ini: ${cartItem.item.stok} unit",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.removeIncomingCartItem(cartItem.item.id) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Hapus",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = if (cartItem.jumlahMasuk == 0) "" else cartItem.jumlahMasuk.toString(),
                                    onValueChange = { input ->
                                        val filtered = input.filter { char -> char.isDigit() }
                                        val qty = if (filtered.isBlank()) 0 else (filtered.toIntOrNull() ?: 0)
                                        viewModel.updateIncomingCartQuantity(cartItem.item.id, qty)
                                    },
                                    label = { Text("Jumlah Masuk") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = if (cartItem.hargaModal > 0) cartItem.hargaModal.toInt().toString() else "",
                                    onValueChange = { input ->
                                        val filtered = input.filter { char -> char.isDigit() }
                                        val cost = filtered.toDoubleOrNull() ?: 0.0
                                        viewModel.updateIncomingCartCost(cartItem.item.id, cost)
                                    },
                                    label = { Text("Harga Modal (Rp)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Price Change & Weighted Average Notification Banner
                            val currentMasterItem = allItemsList.find { it.id == cartItem.item.id } ?: cartItem.item
                            val stokAda = currentMasterItem.totalStokCombined.coerceAtLeast(0)
                            val masukQty = cartItem.jumlahMasuk.coerceAtLeast(1)
                            val totalGabungan = stokAda + masukQty
                            val hargaLama = currentMasterItem.hargaModal
                            val hargaMasuk = cartItem.hargaModal
                            val isPriceDifferent = hargaMasuk > 0.0 && (hargaLama <= 0.0 || hargaMasuk != hargaLama)
                            val calculatedAvgCost = if (stokAda > 0 && hargaLama > 0.0 && hargaMasuk > 0.0) {
                                kotlin.math.round(((stokAda * hargaLama) + (masukQty * hargaMasuk)) / totalGabungan)
                            } else {
                                hargaMasuk
                            }
                            val selisihHargaPerUnit = hargaMasuk - hargaLama

                            if (isPriceDifferent) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "⚖️ Penyesuaian Harga Otomatis",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (hargaLama > 0.0) {
                                                val tanda = if (selisihHargaPerUnit >= 0) "+" else ""
                                                Text(
                                                    text = "Selisih: $tanda${Formatters.formatRupiah(selisihHargaPerUnit)}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (selisihHargaPerUnit >= 0) Color(0xFFC62828) else Color(0xFF2E7D32)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Stok Ada: $stokAda unit @ ${Formatters.formatRupiah(hargaLama)}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "Masuk: $masukQty unit @ ${Formatters.formatRupiah(hargaMasuk)}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                modifier = Modifier.padding(start = 4.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                                    Text(
                                                        text = "HPP Otomatis:",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        text = Formatters.formatRupiah(calculatedAvgCost),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "✅ Saat transaksi disimpan, master barang otomatis disesuaikan ke HPP ${Formatters.formatRupiah(calculatedAvgCost)} (berdasarkan total $totalGabungan unit).",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.updateItemPriceDirect(cartItem.item.id, calculatedAvgCost) {
                                                        Toast.makeText(context, "HPP master '${cartItem.item.namaBarang}' langsung diperbarui ke ${Formatters.formatRupiah(calculatedAvgCost)}", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("⚡ Update Master Sekarang", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }

                                            if (hargaLama > 0.0) {
                                                TextButton(
                                                    onClick = {
                                                        viewModel.updateIncomingCartCost(cartItem.item.id, hargaLama)
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("Pakai Harga Lama", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Subtotal: ${Formatters.formatRupiah(cartItem.jumlahMasuk * cartItem.hargaModal)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }

            // Bottom Summary & Payment Method & Save Button
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Ringkasan Transaksi Masuk",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Total Item Barang:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "$totalItemCount Unit", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Total Nilai Pembelian:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = Formatters.formatRupiah(totalNilai),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Status Pembayaran Ke Supplier:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = statusPembayaran == "Tunai",
                                onClick = { statusPembayaran = "Tunai" },
                                label = { Text("Tunai", fontSize = 11.sp) },
                                leadingIcon = if (statusPembayaran == "Tunai") {
                                    { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE8F5E9),
                                    selectedLabelColor = Color(0xFF2E7D32)
                                )
                            )

                            FilterChip(
                                selected = statusPembayaran == "Transfer",
                                onClick = { statusPembayaran = "Transfer" },
                                label = { Text("Transfer Bank", fontSize = 11.sp) },
                                leadingIcon = if (statusPembayaran == "Transfer") {
                                    { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE3F2FD),
                                    selectedLabelColor = Color(0xFF1565C0)
                                )
                            )

                            FilterChip(
                                selected = statusPembayaran == "Tunai & Transfer",
                                onClick = { statusPembayaran = "Tunai & Transfer" },
                                label = { Text("Tunai & Transfer", fontSize = 11.sp) },
                                leadingIcon = if (statusPembayaran == "Tunai & Transfer") {
                                    { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFF3E0),
                                    selectedLabelColor = Color(0xFFE65100)
                                )
                            )

                            FilterChip(
                                selected = statusPembayaran == "Hutang",
                                onClick = { statusPembayaran = "Hutang" },
                                label = { Text("Hutang", fontSize = 11.sp) },
                                leadingIcon = if (statusPembayaran == "Hutang") {
                                    { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFEBEE),
                                    selectedLabelColor = Color(0xFFD32F2F)
                                )
                            )
                        }

                        if (statusPembayaran == "Tunai") {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                Text(
                                    text = "Pilih Akun Kas Tunai Sumber:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(cashAccounts, key = { it.accountType }) { cash ->
                                        FilterChip(
                                            selected = selectedCashCode == cash.accountType,
                                            onClick = { selectedCashCode = cash.accountType },
                                            label = { Text("${cash.accountName} (${Formatters.formatRupiah(cash.saldo)})") },
                                            leadingIcon = if (selectedCashCode == cash.accountType) {
                                                { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            } else null,
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFFE8F5E9),
                                                selectedLabelColor = Color(0xFF2E7D32)
                                            )
                                        )
                                    }
                                }
                                val selectedCashName = cashAccounts.find { it.accountType == selectedCashCode }?.accountName ?: "Kas Tunai"
                                Text(
                                    text = "* Mengurangi saldo $selectedCashName sebesar ${Formatters.formatRupiah(totalNilai)}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        } else if (statusPembayaran == "Transfer") {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                Text(
                                    text = "Pilih Bank / E-Wallet Pembayaran (dari Kas & Bank):",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val availableAccounts = if (bankAccounts.isNotEmpty()) bankAccounts else allCashAccounts
                                if (availableAccounts.isEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFFF3E0),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Belum ada akun bank yang diinput di menu Kas & Bank. Anda dapat menambahkannya di menu Kas & Bank.",
                                            fontSize = 11.sp,
                                            color = Color(0xFFE65100),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                } else {
                                    androidx.compose.foundation.lazy.LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(availableAccounts, key = { it.accountType }) { bank ->
                                            FilterChip(
                                                selected = selectedBankCode == bank.accountType,
                                                onClick = { selectedBankCode = bank.accountType },
                                                label = { Text("${bank.accountName} (${Formatters.formatRupiah(bank.saldo)})") },
                                                leadingIcon = if (selectedBankCode == bank.accountType) {
                                                    { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                                } else null,
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Color(0xFFE3F2FD),
                                                    selectedLabelColor = Color(0xFF1565C0)
                                                )
                                            )
                                        }
                                    }
                                }
                                val selectedAccountName = allCashAccounts.find { it.accountType == selectedBankCode }?.accountName ?: selectedBankCode
                                Text(
                                    text = "* Mengurangi saldo $selectedAccountName sebesar ${Formatters.formatRupiah(totalNilai)}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF1565C0),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        } else if (statusPembayaran == "Tunai & Transfer") {
                            Column(
                                modifier = Modifier.padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Rincian Pembayaran Sebagian Tunai & Sebagian Transfer:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )

                                Text(
                                    text = "1. Pilih Akun Kas Tunai Sumber:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(cashAccounts, key = { it.accountType }) { cash ->
                                        FilterChip(
                                            selected = selectedCashCode == cash.accountType,
                                            onClick = { selectedCashCode = cash.accountType },
                                            label = { Text("${cash.accountName} (${Formatters.formatRupiah(cash.saldo)})") },
                                            leadingIcon = if (selectedCashCode == cash.accountType) {
                                                { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            } else null,
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFFE8F5E9),
                                                selectedLabelColor = Color(0xFF2E7D32)
                                            )
                                        )
                                    }
                                }

                                Text(
                                    text = "2. Pilih Bank / E-Wallet Pembayaran:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val availableAccounts = if (bankAccounts.isNotEmpty()) bankAccounts else allCashAccounts
                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(availableAccounts, key = { it.accountType }) { bank ->
                                        FilterChip(
                                            selected = selectedBankCode == bank.accountType,
                                            onClick = { selectedBankCode = bank.accountType },
                                            label = { Text("${bank.accountName} (${Formatters.formatRupiah(bank.saldo)})") },
                                            leadingIcon = if (selectedBankCode == bank.accountType) {
                                                { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            } else null,
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFFE3F2FD),
                                                selectedLabelColor = Color(0xFF1565C0)
                                            )
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = nominalTunaiSplitInput,
                                        onValueChange = { nominalTunaiSplitInput = it },
                                        label = { Text("Nominal Tunai (Rp)") },
                                        placeholder = { Text("Contoh: 50000") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        trailingIcon = {
                                            IconButton(onClick = { calcTargetField = "split_tunai" }) {
                                                Icon(Icons.Default.Calculate, contentDescription = "Kalkulator", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = nominalTransferSplitInput,
                                        onValueChange = { nominalTransferSplitInput = it },
                                        label = { Text("Nominal Transfer (Rp)") },
                                        placeholder = { Text("Contoh: 100000") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        trailingIcon = {
                                            IconButton(onClick = { calcTargetField = "split_transfer" }) {
                                                Icon(Icons.Default.Calculate, contentDescription = "Kalkulator", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                val tSplit = nominalTunaiSplitInput.toDoubleOrNull() ?: 0.0
                                val trSplit = nominalTransferSplitInput.toDoubleOrNull() ?: 0.0
                                val sumSplit = tSplit + trSplit
                                val selCashName = cashAccounts.find { it.accountType == selectedCashCode }?.accountName ?: "Kas Tunai"
                                val selAccName = allCashAccounts.find { it.accountType == selectedBankCode }?.accountName ?: selectedBankCode

                                Surface(
                                    color = if (sumSplit == totalNilai) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Potong $selCashName: ${Formatters.formatRupiah(tSplit)} | Potong $selAccName: ${Formatters.formatRupiah(trSplit)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (sumSplit == totalNilai) Color(0xFF2E7D32) else Color(0xFFE65100)
                                        )
                                        Text(
                                            text = "Total Bayar: ${Formatters.formatRupiah(sumSplit)} (Nilai Pembelian: ${Formatters.formatRupiah(totalNilai)})",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else if (statusPembayaran == "Hutang") {
                            Text(
                                text = "* Menambah data Hutang Supplier sebesar ${Formatters.formatRupiah(totalNilai)} secara otomatis.",
                                fontSize = 11.sp,
                                color = Color(0xFFD32F2F),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }



                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (namaSupplier.isBlank()) {
                                    Toast.makeText(context, "Mohon isi Nama Supplier terlebih dahulu!", Toast.LENGTH_SHORT).show()
                                } else if (incomingCart.isEmpty()) {
                                    Toast.makeText(context, "Pilih setidaknya 1 barang masuk!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val tunaiSplit = nominalTunaiSplitInput.toDoubleOrNull() ?: 0.0
                                    val transferSplit = nominalTransferSplitInput.toDoubleOrNull() ?: 0.0

                                    viewModel.saveIncomingTransaction(
                                        tanggal = tanggal,
                                        supplierName = namaSupplier,
                                        fakturNumber = nomorFaktur,
                                        catatan = catatan,
                                        statusPembayaran = statusPembayaran,
                                        targetAccountCode = if (statusPembayaran == "Transfer" || statusPembayaran == "Tunai & Transfer") selectedBankCode else selectedCashCode,
                                        nominalTunaiSplit = if (statusPembayaran == "Tunai & Transfer") tunaiSplit else 0.0,
                                        nominalTransferSplit = if (statusPembayaran == "Tunai & Transfer") transferSplit else 0.0,
                                        targetTunaiAccountCode = selectedCashCode,
                                        onSuccess = {
                                            namaSupplier = ""
                                            nomorFaktur = ""
                                            catatan = ""
                                            nominalTunaiSplitInput = ""
                                            nominalTransferSplitInput = ""
                                            Toast.makeText(context, "Berhasil menyimpan Transaksi Barang Masuk!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_incoming_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Simpan Transaksi Masuk & Update Stok", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Recent Incoming Transactions History & Cancellation Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Riwayat Transaksi Barang Masuk & Pembatalan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Daftar transaksi barang masuk terbaru. Klik 'Batalkan' untuk membatalkan transaksi, menarik kembali stok, dan memulihkan saldo/hutang.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (allIncomingTransactions.isEmpty()) {
                            Text(
                                text = "Belum ada riwayat transaksi barang masuk.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                allIncomingTransactions.forEach { tx ->
                                    val timeStr = Formatters.formatTimeOnly(tx.timestamp)
                                    val dateTimeStr = if (timeStr.isNotBlank() && timeStr != "00:00") "${Formatters.formatDateToIndonesian(tx.tanggal)} $timeStr" else Formatters.formatDateToIndonesian(tx.tanggal)
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
                                                    text = "Supplier: ${tx.namaSupplier} • ${Formatters.formatRupiah(tx.totalNilai)}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF1565C0)
                                                )
                                                Text(
                                                    text = "Tgl: $dateTimeStr ${if (tx.nomorFaktur.isNotBlank()) "| No. Faktur: ${tx.nomorFaktur}" else ""} | ${tx.statusPembayaran}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (tx.catatan.isNotBlank()) {
                                                    Text(
                                                        text = tx.catatan,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                             Row(verticalAlignment = Alignment.CenterVertically) {
                                                androidx.compose.material3.IconButton(
                                                    onClick = { incomingTransactionToEdit = tx },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit Barang Masuk",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                androidx.compose.material3.TextButton(
                                                    onClick = { incomingTransactionToCancel = tx },
                                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F))
                                                ) {
                                                    Text("Batalkan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
        }
    }

    // Dialog Edit Transaksi Barang Masuk
    incomingTransactionToEdit?.let { tx ->
        var supplierText by remember(tx) { mutableStateOf(tx.namaSupplier) }
        var fakturText by remember(tx) { mutableStateOf(tx.nomorFaktur) }
        var totalText by remember(tx) { mutableStateOf(tx.totalNilai.toLong().toString()) }
        var statusText by remember(tx) { mutableStateOf(tx.statusPembayaran) }
        var selectedAccountCode by remember(tx) { 
            mutableStateOf(
                if (tx.statusPembayaran.contains("Transfer", ignoreCase = true)) {
                    bankAccounts.firstOrNull()?.accountType ?: "BANK"
                } else {
                    cashAccounts.firstOrNull()?.accountType ?: "TUNAI"
                }
            ) 
        }
        var catatanText by remember(tx) { mutableStateOf(tx.catatan) }
        var tanggalText by remember(tx) { mutableStateOf(tx.tanggal) }

        var editableItems by remember(tx) { mutableStateOf<List<com.example.data.entity.IncomingItemEntity>>(emptyList()) }
        var isLoadingItems by remember(tx) { mutableStateOf(true) }
        var showAddItemDialog by remember(tx) { mutableStateOf(false) }
        var itemToChangeIndex by remember(tx) { mutableStateOf<Int?>(null) }

        LaunchedEffect(tx.id) {
            isLoadingItems = true
            val items = viewModel.getIncomingTransactionItems(tx.id)
            editableItems = items
            isLoadingItems = false
        }

        // Sub-dialog to Pick or Add Item from Master Stok
        if (showAddItemDialog) {
            var searchQuery by remember { mutableStateOf("") }
            val filteredItems = remember(searchQuery, allItemsList) {
                if (searchQuery.isBlank()) allItemsList
                else com.example.util.ItemMatcher.searchItems(searchQuery, allItemsList)
            }

            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showAddItemDialog = false },
                title = { Text("Pilih Barang dari Master Stok", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(modifier = Modifier.heightIn(max = 400.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Cari nama/kode barang...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (filteredItems.isEmpty()) {
                            Text("Tidak ada barang ditemukan.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(filteredItems) { item ->
                                    Card(
                                        onClick = {
                                            val newItem = com.example.data.entity.IncomingItemEntity(
                                                transactionId = tx.id,
                                                itemId = item.id,
                                                kodeBarang = item.kodeBarang,
                                                namaBarang = item.namaBarang,
                                                jumlahMasuk = 1,
                                                hargaModal = item.hargaModal
                                            )
                                            editableItems = editableItems + newItem
                                            showAddItemDialog = false
                                            val calcTotal = editableItems.sumOf { it.jumlahMasuk * it.hargaModal }
                                            totalText = calcTotal.toLong().toString()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(item.namaBarang, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("Kode: ${item.kodeBarang} | Stok: ${item.totalStokCombined}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text(Formatters.formatRupiah(item.hargaModal), fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showAddItemDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Sub-dialog to Change Existing Item Selection
        itemToChangeIndex?.let { index ->
            var searchQuery by remember { mutableStateOf("") }
            val filteredItems = remember(searchQuery, allItemsList) {
                if (searchQuery.isBlank()) allItemsList
                else com.example.util.ItemMatcher.searchItems(searchQuery, allItemsList)
            }

            androidx.compose.material3.AlertDialog(
                onDismissRequest = { itemToChangeIndex = null },
                title = { Text("Ganti Barang Masuk", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(modifier = Modifier.heightIn(max = 400.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Cari barang pengganti...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(filteredItems) { item ->
                                Card(
                                    onClick = {
                                        val old = editableItems[index]
                                        val updated = old.copy(
                                            itemId = item.id,
                                            kodeBarang = item.kodeBarang,
                                            namaBarang = item.namaBarang,
                                            hargaModal = item.hargaModal
                                        )
                                        val list = editableItems.toMutableList()
                                        list[index] = updated
                                        editableItems = list
                                        itemToChangeIndex = null
                                        val calcTotal = list.sumOf { it.jumlahMasuk * it.hargaModal }
                                        totalText = calcTotal.toLong().toString()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.namaBarang, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text("Kode: ${item.kodeBarang} | Stok: ${item.totalStokCombined}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(Formatters.formatRupiah(item.hargaModal), fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { itemToChangeIndex = null }) {
                        Text("Batal")
                    }
                }
            )
        }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { incomingTransactionToEdit = null },
            title = { Text("Edit Transaksi Barang Masuk #${tx.id}", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = supplierText,
                        onValueChange = { supplierText = it },
                        label = { Text("Nama Supplier") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = fakturText,
                        onValueChange = { fakturText = it },
                        label = { Text("Nomor Faktur / Nota (Opsional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Header & Items Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Daftar Barang Masuk:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        androidx.compose.material3.OutlinedButton(
                            onClick = { showAddItemDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tambah", fontSize = 11.sp)
                        }
                    }

                    if (isLoadingItems) {
                        Text("Memuat daftar barang...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (editableItems.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Belum ada barang di transaksi ini. Klik '+ Tambah' untuk memasukkan barang.",
                                fontSize = 11.sp,
                                modifier = Modifier.padding(10.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            editableItems.forEachIndexed { index, item ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${index + 1}. ${item.namaBarang}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = { itemToChangeIndex = index },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Ganti Barang",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    val list = editableItems.toMutableList()
                                                    list.removeAt(index)
                                                    editableItems = list
                                                    val calcTotal = list.sumOf { it.jumlahMasuk * it.hargaModal }
                                                    totalText = calcTotal.toLong().toString()
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Hapus Barang",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Quantity Selector
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = {
                                                        if (item.jumlahMasuk > 1) {
                                                            val updated = item.copy(
                                                                jumlahMasuk = item.jumlahMasuk - 1
                                                            )
                                                            val list = editableItems.toMutableList()
                                                            list[index] = updated
                                                            editableItems = list
                                                            val calcTotal = list.sumOf { it.jumlahMasuk * it.hargaModal }
                                                            totalText = calcTotal.toLong().toString()
                                                        }
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(14.dp))
                                                }
                                                Text(
                                                    text = "${item.jumlahMasuk}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp)
                                                )
                                                IconButton(
                                                    onClick = {
                                                        val updated = item.copy(
                                                            jumlahMasuk = item.jumlahMasuk + 1
                                                        )
                                                        val list = editableItems.toMutableList()
                                                        list[index] = updated
                                                        editableItems = list
                                                        val calcTotal = list.sumOf { it.jumlahMasuk * it.hargaModal }
                                                        totalText = calcTotal.toLong().toString()
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(14.dp))
                                                }
                                            }

                                            // Cost price input
                                            var priceInput by remember(item.hargaModal) { mutableStateOf(item.hargaModal.toLong().toString()) }
                                            OutlinedTextField(
                                                value = priceInput,
                                                onValueChange = { str ->
                                                    priceInput = str.filter { c -> c.isDigit() }
                                                    val newPrice = priceInput.toDoubleOrNull() ?: 0.0
                                                    val updated = item.copy(
                                                        hargaModal = newPrice
                                                    )
                                                    val list = editableItems.toMutableList()
                                                    list[index] = updated
                                                    editableItems = list
                                                    val calcTotal = list.sumOf { it.jumlahMasuk * it.hargaModal }
                                                    totalText = calcTotal.toLong().toString()
                                                },
                                                label = { Text("Harga Modal", fontSize = 10.sp) },
                                                modifier = Modifier.width(110.dp),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                            )

                                            // Item Subtotal
                                            Text(
                                                text = Formatters.formatRupiah(item.jumlahMasuk * item.hargaModal),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val calcTotal = editableItems.sumOf { it.jumlahMasuk * it.hargaModal }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hitung Otomatis Total:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        androidx.compose.material3.TextButton(
                            onClick = { totalText = calcTotal.toLong().toString() },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text("Pakai Total Barang (${Formatters.formatRupiah(calcTotal)})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedTextField(
                        value = totalText,
                        onValueChange = { totalText = it.filter { c -> c.isDigit() } },
                        label = { Text("Total Nilai Transaksi (Rp)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Text("Status / Metode Pembayaran:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Tunai", "Transfer", "Hutang").forEach { s ->
                            FilterChip(
                                selected = statusText.startsWith(s),
                                onClick = { 
                                    statusText = s 
                                    if (s == "Tunai") {
                                        selectedAccountCode = cashAccounts.firstOrNull()?.accountType ?: "TUNAI"
                                    } else if (s == "Transfer") {
                                        selectedAccountCode = bankAccounts.firstOrNull()?.accountType ?: "BANK"
                                    }
                                },
                                label = { Text(s, fontSize = 11.sp) }
                            )
                        }
                    }

                    if (statusText.startsWith("Tunai")) {
                        Text("Pilih Kas Tunai Sumber:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            cashAccounts.forEach { acc ->
                                FilterChip(
                                    selected = selectedAccountCode == acc.accountType,
                                    onClick = { selectedAccountCode = acc.accountType },
                                    label = { Text("${acc.accountName} (${Formatters.formatRupiah(acc.saldo)})", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFE8F5E9),
                                        selectedLabelColor = Color(0xFF2E7D32)
                                    )
                                )
                            }
                        }
                    } else if (statusText.startsWith("Transfer")) {
                        Text("Pilih Sumber Bank / E-Wallet:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            bankAccounts.forEach { acc ->
                                FilterChip(
                                    selected = selectedAccountCode == acc.accountType,
                                    onClick = { selectedAccountCode = acc.accountType },
                                    label = { Text("${acc.accountName.ifBlank { com.example.data.entity.CashAccountDefaults.getAccountName(acc.accountType) }} (${Formatters.formatRupiah(acc.saldo)})", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFE3F2FD),
                                        selectedLabelColor = Color(0xFF1565C0)
                                    )
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = catatanText,
                        onValueChange = { catatanText = it },
                        label = { Text("Catatan / Keterangan") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = tanggalText,
                        onValueChange = { tanggalText = it },
                        label = { Text("Tanggal (Format: TTTT-BB-HH)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tot = totalText.toDoubleOrNull() ?: 0.0
                        if (tot <= 0) {
                            Toast.makeText(context, "Total nilai transaksi tidak valid", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (editableItems.isEmpty()) {
                            Toast.makeText(context, "Daftar barang masuk tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        viewModel.updateIncomingTransaction(
                            transactionId = tx.id,
                            newSupplier = supplierText.ifBlank { "Supplier Umum" },
                            newFakturNumber = fakturText,
                            newTanggal = tanggalText.ifBlank { tx.tanggal },
                            newTotalNilai = tot,
                            newStatusPembayaran = statusText,
                            newTargetAccountCode = selectedAccountCode,
                            newCatatan = catatanText,
                            updatedItems = editableItems,
                            onSuccess = {
                                incomingTransactionToEdit = null
                                Toast.makeText(context, "Transaksi Barang Masuk #${tx.id} berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                ) {
                    Text("Simpan Perubahan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(onClick = { incomingTransactionToEdit = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Confirmation Dialog for Incoming Transaction Cancellation
    incomingTransactionToCancel?.let { tx ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { incomingTransactionToCancel = null },
            title = { Text("Batalkan Transaksi Barang Masuk?", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F)) },
            text = {
                Text(
                    "Apakah Anda yakin ingin membatalkan transaksi Barang Masuk dari '${tx.namaSupplier}' sebesar ${Formatters.formatRupiah(tx.totalNilai)}?\n\nStok barang yang masuk pada transaksi ini akan dikurangi dari inventaris, dan dampak saldo/hutang supplier akan diretur/dibatalkan secara otomatis."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelIncomingTransaction(tx.id) {
                            incomingTransactionToCancel = null
                            Toast.makeText(context, "Transaksi Barang Masuk berhasil dibatalkan dan stok telah ditarik kembali!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Ya, Batalkan Barang Masuk", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(onClick = { incomingTransactionToCancel = null }) {
                    Text("Tidak")
                }
            }
        )
    }

    if (showCreateNewItemDialog) {
        ItemFormDialog(
            title = "Tambah Barang Baru ke Master",
            initialKode = "SKU-${System.currentTimeMillis().toString().takeLast(6)}",
            initialNama = newInitialItemName,
            initialStok = "0",
            initialHargaModal = "",
            initialKeterangan = "Didaftarkan dari Menu Barang Masuk",
            onDismiss = { showCreateNewItemDialog = false },
            onConfirm = { kode, nama, stok, harga, ket ->
                val cost = harga.toDoubleOrNull() ?: 0.0
                val inputQty = stok.toIntOrNull() ?: 0
                viewModel.addItem(
                    kodeBarang = kode,
                    namaBarang = nama,
                    stok = inputQty,
                    hargaModal = cost,
                    keterangan = ket,
                    onItemCreated = { newItem ->
                        viewModel.addIncomingCartItem(newItem, qty = maxOf(1, inputQty), customCost = cost)
                    }
                )
                showCreateNewItemDialog = false
                Toast.makeText(context, "Barang '$nama' berhasil didaftarkan ke master & dimasukkan ke draf!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showScanReceiptDialog) {
        ScanReceiptDialog(
            title = "Scan Foto Nota Pembelian (Online & Offline)",
            targetLabel = "Impor ke Barang Masuk",
            databaseItems = allItemsList,
            onDismiss = { showScanReceiptDialog = false },
            onConfirmImport = { supplier, noFaktur, tgl, items ->
                if (supplier.isNotBlank()) namaSupplier = supplier
                if (noFaktur.isNotBlank()) nomorFaktur = noFaktur
                if (tgl.isNotBlank()) tanggal = tgl

                viewModel.processScannedIncomingReceipt(items) { count ->
                    Toast.makeText(context, "$count barang dari foto nota berhasil ditambahkan ke keranjang!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    calcTargetField?.let { target ->
        val initialVal = when (target) {
            "split_tunai" -> nominalTunaiSplitInput
            "split_transfer" -> nominalTransferSplitInput
            else -> {
                val itemId = target.removePrefix("item_").toLongOrNull()
                val itemInCart = incomingCart.find { it.item.id == itemId }
                if (itemInCart != null && itemInCart.hargaModal > 0) itemInCart.hargaModal.toInt().toString() else ""
            }
        }

        CalculatorDialog(
            initialValue = initialVal,
            onDismiss = { calcTargetField = null },
            onApplyResult = { result ->
                val strVal = if (result % 1.0 == 0.0) result.toLong().toString() else result.toString()
                when (target) {
                    "split_tunai" -> nominalTunaiSplitInput = strVal
                    "split_transfer" -> nominalTransferSplitInput = strVal
                    else -> {
                        val itemId = target.removePrefix("item_").toLongOrNull()
                        if (itemId != null) {
                            viewModel.updateIncomingCartCost(itemId, result)
                        }
                    }
                }
                calcTargetField = null
            }
        )
    }
}
