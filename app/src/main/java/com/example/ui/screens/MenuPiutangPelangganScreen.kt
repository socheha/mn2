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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material.icons.filled.Payment
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

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var receivableForPayment by remember { mutableStateOf<CustomerReceivableEntity?>(null) }

    val filteredReceivables = remember(receivablesList, searchQuery) {
        if (searchQuery.isBlank()) receivablesList
        else receivablesList.filter {
            it.namaPelanggan.contains(searchQuery, ignoreCase = true) ||
                    it.nomorHp.contains(searchQuery, ignoreCase = true) ||
                    it.catatan.contains(searchQuery, ignoreCase = true)
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
            // Unpaid Total Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F7FA))
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
                                text = "Total Piutang Belum Lunas",
                                fontSize = 13.sp,
                                color = Color(0xFF006064),
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = Formatters.formatRupiah(totalUnpaid),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF006064)
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color(0xFF006064)
                        )
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
                }
            }

            if (filteredReceivables.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "Tidak ada data piutang pelanggan yang cocok dengan pencarian '$searchQuery'." else "Belum ada catatan piutang pelanggan.",
                            modifier = Modifier.padding(16.dp),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredReceivables, key = { it.id }) { receivable ->
                    ReceivableCard(
                        receivable = receivable,
                        onPayment = { receivableForPayment = receivable },
                        onDirectLunas = {
                            viewModel.markCustomerReceivableLunas(
                                piutangId = receivable.id,
                                tanggal = Formatters.getCurrentDateFormatted()
                            )
                            Toast.makeText(context, "Piutang '${receivable.namaPelanggan}' telah dilunasi!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // Add Receivable Dialog
    if (showAddDialog) {
        AddReceivableDialog(
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

    // Payment Dialog
    receivableForPayment?.let { receivable ->
        PaymentDialog(
            title = "Pembayaran Piutang - ${receivable.namaPelanggan}",
            sisaTagihan = receivable.nominalSisa,
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
}

@Composable
fun ReceivableCard(
    receivable: CustomerReceivableEntity,
    onPayment: () -> Unit,
    onDirectLunas: () -> Unit
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
                    Text(
                        text = "Tgl Transaksi: ${Formatters.formatDateToIndonesian(receivable.tanggal)}",
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
                    Text(text = "Nominal Awal:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = Formatters.formatRupiah(receivable.nominalAwal), fontSize = 13.sp)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Sisa Piutang:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = Formatters.formatRupiah(receivable.nominalSisa),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLunas) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                    )
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

            if (!isLunas) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onPayment,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Payment, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Bayar Sebagian", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onDirectLunas,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Pelunasan", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AddReceivableDialog(
    onDismiss: () -> Unit,
    onConfirm: (nama: String, hp: String, nominal: Double, tgl: String, cat: String, jatuhTempo: String) -> Unit
) {
    var nama by remember { mutableStateOf("") }
    var hp by remember { mutableStateOf("") }
    var nominalStr by remember { mutableStateOf("") }
    var tanggal by remember { mutableStateOf(Formatters.getCurrentDateFormatted()) }
    var jatuhTempo by remember { mutableStateOf(Formatters.getAddDaysDate(Formatters.getCurrentDateFormatted(), 14)) }
    var catatan by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Piutang Pelanggan", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nama,
                    onValueChange = { nama = it },
                    label = { Text("Nama Pelanggan *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

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
    onDismiss: () -> Unit,
    onConfirm: (nominal: Double, tgl: String, cat: String, metode: String) -> Unit
) {
    var nominalStr by remember { mutableStateOf("") }
    var tanggal by remember { mutableStateOf(Formatters.getCurrentDateFormatted()) }
    var catatan by remember { mutableStateOf("") }
    var metodePembayaran by remember { mutableStateOf("Tunai") } // "Tunai" or "Transfer"

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
                    text = "Metode Pembayaran Masuk:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = metodePembayaran == "Tunai",
                        onClick = { metodePembayaran = "Tunai" },
                        label = { Text("Kas Tunai") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE8F5E9),
                            selectedLabelColor = Color(0xFF2E7D32)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = metodePembayaran == "Transfer",
                        onClick = { metodePembayaran = "Transfer" },
                        label = { Text("Kas Bank") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE3F2FD),
                            selectedLabelColor = Color(0xFF1565C0)
                        ),
                        modifier = Modifier.weight(1f)
                    )
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
