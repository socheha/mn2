package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.foundation.layout.size
import com.example.ui.components.ScanReceiptDialog
import com.example.util.PrinterUtils
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.CalculatorDialog
import com.example.ui.components.DatePickerField
import com.example.ui.components.SearchableItemAutocomplete
import com.example.util.Formatters

@Composable
fun MenuPenjualanHarianScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val allItemsList by viewModel.allItems.collectAsStateWithLifecycle()
    val salesCart by viewModel.salesCart.collectAsStateWithLifecycle()
    val selectedStoreForSales by viewModel.selectedStoreForSales.collectAsStateWithLifecycle()
    val allCashAccounts by viewModel.allCashAccounts.collectAsStateWithLifecycle()
    val allSalesTransactions by viewModel.allSalesTransactions.collectAsStateWithLifecycle()

    var salesTransactionToCancel by remember { mutableStateOf<com.example.data.entity.SalesTransactionEntity?>(null) }
    var salesTransactionToEdit by remember { mutableStateOf<com.example.data.entity.SalesTransactionEntity?>(null) }

    val registeredTransferAccounts = remember(allCashAccounts) {
        val nonCash = allCashAccounts.filter { it.accountType != "TUNAI" }.map { com.example.data.entity.AccountTypeInfo(it.accountType, it.accountName, "BANK") }
        if (nonCash.isNotEmpty()) nonCash else com.example.data.entity.CashAccountDefaults.ALL_DEFAULT_ACCOUNTS.filter { it.category != "TUNAI" }
    }

    var isNotaTotalMode by remember { mutableStateOf(true) } // true = Mode Nota Total, false = Mode Penjualan Rinci (Per Item / Toko)
    var tanggal by remember { mutableStateOf(Formatters.getCurrentDateFormatted()) }
    var totalUangNotaInput by remember { mutableStateOf("") }
    var customTotalUang by remember { mutableStateOf("") }
    var catatan by remember { mutableStateOf("") }

    // State khusus Penjualan Rinci & Toko Terafiliasi
    var namaTokoPelanggan by remember { mutableStateOf("") }
    var nomorHpPelanggan by remember { mutableStateOf("") }
    var selectedMetodePembayaran by remember { mutableStateOf("Tunai") } // "Tunai", "Transfer", "Tunai & Transfer", "Piutang"
    var selectedTransferAccountCode by remember { mutableStateOf("BCA") } // "BCA", "MANDIRI", "BRI", "BNI", "BANK_LAIN", "GOPAY", "OVO", "DANA", "SHOPEEPAY", "LINKAJA"
    var nominalTunaiSplitInput by remember { mutableStateOf("") }
    var nominalTransferSplitInput by remember { mutableStateOf("") }
    var calcTargetField by remember { mutableStateOf<String?>(null) } // "nota_total", "custom_total", "dp_money", "split_tunai", "split_transfer", or cart item ID
    val isPiutangPayment = remember(selectedMetodePembayaran) { selectedMetodePembayaran == "Piutang" }
    var nominalUangMukaInput by remember { mutableStateOf("") }
    var jatuhTempoInput by remember { mutableStateOf(Formatters.getAddDaysDate(tanggal, 14)) }
    var showNotaDialog by remember { mutableStateOf(false) }
    var showScanReceiptDialog by remember { mutableStateOf(false) }

    val calculatedTotal = remember(salesCart) { salesCart.sumOf { it.jumlahTerjual * it.hargaSatuan } }
    val totalItemCount = remember(salesCart) { salesCart.sumOf { it.jumlahTerjual } }

    val excludedIds = remember(salesCart) { salesCart.map { it.item.id } }

    Scaffold(
        modifier = Modifier.testTag("menu_penjualan_harian_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Switcher Header Card
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
                                    imageVector = Icons.Default.PointOfSale,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Input Penjualan Toko / Harian",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Mode Selector Filter Chips
                        Text(
                            text = "Pilih Metode Input Penjualan:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = isNotaTotalMode,
                                onClick = { isNotaTotalMode = true },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Receipt,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                        Text("Nota Total (Tanpa Harga Satuan)", fontSize = 12.sp)
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            FilterChip(
                                selected = !isNotaTotalMode,
                                onClick = { isNotaTotalMode = false },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Calculate,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                        Text("Penjualan Rinci / Toko", fontSize = 12.sp)
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Banner Guide per Mode
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isNotaTotalMode) Color(0xFFE3F2FD) else Color(0xFFFFF3E0),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (isNotaTotalMode) Color(0xFF1565C0) else Color(0xFFE65100),
                                    modifier = Modifier.padding(top = 2.dp, end = 8.dp)
                                )
                                Text(
                                    text = if (isNotaTotalMode) {
                                        "Mode Nota Total: Kumpulkan produk & jumlah unit yang terjual. Anda TIDAK PERLU mengisi harga satu per satu, hanya masukkan Total Uang Nota Penjualan Toko di bagian bawah."
                                    } else {
                                        "Mode Penjualan Rinci: Gunakan untuk cetak nota & penjualan ke toko terafiliasi. Jika pelanggan berhutang (piutang), transaksi akan otomatis tercatat ke Rekap Piutang Pelanggan."
                                    },
                                    fontSize = 12.sp,
                                    color = if (isNotaTotalMode) Color(0xFF0D47A1) else Color(0xFFBF360C),
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Lokasi Toko Penjualan Selector
                        Text(
                            text = "Lokasi Toko Penjualan:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = selectedStoreForSales == "Gudang" || selectedStoreForSales == "Toko Utama",
                                onClick = { viewModel.setSelectedStoreForSales("Gudang") },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Storefront,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 4.dp).size(16.dp)
                                        )
                                        Text("Gudang", fontSize = 12.sp)
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            FilterChip(
                                selected = selectedStoreForSales == "Stok Toko" || selectedStoreForSales == "Toko Cabang",
                                onClick = { viewModel.setSelectedStoreForSales("Stok Toko") },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Store,
                                            contentDescription = null,
                                            modifier = Modifier.padding(end = 4.dp).size(16.dp)
                                        )
                                        Text("Stok Toko", fontSize = 12.sp)
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFC8E6C9),
                                    selectedLabelColor = Color(0xFF1B5E20)
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        DatePickerField(
                            value = tanggal,
                            onDateSelected = { selectedDate ->
                                tanggal = selectedDate
                                jatuhTempoInput = Formatters.getAddDaysDate(selectedDate, 14)
                            },
                            label = "Tanggal Penjualan",
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = catatan,
                            onValueChange = { catatan = it },
                            label = { Text("Catatan Penjualan / Nomor Nota (Opsional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // In Rinci Mode: Toko Terafiliasi & Payment Method Selection Card
            if (!isNotaTotalMode) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = "Pelanggan / Toko Terafiliasi & Pembayaran",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = namaTokoPelanggan,
                                onValueChange = { namaTokoPelanggan = it },
                                label = { Text("Nama Toko Terafiliasi / Pelanggan") },
                                placeholder = { Text("Contoh: Toko Berkah / Pak Budi") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = nomorHpPelanggan,
                                onValueChange = { nomorHpPelanggan = it },
                                label = { Text("Nomor HP / WA (Opsional)") },
                                placeholder = { Text("08123456789") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Metode Pembayaran / Status:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilterChip(
                                    selected = selectedMetodePembayaran == "Tunai",
                                    onClick = { selectedMetodePembayaran = "Tunai" },
                                    label = { Text("Tunai", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFE8F5E9),
                                        selectedLabelColor = Color(0xFF2E7D32)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                FilterChip(
                                    selected = selectedMetodePembayaran == "Transfer",
                                    onClick = { selectedMetodePembayaran = "Transfer" },
                                    label = { Text("Transfer", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFE3F2FD),
                                        selectedLabelColor = Color(0xFF1565C0)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                FilterChip(
                                    selected = selectedMetodePembayaran == "Tunai & Transfer",
                                    onClick = { selectedMetodePembayaran = "Tunai & Transfer" },
                                    label = { Text("Tunai & Transfer", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFFF3E0),
                                        selectedLabelColor = Color(0xFFE65100)
                                    ),
                                    modifier = Modifier.weight(1.3f)
                                )

                                FilterChip(
                                    selected = selectedMetodePembayaran == "Piutang",
                                    onClick = { selectedMetodePembayaran = "Piutang" },
                                    label = { Text("Piutang", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFFEBEE),
                                        selectedLabelColor = Color(0xFFC62828)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (selectedMetodePembayaran == "Transfer") {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Pilih Rekening Bank / E-Wallet Penerima:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(registeredTransferAccounts) { acc ->
                                        FilterChip(
                                            selected = selectedTransferAccountCode == acc.type,
                                            onClick = { selectedTransferAccountCode = acc.type },
                                            label = { Text(acc.name, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFFE3F2FD),
                                                selectedLabelColor = Color(0xFF1565C0)
                                            )
                                        )
                                    }
                                }
                            } else if (selectedMetodePembayaran == "Tunai & Transfer") {
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
                                        text = "1. Pilih Rekening Bank / E-Wallet Penerima Transfer:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    androidx.compose.foundation.lazy.LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(registeredTransferAccounts) { acc ->
                                            FilterChip(
                                                selected = selectedTransferAccountCode == acc.type,
                                                onClick = { selectedTransferAccountCode = acc.type },
                                                label = { Text(acc.name, fontSize = 11.sp) },
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
                                }
                            }

                            if (isPiutangPayment) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = nominalUangMukaInput,
                                    onValueChange = { nominalUangMukaInput = it },
                                    label = { Text("Uang Muka / DP Dibayar (Rp) (Opsional)") },
                                    placeholder = { Text("0") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(onClick = { calcTargetField = "dp_money" }) {
                                            Icon(Icons.Default.Calculate, contentDescription = "Kalkulator", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Column {
                                    Text(
                                        text = "Tanggal Jatuh Tempo Piutang:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        FilterChip(
                                            selected = jatuhTempoInput == Formatters.getAddDaysDate(tanggal, 7),
                                            onClick = { jatuhTempoInput = Formatters.getAddDaysDate(tanggal, 7) },
                                            label = { Text("+7 Hari", fontSize = 11.sp) }
                                        )
                                        FilterChip(
                                            selected = jatuhTempoInput == Formatters.getAddDaysDate(tanggal, 14),
                                            onClick = { jatuhTempoInput = Formatters.getAddDaysDate(tanggal, 14) },
                                            label = { Text("+14 Hari", fontSize = 11.sp) }
                                        )
                                        FilterChip(
                                            selected = jatuhTempoInput == Formatters.getAddDaysDate(tanggal, 30),
                                            onClick = { jatuhTempoInput = Formatters.getAddDaysDate(tanggal, 30) },
                                            label = { Text("+30 Hari", fontSize = 11.sp) }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    OutlinedTextField(
                                        value = jatuhTempoInput,
                                        onValueChange = { jatuhTempoInput = it },
                                        label = { Text("Tanggal Jatuh Tempo (YYYY-MM-DD)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                val dpMoney = nominalUangMukaInput.toDoubleOrNull() ?: 0.0
                                val sisaPiutang = (calculatedTotal - dpMoney).coerceAtLeast(0.0)

                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFFEBEE),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Otomatis Masuk Rekap Piutang Pelanggan:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFB71C1C)
                                        )
                                        Text(
                                            text = Formatters.formatRupiah(sisaPiutang),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFC62828)
                                        )
                                        if (jatuhTempoInput.isNotBlank()) {
                                            Text(
                                                text = "Jatuh Tempo: ${Formatters.formatDateToIndonesian(jatuhTempoInput)}",
                                                fontSize = 11.sp,
                                                color = Color(0xFFB71C1C),
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Pilih & Tambah Barang Terjual",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ketik kode atau nama barang (misal: 609, 663, kabel):",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val draftQuantitiesMap = remember(salesCart) {
                            salesCart.associate { it.item.id to it.jumlahTerjual }
                        }

                        SearchableItemAutocomplete(
                            itemsList = allItemsList,
                            draftItemQuantities = draftQuantitiesMap,
                            isPenjualanMode = true,
                            onItemSelectedWithQty = { selectedItem, qty ->
                                if (selectedItem.stok <= 0) {
                                    Toast.makeText(context, "Stok '${selectedItem.namaBarang}' habis (0)!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val currentQty = draftQuantitiesMap[selectedItem.id] ?: 0
                                    val maxAvailable = selectedItem.stok - currentQty
                                    if (qty > maxAvailable) {
                                        Toast.makeText(
                                            context,
                                            "Stok tidak mencukupi! Maksimal dapat ditambah $maxAvailable unit lagi (stok: ${selectedItem.stok}, draf: $currentQty).",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        viewModel.addSalesCartItem(selectedItem, qty)
                                        val newQty = currentQty + qty
                                        val toastMsg = if (currentQty > 0) {
                                            "'${selectedItem.namaBarang}' ditambah (+$qty) di draf (Total: $newQty)"
                                        } else {
                                            "'${selectedItem.namaBarang}' x$qty dimasukkan ke draf"
                                        }
                                        Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // SCAN NOTA CARD BUTTON (ONLINE & OFFLINE)
                        Card(
                            onClick = { showScanReceiptDialog = true },
                            modifier = Modifier.fillMaxWidth().testTag("btn_scan_nota_penjualan"),
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
                                            contentDescription = "Scan Nota",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "📷 Scan Foto Nota Penjualan",
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

            // Cart Items Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = "Daftar Draf Terkumpul (${salesCart.size} Jenis Produk)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (salesCart.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearSalesCart() },
                            modifier = Modifier.size(32.dp).testTag("btn_kosongkan_keranjang")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Kosongkan Keranjang",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (salesCart.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Belum ada produk yang dikumpulkan.",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Gunakan pencarian di atas untuk memasukkan barang-barang yang terjual.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            } else {
                items(salesCart, key = { it.item.id }) { cartItem ->
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
                                        text = "Stok Tersedia: ${cartItem.item.stok} unit",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFFF3E0),
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Text(
                                        text = "Draf",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.removeSalesCartItem(cartItem.item.id) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Hapus",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Quantity selector with +/- buttons
                                Column(
                                    modifier = if (isNotaTotalMode) Modifier.fillMaxWidth() else Modifier.weight(1.2f)
                                ) {
                                    Text(
                                        text = "Jumlah Unit Terjual",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                if (cartItem.jumlahTerjual > 1) {
                                                    viewModel.updateSalesCartQuantity(cartItem.item.id, cartItem.jumlahTerjual - 1)
                                                }
                                            },
                                            modifier = Modifier.height(36.dp).width(36.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Remove, contentDescription = "Kurang")
                                        }

                                        OutlinedTextField(
                                            value = if (cartItem.jumlahTerjual == 0) "" else cartItem.jumlahTerjual.toString(),
                                            onValueChange = { input ->
                                                val filtered = input.filter { char -> char.isDigit() }
                                                val qty = if (filtered.isBlank()) 0 else (filtered.toIntOrNull() ?: 0)
                                                viewModel.updateSalesCartQuantity(cartItem.item.id, qty)
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )

                                        IconButton(
                                            onClick = {
                                                if (cartItem.jumlahTerjual < cartItem.item.stok) {
                                                    viewModel.updateSalesCartQuantity(cartItem.item.id, cartItem.jumlahTerjual + 1)
                                                }
                                            },
                                            modifier = Modifier.height(36.dp).width(36.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah")
                                        }
                                    }
                                }

                                // Unit Price (ONLY rendered in Detailed Mode!)
                                if (!isNotaTotalMode) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Harga Satuan (Rp)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        OutlinedTextField(
                                            value = if (cartItem.hargaSatuan > 0) cartItem.hargaSatuan.toInt().toString() else "",
                                            onValueChange = { input ->
                                                val filtered = input.filter { char -> char.isDigit() }
                                                val price = filtered.toDoubleOrNull() ?: 0.0
                                                viewModel.updateSalesCartPrice(cartItem.item.id, price)
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            trailingIcon = {
                                                IconButton(onClick = { calcTargetField = "item_${cartItem.item.id}" }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Calculate,
                                                        contentDescription = "Kalkulator",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            if (!isNotaTotalMode) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val itemProfit = (cartItem.hargaSatuan - cartItem.item.hargaModal) * cartItem.jumlahTerjual
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Keuntungan Item: ${Formatters.formatRupiah(itemProfit)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (itemProfit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                    Text(
                                        text = "Subtotal: ${Formatters.formatRupiah(cartItem.jumlahTerjual * cartItem.hargaSatuan)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Summary & Batch Save Action Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (isNotaTotalMode) "Total Penjualan Nota Toko" else "Ringkasan Penjualan Rinci",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Jumlah Jenis Barang:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "${salesCart.size} Jenis", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Total Unit Terjual:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "$totalItemCount Unit Barang", fontWeight = FontWeight.Bold)
                        }

                        if (!isNotaTotalMode) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Total Hitungan Items:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = Formatters.formatRupiah(calculatedTotal),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isNotaTotalMode) {
                            OutlinedTextField(
                                value = totalUangNotaInput,
                                onValueChange = { totalUangNotaInput = it },
                                label = { Text("Total Keseluruhan Uang Nota / Penjualan (Rp) *") },
                                placeholder = { Text("Contoh: 350000") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { calcTargetField = "nota_total" }) {
                                        Icon(
                                            imageVector = Icons.Default.Calculate,
                                            contentDescription = "Kalkulator",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Masukkan total nominal uang yang didapat dari nota penjualan ini.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Uang Masuk Ke Kas / Rekening Bank:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilterChip(
                                    selected = selectedMetodePembayaran == "Tunai",
                                    onClick = { selectedMetodePembayaran = "Tunai" },
                                    label = { Text("Kas (Tunai)", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFE8F5E9),
                                        selectedLabelColor = Color(0xFF2E7D32)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = selectedMetodePembayaran == "Transfer",
                                    onClick = { selectedMetodePembayaran = "Transfer" },
                                    label = { Text("Transfer", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFE3F2FD),
                                        selectedLabelColor = Color(0xFF1565C0)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = selectedMetodePembayaran == "Tunai & Transfer",
                                    onClick = { selectedMetodePembayaran = "Tunai & Transfer" },
                                    label = { Text("Tunai & Transfer", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFFFF3E0),
                                        selectedLabelColor = Color(0xFFE65100)
                                    ),
                                    modifier = Modifier.weight(1.2f)
                                )
                            }

                            if (selectedMetodePembayaran == "Transfer") {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Pilih Bank / E-Wallet Penerima:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                androidx.compose.foundation.lazy.LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(registeredTransferAccounts) { acc ->
                                        FilterChip(
                                            selected = selectedTransferAccountCode == acc.type,
                                            onClick = { selectedTransferAccountCode = acc.type },
                                            label = { Text(acc.name, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = Color(0xFFE3F2FD),
                                                selectedLabelColor = Color(0xFF1565C0)
                                            )
                                        )
                                    }
                                }
                            } else if (selectedMetodePembayaran == "Tunai & Transfer") {
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
                                        text = "1. Pilih Bank / E-Wallet Penerima Transfer:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    androidx.compose.foundation.lazy.LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(registeredTransferAccounts) { acc ->
                                            FilterChip(
                                                selected = selectedTransferAccountCode == acc.type,
                                                onClick = { selectedTransferAccountCode = acc.type },
                                                label = { Text(acc.name, fontSize = 11.sp) },
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
                                    val totalNotaVal = totalUangNotaInput.toDoubleOrNull() ?: 0.0

                                    Surface(
                                        color = if (totalNotaVal > 0 && sumSplit == totalNotaVal) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "Masuk Kas Tunai: ${Formatters.formatRupiah(tSplit)} | Masuk $selectedTransferAccountCode: ${Formatters.formatRupiah(trSplit)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (totalNotaVal > 0 && sumSplit == totalNotaVal) Color(0xFF2E7D32) else Color(0xFFE65100)
                                            )
                                            Text(
                                                text = "Total Rincian: ${Formatters.formatRupiah(sumSplit)}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = customTotalUang,
                                onValueChange = { customTotalUang = it },
                                label = { Text("Total Uang Penjualan (Rp) (Opsional Override)") },
                                placeholder = { Text(Formatters.formatRupiah(calculatedTotal)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { calcTargetField = "custom_total" }) {
                                        Icon(
                                            imageVector = Icons.Default.Calculate,
                                            contentDescription = "Kalkulator",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Kosongkan jika menyamakan dengan total hitungan items.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        // --- Kartu Estimasi Keuntungan (Laba Clean) ---
                        val totalHppModal = remember(salesCart) { salesCart.sumOf { it.jumlahTerjual * it.item.hargaModal } }
                        val finalEstimatedRevenue = if (isNotaTotalMode) {
                            totalUangNotaInput.toDoubleOrNull() ?: calculatedTotal
                        } else {
                            customTotalUang.toDoubleOrNull() ?: calculatedTotal
                        }
                        val estimatedProfit = finalEstimatedRevenue - totalHppModal

                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (estimatedProfit >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Total Modal (HPP):",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = Formatters.formatRupiah(totalHppModal),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Sell,
                                            contentDescription = null,
                                            tint = if (estimatedProfit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                                        )
                                        Text(
                                            text = "Estimasi Keuntungan Penjualan:",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (estimatedProfit >= 0) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                                        )
                                    }
                                    Text(
                                        text = Formatters.formatRupiah(estimatedProfit),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (estimatedProfit >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Preview & Cetak Nota Button (In Penjualan Rinci Mode)
                        if (!isNotaTotalMode && salesCart.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { showNotaDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text("Cetak / Preview Nota Penjualan", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Button(
                            onClick = {
                                if (salesCart.isEmpty()) {
                                    Toast.makeText(context, "Kumpulkan setidaknya 1 produk terjual!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                if (!isNotaTotalMode && isPiutangPayment && namaTokoPelanggan.isBlank()) {
                                    Toast.makeText(context, "Mohon isi Nama Toko Terafiliasi / Pelanggan untuk penjualan Piutang!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val tunaiSplit = nominalTunaiSplitInput.toDoubleOrNull() ?: 0.0
                                val transferSplit = nominalTransferSplitInput.toDoubleOrNull() ?: 0.0

                                val finalMoney = if (isNotaTotalMode) {
                                    val money = totalUangNotaInput.toDoubleOrNull() ?: 0.0
                                    if (money <= 0 && selectedMetodePembayaran != "Tunai & Transfer") {
                                        Toast.makeText(context, "Mohon masukkan Total Uang Nota Penjualan!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (selectedMetodePembayaran == "Tunai & Transfer") tunaiSplit + transferSplit else money
                                } else {
                                    if (selectedMetodePembayaran == "Tunai & Transfer") tunaiSplit + transferSplit else (customTotalUang.toDoubleOrNull() ?: calculatedTotal)
                                }

                                val dpMoney = nominalUangMukaInput.toDoubleOrNull() ?: 0.0

                                val paymentMethodToSave = if (selectedMetodePembayaran == "Transfer") {
                                    "Transfer"
                                } else if (selectedMetodePembayaran == "Tunai & Transfer") {
                                    "Tunai & Transfer"
                                } else if (!isNotaTotalMode && isPiutangPayment) {
                                    "Piutang"
                                } else {
                                    "Tunai"
                                }

                                viewModel.saveSalesTransaction(
                                    tanggal = tanggal,
                                    totalMoneyInput = finalMoney,
                                    catatan = catatan,
                                    isPiutang = (!isNotaTotalMode && isPiutangPayment),
                                    namaPelanggan = namaTokoPelanggan,
                                    nomorHp = nomorHpPelanggan,
                                    uangMuka = dpMoney,
                                    jatuhTempo = if (isPiutangPayment) jatuhTempoInput else "",
                                    metodePembayaran = paymentMethodToSave,
                                    targetAccountCode = if (selectedMetodePembayaran == "Transfer" || selectedMetodePembayaran == "Tunai & Transfer") selectedTransferAccountCode else "TUNAI",
                                    nominalTunaiSplit = if (selectedMetodePembayaran == "Tunai & Transfer") tunaiSplit else 0.0,
                                    nominalTransferSplit = if (selectedMetodePembayaran == "Tunai & Transfer") transferSplit else 0.0,
                                    onSuccess = {
                                        totalUangNotaInput = ""
                                        customTotalUang = ""
                                        catatan = ""
                                        namaTokoPelanggan = ""
                                        nomorHpPelanggan = ""
                                        nominalUangMukaInput = ""
                                        nominalTunaiSplitInput = ""
                                        nominalTransferSplitInput = ""
                                        selectedMetodePembayaran = "Tunai"

                                        val message = if (!isNotaTotalMode && isPiutangPayment) {
                                            "Berhasil simpan Penjualan & Otomatis Dicatat ke Rekap Piutang Pelanggan!"
                                        } else {
                                            "Berhasil menyimpan Penjualan (${salesCart.size} jenis barang, ${totalItemCount} unit) & potong stok!"
                                        }

                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("save_sales_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            enabled = salesCart.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = if (salesCart.isEmpty()) "Kumpulkan Barang Terlebih Dahulu" else "Simpan Penjualan (${salesCart.size} Jenis Produk)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Recent Sales History & Cancellation Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Riwayat Transaksi Penjualan & Pembatalan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Daftar nota penjualan terbaru. Klik 'Batalkan' untuk membatalkan transaksi dan mengembalikan stok.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (allSalesTransactions.isEmpty()) {
                            Text(
                                text = "Belum ada riwayat transaksi penjualan.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                allSalesTransactions.take(10).forEach { tx ->
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
                                                    text = "Nota #${tx.id} • ${Formatters.formatRupiah(tx.totalUangPenjualan)}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color(0xFF2E7D32)
                                                )
                                                Text(
                                                    text = "Tgl: ${Formatters.formatDateToIndonesian(tx.tanggal)} | ${tx.namaToko} | ${tx.metodePembayaran}",
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
                                                IconButton(
                                                    onClick = { salesTransactionToEdit = tx },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit Penjualan",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                TextButton(
                                                    onClick = { salesTransactionToCancel = tx },
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

    // Dialog Edit Transaksi Penjualan
    salesTransactionToEdit?.let { tx ->
        var totalText by remember(tx) { mutableStateOf(tx.totalUangPenjualan.toLong().toString()) }
        var metodeText by remember(tx) { mutableStateOf(tx.metodePembayaran) }
        var selectedAccountCode by remember(tx) { mutableStateOf("BCA") }
        var catatanText by remember(tx) { mutableStateOf(tx.catatan) }
        var tanggalText by remember(tx) { mutableStateOf(tx.tanggal) }
        var namaPelangganText by remember(tx) { mutableStateOf("") }

        var editableItems by remember(tx) { mutableStateOf<List<com.example.data.entity.SalesItemEntity>>(emptyList()) }
        var isLoadingItems by remember(tx) { mutableStateOf(true) }
        var showAddItemDialog by remember(tx) { mutableStateOf(false) }
        var itemToChangeIndex by remember(tx) { mutableStateOf<Int?>(null) }

        LaunchedEffect(tx.id) {
            isLoadingItems = true
            val items = viewModel.getTransactionItems(tx.id)
            editableItems = items

            val receivables = viewModel.allReceivables.value
            val match = receivables.find { it.catatan.contains("Nota #${tx.id}") }
            if (match != null) {
                namaPelangganText = match.namaPelanggan
                if (!metodeText.contains("Piutang", ignoreCase = true)) {
                    metodeText = "Piutang"
                }
            }
            isLoadingItems = false
        }

        // Sub-dialog to Pick or Add Item from Inventory
        if (showAddItemDialog) {
            var searchQuery by remember { mutableStateOf("") }
            val filteredItems = remember(searchQuery, allItemsList) {
                if (searchQuery.isBlank()) allItemsList
                else com.example.util.ItemMatcher.searchItems(searchQuery, allItemsList)
            }

            AlertDialog(
                onDismissRequest = { showAddItemDialog = false },
                title = { Text("Pilih Barang dari Stok Toko", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
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
                                            val initialPrice = item.hargaModal
                                            val newItem = com.example.data.entity.SalesItemEntity(
                                                transactionId = tx.id,
                                                itemId = item.id,
                                                kodeBarang = item.kodeBarang,
                                                namaBarang = item.namaBarang,
                                                jumlahTerjual = 1,
                                                hargaSatuan = initialPrice,
                                                totalHarga = initialPrice
                                            )
                                            editableItems = editableItems + newItem
                                            showAddItemDialog = false
                                            val calcTotal = editableItems.sumOf { it.jumlahTerjual * it.hargaSatuan }
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
                    TextButton(onClick = { showAddItemDialog = false }) {
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

            AlertDialog(
                onDismissRequest = { itemToChangeIndex = null },
                title = { Text("Ganti Barang Terjual", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
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
                                        val newPrice = item.hargaModal
                                        val updated = old.copy(
                                            itemId = item.id,
                                            kodeBarang = item.kodeBarang,
                                            namaBarang = item.namaBarang,
                                            hargaSatuan = newPrice,
                                            totalHarga = old.jumlahTerjual * newPrice
                                        )
                                        val list = editableItems.toMutableList()
                                        list[index] = updated
                                        editableItems = list
                                        itemToChangeIndex = null
                                        val calcTotal = list.sumOf { it.jumlahTerjual * it.hargaSatuan }
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
                    TextButton(onClick = { itemToChangeIndex = null }) {
                        Text("Batal")
                    }
                }
            )
        }

        AlertDialog(
            onDismissRequest = { salesTransactionToEdit = null },
            title = { Text("Edit Transaksi Penjualan #${tx.id}", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header & Items Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Daftar Barang Terjual:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        OutlinedButton(
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
                                "Belum ada barang di nota ini. Klik '+ Tambah' untuk memasukkan barang.",
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
                                                    val calcTotal = list.sumOf { it.jumlahTerjual * it.hargaSatuan }
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
                                                        if (item.jumlahTerjual > 1) {
                                                            val updated = item.copy(
                                                                jumlahTerjual = item.jumlahTerjual - 1,
                                                                totalHarga = (item.jumlahTerjual - 1) * item.hargaSatuan
                                                            )
                                                            val list = editableItems.toMutableList()
                                                            list[index] = updated
                                                            editableItems = list
                                                            val calcTotal = list.sumOf { it.jumlahTerjual * it.hargaSatuan }
                                                            totalText = calcTotal.toLong().toString()
                                                        }
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(14.dp))
                                                }
                                                Text(
                                                    text = "${item.jumlahTerjual}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp)
                                                )
                                                IconButton(
                                                    onClick = {
                                                        val updated = item.copy(
                                                            jumlahTerjual = item.jumlahTerjual + 1,
                                                            totalHarga = (item.jumlahTerjual + 1) * item.hargaSatuan
                                                        )
                                                        val list = editableItems.toMutableList()
                                                        list[index] = updated
                                                        editableItems = list
                                                        val calcTotal = list.sumOf { it.jumlahTerjual * it.hargaSatuan }
                                                        totalText = calcTotal.toLong().toString()
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah", modifier = Modifier.size(14.dp))
                                                }
                                            }

                                            // Price input
                                            var priceInput by remember(item.hargaSatuan) { mutableStateOf(item.hargaSatuan.toLong().toString()) }
                                            OutlinedTextField(
                                                value = priceInput,
                                                onValueChange = { str ->
                                                    priceInput = str.filter { c -> c.isDigit() }
                                                    val newPrice = priceInput.toDoubleOrNull() ?: 0.0
                                                    val updated = item.copy(
                                                        hargaSatuan = newPrice,
                                                        totalHarga = item.jumlahTerjual * newPrice
                                                    )
                                                    val list = editableItems.toMutableList()
                                                    list[index] = updated
                                                    editableItems = list
                                                    val calcTotal = list.sumOf { it.jumlahTerjual * it.hargaSatuan }
                                                    totalText = calcTotal.toLong().toString()
                                                },
                                                label = { Text("Harga Satuan", fontSize = 10.sp) },
                                                modifier = Modifier.width(110.dp),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                            )

                                            // Item Subtotal
                                            Text(
                                                text = Formatters.formatRupiah(item.jumlahTerjual * item.hargaSatuan),
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

                    val calcTotal = editableItems.sumOf { it.jumlahTerjual * it.hargaSatuan }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hitung Otomatis Total:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(
                            onClick = { totalText = calcTotal.toLong().toString() },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text("Pakai Total Barang (${Formatters.formatRupiah(calcTotal)})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedTextField(
                        value = totalText,
                        onValueChange = { totalText = it.filter { c -> c.isDigit() } },
                        label = { Text("Total Nilai Penjualan (Rp)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Text("Metode Pembayaran:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Tunai", "Transfer", "Piutang").forEach { m ->
                            FilterChip(
                                selected = metodeText.contains(m, ignoreCase = true),
                                onClick = { metodeText = m },
                                label = { Text(m, fontSize = 11.sp) }
                            )
                        }
                    }

                    if (metodeText.contains("Transfer", ignoreCase = true)) {
                        Text("Pilih Bank / E-Wallet Tujuan:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            registeredTransferAccounts.forEach { acc ->
                                FilterChip(
                                    selected = selectedAccountCode == acc.type,
                                    onClick = { selectedAccountCode = acc.type },
                                    label = { Text(acc.name, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    if (metodeText.contains("Piutang", ignoreCase = true)) {
                        OutlinedTextField(
                            value = namaPelangganText,
                            onValueChange = { namaPelangganText = it },
                            label = { Text("Nama yang Berhutang (Pelanggan)*", fontWeight = FontWeight.SemiBold) },
                            placeholder = { Text("Ketik nama pembeli/pelanggan yang berhutang") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (namaPelangganText.isNotBlank()) "Catatan piutang atas nama '${namaPelangganText.trim()}' akan dimasukkan/diperbarui di Daftar Piutang Pelanggan."
                                    else "Mohon ketik nama pelanggan yang berhutang.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = catatanText,
                        onValueChange = { catatanText = it },
                        label = { Text("Catatan Transaksi") },
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
                            Toast.makeText(context, "Total nilai penjualan tidak valid", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (metodeText.contains("Piutang", ignoreCase = true) && namaPelangganText.isBlank()) {
                            Toast.makeText(context, "Mohon masukkan nama pelanggan yang berhutang!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (editableItems.isEmpty()) {
                            Toast.makeText(context, "Daftar barang terjual tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        viewModel.updateSalesTransaction(
                            transactionId = tx.id,
                            newTanggal = tanggalText.ifBlank { tx.tanggal },
                            newTotalUangPenjualan = tot,
                            newMetodePembayaran = metodeText,
                            newTargetAccountCode = selectedAccountCode,
                            newCatatan = catatanText,
                            newNamaPelanggan = namaPelangganText,
                            updatedItems = editableItems,
                            onSuccess = {
                                salesTransactionToEdit = null
                                Toast.makeText(context, "Nota Penjualan #${tx.id} berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                ) {
                    Text("Simpan Perubahan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { salesTransactionToEdit = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Confirmation Dialog for Sales Transaction Cancellation
    salesTransactionToCancel?.let { tx ->
        AlertDialog(
            onDismissRequest = { salesTransactionToCancel = null },
            title = { Text("Batalkan Transaksi Penjualan?", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F)) },
            text = {
                Text(
                    "Apakah Anda yakin ingin membatalkan Nota Penjualan #${tx.id} sebesar ${Formatters.formatRupiah(tx.totalUangPenjualan)}?\n\nSemua stok barang pada nota ini akan dikembalikan ke inventaris toko dan saldo kas/bank terkait akan disesuaikan secara otomatis."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelSalesTransaction(tx.id) {
                            salesTransactionToCancel = null
                            Toast.makeText(context, "Nota Penjualan #${tx.id} berhasil dibatalkan dan stok dikembalikan!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Ya, Batalkan Penjualan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { salesTransactionToCancel = null }) {
                    Text("Tidak")
                }
            }
        )
    }

    // Dialog Cetak / Preview Nota Penjualan
    if (showNotaDialog) {
        val dpMoney = nominalUangMukaInput.toDoubleOrNull() ?: 0.0
        val finalTotal = customTotalUang.toDoubleOrNull() ?: calculatedTotal
        val sisaHutang = (finalTotal - dpMoney).coerceAtLeast(0.0)

        // Generate Text String for Receipt
        val notaTextBuilder = StringBuilder()
        notaTextBuilder.appendLine("=================================")
        notaTextBuilder.appendLine("     NOTA PENJUALAN TOKO")
        notaTextBuilder.appendLine("=================================")
        notaTextBuilder.appendLine("Tgl: $tanggal")
        if (namaTokoPelanggan.isNotBlank()) {
            notaTextBuilder.appendLine("Kepada: $namaTokoPelanggan")
            if (nomorHpPelanggan.isNotBlank()) notaTextBuilder.appendLine("No. HP: $nomorHpPelanggan")
        }
        if (catatan.isNotBlank()) notaTextBuilder.appendLine("Catatan: $catatan")
        notaTextBuilder.appendLine("---------------------------------")
        salesCart.forEach { item ->
            val sub = item.jumlahTerjual * item.hargaSatuan
            notaTextBuilder.appendLine("${item.item.namaBarang}")
            notaTextBuilder.appendLine("${item.jumlahTerjual} x ${Formatters.formatRupiah(item.hargaSatuan)} = ${Formatters.formatRupiah(sub)}")
        }
        notaTextBuilder.appendLine("---------------------------------")
        notaTextBuilder.appendLine("Total Belanja : ${Formatters.formatRupiah(finalTotal)}")
        notaTextBuilder.appendLine("Status Bayar  : ${if (isPiutangPayment) "PIUTANG / HUTANG" else "TUNAI (LUNAS)"}")
        if (isPiutangPayment) {
            notaTextBuilder.appendLine("Uang Muka (DP): ${Formatters.formatRupiah(dpMoney)}")
            notaTextBuilder.appendLine("Sisa Piutang  : ${Formatters.formatRupiah(sisaHutang)}")
            if (jatuhTempoInput.isNotBlank()) {
                notaTextBuilder.appendLine("Jatuh Tempo   : ${Formatters.formatDateToIndonesian(jatuhTempoInput)}")
            }
        }
        notaTextBuilder.appendLine("=================================")
        notaTextBuilder.appendLine("  Terima Kasih atas Kunjungan Anda")
        val notaText = notaTextBuilder.toString()

        val htmlReceipt = PrinterUtils.generateReceiptHtml(
            tanggal = tanggal,
            namaPelanggan = namaTokoPelanggan,
            nomorHp = nomorHpPelanggan,
            catatan = catatan,
            cartItems = salesCart,
            totalBelanja = finalTotal,
            isPiutang = isPiutangPayment,
            uangMuka = dpMoney,
            sisaPiutang = sisaHutang,
            jatuhTempo = if (isPiutangPayment) jatuhTempoInput else ""
        )

        AlertDialog(
            onDismissRequest = { showNotaDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Cetak / Preview Nota Penjualan", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFAFAFA),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = notaText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Direct Print Action Button
                    Button(
                        onClick = {
                            PrinterUtils.printReceiptHtml(
                                context = context,
                                jobName = "Nota_Penjualan_$tanggal",
                                htmlContent = htmlReceipt
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                        Text("Cetak ke Printer (Wi-Fi / BT / PDF)", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Share Action Button (for Bluetooth Thermal Printer Apps / WA)
                    OutlinedButton(
                        onClick = {
                            PrinterUtils.shareReceiptText(
                                context = context,
                                receiptText = notaText,
                                subject = "Nota Penjualan $namaTokoPelanggan ($tanggal)"
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                        Text("Bagikan ke App Thermal / WA")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(notaText))
                        Toast.makeText(context, "Teks Nota disalin ke Clipboard!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Salin Teks")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotaDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    if (showScanReceiptDialog) {
        ScanReceiptDialog(
            title = "Scan Foto Nota Penjualan (Online & Offline)",
            targetLabel = "Impor ke Penjualan Rinci",
            databaseItems = allItemsList,
            onDismiss = { showScanReceiptDialog = false },
            onConfirmImport = { supplier, noFaktur, tgl, items ->
                if (supplier.isNotBlank()) namaTokoPelanggan = supplier
                if (tgl.isNotBlank()) tanggal = tgl
                isNotaTotalMode = false // Switch to detailed item sales mode

                viewModel.processScannedSalesReceipt(items) { count ->
                    Toast.makeText(context, "$count barang dari foto nota berhasil ditambahkan ke Penjualan!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    calcTargetField?.let { target ->
        val initialVal = when (target) {
            "nota_total" -> totalUangNotaInput
            "custom_total" -> customTotalUang
            "dp_money" -> nominalUangMukaInput
            "split_tunai" -> nominalTunaiSplitInput
            "split_transfer" -> nominalTransferSplitInput
            else -> {
                val itemId = target.removePrefix("item_").toLongOrNull()
                val itemInCart = salesCart.find { it.item.id == itemId }
                if (itemInCart != null && itemInCart.hargaSatuan > 0) itemInCart.hargaSatuan.toInt().toString() else ""
            }
        }

        CalculatorDialog(
            initialValue = initialVal,
            onDismiss = { calcTargetField = null },
            onApplyResult = { result ->
                val strVal = if (result % 1.0 == 0.0) result.toLong().toString() else result.toString()
                when (target) {
                    "nota_total" -> totalUangNotaInput = strVal
                    "custom_total" -> customTotalUang = strVal
                    "dp_money" -> nominalUangMukaInput = strVal
                    "split_tunai" -> nominalTunaiSplitInput = strVal
                    "split_transfer" -> nominalTransferSplitInput = strVal
                    else -> {
                        val itemId = target.removePrefix("item_").toLongOrNull()
                        if (itemId != null) {
                            viewModel.updateSalesCartPrice(itemId, result)
                        }
                    }
                }
                calcTargetField = null
            }
        )
    }
}

