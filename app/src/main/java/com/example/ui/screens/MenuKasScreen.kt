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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SwapHoriz
import com.example.ui.components.CalculatorDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.components.DatePickerField
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuKasScreen(
    viewModel: MainViewModel
) {
    val totalCash by viewModel.totalCashBalance.collectAsStateWithLifecycle()
    val kasTunai by viewModel.kasTunaiBalance.collectAsStateWithLifecycle()
    val kasBank by viewModel.kasBankBalance.collectAsStateWithLifecycle()
    val allAccounts by viewModel.allCashAccounts.collectAsStateWithLifecycle()
    val mutations by viewModel.allCashMutations.collectAsStateWithLifecycle()

    var showKasMasukDialog by remember { mutableStateOf(false) }
    var showKasKeluarDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showEditSaldoDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var targetAccountForEdit by remember { mutableStateOf("TUNAI") }
    var accountToDelete by remember { mutableStateOf<com.example.data.entity.CashAccountEntity?>(null) }
    var mutationToCancel by remember { mutableStateOf<com.example.data.entity.CashMutationEntity?>(null) }

    var selectedFilterAccount by remember { mutableStateOf("ALL") } // "ALL", "TUNAI", "NON_TUNAI"

    val filteredMutations = remember(mutations, selectedFilterAccount) {
        if (selectedFilterAccount == "ALL") {
            mutations
        } else if (selectedFilterAccount == "TUNAI") {
            mutations.filter { it.accountType == "TUNAI" }
        } else {
            mutations.filter { it.accountType != "TUNAI" }
        }
    }

    Scaffold(
        modifier = Modifier.testTag("menu_kas_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Manajemen Kas & Bank",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Total Likuiditas Keuangan Toko",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    onClick = { showClearConfirmDialog = true },
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White.copy(alpha = 0.2f),
                                    contentColor = Color.White,
                                    modifier = Modifier.testTag("btn_kosongkan_kas")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteForever,
                                            contentDescription = "Kosongkan Kas",
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Kosongkan Kas", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = Formatters.formatRupiah(totalCash),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { showKasMasukDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2E7D32),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("+ Masuk", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { showKasKeluarDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoneyOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("+ Keluar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { showTransferDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1565C0),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Transfer", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Cash Accounts Grid & Breakdown (Tunai, Bank, E-Wallet)
            item {
                Text(
                    text = "Rincian Saldo Kas, Bank & E-Wallet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Kas Tunai Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Money,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Kas Tunai",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF1B5E20)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        targetAccountForEdit = "TUNAI"
                                        showEditSaldoDialog = true
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Saldo",
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = Formatters.formatRupiah(kasTunai),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Uang fisik di kasir toko",
                                fontSize = 11.sp,
                                color = Color(0xFF2E7D32).copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Kas Bank & E-Wallet Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = Color(0xFF1565C0),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Total Bank/E-Wallet",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0D47A1)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = Formatters.formatRupiah(kasBank),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0D47A1)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Transfer Bank & Digital",
                                fontSize = 11.sp,
                                color = Color(0xFF1565C0).copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Breakdown list of Bank & E-Wallet Accounts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Daftar Bank & E-Wallet:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        var showAddAccountDialog by remember { mutableStateOf(false) }
                        var showClearConfirmDialog by remember { mutableStateOf(false) }

                        OutlinedButton(
                            onClick = { showAddAccountDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Tambah Bank", fontSize = 11.sp)
                        }

                        IconButton(
                            onClick = { showClearConfirmDialog = true },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFFFFEBEE), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = "Kosongkan Bank",
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (showClearConfirmDialog) {
                            AlertDialog(
                                onDismissRequest = { showClearConfirmDialog = false },
                                title = { Text("Kosongkan Semua Bank & E-Wallet?") },
                                text = { Text("Semua akun Bank dan E-Wallet akan direset saldonya menjadi 0. Anda dapat menginput bank/e-wallet sendiri.") },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            viewModel.clearAllBankAccounts()
                                            showClearConfirmDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                                    ) {
                                        Text("Ya, Kosongkan", color = Color.White)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showClearConfirmDialog = false }) {
                                        Text("Batal")
                                    }
                                }
                            )
                        }

                        if (showAddAccountDialog) {
                            var bankName by remember { mutableStateOf("") }
                            var initialSaldoStr by remember { mutableStateOf("0") }
                            var selectedCategory by remember { mutableStateOf("BANK") } // "BANK" or "E-WALLET"

                            AlertDialog(
                                onDismissRequest = { showAddAccountDialog = false },
                                title = { Text("Tambah Bank / E-Wallet Baru", fontWeight = FontWeight.Bold) },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            FilterChip(
                                                selected = selectedCategory == "BANK",
                                                onClick = { selectedCategory = "BANK" },
                                                label = { Text("Bank") }
                                            )
                                            FilterChip(
                                                selected = selectedCategory == "E-WALLET",
                                                onClick = { selectedCategory = "E-WALLET" },
                                                label = { Text("E-Wallet") }
                                            )
                                        }

                                        OutlinedTextField(
                                            value = bankName,
                                            onValueChange = { bankName = it },
                                            label = { Text("Nama Bank / E-Wallet") },
                                            placeholder = { Text("Contoh: Bank BCA, Gopay, ShopeePay") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        OutlinedTextField(
                                            value = initialSaldoStr,
                                            onValueChange = { initialSaldoStr = it },
                                            label = { Text("Saldo Awal (Rp)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            if (bankName.isNotBlank()) {
                                                val initSaldo = initialSaldoStr.toDoubleOrNull() ?: 0.0
                                                val cleanCode = bankName.trim().uppercase().replace(" ", "_")
                                                viewModel.addCustomCashAccount(
                                                    accountType = cleanCode,
                                                    accountName = bankName.trim(),
                                                    initialBalance = initSaldo
                                                )
                                                showAddAccountDialog = false
                                            }
                                        }
                                    ) {
                                        Text("Simpan Akun")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showAddAccountDialog = false }) {
                                        Text("Batal")
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                val nonTunaiAccounts = remember(allAccounts) {
                    allAccounts.filter { it.accountType != "TUNAI" }
                }

                if (nonTunaiAccounts.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Belum ada Bank / E-Wallet terdaftar. Klik '+ Tambah Bank' di atas untuk menginput akun baru.",
                            modifier = Modifier.padding(12.dp),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(nonTunaiAccounts, key = { it.accountType }) { acc ->
                            val currentSaldo = acc.saldo

                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (currentSaldo > 0) Color(0xFFE3F2FD) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable {
                                                targetAccountForEdit = acc.accountType
                                                showEditSaldoDialog = true
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (com.example.data.entity.CashAccountDefaults.getAccountCategory(acc.accountType) == "E-WALLET") Icons.Default.Payments else Icons.Default.AccountBalance,
                                                contentDescription = null,
                                                tint = Color(0xFF1565C0),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = acc.accountName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                        }

                                        IconButton(
                                            onClick = { accountToDelete = acc },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteForever,
                                                contentDescription = "Hapus Akun",
                                                tint = Color(0xFFC62828),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = Formatters.formatRupiah(currentSaldo),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentSaldo > 0) Color(0xFF0D47A1) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.clickable {
                                            targetAccountForEdit = acc.accountType
                                            showEditSaldoDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Cash Mutations / History Section Header & Filter Tabs
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Riwayat Mutasi & Kas Keluar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Centered Segmented Filter Row
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val options = listOf(
                                "ALL" to "Semua",
                                "TUNAI" to "Tunai",
                                "NON_TUNAI" to "Transfer"
                            )
                            options.forEach { (code, label) ->
                                val isSelected = selectedFilterAccount == code
                                Surface(
                                    onClick = { selectedFilterAccount = code },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (filteredMutations.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = "Belum ada catatan mutasi kas.",
                            modifier = Modifier.padding(16.dp),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredMutations, key = { it.id }) { mutation ->
                    val isMasuk = mutation.jenis == "MASUK" || (mutation.jenis == "PENYESUAIAN" && mutation.nominal >= 0)
                    val isPenyesuaian = mutation.jenis == "PENYESUAIAN"

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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isPenyesuaian) Color(0xFFFFF3E0)
                                            else if (isMasuk) Color(0xFFE8F5E9)
                                            else Color(0xFFFFEBEE)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPenyesuaian) Icons.Default.Edit
                                        else if (isMasuk) Icons.Default.ArrowDownward
                                        else Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = if (isPenyesuaian) Color(0xFFE65100)
                                        else if (isMasuk) Color(0xFF2E7D32)
                                        else Color(0xFFC62828),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val accountName = com.example.data.entity.CashAccountDefaults.getAccountName(mutation.accountType)
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (mutation.accountType == "TUNAI") Color(0xFFE8F5E9) else Color(0xFFE3F2FD),
                                            modifier = Modifier.padding(end = 6.dp)
                                        ) {
                                            Text(
                                                text = accountName,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (mutation.accountType == "TUNAI") Color(0xFF2E7D32) else Color(0xFF1565C0),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Text(
                                            text = mutation.kategori,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    if (mutation.keterangan.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = mutation.keterangan,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${mutation.tanggal} • Saldo: ${Formatters.formatRupiah(mutation.saldoSesudah)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${if (isMasuk) "+" else "-"}${Formatters.formatRupiah(kotlin.math.abs(mutation.nominal))}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isPenyesuaian) Color(0xFFE65100)
                                    else if (isMasuk) Color(0xFF2E7D32)
                                    else Color(0xFFC62828)
                                )
                                TextButton(
                                    onClick = { mutationToCancel = mutation },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text("Batalkan", fontSize = 10.sp, color = Color(0xFFD32F2F), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Mutation Cancellation
    mutationToCancel?.let { mut ->
        val context = androidx.compose.ui.platform.LocalContext.current
        AlertDialog(
            onDismissRequest = { mutationToCancel = null },
            title = { Text("Batalkan Transaksi Kas?", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F)) },
            text = {
                Text(
                    "Apakah Anda yakin ingin membatalkan transaksi mutasi kas '${mut.kategori}' sebesar ${Formatters.formatRupiah(kotlin.math.abs(mut.nominal))}? Saldo kas ${mut.accountType} akan dikembalikan/di-adjust secara otomatis."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelCashMutation(mut.id) {
                            mutationToCancel = null
                            android.widget.Toast.makeText(context, "Transaksi mutasi kas berhasil dibatalkan!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Ya, Batalkan Transaksi", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { mutationToCancel = null }) {
                    Text("Tidak")
                }
            }
        )
    }

    // --- Dialog Kas Masuk (Income) ---
    if (showKasMasukDialog) {
        var nominalStr by remember { mutableStateOf("") }
        var targetAccountCode by remember { mutableStateOf("TUNAI") }
        var kategori by remember { mutableStateOf("Setoran Kasir") }
        var keterangan by remember { mutableStateOf("") }
        var tanggal by remember { mutableStateOf(Formatters.getCurrentDateFormatted()) }

        val kategoriOptions = listOf(
            "Setoran Kasir",
            "Modal Awal Toko",
            "Bunga / Bagi Hasil",
            "Cashback / Bonus",
            "Pemasukan Lain-lain"
        )
        var categoryExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showKasMasukDialog = false },
            title = {
                Text(
                    text = "Catat Kas Masuk (Pemasukan / Setoran)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Target Rekening Kas / Bank / E-Wallet:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(com.example.data.entity.CashAccountDefaults.ALL_DEFAULT_ACCOUNTS) { acc ->
                            FilterChip(
                                selected = targetAccountCode == acc.type,
                                onClick = { targetAccountCode = acc.type },
                                label = { Text(acc.name, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFE8F5E9),
                                    selectedLabelColor = Color(0xFF2E7D32)
                                )
                            )
                        }
                    }

                    var showCalcMasuk by remember { mutableStateOf(false) }
                    if (showCalcMasuk) {
                        CalculatorDialog(
                            initialValue = nominalStr,
                            onDismiss = { showCalcMasuk = false },
                            onApplyResult = { res ->
                                nominalStr = if (res % 1.0 == 0.0) res.toLong().toString() else res.toString()
                            }
                        )
                    }

                    OutlinedTextField(
                        value = nominalStr,
                        onValueChange = { nominalStr = it },
                        label = { Text("Nominal Kas Masuk (Rp)") },
                        placeholder = { Text("100000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { showCalcMasuk = true }) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = "Kalkulator",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Kategori Dropdown
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = kategori,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kategori Pemasukan") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            kategoriOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        kategori = option
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = keterangan,
                        onValueChange = { keterangan = it },
                        label = { Text("Keterangan / Catatan") },
                        placeholder = { Text("Contoh: Tambahan modal toko dari pemilik") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    DatePickerField(
                        value = tanggal,
                        onDateSelected = { tanggal = it },
                        label = "Tanggal Mutasi",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = nominalStr.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            viewModel.addIncome(
                                accountType = targetAccountCode,
                                amount = amount,
                                category = kategori,
                                note = keterangan.ifBlank { kategori },
                                date = tanggal
                            )
                            showKasMasukDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Simpan Kas Masuk", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showKasMasukDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // --- Dialog Kas Keluar (Expense) ---
    if (showKasKeluarDialog) {
        var nominalStr by remember { mutableStateOf("") }
        var sumberKasCode by remember { mutableStateOf("TUNAI") }
        var kategori by remember { mutableStateOf("Biaya Operasional") }
        var keterangan by remember { mutableStateOf("") }
        var tanggal by remember { mutableStateOf(Formatters.getCurrentDateFormatted()) }

        val kategoriOptions = listOf(
            "Biaya Operasional",
            "Gaji Karyawan",
            "Listrik & Air",
            "Sewa Tempat",
            "Transport / Kurir",
            "Konsumsi & Keperluan Toko",
            "Lain-lain"
        )
        var categoryExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showKasKeluarDialog = false },
            title = {
                Text(
                    text = "Catat Kas Keluar (Pengeluaran)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Sumber Uang Kas / Bank / E-Wallet:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(com.example.data.entity.CashAccountDefaults.ALL_DEFAULT_ACCOUNTS) { acc ->
                            FilterChip(
                                selected = sumberKasCode == acc.type,
                                onClick = { sumberKasCode = acc.type },
                                label = { Text(acc.name, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFEBEE),
                                    selectedLabelColor = Color(0xFFC62828)
                                )
                            )
                        }
                    }

                    var showCalcKeluar by remember { mutableStateOf(false) }
                    if (showCalcKeluar) {
                        CalculatorDialog(
                            initialValue = nominalStr,
                            onDismiss = { showCalcKeluar = false },
                            onApplyResult = { res ->
                                nominalStr = if (res % 1.0 == 0.0) res.toLong().toString() else res.toString()
                            }
                        )
                    }

                    OutlinedTextField(
                        value = nominalStr,
                        onValueChange = { nominalStr = it },
                        label = { Text("Nominal Kas Keluar (Rp)") },
                        placeholder = { Text("50000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { showCalcKeluar = true }) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = "Kalkulator",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Kategori Dropdown
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                    ) {
                        OutlinedTextField(
                            value = kategori,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Kategori Pengeluaran") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            kategoriOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        kategori = option
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = keterangan,
                        onValueChange = { keterangan = it },
                        label = { Text("Keterangan / Catatan") },
                        placeholder = { Text("Contoh: Bayar token listrik toko") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    DatePickerField(
                        value = tanggal,
                        onDateSelected = { tanggal = it },
                        label = "Tanggal Mutasi",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = nominalStr.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            viewModel.addExpense(
                                accountType = sumberKasCode,
                                amount = amount,
                                category = kategori,
                                note = keterangan.ifBlank { kategori },
                                date = tanggal
                            )
                            showKasKeluarDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Simpan Kas Keluar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showKasKeluarDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // --- Dialog Transfer Antar Kas / Bank ---
    if (showTransferDialog) {
        var nominalStr by remember { mutableStateOf("") }
        var sumberKasCode by remember { mutableStateOf("TUNAI") }
        var tujuanKasCode by remember { mutableStateOf("BCA") }
        var keterangan by remember { mutableStateOf("") }
        var tanggal by remember { mutableStateOf(Formatters.getCurrentDateFormatted()) }
        var showCalcTransfer by remember { mutableStateOf(false) }

        if (showCalcTransfer) {
            CalculatorDialog(
                initialValue = nominalStr,
                onDismiss = { showCalcTransfer = false },
                onApplyResult = { res ->
                    nominalStr = if (res % 1.0 == 0.0) res.toLong().toString() else res.toString()
                }
            )
        }

        val availableAccounts = remember(allAccounts) {
            val defaults = com.example.data.entity.CashAccountDefaults.ALL_DEFAULT_ACCOUNTS
            val registeredTypes = allAccounts.map { it.accountType }.toSet()
            val combined = allAccounts.map {
                com.example.data.entity.AccountTypeInfo(it.accountType, it.accountName, com.example.data.entity.CashAccountDefaults.getAccountCategory(it.accountType))
            }.toMutableList()

            defaults.forEach { d ->
                if (d.type !in registeredTypes) {
                    combined.add(d)
                }
            }
            combined
        }

        AlertDialog(
            onDismissRequest = { showTransferDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = Color(0xFF1565C0),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Transfer Antar Kas / Bank",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "1. Dari Kas / Rekening (Pengirim):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(availableAccounts) { acc ->
                            val accSaldo = allAccounts.find { it.accountType == acc.type }?.saldo ?: 0.0
                            FilterChip(
                                selected = sumberKasCode == acc.type,
                                onClick = {
                                    sumberKasCode = acc.type
                                    if (tujuanKasCode == acc.type) {
                                        tujuanKasCode = availableAccounts.find { it.type != acc.type }?.type ?: "BCA"
                                    }
                                },
                                label = { Text("${acc.name} (${Formatters.formatRupiah(accSaldo)})", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFF3E0),
                                    selectedLabelColor = Color(0xFFE65100)
                                )
                            )
                        }
                    }

                    Text(
                        text = "2. Ke Kas / Rekening (Penerima):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(availableAccounts.filter { it.type != sumberKasCode }) { acc ->
                            val accSaldo = allAccounts.find { it.accountType == acc.type }?.saldo ?: 0.0
                            FilterChip(
                                selected = tujuanKasCode == acc.type,
                                onClick = { tujuanKasCode = acc.type },
                                label = { Text("${acc.name} (${Formatters.formatRupiah(accSaldo)})", fontSize = 11.sp) },
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
                        label = { Text("Nominal Transfer (Rp)") },
                        placeholder = { Text("100000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { showCalcTransfer = true }) {
                                Icon(
                                    imageVector = Icons.Default.Calculate,
                                    contentDescription = "Kalkulator",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = keterangan,
                        onValueChange = { keterangan = it },
                        label = { Text("Keterangan / Catatan Transfer (Opsional)") },
                        placeholder = { Text("Contoh: Pindah saldo kas toko ke rekening BCA") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    DatePickerField(
                        value = tanggal,
                        onDateSelected = { tanggal = it },
                        label = "Tanggal Mutasi",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = nominalStr.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            viewModel.transferCash(
                                fromAccount = sumberKasCode,
                                toAccount = tujuanKasCode,
                                amount = amount,
                                note = keterangan,
                                date = tanggal
                            )
                            showTransferDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                ) {
                    Text("Proses Transfer", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // --- Dialog Update Saldo Manual ---
    if (showEditSaldoDialog) {
        val accountName = com.example.data.entity.CashAccountDefaults.getAccountName(targetAccountForEdit)
        val currentEntity = allAccounts.find { it.accountType == targetAccountForEdit }
        val currentSaldo = currentEntity?.saldo ?: 0.0
        var newSaldoStr by remember { mutableStateOf(currentSaldo.toLong().toString()) }
        var noteStr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showEditSaldoDialog = false },
            title = {
                Text(
                    text = "Perbarui Saldo $accountName",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Ganti target akun:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(com.example.data.entity.CashAccountDefaults.ALL_DEFAULT_ACCOUNTS) { acc ->
                            FilterChip(
                                selected = targetAccountForEdit == acc.type,
                                onClick = {
                                    targetAccountForEdit = acc.type
                                    val selSaldo = allAccounts.find { it.accountType == acc.type }?.saldo ?: 0.0
                                    newSaldoStr = selSaldo.toLong().toString()
                                },
                                label = { Text(acc.name, fontSize = 11.sp) }
                            )
                        }
                    }

                    Text(
                        text = "Saldo saat ini: ${Formatters.formatRupiah(currentSaldo)}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = newSaldoStr,
                        onValueChange = { newSaldoStr = it },
                        label = { Text("Saldo Baru (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = noteStr,
                        onValueChange = { noteStr = it },
                        label = { Text("Alasan / Catatan Penyesuaian") },
                        placeholder = { Text("Contoh: Opnam kas/saldo fisik") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newAmount = newSaldoStr.toDoubleOrNull()
                        if (newAmount != null) {
                            viewModel.updateCashBalanceManual(
                                accountType = targetAccountForEdit,
                                newBalance = newAmount,
                                note = noteStr
                            )
                            showEditSaldoDialog = false
                        }
                    }
                ) {
                    Text("Simpan Saldo Baru")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditSaldoDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Kosongkan Riwayat & Saldo Kas", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFC62828)) },
            text = {
                Text(
                    text = "Apakah Anda yakin ingin mengosongkan seluruh riwayat mutasi kas dan mereset saldo Kas & Bank menjadi 0?",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearConfirmDialog = false
                        viewModel.clearCashHistoryAndBalances()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("Kosongkan Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    accountToDelete?.let { acc ->
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = { Text("Hapus ${acc.accountName}?", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFC62828)) },
            text = { Text("Akun ${acc.accountName} akan dihapus dari daftar Bank / E-Wallet.", fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCashAccount(acc.accountType)
                        accountToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("Hapus Akun")
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}
