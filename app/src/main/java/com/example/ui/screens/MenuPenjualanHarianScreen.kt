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
import androidx.compose.material.icons.filled.Info
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
    var selectedMetodePembayaran by remember { mutableStateOf("Tunai") } // "Tunai", "Transfer", "Piutang"
    var selectedTransferAccountCode by remember { mutableStateOf("BCA") } // "BCA", "MANDIRI", "BRI", "BNI", "BANK_LAIN", "GOPAY", "OVO", "DANA", "SHOPEEPAY", "LINKAJA"
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
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilterChip(
                                    selected = selectedMetodePembayaran == "Tunai",
                                    onClick = { selectedMetodePembayaran = "Tunai" },
                                    label = {
                                        Text("Tunai", fontSize = 11.sp)
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFE8F5E9),
                                        selectedLabelColor = Color(0xFF2E7D32)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                FilterChip(
                                    selected = selectedMetodePembayaran == "Transfer",
                                    onClick = { selectedMetodePembayaran = "Transfer" },
                                    label = {
                                        Text("Transfer", fontSize = 11.sp)
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFE3F2FD),
                                        selectedLabelColor = Color(0xFF1565C0)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                FilterChip(
                                    selected = selectedMetodePembayaran == "Piutang",
                                    onClick = { selectedMetodePembayaran = "Piutang" },
                                    label = {
                                        Text("Piutang", fontSize = 11.sp)
                                    },
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
                            onItemSelected = { selectedItem ->
                                if (selectedItem.stok <= 0) {
                                    Toast.makeText(context, "Stok '${selectedItem.namaBarang}' habis (0)!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val currentQty = draftQuantitiesMap[selectedItem.id] ?: 0
                                    viewModel.addSalesCartItem(selectedItem)
                                    val newQty = currentQty + 1
                                    val toastMsg = if (currentQty > 0) {
                                        "'${selectedItem.namaBarang}' ditambah (+1) di draf (Total: $newQty)"
                                    } else {
                                        "'${selectedItem.namaBarang}' dimasukkan ke draf"
                                    }
                                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
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
                                                val qty = if (input.isBlank()) 0 else (input.toIntOrNull() ?: 0)
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
                                                val price = input.toDoubleOrNull() ?: 0.0
                                                viewModel.updateSalesCartPrice(cartItem.item.id, price)
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            if (!isNotaTotalMode) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Subtotal: ${Formatters.formatRupiah(cartItem.jumlahTerjual * cartItem.hargaSatuan)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.End)
                                )
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                    label = { Text("Transfer Bank / E-Wallet", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFE3F2FD),
                                        selectedLabelColor = Color(0xFF1565C0)
                                    ),
                                    modifier = Modifier.weight(1f)
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
                            }
                        } else {
                            OutlinedTextField(
                                value = customTotalUang,
                                onValueChange = { customTotalUang = it },
                                label = { Text("Total Uang Penjualan (Rp) (Opsional Override)") },
                                placeholder = { Text(Formatters.formatRupiah(calculatedTotal)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Kosongkan jika menyamakan dengan total hitungan items.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
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

                                val finalMoney = if (isNotaTotalMode) {
                                    val money = totalUangNotaInput.toDoubleOrNull() ?: 0.0
                                    if (money <= 0) {
                                        Toast.makeText(context, "Mohon masukkan Total Uang Nota Penjualan!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    money
                                } else {
                                    customTotalUang.toDoubleOrNull() ?: calculatedTotal
                                }

                                val dpMoney = nominalUangMukaInput.toDoubleOrNull() ?: 0.0

                                val paymentMethodToSave = if (selectedMetodePembayaran == "Transfer") {
                                    "Transfer"
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
                                    targetAccountCode = if (selectedMetodePembayaran == "Transfer") selectedTransferAccountCode else "TUNAI",
                                    onSuccess = {
                                        totalUangNotaInput = ""
                                        customTotalUang = ""
                                        catatan = ""
                                        namaTokoPelanggan = ""
                                        nomorHpPelanggan = ""
                                        nominalUangMukaInput = ""
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
        }
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
}

