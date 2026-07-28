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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

    val bankAccounts = remember(allCashAccounts) {
        allCashAccounts.filter { it.accountType != "TUNAI" }
    }

    var tanggal by remember { mutableStateOf(Formatters.getCurrentDateFormatted()) }
    var namaSupplier by remember { mutableStateOf("") }
    var nomorFaktur by remember { mutableStateOf("") }
    var catatan by remember { mutableStateOf("") }
    var statusPembayaran by remember { mutableStateOf("Tunai") } // "Tunai", "Transfer", "Hutang"
    var selectedBankCode by remember { mutableStateOf("BANK") }
    var showScanReceiptDialog by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(allCashAccounts) {
        if (bankAccounts.isNotEmpty() && (selectedBankCode == "BANK" || bankAccounts.none { it.accountType == selectedBankCode })) {
            selectedBankCode = bankAccounts.first().accountType
        } else if (allCashAccounts.isNotEmpty() && allCashAccounts.none { it.accountType == selectedBankCode }) {
            selectedBankCode = allCashAccounts.first().accountType
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
                        Text(
                            text = "Tambah Barang Masuk",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
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
                            onItemSelected = { selectedItem ->
                                val currentQty = draftQuantitiesMap[selectedItem.id] ?: 0
                                viewModel.addIncomingCartItem(selectedItem)
                                val newQty = currentQty + 1
                                val toastMsg = if (currentQty > 0) {
                                    "'${selectedItem.namaBarang}' ditambah (+1) di draf (Total: $newQty)"
                                } else {
                                    "'${selectedItem.namaBarang}' dimasukkan ke draf"
                                }
                                Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
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
                                        val qty = if (input.isBlank()) 0 else (input.toIntOrNull() ?: 0)
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
                                        val cost = input.toDoubleOrNull() ?: 0.0
                                        viewModel.updateIncomingCartCost(cartItem.item.id, cost)
                                    },
                                    label = { Text("Harga Modal (Rp)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
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

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = statusPembayaran == "Tunai",
                                onClick = { statusPembayaran = "Tunai" },
                                label = { Text("Tunai") },
                                leadingIcon = if (statusPembayaran == "Tunai") {
                                    { Icon(imageVector = Icons.Default.Check, contentDescription = null) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE8F5E9),
                                    selectedLabelColor = Color(0xFF2E7D32)
                                )
                            )

                            FilterChip(
                                selected = statusPembayaran == "Transfer",
                                onClick = { statusPembayaran = "Transfer" },
                                label = { Text("Transfer Bank") },
                                leadingIcon = if (statusPembayaran == "Transfer") {
                                    { Icon(imageVector = Icons.Default.Check, contentDescription = null) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE3F2FD),
                                    selectedLabelColor = Color(0xFF1565C0)
                                )
                            )

                            FilterChip(
                                selected = statusPembayaran == "Hutang",
                                onClick = { statusPembayaran = "Hutang" },
                                label = { Text("Hutang") },
                                leadingIcon = if (statusPembayaran == "Hutang") {
                                    { Icon(imageVector = Icons.Default.Check, contentDescription = null) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFEBEE),
                                    selectedLabelColor = Color(0xFFD32F2F)
                                )
                            )
                        }

                        if (statusPembayaran == "Tunai") {
                            Text(
                                text = "* Mengurangi Kas Tunai sebesar ${Formatters.formatRupiah(totalNilai)}",
                                fontSize = 11.sp,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(top = 4.dp)
                            )
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
                                    viewModel.saveIncomingTransaction(
                                        tanggal = tanggal,
                                        supplierName = namaSupplier,
                                        fakturNumber = nomorFaktur,
                                        catatan = catatan,
                                        statusPembayaran = statusPembayaran,
                                        targetAccountCode = if (statusPembayaran == "Transfer") selectedBankCode else "TUNAI",
                                        onSuccess = {
                                            namaSupplier = ""
                                            nomorFaktur = ""
                                            catatan = ""
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
        }
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
}
