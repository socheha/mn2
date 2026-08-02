package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.SupplierPayableEntity
import com.example.ui.MainViewModel
import com.example.util.Formatters

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.FloatingActionButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.ui.components.DatePickerField
import androidx.compose.material.icons.filled.WidthFull
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material3.TextButton

@Composable
fun MenuHutangSupplierScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val payablesList by viewModel.allPayables.collectAsStateWithLifecycle()
    val totalUnpaid by viewModel.totalUnpaidPayables.collectAsStateWithLifecycle()
    val allCashAccounts by viewModel.allCashAccounts.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Belum Lunas") }
    var payableForPayment by remember { mutableStateOf<SupplierPayableEntity?>(null) }
    var payableForConfirmLunas by remember { mutableStateOf<SupplierPayableEntity?>(null) }
    var payableForHistory by remember { mutableStateOf<SupplierPayableEntity?>(null) }
    var payableToCancel by remember { mutableStateOf<SupplierPayableEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredPayables = remember(payablesList, searchQuery, selectedFilter) {
        payablesList.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                    item.namaSupplier.contains(searchQuery, ignoreCase = true) ||
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
        modifier = Modifier.testTag("menu_hutang_screen"),
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
                    Text(text = "Tambah Hutang", fontWeight = FontWeight.Bold)
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
            // Unpaid Total Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Hutang Supplier Belum Lunas",
                                fontSize = 13.sp,
                                color = Color(0xFFB71C1C),
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = Formatters.formatRupiah(totalUnpaid),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB71C1C)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.MoneyOff,
                            contentDescription = null,
                            tint = Color(0xFFB71C1C)
                        )
                    }
                }
            }

            item {
                Column {
                    Text(
                        text = "Daftar Hutang Ke Supplier",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Cari nama supplier, no faktur, catatan...") },
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
                            "Semua" to "Semua Hutang"
                        )
                        items(filters) { (key, label) ->
                            val isSelected = selectedFilter == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedFilter = key },
                                label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (key == "Nota Lunas") Color(0xFFE8F5E9) else MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = if (key == "Nota Lunas") Color(0xFF2E7D32) else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            if (filteredPayables.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "Tidak ada data hutang supplier yang cocok dengan pencarian '$searchQuery'." else "Belum ada catatan hutang supplier. Transaksi barang masuk bertipe 'Hutang' akan otomatis muncul di sini.",
                            modifier = Modifier.padding(16.dp),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredPayables, key = { it.id }) { payable ->
                    PayableCard(
                        payable = payable,
                        onPayment = { payableForPayment = payable },
                        onDirectLunas = { payableForConfirmLunas = payable },
                        onViewHistory = { payableForHistory = payable },
                        onCancel = { payableToCancel = payable }
                    )
                }
            }
        }
    }

    // Cancellation Dialog for Hutang Supplier
    payableToCancel?.let { payable ->
        AlertDialog(
            onDismissRequest = { payableToCancel = null },
            title = { Text("Batalkan Catatan Hutang Supplier?", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F)) },
            text = {
                Text(
                    "Apakah Anda yakin ingin membatalkan dan menghapus catatan hutang ke supplier '${payable.namaSupplier}' sebesar ${Formatters.formatRupiah(payable.nominalAwal)}? Seluruh riwayat pembayarannya juga akan dihapus."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelSupplierPayable(payable.id) {
                            payableToCancel = null
                            Toast.makeText(context, "Catatan hutang supplier berhasil dibatalkan!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Ya, Batalkan Hutang", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { payableToCancel = null }) {
                    Text("Tidak")
                }
            }
        )
    }

    // Add Payable Dialog
    if (showAddDialog) {
        AddPayableDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { supplier, faktur, nominal, tgl, cat ->
                viewModel.addSupplierPayable(
                    namaSupplier = supplier,
                    nomorFaktur = faktur,
                    nominal = nominal,
                    tanggal = tgl,
                    catatan = cat
                )
                showAddDialog = false
            }
        )
    }

    // Confirmation Dialog for Pelunasan Hutang (Ya / Tidak)
    payableForConfirmLunas?.let { payable ->
        var selectedAccountCode by remember { mutableStateOf("TUNAI") }
        AlertDialog(
            onDismissRequest = { payableForConfirmLunas = null },
            title = { Text("Konfirmasi Pelunasan Hutang", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Apakah Anda yakin ingin melunasi seluruh sisa hutang ke supplier ${payable.namaSupplier} sebesar ${Formatters.formatRupiah(payable.nominalSisa)}?",
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Pilih Kas / Bank Sumber Pembayaran:",
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
                        viewModel.markSupplierPayableLunas(
                            hutangId = payable.id,
                            tanggal = Formatters.getCurrentDateFormatted(),
                            metodePembayaran = selectedAccountCode
                        )
                        payableForConfirmLunas = null
                        Toast.makeText(context, "Hutang ke supplier '${payable.namaSupplier}' berhasil dilunasi!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Ya, Pelunasan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { payableForConfirmLunas = null }) {
                    Text("Tidak / Batal")
                }
            }
        )
    }

    // Payment Dialog
    payableForPayment?.let { payable ->
        PaymentDialog(
            title = "Pembayaran Hutang Supplier - ${payable.namaSupplier}",
            sisaTagihan = payable.nominalSisa,
            allAccounts = allCashAccounts,
            onDismiss = { payableForPayment = null },
            onConfirm = { nominal, tgl, cat, metode ->
                viewModel.addSupplierPayment(
                    hutangId = payable.id,
                    nominalBayar = nominal,
                    tanggal = tgl,
                    catatan = cat,
                    metodePembayaran = metode
                )
                payableForPayment = null
                Toast.makeText(context, "Pembayaran hutang supplier ($metode) berhasil dicatat!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Payment History & Cancellation Dialog
    payableForHistory?.let { payable ->
        val paymentsList by viewModel.getSupplierPayments(payable.id).collectAsStateWithLifecycle(emptyList())
        var paymentToCancel by remember { mutableStateOf<com.example.data.entity.SupplierPaymentEntity?>(null) }

        AlertDialog(
            onDismissRequest = { payableForHistory = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Riwayat Pelunasan - ${payable.namaSupplier}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (paymentsList.isEmpty()) {
                        Text(
                            text = "Belum ada riwayat pembayaran untuk hutang ini.",
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
                                                color = Color(0xFFD32F2F),
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "Tgl: ${Formatters.formatDateToIndonesian(payment.tanggal)} | Akun: ${payment.metodePembayaran}",
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
                TextButton(onClick = { payableForHistory = null }) {
                    Text("Tutup")
                }
            }
        )

        paymentToCancel?.let { payment ->
            AlertDialog(
                onDismissRequest = { paymentToCancel = null },
                title = { Text("Batalkan Pelunasan?", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F)) },
                text = {
                    Text("Apakah Anda yakin ingin membatalkan pembayaran hutang sebesar ${Formatters.formatRupiah(payment.nominalBayar)}? Sisa hutang akan bertambah kembali dan saldo kas/bank akan dikembalikan.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.cancelSupplierPayment(payment.id) {
                                paymentToCancel = null
                                Toast.makeText(context, "Pelunasan hutang berhasil dibatalkan!", Toast.LENGTH_SHORT).show()
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
fun PayableCard(
    payable: SupplierPayableEntity,
    onPayment: () -> Unit,
    onDirectLunas: () -> Unit,
    onViewHistory: () -> Unit,
    onCancel: () -> Unit
) {
    val isLunas = payable.status == "Lunas"

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
                        text = payable.namaSupplier,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Tgl Masuk: ${Formatters.formatDateToIndonesian(payable.tanggal)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isLunas) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ) {
                    Text(
                        text = payable.status,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isLunas) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Nominal Pembelian:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = Formatters.formatRupiah(payable.nominalAwal), fontSize = 13.sp)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Sisa Hutang:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = Formatters.formatRupiah(payable.nominalSisa),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLunas) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                    )
                }
            }

            if (payable.catatan.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Catatan/Faktur: ${payable.catatan}",
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
                        Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(14.dp).padding(end = 2.dp))
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
                        Text("Lunas", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AddPayableDialog(
    onDismiss: () -> Unit,
    onConfirm: (supplier: String, faktur: String, nominal: Double, tgl: String, cat: String) -> Unit
) {
    var supplier by remember { mutableStateOf("") }
    var faktur by remember { mutableStateOf("") }
    var nominalStr by remember { mutableStateOf("") }
    var tanggal by remember { mutableStateOf(Formatters.getCurrentDateFormatted()) }
    var catatan by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Catatan Hutang Supplier", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = supplier,
                    onValueChange = { supplier = it },
                    label = { Text("Nama Supplier *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = faktur,
                    onValueChange = { faktur = it },
                    label = { Text("No. Faktur / Nota (Opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nominalStr,
                    onValueChange = { nominalStr = it.filter { char -> char.isDigit() } },
                    label = { Text("Nominal Hutang (Rp) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                DatePickerField(
                    label = "Tanggal Transaksi",
                    value = tanggal,
                    onDateSelected = { tanggal = it },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = catatan,
                    onValueChange = { catatan = it },
                    label = { Text("Catatan / Keterangan (Opsional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val nominal = nominalStr.toDoubleOrNull() ?: 0.0
                    if (supplier.isNotBlank() && nominal > 0) {
                        onConfirm(supplier, faktur, nominal, tanggal, catatan)
                    }
                }
            ) {
                Text("Simpan Hutang")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
