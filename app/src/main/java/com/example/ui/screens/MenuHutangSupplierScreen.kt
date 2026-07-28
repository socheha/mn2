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

@Composable
fun MenuHutangSupplierScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val payablesList by viewModel.allPayables.collectAsStateWithLifecycle()
    val totalUnpaid by viewModel.totalUnpaidPayables.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var payableForPayment by remember { mutableStateOf<SupplierPayableEntity?>(null) }

    val filteredPayables = remember(payablesList, searchQuery) {
        if (searchQuery.isBlank()) payablesList
        else payablesList.filter {
            it.namaSupplier.contains(searchQuery, ignoreCase = true) ||
                    it.catatan.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        modifier = Modifier.testTag("menu_hutang_screen")
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
                        onDirectLunas = {
                            viewModel.markSupplierPayableLunas(
                                hutangId = payable.id,
                                tanggal = Formatters.getCurrentDateFormatted()
                            )
                            Toast.makeText(context, "Hutang ke supplier '${payable.namaSupplier}' telah dilunasi!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // Payment Dialog
    payableForPayment?.let { payable ->
        PaymentDialog(
            title = "Pembayaran Hutang Supplier - ${payable.namaSupplier}",
            sisaTagihan = payable.nominalSisa,
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
}

@Composable
fun PayableCard(
    payable: SupplierPayableEntity,
    onPayment: () -> Unit,
    onDirectLunas: () -> Unit
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
                        Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Bayar Sebagian", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onDirectLunas,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Lunas", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
