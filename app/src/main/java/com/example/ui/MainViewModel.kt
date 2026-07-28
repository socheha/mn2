package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.dao.SoldItemSummary
import com.example.data.entity.CashAccountEntity
import com.example.data.entity.CashMutationEntity
import com.example.data.entity.CustomerPaymentEntity
import com.example.data.entity.CustomerReceivableEntity
import com.example.data.entity.IncomingItemEntity
import com.example.data.entity.IncomingTransactionEntity
import com.example.data.entity.ItemEntity
import com.example.data.entity.SalesItemEntity
import com.example.data.entity.SalesTransactionEntity
import com.example.data.entity.StockHistoryEntity
import com.example.data.entity.SupplierPayableEntity
import com.example.data.entity.SupplierPaymentEntity
import com.example.util.Formatters
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CartItemIncoming(
    val item: ItemEntity,
    var jumlahMasuk: Int = 1,
    var hargaModal: Double = item.hargaModal
)

data class CartItemSales(
    val item: ItemEntity,
    var jumlahTerjual: Int = 1,
    var hargaSatuan: Double = item.hargaModal
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val itemDao = db.itemDao()
    private val stockHistoryDao = db.stockHistoryDao()
    private val incomingDao = db.incomingDao()
    private val salesDao = db.salesDao()
    private val customerReceivableDao = db.customerReceivableDao()
    private val supplierPayableDao = db.supplierPayableDao()
    private val cashDao = db.cashDao()

    // --- Cash Management States ---
    val allCashAccounts: StateFlow<List<CashAccountEntity>> = cashDao.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCashBalance: StateFlow<Double> = cashDao.getTotalCashBalance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val kasTunaiBalance: StateFlow<Double> = cashDao.getAllAccounts().map { accounts ->
        accounts.find { it.accountType == "TUNAI" }?.saldo ?: 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val kasBankBalance: StateFlow<Double> = cashDao.getAllAccounts().map { accounts ->
        accounts.filter { it.accountType != "TUNAI" }.sumOf { it.saldo }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val allCashMutations: StateFlow<List<CashMutationEntity>> = cashDao.getAllMutations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun recordCashInDirect(accountType: String, amount: Double, category: String, note: String, date: String) {
        if (amount <= 0) return
        val currentAccount = cashDao.getAccountDirect(accountType) ?: CashAccountEntity(
            accountType = accountType,
            accountName = com.example.data.entity.CashAccountDefaults.getAccountName(accountType),
            saldo = 0.0
        )
        val newSaldo = currentAccount.saldo + amount
        cashDao.insertOrUpdateAccount(currentAccount.copy(saldo = newSaldo, lastUpdated = System.currentTimeMillis()))
        cashDao.insertMutation(
            CashMutationEntity(
                tanggal = date.ifBlank { Formatters.getCurrentDateFormatted() },
                accountType = accountType,
                jenis = "MASUK",
                nominal = amount,
                saldoSesudah = newSaldo,
                kategori = category,
                keterangan = note
            )
        )
    }

    suspend fun recordCashOutDirect(accountType: String, amount: Double, category: String, note: String, date: String) {
        if (amount <= 0) return
        val currentAccount = cashDao.getAccountDirect(accountType) ?: CashAccountEntity(
            accountType = accountType,
            accountName = com.example.data.entity.CashAccountDefaults.getAccountName(accountType),
            saldo = 0.0
        )
        val newSaldo = currentAccount.saldo - amount
        cashDao.insertOrUpdateAccount(currentAccount.copy(saldo = newSaldo, lastUpdated = System.currentTimeMillis()))
        cashDao.insertMutation(
            CashMutationEntity(
                tanggal = date.ifBlank { Formatters.getCurrentDateFormatted() },
                accountType = accountType,
                jenis = "KELUAR",
                nominal = amount,
                saldoSesudah = newSaldo,
                kategori = category,
                keterangan = note
            )
        )
    }

    fun updateCashBalanceManual(accountType: String, newBalance: Double, note: String) {
        viewModelScope.launch {
            val currentAccount = cashDao.getAccountDirect(accountType) ?: CashAccountEntity(
                accountType = accountType,
                accountName = com.example.data.entity.CashAccountDefaults.getAccountName(accountType),
                saldo = 0.0
            )
            val diff = newBalance - currentAccount.saldo
            cashDao.insertOrUpdateAccount(currentAccount.copy(saldo = newBalance, lastUpdated = System.currentTimeMillis()))
            cashDao.insertMutation(
                CashMutationEntity(
                    tanggal = Formatters.getCurrentDateFormatted(),
                    accountType = accountType,
                    jenis = "PENYESUAIAN",
                    nominal = diff,
                    saldoSesudah = newBalance,
                    kategori = "Penyesuaian Saldo",
                    keterangan = note.ifBlank { "Penyesuaian Saldo Manual" }
                )
            )
        }
    }

    fun addIncome(accountType: String, amount: Double, category: String, note: String, date: String) {
        viewModelScope.launch {
            recordCashInDirect(
                accountType = accountType,
                amount = amount,
                category = category.ifBlank { "Kas Masuk / Pemasukan" },
                note = note,
                date = date
            )
        }
    }

    fun addExpense(accountType: String, amount: Double, category: String, note: String, date: String) {
        viewModelScope.launch {
            recordCashOutDirect(
                accountType = accountType,
                amount = amount,
                category = category.ifBlank { "Kas Keluar / Operasional" },
                note = note,
                date = date
            )
        }
    }

    fun deleteCashMutation(id: Long) {
        viewModelScope.launch {
            cashDao.deleteMutation(id)
        }
    }

    // --- Search Query State ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Dashboard States ---
    val totalItemTypes: StateFlow<Int> = itemDao.getTotalItemTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalStockCount: StateFlow<Int> = itemDao.getTotalStockCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalStockUtama: StateFlow<Int> = itemDao.getTotalStockUtama()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalStockCabang: StateFlow<Int> = itemDao.getTotalStockCabang()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _selectedStoreForSales = MutableStateFlow("Toko Utama")
    val selectedStoreForSales: StateFlow<String> = _selectedStoreForSales.asStateFlow()

    fun setSelectedStoreForSales(storeName: String) {
        _selectedStoreForSales.value = storeName
    }

    val todayRevenue: StateFlow<Double> = salesDao.getRevenueForDate(Formatters.getCurrentDateFormatted())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayProfit: StateFlow<Double> = salesDao.getProfitForDate(Formatters.getCurrentDateFormatted())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val weeklyRevenue: StateFlow<Double> = salesDao.getRevenueBetweenDates(Formatters.getSevenDaysAgoDate(), Formatters.getCurrentDateFormatted())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val weeklyProfit: StateFlow<Double> = salesDao.getProfitBetweenDates(Formatters.getSevenDaysAgoDate(), Formatters.getCurrentDateFormatted())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyRevenue: StateFlow<Double> = salesDao.getRevenueBetweenDates(Formatters.getStartOfMonthDate(), Formatters.getCurrentDateFormatted())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyProfit: StateFlow<Double> = salesDao.getProfitBetweenDates(Formatters.getStartOfMonthDate(), Formatters.getCurrentDateFormatted())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val yearlyRevenue: StateFlow<Double> = salesDao.getRevenueBetweenDates(Formatters.getStartOfYearDate(), Formatters.getCurrentDateFormatted())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val yearlyProfit: StateFlow<Double> = salesDao.getProfitBetweenDates(Formatters.getStartOfYearDate(), Formatters.getCurrentDateFormatted())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun addCustomCashAccount(accountType: String, accountName: String, initialBalance: Double) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val code = accountType.trim().uppercase().replace(" ", "_")
            val name = accountName.trim()
            val account = CashAccountEntity(
                accountType = code,
                accountName = name,
                saldo = initialBalance,
                lastUpdated = System.currentTimeMillis()
            )
            cashDao.insertOrUpdateAccount(account)
            if (initialBalance > 0) {
                cashDao.insertMutation(
                    CashMutationEntity(
                        tanggal = Formatters.getCurrentDateFormatted(),
                        accountType = code,
                        jenis = "MASUK",
                        nominal = initialBalance,
                        saldoSesudah = initialBalance,
                        kategori = "Saldo Awal",
                        keterangan = "Saldo Awal $name"
                    )
                )
            }
        }
    }

    fun deleteCashAccount(accountType: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            cashDao.deleteAccount(accountType)
        }
    }

    fun clearAllBankAccounts() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            cashDao.deleteAllBankAccounts()
        }
    }

    val lowStockItems: StateFlow<List<ItemEntity>> = itemDao.getLowStockItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalUnpaidReceivables: StateFlow<Double> = customerReceivableDao.getTotalUnpaidReceivables()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalUnpaidPayables: StateFlow<Double> = supplierPayableDao.getTotalUnpaidPayables()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // --- Barang List ---
    @OptIn(ExperimentalCoroutinesApi::class)
    val allItems: StateFlow<List<ItemEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                itemDao.getAllItems()
            } else {
                itemDao.searchItems(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addItem(
        kodeBarang: String,
        namaBarang: String,
        stok: Int,
        hargaModal: Double,
        keterangan: String,
        stokUtamaInput: Int? = null,
        stokCabangInput: Int? = null
    ) {
        viewModelScope.launch {
            val sUtama = stokUtamaInput ?: stok
            val sCabang = stokCabangInput ?: 0
            val totalS = sUtama + sCabang

            val newItem = ItemEntity(
                kodeBarang = kodeBarang.trim(),
                namaBarang = namaBarang.trim(),
                stok = totalS,
                stokTokoUtama = sUtama,
                stokTokoCabang = sCabang,
                hargaModal = hargaModal,
                keterangan = keterangan.trim()
            )
            val id = itemDao.insertItem(newItem)
            stockHistoryDao.insertHistory(
                StockHistoryEntity(
                    itemId = id,
                    kodeBarang = newItem.kodeBarang,
                    namaBarang = newItem.namaBarang,
                    jumlahPerubahan = totalS,
                    stokAwal = 0,
                    stokAkhir = totalS,
                    jenis = "Awal",
                    keterangan = "Barang baru ditambahkan (Utama: $sUtama, Cabang: $sCabang)",
                    namaToko = "Semua Toko"
                )
            )
        }
    }

    fun importItemsBulk(items: List<com.example.util.ImportedItemRaw>, onComplete: (addedCount: Int) -> Unit) {
        viewModelScope.launch {
            var addedCount = 0
            for (raw in items) {
                if (raw.namaBarang.isBlank() && raw.kodeBarang.isBlank()) continue

                // PERBAIKAN IMPORT EXCEL:
                // Simpan harga modal persis sesuai nilai yang dibaca dari file Excel (raw.hargaModal)
                // tanpa perkalian, pembagian, atau penambahan digit 0.
                val validatedPrice = raw.hargaModal

                val newItem = ItemEntity(
                    kodeBarang = raw.kodeBarang.trim(),
                    namaBarang = raw.namaBarang.trim(),
                    stok = raw.stok,
                    stokTokoUtama = raw.stok,
                    stokTokoCabang = 0,
                    hargaModal = validatedPrice,
                    keterangan = raw.keterangan.trim()
                )
                val id = itemDao.insertItem(newItem)
                stockHistoryDao.insertHistory(
                    StockHistoryEntity(
                        itemId = id,
                        kodeBarang = newItem.kodeBarang,
                        namaBarang = newItem.namaBarang,
                        jumlahPerubahan = newItem.stok,
                        stokAwal = 0,
                        stokAkhir = newItem.stok,
                        jenis = "Awal",
                        keterangan = "Import Bulk Excel",
                        namaToko = "Toko Utama"
                    )
                )
                addedCount++
            }
            onComplete(addedCount)
        }
    }

    fun updateItem(
        item: ItemEntity,
        newStok: Int,
        newHargaModal: Double,
        newKode: String,
        newNama: String,
        newKet: String,
        newStokUtama: Int? = null,
        newStokCabang: Int? = null
    ) {
        viewModelScope.launch {
            val oldItem = itemDao.getItemById(item.id) ?: return@launch
            val sUtama = newStokUtama ?: oldItem.actualStokUtama
            val sCabang = newStokCabang ?: oldItem.actualStokCabang
            val totalS = sUtama + sCabang
            val stokDiff = totalS - oldItem.totalStokCombined
            
            val updated = oldItem.copy(
                kodeBarang = newKode.trim(),
                namaBarang = newNama.trim(),
                stok = totalS,
                stokTokoUtama = sUtama,
                stokTokoCabang = sCabang,
                hargaModal = newHargaModal,
                keterangan = newKet.trim(),
                updatedAt = System.currentTimeMillis()
            )
            itemDao.updateItem(updated)

            if (stokDiff != 0) {
                stockHistoryDao.insertHistory(
                    StockHistoryEntity(
                        itemId = item.id,
                        kodeBarang = updated.kodeBarang,
                        namaBarang = updated.namaBarang,
                        jumlahPerubahan = stokDiff,
                        stokAwal = oldItem.totalStokCombined,
                        stokAkhir = totalS,
                        jenis = "Edit Stok",
                        keterangan = "Update Stok (Utama: $sUtama, Cabang: $sCabang)",
                        namaToko = "Semua Toko"
                    )
                )
            }
        }
    }

    fun transferStockBetweenStores(
        itemId: Long,
        fromStore: String,
        toStore: String,
        qty: Int,
        note: String = ""
    ) {
        if (qty <= 0 || fromStore == toStore) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val item = itemDao.getItemById(itemId) ?: return@launch
            val oldUtama = item.actualStokUtama
            val oldCabang = item.actualStokCabang

            var newUtama = oldUtama
            var newCabang = oldCabang

            val isFromUtama = fromStore == "Gudang" || fromStore == "Toko Utama" || fromStore.contains("Gudang", ignoreCase = true) || fromStore.contains("Utama", ignoreCase = true)
            val isToCabang = toStore == "Toko Cabang" || toStore == "Stok Toko" || toStore.contains("Cabang", ignoreCase = true) || toStore.contains("Toko", ignoreCase = true)

            val isFromCabang = fromStore == "Toko Cabang" || fromStore == "Stok Toko" || fromStore.contains("Cabang", ignoreCase = true)
            val isToUtama = toStore == "Gudang" || toStore == "Toko Utama" || toStore.contains("Gudang", ignoreCase = true) || toStore.contains("Utama", ignoreCase = true)

            if (isFromUtama && isToCabang) {
                val actualQty = qty.coerceAtMost(oldUtama)
                newUtama = oldUtama - actualQty
                newCabang = oldCabang + actualQty
            } else if (isFromCabang && isToUtama) {
                val actualQty = qty.coerceAtMost(oldCabang)
                newCabang = oldCabang - actualQty
                newUtama = oldUtama + actualQty
            } else {
                // Fallback default transfer from Utama to Cabang
                val actualQty = qty.coerceAtMost(oldUtama)
                newUtama = oldUtama - actualQty
                newCabang = oldCabang + actualQty
            }

            val totalS = newUtama + newCabang
            itemDao.updateStoreStocks(itemId, newUtama, newCabang, totalS)

            stockHistoryDao.insertHistory(
                StockHistoryEntity(
                    itemId = itemId,
                    kodeBarang = item.kodeBarang,
                    namaBarang = item.namaBarang,
                    jumlahPerubahan = qty,
                    stokAwal = if (isFromUtama) oldUtama else oldCabang,
                    stokAkhir = if (isToCabang) newCabang else newUtama,
                    jenis = "Transfer Toko",
                    keterangan = "Transfer $qty pcs dari $fromStore ke $toStore. ${note.trim()}".trimEnd(),
                    namaToko = "$fromStore -> $toStore"
                )
            )
        }
    }

    fun updateStoreStockDirect(
        itemId: Long,
        newStokUtama: Int,
        newStokCabang: Int,
        note: String = ""
    ) {
        viewModelScope.launch {
            val item = itemDao.getItemById(itemId) ?: return@launch
            val sUtama = newStokUtama.coerceAtLeast(0)
            val sCabang = newStokCabang.coerceAtLeast(0)
            val totalS = sUtama + sCabang
            val oldTotal = item.totalStokCombined

            itemDao.updateStoreStocks(itemId, sUtama, sCabang, totalS)

            stockHistoryDao.insertHistory(
                StockHistoryEntity(
                    itemId = itemId,
                    kodeBarang = item.kodeBarang,
                    namaBarang = item.namaBarang,
                    jumlahPerubahan = totalS - oldTotal,
                    stokAwal = oldTotal,
                    stokAkhir = totalS,
                    jenis = "Edit Stok",
                    keterangan = "Penyesuaian Stok Toko (Utama: $sUtama, Cabang: $sCabang). ${note.trim()}".trimEnd(),
                    namaToko = "Semua Toko"
                )
            )
        }
    }

    fun addStockToStore(
        itemId: Long,
        targetStore: String,
        qtyToAdd: Int,
        note: String = ""
    ) {
        if (qtyToAdd <= 0) return
        viewModelScope.launch {
            val item = itemDao.getItemById(itemId) ?: return@launch
            val oldUtama = item.actualStokUtama
            val oldCabang = item.actualStokCabang

            val newUtama = if (targetStore == "Toko Utama") oldUtama + qtyToAdd else oldUtama
            val newCabang = if (targetStore == "Toko Cabang") oldCabang + qtyToAdd else oldCabang
            val totalS = newUtama + newCabang

            itemDao.updateStoreStocks(itemId, newUtama, newCabang, totalS)

            stockHistoryDao.insertHistory(
                StockHistoryEntity(
                    itemId = itemId,
                    kodeBarang = item.kodeBarang,
                    namaBarang = item.namaBarang,
                    jumlahPerubahan = qtyToAdd,
                    stokAwal = if (targetStore == "Toko Utama") oldUtama else oldCabang,
                    stokAkhir = if (targetStore == "Toko Utama") newUtama else newCabang,
                    jenis = "Masuk Stok Toko",
                    keterangan = "Input stok $qtyToAdd pcs ke $targetStore. ${note.trim()}".trimEnd(),
                    namaToko = targetStore
                )
            )
        }
    }

    fun deleteItem(item: ItemEntity) {
        viewModelScope.launch {
            itemDao.deleteItem(item)
        }
    }

    fun getStockHistoryForItem(itemId: Long): Flow<List<StockHistoryEntity>> {
        return stockHistoryDao.getHistoryByItem(itemId)
    }

    val allStockHistory: StateFlow<List<StockHistoryEntity>> = stockHistoryDao.getAllHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Barang Masuk Transactions ---
    private val _incomingCart = MutableStateFlow<List<CartItemIncoming>>(emptyList())
    val incomingCart: StateFlow<List<CartItemIncoming>> = _incomingCart.asStateFlow()

    fun addIncomingCartItem(item: ItemEntity) {
        val current = _incomingCart.value.toMutableList()
        val index = current.indexOfFirst { it.item.id == item.id }
        if (index != -1) {
            val existing = current[index]
            current[index] = existing.copy(jumlahMasuk = existing.jumlahMasuk + 1)
        } else {
            current.add(CartItemIncoming(item = item, jumlahMasuk = 1, hargaModal = item.hargaModal))
        }
        _incomingCart.value = current
    }

    fun updateIncomingCartQuantity(itemId: Long, qty: Int) {
        val current = _incomingCart.value.toMutableList()
        val index = current.indexOfFirst { it.item.id == itemId }
        if (index != -1) {
            current[index] = current[index].copy(jumlahMasuk = qty.coerceAtLeast(0))
            _incomingCart.value = current
        }
    }

    fun updateIncomingCartCost(itemId: Long, cost: Double) {
        val current = _incomingCart.value.toMutableList()
        val index = current.indexOfFirst { it.item.id == itemId }
        if (index != -1) {
            current[index] = current[index].copy(hargaModal = cost.coerceAtLeast(0.0))
            _incomingCart.value = current
        }
    }

    fun removeIncomingCartItem(itemId: Long) {
        val current = _incomingCart.value.filterNot { it.item.id == itemId }
        _incomingCart.value = current
    }

    fun clearIncomingCart() {
        _incomingCart.value = emptyList()
    }

    fun processScannedIncomingReceipt(
        scannedItems: List<com.example.util.ScannedReceiptItem>,
        onComplete: (addedCount: Int) -> Unit
    ) {
        viewModelScope.launch {
            var addedCount = 0
            val currentCart = _incomingCart.value.toMutableList()
            val allDbItems = itemDao.getAllItemsList()

            for (scanned in scannedItems) {
                if (scanned.namaBarang.isBlank()) continue
                val existingInDb = allDbItems.find {
                    it.namaBarang.equals(scanned.namaBarang, ignoreCase = true)
                }

                val finalItem: ItemEntity = if (existingInDb != null) {
                    existingInDb
                } else {
                    val newItem = ItemEntity(
                        kodeBarang = "SKU-${System.currentTimeMillis().toString().takeLast(6)}",
                        namaBarang = scanned.namaBarang.trim(),
                        stok = 0,
                        hargaModal = scanned.hargaSatuan,
                        keterangan = "Hasil Scan Nota AI"
                    )
                    val id = itemDao.insertItem(newItem)
                    val inserted = newItem.copy(id = id)

                    stockHistoryDao.insertHistory(
                        StockHistoryEntity(
                            itemId = id,
                            kodeBarang = inserted.kodeBarang,
                            namaBarang = inserted.namaBarang,
                            jumlahPerubahan = 0,
                            stokAwal = 0,
                            stokAkhir = 0,
                            jenis = "Awal",
                            keterangan = "Dibuat dari Scan Nota AI"
                        )
                    )
                    inserted
                }

                val cartIndex = currentCart.indexOfFirst { it.item.id == finalItem.id }
                if (cartIndex != -1) {
                    val prev = currentCart[cartIndex]
                    currentCart[cartIndex] = prev.copy(
                        jumlahMasuk = prev.jumlahMasuk + scanned.jumlah,
                        hargaModal = if (scanned.hargaSatuan > 0) scanned.hargaSatuan else prev.hargaModal
                    )
                } else {
                    currentCart.add(
                        CartItemIncoming(
                            item = finalItem,
                            jumlahMasuk = scanned.jumlah,
                            hargaModal = scanned.hargaSatuan
                        )
                    )
                }
                addedCount++
            }

            _incomingCart.value = currentCart
            onComplete(addedCount)
        }
    }

    fun saveIncomingTransaction(
        tanggal: String,
        supplierName: String,
        fakturNumber: String,
        catatan: String,
        statusPembayaran: String,
        targetAccountCode: String = "BANK",
        onSuccess: () -> Unit
    ) {
        val rawCart = _incomingCart.value
        if (rawCart.isEmpty() || supplierName.isBlank()) return
        val cart = rawCart.map { if (it.jumlahMasuk <= 0) it.copy(jumlahMasuk = 1) else it }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val totalNilai = cart.sumOf { it.jumlahMasuk * it.hargaModal }
                val totalItem = cart.sumOf { it.jumlahMasuk }

                val transaction = IncomingTransactionEntity(
                    tanggal = tanggal,
                    namaSupplier = supplierName.trim(),
                    nomorFaktur = fakturNumber.trim(),
                    catatan = catatan.trim(),
                    totalNilai = totalNilai,
                    totalItem = totalItem,
                    statusPembayaran = statusPembayaran
                )

                val txId = incomingDao.insertTransaction(transaction)

                val incomingItemsList = cart.map {
                    IncomingItemEntity(
                        transactionId = txId,
                        itemId = it.item.id,
                        kodeBarang = it.item.kodeBarang,
                        namaBarang = it.item.namaBarang,
                        jumlahMasuk = it.jumlahMasuk,
                        hargaModal = it.hargaModal
                    )
                }
                incomingDao.insertItems(incomingItemsList)

                // Update item stock & history
                cart.forEach { cartItem ->
                    val currentItem = itemDao.getItemById(cartItem.item.id)
                    if (currentItem != null) {
                        val stokAwal = currentItem.stok
                        val stokAkhir = stokAwal + cartItem.jumlahMasuk
                        itemDao.updateStockAndPrice(
                            id = cartItem.item.id,
                            delta = cartItem.jumlahMasuk,
                            newHargaModal = cartItem.hargaModal
                        )
                        stockHistoryDao.insertHistory(
                            StockHistoryEntity(
                                itemId = cartItem.item.id,
                                kodeBarang = cartItem.item.kodeBarang,
                                namaBarang = cartItem.item.namaBarang,
                                jumlahPerubahan = cartItem.jumlahMasuk,
                                stokAwal = stokAwal,
                                stokAkhir = stokAkhir,
                                jenis = "Barang Masuk",
                                keterangan = "Faktur: ${fakturNumber.ifBlank { "Supplier $supplierName" }}"
                            )
                        )
                    }
                }

                // Handle Cash Deduction or Supplier Payable
                if (statusPembayaran == "Tunai") {
                    recordCashOutDirect(
                        accountType = "TUNAI",
                        amount = totalNilai,
                        category = "Pembelian Barang (Tunai)",
                        note = "Supplier: ${supplierName.trim()} | Faktur: ${fakturNumber.ifBlank { "-" }}",
                        date = tanggal
                    )
                } else if (statusPembayaran == "Transfer") {
                    val accType = if (targetAccountCode.isNotBlank() && targetAccountCode != "TUNAI") targetAccountCode else "BANK"
                    recordCashOutDirect(
                        accountType = accType,
                        amount = totalNilai,
                        category = "Pembelian Barang (Transfer)",
                        note = "Supplier: ${supplierName.trim()} | Faktur: ${fakturNumber.ifBlank { "-" }}",
                        date = tanggal
                    )
                } else if (statusPembayaran == "Hutang") {
                    val payable = SupplierPayableEntity(
                        namaSupplier = supplierName.trim(),
                        nominalAwal = totalNilai,
                        nominalSisa = totalNilai,
                        tanggal = tanggal,
                        catatan = "Faktur: ${fakturNumber.ifBlank { "-" }} | ${catatan.ifBlank { "Barang Masuk" }}",
                        status = "Belum Lunas",
                        incomingTransactionId = txId
                    )
                    supplierPayableDao.insertPayable(payable)
                }

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    clearIncomingCart()
                    onSuccess()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val allIncomingTransactions: StateFlow<List<IncomingTransactionEntity>> = incomingDao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Penjualan Harian Transactions ---
    private val _salesCart = MutableStateFlow<List<CartItemSales>>(emptyList())
    val salesCart: StateFlow<List<CartItemSales>> = _salesCart.asStateFlow()

    fun addSalesCartItem(item: ItemEntity) {
        val current = _salesCart.value.toMutableList()
        val index = current.indexOfFirst { it.item.id == item.id }
        if (index != -1) {
            val existing = current[index]
            val maxStock = maxOf(1, existing.item.stok)
            val newQty = (existing.jumlahTerjual + 1).coerceAtMost(maxStock)
            current[index] = existing.copy(jumlahTerjual = newQty)
        } else {
            current.add(CartItemSales(item = item, jumlahTerjual = 1, hargaSatuan = item.hargaModal))
        }
        _salesCart.value = current
    }

    fun updateSalesCartQuantity(itemId: Long, qty: Int) {
        val current = _salesCart.value.toMutableList()
        val index = current.indexOfFirst { it.item.id == itemId }
        if (index != -1) {
            val maxStock = current[index].item.stok
            current[index] = current[index].copy(jumlahTerjual = qty.coerceIn(0, maxOf(0, maxStock)))
            _salesCart.value = current
        }
    }

    fun updateSalesCartPrice(itemId: Long, price: Double) {
        val current = _salesCart.value.toMutableList()
        val index = current.indexOfFirst { it.item.id == itemId }
        if (index != -1) {
            current[index] = current[index].copy(hargaSatuan = price.coerceAtLeast(0.0))
            _salesCart.value = current
        }
    }

    fun removeSalesCartItem(itemId: Long) {
        val current = _salesCart.value.filterNot { it.item.id == itemId }
        _salesCart.value = current
    }

    fun clearSalesCart() {
        _salesCart.value = emptyList()
    }

    fun processScannedSalesReceipt(
        scannedItems: List<com.example.util.ScannedReceiptItem>,
        onComplete: (addedCount: Int) -> Unit
    ) {
        viewModelScope.launch {
            var addedCount = 0
            val currentCart = _salesCart.value.toMutableList()
            val allDbItems = itemDao.getAllItemsList()

            for (scanned in scannedItems) {
                if (scanned.namaBarang.isBlank()) continue

                val existingInDb = (if (scanned.matchedItemId != null) {
                    allDbItems.find { it.id == scanned.matchedItemId }
                } else null) ?: (if (!scanned.matchedKodeBarang.isNullOrBlank()) {
                    allDbItems.find { it.kodeBarang.equals(scanned.matchedKodeBarang, ignoreCase = true) }
                } else null) ?: com.example.util.ItemMatcher.findBestMatch(scanned.namaBarang, allDbItems)
                ?: allDbItems.find { it.namaBarang.equals(scanned.namaBarang, ignoreCase = true) }

                val finalItem: ItemEntity = if (existingInDb != null) {
                    existingInDb
                } else {
                    val newItem = ItemEntity(
                        kodeBarang = "SKU-${System.currentTimeMillis().toString().takeLast(6)}",
                        namaBarang = scanned.namaBarang.trim(),
                        stok = scanned.jumlah,
                        hargaModal = scanned.hargaSatuan,
                        keterangan = "Hasil Scan Nota Penjualan AI"
                    )
                    val id = itemDao.insertItem(newItem)
                    val inserted = newItem.copy(id = id)

                    stockHistoryDao.insertHistory(
                        StockHistoryEntity(
                            itemId = id,
                            kodeBarang = inserted.kodeBarang,
                            namaBarang = inserted.namaBarang,
                            jumlahPerubahan = scanned.jumlah,
                            stokAwal = 0,
                            stokAkhir = scanned.jumlah,
                            jenis = "Awal",
                            keterangan = "Dibuat dari Scan Nota Penjualan AI"
                        )
                    )
                    inserted
                }

                val cartIndex = currentCart.indexOfFirst { it.item.id == finalItem.id }
                if (cartIndex != -1) {
                    val prev = currentCart[cartIndex]
                    currentCart[cartIndex] = prev.copy(
                        jumlahTerjual = prev.jumlahTerjual + scanned.jumlah,
                        hargaSatuan = if (scanned.hargaSatuan > 0) scanned.hargaSatuan else prev.hargaSatuan
                    )
                } else {
                    currentCart.add(
                        CartItemSales(
                            item = finalItem,
                            jumlahTerjual = scanned.jumlah,
                            hargaSatuan = scanned.hargaSatuan
                        )
                    )
                }
                addedCount++
            }

            _salesCart.value = currentCart
            onComplete(addedCount)
        }
    }

    fun saveSalesTransaction(
        tanggal: String,
        totalMoneyInput: Double,
        catatan: String,
        isPiutang: Boolean = false,
        namaPelanggan: String = "",
        nomorHp: String = "",
        uangMuka: Double = 0.0,
        jatuhTempo: String = "",
        metodePembayaran: String = "Tunai", // "Tunai", "Transfer", "Piutang", "Transfer (Bank BCA)", etc.
        targetAccountCode: String = "TUNAI", // "TUNAI", "BCA", "MANDIRI", "BRI", "BNI", "BANK_LAIN", "GOPAY", "OVO", "DANA", "SHOPEEPAY", "LINKAJA"
        selectedStore: String = _selectedStoreForSales.value,
        onSuccess: () -> Unit
    ) {
        val rawCart = _salesCart.value
        if (rawCart.isEmpty()) return
        val cart = rawCart.map { if (it.jumlahTerjual <= 0) it.copy(jumlahTerjual = 1) else it }

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val calculatedTotal = cart.sumOf { it.jumlahTerjual * it.hargaSatuan }
                val finalMoney = if (totalMoneyInput > 0) totalMoneyInput else calculatedTotal
                val totalItemTerjual = cart.sumOf { it.jumlahTerjual }
                val totalModal = cart.sumOf { it.jumlahTerjual * it.item.hargaModal }
                val profit = (finalMoney - totalModal).coerceAtLeast(0.0)

                val methodLabel = if (isPiutang) "Piutang" else metodePembayaran
                val fullCatatan = if (isPiutang && namaPelanggan.isNotBlank()) {
                    "[$selectedStore] Pelanggan: ${namaPelanggan.trim()} | ${catatan.trim()}".trimEnd(' ', '|')
                } else {
                    if (catatan.isNotBlank()) "[$selectedStore] ${catatan.trim()}" else "Penjualan $selectedStore"
                }

                val transaction = SalesTransactionEntity(
                    tanggal = tanggal,
                    totalUangPenjualan = finalMoney,
                    totalModal = totalModal,
                    keuntungan = profit,
                    totalItemTerjual = totalItemTerjual,
                    catatan = fullCatatan,
                    metodePembayaran = methodLabel,
                    namaToko = selectedStore
                )

                val txId = salesDao.insertTransaction(transaction)

                val salesItemsList = cart.map {
                    SalesItemEntity(
                        transactionId = txId,
                        itemId = it.item.id,
                        kodeBarang = it.item.kodeBarang,
                        namaBarang = it.item.namaBarang,
                        jumlahTerjual = it.jumlahTerjual,
                        hargaSatuan = it.hargaSatuan,
                        totalHarga = it.jumlahTerjual * it.hargaSatuan
                    )
                }
                salesDao.insertItems(salesItemsList)

                // Auto-create Customer Receivable (Piutang Pelanggan / Toko) or record Cash In
                if (isPiutang && namaPelanggan.isNotBlank()) {
                    val sisaPiutang = (finalMoney - uangMuka).coerceAtLeast(0.0)
                    val statusPiutang = if (sisaPiutang <= 0) "Lunas" else "Belum Lunas"
                    val piutangEntity = CustomerReceivableEntity(
                        namaPelanggan = namaPelanggan.trim(),
                        nomorHp = nomorHp.trim(),
                        nominalAwal = finalMoney,
                        nominalSisa = sisaPiutang,
                        tanggal = tanggal,
                        jatuhTempo = jatuhTempo,
                        catatan = "Penjualan $selectedStore Nota #${txId} (${if (catatan.isNotBlank()) catatan.trim() else "Penjualan Rinci"})",
                        status = statusPiutang
                    )
                    val piutangId = customerReceivableDao.insertReceivable(piutangEntity)
                    if (uangMuka > 0) {
                        val payMethod = if (metodePembayaran.startsWith("Transfer")) "Transfer" else "Tunai"
                        val payment = CustomerPaymentEntity(
                            piutangId = piutangId,
                            nominalBayar = uangMuka,
                            tanggal = tanggal,
                            catatan = "DP Penjualan $selectedStore Nota #${txId}",
                            metodePembayaran = payMethod
                        )
                        customerReceivableDao.insertPayment(payment)

                        val targetAccount = if (metodePembayaran.startsWith("Transfer") || metodePembayaran == "Transfer") {
                            if (targetAccountCode.isNotBlank() && targetAccountCode != "TUNAI") targetAccountCode else "BANK"
                        } else "TUNAI"

                        recordCashInDirect(
                            accountType = targetAccount,
                            amount = uangMuka,
                            category = "DP Penjualan ($selectedStore)",
                            note = "DP Nota #${txId} - ${namaPelanggan.trim()}",
                            date = tanggal
                        )
                    }
                } else {
                    val targetAccount = if (metodePembayaran.startsWith("Transfer") || metodePembayaran == "Transfer") {
                        if (targetAccountCode.isNotBlank() && targetAccountCode != "TUNAI") targetAccountCode else "BANK"
                    } else "TUNAI"

                    val categoryLabel = "Penjualan $selectedStore ($metodePembayaran)"
                    recordCashInDirect(
                        accountType = targetAccount,
                        amount = finalMoney,
                        category = categoryLabel,
                        note = fullCatatan.ifBlank { "Penjualan $selectedStore Nota #${txId}" },
                        date = tanggal
                    )
                }

                // Deduct store stock & log stock history
                cart.forEach { cartItem ->
                    val currentItem = itemDao.getItemById(cartItem.item.id)
                    if (currentItem != null) {
                        val oldUtama = currentItem.actualStokUtama
                        val oldCabang = currentItem.actualStokCabang

                        val qtyDeducted = cartItem.jumlahTerjual
                        var newUtama = oldUtama
                        var newCabang = oldCabang

                        val isCabang = selectedStore == "Toko Cabang" || selectedStore == "Stok Toko" || selectedStore.contains("Cabang", ignoreCase = true)
                        if (isCabang) {
                            newCabang = (oldCabang - qtyDeducted).coerceAtLeast(0)
                        } else {
                            newUtama = (oldUtama - qtyDeducted).coerceAtLeast(0)
                        }
                        val totalS = newUtama + newCabang

                        itemDao.updateStoreStocks(
                            id = cartItem.item.id,
                            stokUtama = newUtama,
                            stokCabang = newCabang,
                            totalStok = totalS
                        )

                        stockHistoryDao.insertHistory(
                            StockHistoryEntity(
                                itemId = cartItem.item.id,
                                kodeBarang = cartItem.item.kodeBarang,
                                namaBarang = cartItem.item.namaBarang,
                                jumlahPerubahan = -qtyDeducted,
                                stokAwal = if (isCabang) oldCabang else oldUtama,
                                stokAkhir = if (isCabang) newCabang else newUtama,
                                jenis = "Penjualan Harian",
                                keterangan = "Penjualan $selectedStore Tgl: $tanggal ${if (namaPelanggan.isNotBlank()) "($namaPelanggan)" else ""}",
                                namaToko = selectedStore
                            )
                        )
                    }
                }

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    clearSalesCart()
                    onSuccess()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val allSalesTransactions: StateFlow<List<SalesTransactionEntity>> = salesDao.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Piutang Pelanggan ---
    val allReceivables: StateFlow<List<CustomerReceivableEntity>> = customerReceivableDao.getAllReceivables()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addReceivable(
        namaPelanggan: String,
        nomorHp: String,
        nominal: Double,
        tanggal: String,
        catatan: String,
        jatuhTempo: String = ""
    ) {
        if (namaPelanggan.isBlank() || nominal <= 0) return
        viewModelScope.launch {
            val entity = CustomerReceivableEntity(
                namaPelanggan = namaPelanggan.trim(),
                nomorHp = nomorHp.trim(),
                nominalAwal = nominal,
                nominalSisa = nominal,
                tanggal = tanggal,
                jatuhTempo = jatuhTempo,
                catatan = catatan.trim(),
                status = "Belum Lunas"
            )
            customerReceivableDao.insertReceivable(entity)
        }
    }

    fun addCustomerPayment(
        piutangId: Long,
        nominalBayar: Double,
        tanggal: String,
        catatan: String,
        metodePembayaran: String = "Tunai"
    ) {
        if (nominalBayar <= 0) return
        viewModelScope.launch {
            val receivable = customerReceivableDao.getReceivableById(piutangId) ?: return@launch
            val newSisa = (receivable.nominalSisa - nominalBayar).coerceAtLeast(0.0)
            val newStatus = if (newSisa <= 0) "Lunas" else "Belum Lunas"

            val updated = receivable.copy(
                nominalSisa = newSisa,
                status = newStatus
            )
            customerReceivableDao.updateReceivable(updated)

            val payment = CustomerPaymentEntity(
                piutangId = piutangId,
                nominalBayar = nominalBayar,
                tanggal = tanggal,
                catatan = catatan.trim(),
                metodePembayaran = metodePembayaran
            )
            customerReceivableDao.insertPayment(payment)

            val targetAccount = if (metodePembayaran == "Transfer") "BANK" else "TUNAI"
            recordCashInDirect(
                accountType = targetAccount,
                amount = nominalBayar,
                category = "Bayar Piutang (${if (metodePembayaran == "Transfer") "Transfer" else "Tunai"})",
                note = "Pelanggan: ${receivable.namaPelanggan} | ${catatan.ifBlank { "Pelunasan Piutang" }}",
                date = tanggal
            )
        }
    }

    fun markCustomerReceivableLunas(piutangId: Long, tanggal: String, metodePembayaran: String = "Tunai") {
        viewModelScope.launch {
            val receivable = customerReceivableDao.getReceivableById(piutangId) ?: return@launch
            if (receivable.nominalSisa > 0) {
                addCustomerPayment(piutangId, receivable.nominalSisa, tanggal, "Pelunasan Langsung", metodePembayaran)
            }
        }
    }

    fun getCustomerPayments(piutangId: Long): Flow<List<CustomerPaymentEntity>> {
        return customerReceivableDao.getPaymentsByReceivable(piutangId)
    }

    // --- Hutang Supplier ---
    val allPayables: StateFlow<List<SupplierPayableEntity>> = supplierPayableDao.getAllPayables()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSupplierPayment(
        hutangId: Long,
        nominalBayar: Double,
        tanggal: String,
        catatan: String,
        metodePembayaran: String = "Tunai"
    ) {
        if (nominalBayar <= 0) return
        viewModelScope.launch {
            val payable = supplierPayableDao.getPayableById(hutangId) ?: return@launch
            val newSisa = (payable.nominalSisa - nominalBayar).coerceAtLeast(0.0)
            val newStatus = if (newSisa <= 0) "Lunas" else "Belum Lunas"

            val updated = payable.copy(
                nominalSisa = newSisa,
                status = newStatus
            )
            supplierPayableDao.updatePayable(updated)

            val payment = SupplierPaymentEntity(
                hutangId = hutangId,
                nominalBayar = nominalBayar,
                tanggal = tanggal,
                catatan = catatan.trim(),
                metodePembayaran = metodePembayaran
            )
            supplierPayableDao.insertPayment(payment)

            val targetAccount = if (metodePembayaran == "Transfer") "BANK" else "TUNAI"
            recordCashOutDirect(
                accountType = targetAccount,
                amount = nominalBayar,
                category = "Bayar Hutang (${if (metodePembayaran == "Transfer") "Transfer" else "Tunai"})",
                note = "Supplier: ${payable.namaSupplier} | ${catatan.ifBlank { "Bayar Hutang" }}",
                date = tanggal
            )
        }
    }

    fun markSupplierPayableLunas(hutangId: Long, tanggal: String, metodePembayaran: String = "Tunai") {
        viewModelScope.launch {
            val payable = supplierPayableDao.getPayableById(hutangId) ?: return@launch
            if (payable.nominalSisa > 0) {
                addSupplierPayment(hutangId, payable.nominalSisa, tanggal, "Pelunasan Langsung", metodePembayaran)
            }
        }
    }

    fun getSupplierPayments(hutangId: Long): Flow<List<SupplierPaymentEntity>> {
        return supplierPayableDao.getPaymentsByPayable(hutangId)
    }

    // --- Rekap Laporan State ---
    private val _rekapStartDate = MutableStateFlow(Formatters.getCurrentDateFormatted())
    val rekapStartDate: StateFlow<String> = _rekapStartDate.asStateFlow()

    private val _rekapEndDate = MutableStateFlow(Formatters.getCurrentDateFormatted())
    val rekapEndDate: StateFlow<String> = _rekapEndDate.asStateFlow()

    fun setRekapDateRange(start: String, end: String) {
        _rekapStartDate.value = start
        _rekapEndDate.value = end
    }

    fun setRekapPreset(preset: String) { // "Harian", "Mingguan", "Bulanan", "Tahunan"
        val today = Formatters.getCurrentDateFormatted()
        when (preset) {
            "Harian" -> {
                _rekapStartDate.value = today
                _rekapEndDate.value = today
            }
            "Mingguan" -> {
                _rekapStartDate.value = Formatters.getSevenDaysAgoDate()
                _rekapEndDate.value = today
            }
            "Bulanan" -> {
                _rekapStartDate.value = Formatters.getStartOfMonthDate()
                _rekapEndDate.value = today
            }
            "Tahunan" -> {
                _rekapStartDate.value = Formatters.getStartOfYearDate()
                _rekapEndDate.value = today
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val rekapTransactions: StateFlow<List<SalesTransactionEntity>> = combine(_rekapStartDate, _rekapEndDate) { start, end ->
        Pair(start, end)
    }.flatMapLatest { (start, end) ->
        salesDao.getTransactionsBetweenDates(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val rekapSoldItems: StateFlow<List<SoldItemSummary>> = combine(_rekapStartDate, _rekapEndDate) { start, end ->
        Pair(start, end)
    }.flatMapLatest { (start, end) ->
        salesDao.getSoldItemsSummaryBetweenDates(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun getTransactionItems(transactionId: Long): List<SalesItemEntity> {
        return salesDao.getItemsForTransaction(transactionId)
    }

    suspend fun getIncomingTransactionItems(transactionId: Long): List<com.example.data.entity.IncomingItemEntity> {
        return incomingDao.getItemsForTransaction(transactionId)
    }

    // --- Backup, Restore & Reset Data ---
    fun exportFullBackupJson(onResult: (String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val json = com.example.util.AppBackupUtils.exportDatabaseToJson(db)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onResult(json)
            }
        }
    }

    fun restoreFromBackupJson(jsonString: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val result = com.example.util.AppBackupUtils.restoreDatabaseFromJson(db, jsonString)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                result.fold(
                    onSuccess = { msg -> onResult(true, msg) },
                    onFailure = { err -> onResult(false, err.message ?: "Gagal restore") }
                )
            }
        }
    }

    fun deleteAllItems(onSuccess: () -> Unit = {}) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            db.itemDao().deleteAllItems()
            db.stockHistoryDao().deleteAllHistory()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    fun clearCashHistoryAndBalances(onSuccess: () -> Unit = {}) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            cashDao.deleteAllCashAccounts()
            cashDao.deleteAllCashMutations()
            cashDao.insertOrUpdateAccount(
                CashAccountEntity(
                    accountType = "TUNAI",
                    accountName = "Kas Tunai Toko",
                    saldo = 0.0,
                    lastUpdated = System.currentTimeMillis()
                )
            )
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    fun resetTransactionsOnly(onSuccess: () -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            db.clearAllTables()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    fun resetAllAppData(onSuccess: () -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            db.itemDao().deleteAllItems()
            db.stockHistoryDao().deleteAllHistory()
            cashDao.deleteAllCashAccounts()
            cashDao.deleteAllCashMutations()
            db.clearAllTables()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onSuccess()
            }
        }
    }
}
