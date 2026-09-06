package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ItemEntity
import com.example.util.Formatters
import com.example.util.ItemMatcher

@Composable
fun SearchableItemAutocomplete(
    itemsList: List<ItemEntity>,
    onItemSelected: (ItemEntity) -> Unit = {},
    onItemSelectedWithQty: ((ItemEntity, Int) -> Unit)? = null,
    onItemSelectedWithQtyAndPrice: ((ItemEntity, Int, Double?) -> Unit)? = null,
    placeholderText: String = "Ketik nama atau kode barang (misal: 609, 663)...",
    excludedItemIds: List<Long> = emptyList(),
    draftItemQuantities: Map<Long, Int> = emptyMap(),
    isPenjualanMode: Boolean = false,
    onAddNewItemClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }

    var selectedItemForQty by remember { mutableStateOf<ItemEntity?>(null) }
    var qtyInputString by remember { mutableStateOf("1") }
    var priceInputString by remember { mutableStateOf("") }

    val filteredItems = remember(searchQuery, itemsList) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            ItemMatcher.searchItems(searchQuery, itemsList)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                isExpanded = it.isNotBlank()
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("searchable_autocomplete_input"),
            placeholder = {
                Text(
                    text = placeholderText,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Cari",
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        isExpanded = false
                    }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Hapus")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        AnimatedVisibility(visible = isExpanded && filteredItems.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .heightIn(max = 240.dp),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                LazyColumn(modifier = Modifier.padding(vertical = 4.dp)) {
                    items(filteredItems, key = { it.id }) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedItemForQty = item
                                    qtyInputString = "1"
                                    priceInputString = if (item.hargaModal > 0) item.hargaModal.toInt().toString() else ""
                                    searchQuery = ""
                                    isExpanded = false
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (item.kodeBarang.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                modifier = Modifier.padding(end = 6.dp)
                                            ) {
                                                Text(
                                                    text = item.kodeBarang,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = item.namaBarang,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Modal: ${Formatters.formatRupiah(item.hargaModal)}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            val inDraftQty = draftItemQuantities[item.id] ?: 0
                            val totalStk = item.totalStokCombined

                            Column(horizontalAlignment = Alignment.End) {
                                if (inDraftQty > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFFFF3E0),
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    ) {
                                        Text(
                                            text = "Di Draf: x$inDraftQty (+1)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color(0xFFE65100),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (totalStk <= 0) Color(0xFFFFEBEE) else if (totalStk < 10) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
                                ) {
                                    Text(
                                        text = if (totalStk <= 0) "Habis (0)" else "Total: $totalStk",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (totalStk <= 0) Color(0xFFD32F2F) else if (totalStk < 10) Color(0xFFE65100) else Color(0xFF2E7D32),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                Text(
                                    text = "G:${item.actualStokUtama} | T:${item.actualStokCabang}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isExpanded && searchQuery.isNotBlank() && filteredItems.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Tidak ada barang ditemukan dengan kata kunci '$searchQuery'",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (onAddNewItemClick != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val q = searchQuery
                                searchQuery = ""
                                isExpanded = false
                                onAddNewItemClick(q)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tambah '$searchQuery' Sebagai Barang Baru", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    selectedItemForQty?.let { item ->
        val draftQty = draftItemQuantities[item.id] ?: 0
        val parsedInputQty = qtyInputString.toIntOrNull() ?: 0
        val totalAfterAdd = draftQty + parsedInputQty
        val totalStock = item.totalStokCombined
        val maxAvailableStock = if (isPenjualanMode) totalStock - draftQty else Int.MAX_VALUE
        val isExceedingStock = isPenjualanMode && (parsedInputQty > maxAvailableStock)

        AlertDialog(
            onDismissRequest = { selectedItemForQty = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Input Jumlah Barang",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (item.kodeBarang.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.padding(end = 6.dp)
                                    ) {
                                        Text(
                                            text = item.kodeBarang,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = item.namaBarang,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Total Stok: $totalStock unit (Gudang: ${item.actualStokUtama} | Toko: ${item.actualStokCabang})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isPenjualanMode && totalStock < 10) Color(0xFFD32F2F) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isPenjualanMode && (item.actualStokUtama == 0 || item.actualStokCabang == 0) && totalStock > 0) {
                        Surface(
                            color = Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚡ Otomatis: Jika salah satu stok toko/gudang kosong (0), penjualan akan otomatis memotong dari sumber stok yang tersedia.",
                                fontSize = 11.sp,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    if (draftQty > 0) {
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "💡 Sudah ada $draftQty unit di draf. Jumlah yang Anda input akan ditambahkan ke draf.",
                                fontSize = 11.sp,
                                color = Color(0xFFE65100),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Text(
                        text = "Masukkan Jumlah Barang:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = qtyInputString,
                        onValueChange = { input ->
                            val digitsOnly = input.filter { it.isDigit() }
                            qtyInputString = digitsOnly
                        },
                        label = { Text("Jumlah (Unit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = isExceedingStock,
                        trailingIcon = {
                            if (qtyInputString.isNotEmpty()) {
                                IconButton(onClick = { qtyInputString = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("input_qty_popup")
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(1, 5, 10, 50).forEach { qtyAdd ->
                            OutlinedButton(
                                onClick = {
                                    val current = qtyInputString.toIntOrNull() ?: 0
                                    qtyInputString = (current + qtyAdd).toString()
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+$qtyAdd", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (!isPenjualanMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Harga Beli / Modal Baru (Rp):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = priceInputString,
                            onValueChange = { input ->
                                val digitsOnly = input.filter { it.isDigit() }
                                priceInputString = digitsOnly
                            },
                            label = { Text("Harga Modal (Rp)") },
                            placeholder = { Text(if (item.hargaModal > 0) item.hargaModal.toInt().toString() else "0") },
                            supportingText = {
                                if (item.hargaModal > 0) {
                                    Text("Harga master saat ini: ${Formatters.formatRupiah(item.hargaModal)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            trailingIcon = {
                                if (priceInputString.isNotEmpty()) {
                                    IconButton(onClick = { priceInputString = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("input_price_incoming_popup")
                        )

                        val parsedPrice = priceInputString.toDoubleOrNull() ?: 0.0
                        val stokAda = item.totalStokCombined.coerceAtLeast(0)
                        val inQty = maxOf(1, parsedInputQty)
                        val totalGabungan = stokAda + inQty
                        val selisih = parsedPrice - item.hargaModal
                        val isPriceDiff = parsedPrice > 0.0 && item.hargaModal > 0.0 && parsedPrice != item.hargaModal
                        val calculatedAvg = if (stokAda > 0 && item.hargaModal > 0.0 && parsedPrice > 0.0) {
                            kotlin.math.round(((stokAda * item.hargaModal) + (inQty * parsedPrice)) / totalGabungan)
                        } else if (parsedPrice > 0.0) {
                            parsedPrice
                        } else {
                            item.hargaModal
                        }

                        if (isPriceDiff) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
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
                                        val tanda = if (selisih >= 0) "+" else ""
                                        Text(
                                            text = "Selisih: $tanda${Formatters.formatRupiah(selisih)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selisih >= 0) androidx.compose.ui.graphics.Color(0xFFC62828) else androidx.compose.ui.graphics.Color(0xFF2E7D32)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Stok Ada: $stokAda unit @ ${Formatters.formatRupiah(item.hargaModal)}\nMasuk Baru: $inQty unit @ ${Formatters.formatRupiah(parsedPrice)}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "HPP Baru Otomatis ($totalGabungan unit):",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = Formatters.formatRupiah(calculatedAvg),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isExceedingStock) {
                        Text(
                            text = "⚠️ Jumlah melebihi stok yang tersedia (Maksimal sisa: ${maxOf(0, maxAvailableStock)} unit)!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else if (parsedInputQty > 0 && draftQty > 0) {
                        Text(
                            text = "Total di draf nanti: $totalAfterAdd unit",
                            fontSize = 12.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (parsedInputQty > 0 && !isExceedingStock) {
                            val parsedPrice = priceInputString.toDoubleOrNull()
                            if (onItemSelectedWithQtyAndPrice != null) {
                                onItemSelectedWithQtyAndPrice(item, parsedInputQty, parsedPrice)
                            } else if (onItemSelectedWithQty != null) {
                                onItemSelectedWithQty(item, parsedInputQty)
                            } else {
                                repeat(parsedInputQty) { onItemSelected(item) }
                            }
                            selectedItemForQty = null
                        }
                    },
                    enabled = parsedInputQty > 0 && !isExceedingStock,
                    modifier = Modifier.testTag("btn_konfirmasi_tambah_qty")
                ) {
                    Text("Tambah ke Draf")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedItemForQty = null }) {
                    Text("Batal")
                }
            }
        )
    }
}
