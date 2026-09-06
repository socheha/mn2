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

data class MonthlySalesData(
    val yearMonth: String,
    val monthLabel: String,
    val totalSales: Double,
    val totalProfit: Double
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
    private val syncQueueDao = db.syncQueueDao()
    private val transactionHistoryDao = db.transactionHistoryDao()

    val allTransactionLogs: StateFlow<List<com.example.data.entity.TransactionHistoryLogEntity>> = transactionHistoryDao.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Automatic startup recovery check & continuous snapshot
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val existingItems = itemDao.getAllItemsList()
            if (existingItems.isEmpty()) {
                // If tables are empty after an app update, attempt to auto-recover from local snapshot
                com.example.util.AppBackupUtils.restoreFromLatestAutoSnapshot(application, db)
            } else {
                // Keep continuous pre-update snapshot fresh
                com.example.util.AppBackupUtils.saveContinuousSnapshot(application, db)
            }

            // Automatic periodic auto backup check on app startup
            val autoBackupMsg = com.example.util.AutoBackupManager.checkAndPerformWeeklyAutoBackup(application, db)
            if (autoBackupMsg != null) {
                _lastAutoBackupTime.value = com.example.util.AutoBackupManager.getLastBackupTimestamp(application)
            }
        }

        // Proactively keep continuous snapshot synchronized on data changes (with throttle to prevent disk overload)
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var lastSnapshotTime = 0L
            itemDao.getAllItems().collect { items ->
                val now = System.currentTimeMillis()
                if (items.isNotEmpty() && (now - lastSnapshotTime > 10000L)) {
                    lastSnapshotTime = now
                    com.example.util.AppBackupUtils.saveContinuousSnapshot(application, db)
                }
            }
        }
    }

    fun persistSnapshotBackground() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                com.example.util.AppBackupUtils.saveContinuousSnapshot(getApplication(), db)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Theme & Appearance States ---
    private val _themeMode = MutableStateFlow(com.example.util.ThemePreferenceManager.getThemeMode(application))
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        com.example.util.ThemePreferenceManager.setThemeMode(getApplication(), mode)
    }

    // --- Security & PIN Lock States ---
    private val _isPinEnabled = MutableStateFlow(com.example.util.SecurityManager.isPinEnabled(application))
    val isPinEnabled: StateFlow<Boolean> = _isPinEnabled.asStateFlow()

    private val _isAppLocked = MutableStateFlow(
        if (com.example.util.SecurityManager.isPinEnabled(application)) {
            !com.example.util.SecurityManager.isSessionValid(application)
        } else {
            false
        }
    )
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _pinTimeoutMinutes = MutableStateFlow(com.example.util.SecurityManager.getPinTimeoutMinutes(application))
    val pinTimeoutMinutes: StateFlow<Int> = _pinTimeoutMinutes.asStateFlow()

    private val _securityQuestion = MutableStateFlow(com.example.util.SecurityManager.getSecurityQuestion(application))
    val securityQuestion: StateFlow<String> = _securityQuestion.asStateFlow()

    fun setPinTimeoutMinutes(minutes: Int) {
        com.example.util.SecurityManager.setPinTimeoutMinutes(getApplication(), minutes)
        _pinTimeoutMinutes.value = minutes
    }

    fun checkSessionLock() {
        if (_isPinEnabled.value) {
            if (!com.example.util.SecurityManager.isSessionValid(getApplication())) {
                _isAppLocked.value = true
            }
        } else {
            _isAppLocked.value = false
        }
    }

    fun lockApp() {
        if (_isPinEnabled.value) {
            com.example.util.SecurityManager.clearUnlockSession(getApplication())
            _isAppLocked.value = true
        }
    }

    fun unlockApp(pinInput: String): Boolean {
        val success = com.example.util.SecurityManager.verifyPin(getApplication(), pinInput)
        if (success) {
            com.example.util.SecurityManager.recordUnlock(getApplication())
            _isAppLocked.value = false
        }
        return success
    }

    fun setupPin(pin: String, question: String, answer: String): Boolean {
        val success = com.example.util.SecurityManager.setPin(getApplication(), pin, question, answer)
        if (success) {
            com.example.util.SecurityManager.recordUnlock(getApplication())
            _isPinEnabled.value = true
            _isAppLocked.value = false
            _securityQuestion.value = com.example.util.SecurityManager.getSecurityQuestion(getApplication())
        }
        return success
    }

    fun disablePin(pinInput: String): Boolean {
        val success = com.example.util.SecurityManager.disablePin(getApplication(), pinInput)
        if (success) {
            com.example.util.SecurityManager.clearUnlockSession(getApplication())
            _isPinEnabled.value = false
            _isAppLocked.value = false
        }
        return success
    }

    fun resetPinWithAnswer(answerInput: String, newPin: String): Boolean {
        val success = com.example.util.SecurityManager.resetPinWithAnswer(getApplication(), answerInput, newPin)
        if (success) {
            com.example.util.SecurityManager.recordUnlock(getApplication())
            _isPinEnabled.value = true
            _isAppLocked.value = false
        }
        return success
    }

    fun verifyPinForAction(pinInput: String): Boolean {
        return com.example.util.SecurityManager.verifyPin(getApplication(), pinInput)
    }

    // --- Storage Location Preference States ---
    private val _storageLocationType = MutableStateFlow(com.example.util.StorageLocationManager.getStorageType(application))
    val storageLocationType: StateFlow<String> = _storageLocationType.asStateFlow()

    private val _customStorageFolderName = MutableStateFlow(com.example.util.StorageLocationManager.getCustomFolderName(application))
    val customStorageFolderName: StateFlow<String> = _customStorageFolderName.asStateFlow()

    private val _customStorageTreeUri = MutableStateFlow(com.example.util.StorageLocationManager.getCustomTreeUri(application))
    val customStorageTreeUri: StateFlow<String?> = _customStorageTreeUri.asStateFlow()

    private val _isDualCopyDownloads = MutableStateFlow(com.example.util.StorageLocationManager.isDualCopyDownloads(application))
    val isDualCopyDownloads: StateFlow<Boolean> = _isDualCopyDownloads.asStateFlow()

    private val _isAlwaysPromptSaveAs = MutableStateFlow(com.example.util.StorageLocationManager.isAlwaysPromptSaveAs(application))
    val isAlwaysPromptSaveAs: StateFlow<Boolean> = _isAlwaysPromptSaveAs.asStateFlow()

    private val _freeStorageSpace = MutableStateFlow(com.example.util.StorageLocationManager.getFreeSpaceFormatted(application))
    val freeStorageSpace: StateFlow<String> = _freeStorageSpace.asStateFlow()

    fun setStorageLocationType(type: String) {
        _storageLocationType.value = type
        com.example.util.StorageLocationManager.setStorageType(getApplication(), type)
        refreshStorageState()
    }

    fun setCustomStorageFolder(uri: android.net.Uri?, folderName: String?) {
        val uriStr = uri?.toString()
        val name = folderName ?: if (uri != null) com.example.util.StorageLocationManager.getFolderNameFromUri(getApplication(), uri) else null
        com.example.util.StorageLocationManager.setCustomFolder(getApplication(), uriStr, name)
        if (uri != null) {
            _storageLocationType.value = com.example.util.StorageLocationManager.STORAGE_CUSTOM_SAF
            com.example.util.StorageLocationManager.setStorageType(getApplication(), com.example.util.StorageLocationManager.STORAGE_CUSTOM_SAF)
        }
        refreshStorageState()
    }

    fun setDualCopyDownloads(enabled: Boolean) {
        _isDualCopyDownloads.value = enabled
        com.example.util.StorageLocationManager.setDualCopyDownloads(getApplication(), enabled)
    }

    fun setAlwaysPromptSaveAs(enabled: Boolean) {
        _isAlwaysPromptSaveAs.value = enabled
        com.example.util.StorageLocationManager.setAlwaysPromptSaveAs(getApplication(), enabled)
    }

    fun refreshStorageState() {
        _storageLocationType.value = com.example.util.StorageLocationManager.getStorageType(getApplication())
        _customStorageFolderName.value = com.example.util.StorageLocationManager.getCustomFolderName(getApplication())
        _customStorageTreeUri.value = com.example.util.StorageLocationManager.getCustomTreeUri(getApplication())
        _isDualCopyDownloads.value = com.example.util.StorageLocationManager.isDualCopyDownloads(getApplication())
        _isAlwaysPromptSaveAs.value = com.example.util.StorageLocationManager.isAlwaysPromptSaveAs(getApplication())
        _freeStorageSpace.value = com.example.util.StorageLocationManager.getFreeSpaceFormatted(getApplication())
    }

    fun testStorageLocation(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val result = com.example.util.StorageLocationManager.testWriteToStorageLocation(getApplication())
            refreshStorageState()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onResult(result.first, result.second)
            }
        }
    }

    // --- 24-Hour Midnight Auto Backup States ---
    private val _isAutoBackupEnabled = MutableStateFlow(com.example.util.AutoBackupManager.isAutoBackupEnabled(application))
    val isAutoBackupEnabled: StateFlow<Boolean> = _isAutoBackupEnabled.asStateFlow()

    private val _autoBackupFrequency = MutableStateFlow(com.example.util.AutoBackupManager.getAutoBackupFrequency(application))
    val autoBackupFrequency: StateFlow<String> = _autoBackupFrequency.asStateFlow()

    private val _autoBackupHour = MutableStateFlow(com.example.util.AutoBackupManager.getAutoBackupHour(application))
    val autoBackupHour: StateFlow<Int> = _autoBackupHour.asStateFlow()

    private val _lastAutoBackupTime = MutableStateFlow(com.example.util.AutoBackupManager.getLastBackupTimestamp(application))
    val lastAutoBackupTime: StateFlow<Long> = _lastAutoBackupTime.asStateFlow()

    private val _lastAutoBackupStatus = MutableStateFlow(com.example.util.AutoBackupManager.getLastBackupStatus(application))
    val lastAutoBackupStatus: StateFlow<String> = _lastAutoBackupStatus.asStateFlow()

    private val _nextScheduledBackupTime = MutableStateFlow(com.example.util.AutoBackupManager.getNextScheduledBackupFormatted(application))
    val nextScheduledBackupTime: StateFlow<String> = _nextScheduledBackupTime.asStateFlow()

    fun setAutoBackupEnabled(enabled: Boolean) {
        _isAutoBackupEnabled.value = enabled
        com.example.util.AutoBackupManager.setAutoBackupEnabled(getApplication(), enabled)
        refreshAutoBackupState()
    }

    fun setAutoBackupFrequency(freq: String) {
        _autoBackupFrequency.value = freq
        com.example.util.AutoBackupManager.setAutoBackupFrequency(getApplication(), freq)
        refreshAutoBackupState()
    }

    fun setAutoBackupHour(hour: Int) {
        _autoBackupHour.value = hour
        com.example.util.AutoBackupManager.setAutoBackupHour(getApplication(), hour)
        refreshAutoBackupState()
    }

    fun refreshAutoBackupState() {
        _lastAutoBackupTime.value = com.example.util.AutoBackupManager.getLastBackupTimestamp(getApplication())
        _lastAutoBackupStatus.value = com.example.util.AutoBackupManager.getLastBackupStatus(getApplication())
        _nextScheduledBackupTime.value = com.example.util.AutoBackupManager.getNextScheduledBackupFormatted(getApplication())
    }

    fun runAutoBackupNow(onResult: (String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val msg = com.example.util.AutoBackupManager.performAutoBackup(getApplication(), db)
                _lastAutoBackupTime.value = com.example.util.AutoBackupManager.getLastBackupTimestamp(getApplication())
                _lastAutoBackupStatus.value = com.example.util.AutoBackupManager.getLastBackupStatus(getApplication())
                _nextScheduledBackupTime.value = com.example.util.AutoBackupManager.getNextScheduledBackupFormatted(getApplication())
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(msg)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult("Gagal melakukan auto backup: ${e.localizedMessage ?: "Terjadi kesalahan sistem"}")
                }
            }
        }
    }

    fun getLocalAutoBackupFiles(): List<java.io.File> {
        return com.example.util.AutoBackupManager.getLocalAutoBackupFiles(getApplication())
    }

    // --- Sync Queue & Offline Mode States ---
    val allSyncQueue: StateFlow<List<com.example.data.entity.SyncQueueEntity>> = syncQueueDao.getAllSyncQueue()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSyncCount: StateFlow<Int> = syncQueueDao.getPendingCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isOnlineStatus: StateFlow<Boolean> = com.example.util.SyncEngine.isOnline
    val isSyncing: StateFlow<Boolean> = com.example.util.SyncEngine.isSyncing

    fun setOnlineMode(online: Boolean) {
        com.example.util.SyncEngine.setOnlineStatus(online)
        if (online) {
            triggerSyncNow { _, _ -> }
        }
    }

    fun triggerSyncNow(onResult: (Int, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val (count, msg) = com.example.util.SyncEngine.processPendingQueue(syncQueueDao)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onResult(count, msg)
            }
        }
    }

    fun clearSyncedQueue() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            syncQueueDao.clearSyncedQueue()
        }
    }

    fun enqueueSyncRecord(type: String, summary: String, payloadJson: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            com.example.util.SyncEngine.enqueueTransaction(syncQueueDao, type, summary, payloadJson)
            if (isOnlineStatus.value) {
                com.example.util.SyncEngine.processPendingQueue(syncQueueDao)
            }
        }
    }

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

    suspend fun recordCashInDirect(
        accountType: String,
        amount: Double,
        category: String,
        note: String,
        date: String,
        accountName: String = ""
    ) {
        if (amount <= 0) return
        val currentAccount = cashDao.getAccountDirect(accountType) ?: CashAccountEntity(
            accountType = accountType,
            accountName = if (accountName.isNotBlank()) accountName else com.example.data.entity.CashAccountDefaults.getAccountName(accountType),
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

    suspend fun recordCashOutDirect(
        accountType: String,
        amount: Double,
        category: String,
        note: String,
        date: String,
        accountName: String = ""
    ) {
        if (amount <= 0) return
        val currentAccount = cashDao.getAccountDirect(accountType) ?: CashAccountEntity(
            accountType = accountType,
            accountName = if (accountName.isNotBlank()) accountName else com.example.data.entity.CashAccountDefaults.getAccountName(accountType),
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

    fun updateCashBalanceManual(accountType: String, newBalance: Double, note: String, accountName: String = "") {
        viewModelScope.launch {
            val currentAccount = cashDao.getAccountDirect(accountType) ?: CashAccountEntity(
                accountType = accountType,
                accountName = if (accountName.isNotBlank()) accountName else com.example.data.entity.CashAccountDefaults.getAccountName(accountType),
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

    fun addIncome(accountType: String, amount: Double, category: String, note: String, date: String, accountName: String = "") {
        viewModelScope.launch {
            recordCashInDirect(
                accountType = accountType,
                amount = amount,
                category = category.ifBlank { "Kas Masuk / Pemasukan" },
                note = note,
                date = date,
                accountName = accountName
            )
        }
    }

    fun addExpense(accountType: String, amount: Double, category: String, note: String, date: String, accountName: String = "") {
        viewModelScope.launch {
            recordCashOutDirect(
                accountType = accountType,
                amount = amount,
                category = category.ifBlank { "Kas Keluar / Operasional" },
                note = note,
                date = date,
                accountName = accountName
            )
        }
    }

    fun transferCash(fromAccount: String, toAccount: String, amount: Double, note: String, date: String) {
        if (amount <= 0 || fromAccount == toAccount) return
        viewModelScope.launch {
            val fromAcc = cashDao.getAccountDirect(fromAccount)
            val toAcc = cashDao.getAccountDirect(toAccount)
            val fromName = fromAcc?.accountName ?: com.example.data.entity.CashAccountDefaults.getAccountName(fromAccount)
            val toName = toAcc?.accountName ?: com.example.data.entity.CashAccountDefaults.getAccountName(toAccount)

            val noteOut = if (note.isBlank()) "Transfer ke $toName" else "Transfer ke $toName: $note"
            val noteIn = if (note.isBlank()) "Transfer dari $fromName" else "Transfer dari $fromName: $note"
            val dateFormatted = date.ifBlank { Formatters.getCurrentDateFormatted() }

            recordCashOutDirect(
                accountType = fromAccount,
                amount = amount,
                category = "Transfer Antar Kas",
                note = noteOut,
                date = dateFormatted,
                accountName = fromName
            )
            recordCashInDirect(
                accountType = toAccount,
                amount = amount,
                category = "Transfer Antar Kas",
                note = noteIn,
                date = dateFormatted,
                accountName = toName
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

    val todayTransactionCount: StateFlow<Int> = salesDao.getTransactionCountForDate(Formatters.getCurrentDateFormatted())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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

    fun clearAllBankAccounts(onSuccess: () -> Unit = {}) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            cashDao.deleteAllBankAccounts()
            cashDao.deleteAllBankAndTransferMutations()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    fun clearTunaiHistory(resetBalance: Boolean = false, onSuccess: () -> Unit = {}) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            cashDao.deleteAllTunaiMutations()
            if (resetBalance) {
                val acc = cashDao.getAccountDirect("TUNAI")
                if (acc != null) {
                    cashDao.insertOrUpdateAccount(acc.copy(saldo = 0.0, lastUpdated = System.currentTimeMillis()))
                }
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    fun clearBankAndTransferHistory(resetBalances: Boolean = false, onSuccess: () -> Unit = {}) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            cashDao.deleteAllBankAndTransferMutations()
            if (resetBalances) {
                val allAccs = cashDao.getAllAccountsList()
                allAccs.filter { it.accountType != "TUNAI" }.forEach { acc ->
                    cashDao.insertOrUpdateAccount(acc.copy(saldo = 0.0, lastUpdated = System.currentTimeMillis()))
                }
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    fun clearTransferOnlyHistory(onSuccess: () -> Unit = {}) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            cashDao.deleteAllTransferOnlyMutations()
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    fun clearHistoryByAccount(accountType: String, resetBalance: Boolean = false, onSuccess: () -> Unit = {}) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (accountType == "ALL") {
                cashDao.deleteAllCashMutations()
                if (resetBalance) {
                    val allAccs = cashDao.getAllAccountsList()
                    allAccs.forEach { acc ->
                        cashDao.insertOrUpdateAccount(acc.copy(saldo = 0.0, lastUpdated = System.currentTimeMillis()))
                    }
                }
            } else if (accountType == "TUNAI") {
                cashDao.deleteAllTunaiMutations()
                if (resetBalance) {
                    val acc = cashDao.getAccountDirect("TUNAI")
                    if (acc != null) {
                        cashDao.insertOrUpdateAccount(acc.copy(saldo = 0.0, lastUpdated = System.currentTimeMillis()))
                    }
                }
            } else {
                cashDao.deleteMutationsByAccount(accountType)
                if (resetBalance) {
                    val acc = cashDao.getAccountDirect(accountType)
                    if (acc != null) {
                        cashDao.insertOrUpdateAccount(acc.copy(saldo = 0.0, lastUpdated = System.currentTimeMillis()))
                    }
                }
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    fun deleteCashMutationDirect(id: Long, revertBalance: Boolean = false, onSuccess: () -> Unit = {}) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (revertBalance) {
                val mutation = cashDao.getMutationById(id)
                if (mutation != null) {
                    val currentAccount = cashDao.getAccountDirect(mutation.accountType)
                    if (currentAccount != null) {
                        val newSaldo = if (mutation.jenis == "MASUK") {
                            currentAccount.saldo - mutation.nominal
                        } else if (mutation.jenis == "KELUAR") {
                            currentAccount.saldo + mutation.nominal
                        } else {
                            currentAccount.saldo
                        }
                        cashDao.insertOrUpdateAccount(currentAccount.copy(saldo = newSaldo, lastUpdated = System.currentTimeMillis()))
                    }
                }
            }
            cashDao.deleteMutation(id)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    val lowStockItems: StateFlow<List<ItemEntity>> = itemDao.getLowStockItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val criticalLowStockItems: StateFlow<List<ItemEntity>> = itemDao.getAllItems().map { list ->
        list.filter { it.stok < 5 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlySalesTrends: StateFlow<List<MonthlySalesData>> = salesDao.getAllTransactions().map { transactions ->
        val sdfYM = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
        val sdfLabel = java.text.SimpleDateFormat("MMM yy", java.util.Locale("id", "ID"))

        val monthKeys = mutableListOf<String>()
        val monthLabels = mutableMapOf<String, String>()
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.MONTH, -5)
        for (i in 0..5) {
            val ym = sdfYM.format(cal.time)
            monthKeys.add(ym)
            monthLabels[ym] = sdfLabel.format(cal.time)
            cal.add(java.util.Calendar.MONTH, 1)
        }

        val grouped = transactions.groupBy {
            if (it.tanggal.length >= 7) it.tanggal.substring(0, 7) else ""
        }

        monthKeys.map { ym ->
            val txs = grouped[ym] ?: emptyList()
            MonthlySalesData(
                yearMonth = ym,
                monthLabel = monthLabels[ym] ?: ym,
                totalSales = txs.sumOf { it.totalUangPenjualan },
                totalProfit = txs.sumOf { it.keuntungan }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalUnpaidReceivables: StateFlow<Double> = customerReceivableDao.getTotalUnpaidReceivables()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalUnpaidPayables: StateFlow<Double> = supplierPayableDao.getTotalUnpaidPayables()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // --- Barang List ---
    val allItems: StateFlow<List<ItemEntity>> = itemDao.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addItem(
        kodeBarang: String,
        namaBarang: String,
        stok: Int,
        hargaModal: Double,
        keterangan: String,
        stokUtamaInput: Int? = null,
        stokCabangInput: Int? = null,
        onItemCreated: ((ItemEntity) -> Unit)? = null
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
            val savedItem = newItem.copy(id = id)
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
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onItemCreated?.invoke(savedItem)
            }
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
            val (sUtama, sCabang) = if (newStokUtama != null || newStokCabang != null) {
                (newStokUtama ?: oldItem.actualStokUtama) to (newStokCabang ?: oldItem.actualStokCabang)
            } else if (oldItem.stokTokoCabang > 0) {
                if (newStok >= oldItem.stokTokoCabang) {
                    (newStok - oldItem.stokTokoCabang) to oldItem.stokTokoCabang
                } else {
                    0 to newStok
                }
            } else {
                newStok to 0
            }
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

    fun processReturnItemStock(
        item: ItemEntity,
        qtyReturn: Int,
        alasan: String,
        lokasiStok: String = "Gudang",
        refundAmount: Double = 0.0,
        targetAccountCode: String? = null,
        onSuccess: (String) -> Unit = {}
    ) {
        if (qtyReturn <= 0) return
        viewModelScope.launch {
            val oldItem = itemDao.getItemById(item.id) ?: return@launch
            val oldUtama = oldItem.actualStokUtama
            val oldCabang = oldItem.actualStokCabang

            var newUtama = oldUtama
            var newCabang = oldCabang

            if (lokasiStok.contains("Cabang", ignoreCase = true)) {
                val actualQty = qtyReturn.coerceAtMost(oldCabang)
                newCabang = oldCabang - actualQty
            } else {
                val actualQty = qtyReturn.coerceAtMost(oldUtama)
                newUtama = oldUtama - actualQty
            }

            val totalS = (newUtama + newCabang).coerceAtLeast(0)
            val updated = oldItem.copy(
                stok = totalS,
                stokTokoUtama = newUtama,
                stokTokoCabang = newCabang,
                updatedAt = System.currentTimeMillis()
            )
            itemDao.updateItem(updated)

            stockHistoryDao.insertHistory(
                StockHistoryEntity(
                    itemId = item.id,
                    kodeBarang = updated.kodeBarang,
                    namaBarang = updated.namaBarang,
                    jumlahPerubahan = -qtyReturn,
                    stokAwal = oldItem.totalStokCombined,
                    stokAkhir = totalS,
                    jenis = "Return Stok",
                    keterangan = "Return/Retur $qtyReturn unit (${alasan.ifBlank { "Retur Stok" }})",
                    namaToko = lokasiStok
                )
            )

            var extraInfo = ""
            if (refundAmount > 0 && !targetAccountCode.isNullOrBlank()) {
                val targetAccount = cashDao.getAccountDirect(targetAccountCode)
                val accountName = targetAccount?.accountName ?: com.example.data.entity.CashAccountDefaults.getAccountName(targetAccountCode)
                recordCashInDirect(
                    accountType = targetAccountCode,
                    amount = refundAmount,
                    category = "Pengembalian Dana Retur Barang",
                    note = "Retur $qtyReturn unit ${updated.namaBarang}: ${alasan.ifBlank { "Pengembalian Dana" }}",
                    date = Formatters.getCurrentDateFormatted(),
                    accountName = accountName
                )
                extraInfo = " & Dana ${Formatters.formatRupiah(refundAmount)} dicatat ke $accountName"
            }

            onSuccess("Return $qtyReturn unit '${updated.namaBarang}' berhasil diproses$extraInfo.")
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
                    jenis = if (isFromUtama) "Transfer: Gudang ➔ Toko" else "Transfer: Toko ➔ Gudang",
                    keterangan = "Transfer $qty unit dari $fromStore ke $toStore${if (note.isNotBlank()) " • $note" else ""}".trim(),
                    namaToko = "$fromStore ➔ $toStore"
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

    fun addIncomingCartItem(item: ItemEntity, qty: Int = 1, customCost: Double? = null) {
        val current = _incomingCart.value.toMutableList()
        val index = current.indexOfFirst { it.item.id == item.id }
        val addQty = qty.coerceAtLeast(1)
        val finalCost = if (customCost != null && customCost > 0.0) customCost else item.hargaModal
        if (index != -1) {
            val existing = current[index]
            current[index] = existing.copy(
                jumlahMasuk = existing.jumlahMasuk + addQty,
                hargaModal = if (customCost != null && customCost > 0.0) customCost else existing.hargaModal
            )
        } else {
            current.add(CartItemIncoming(item = item, jumlahMasuk = addQty, hargaModal = finalCost))
        }
        _incomingCart.value = current
    }

    fun updateItemPriceDirect(itemId: Long, newPrice: Double, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val item = itemDao.getItemById(itemId)
            if (item != null && newPrice > 0.0) {
                itemDao.updateItemPrice(itemId, newPrice)
                itemDao.updateItem(item.copy(hargaModal = newPrice, updatedAt = System.currentTimeMillis()))
                val currentCart = _incomingCart.value.map {
                    if (it.item.id == itemId) it.copy(item = it.item.copy(hargaModal = newPrice)) else it
                }
                _incomingCart.value = currentCart
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onSuccess?.invoke()
                }
            }
        }
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

    fun applyAverageCostForIncomingItem(itemId: Long) {
        val current = _incomingCart.value.toMutableList()
        val index = current.indexOfFirst { it.item.id == itemId }
        if (index != -1) {
            val cartItem = current[index]
            val stokAda = cartItem.item.totalStokCombined.coerceAtLeast(0)
            val masukQty = cartItem.jumlahMasuk.coerceAtLeast(1)
            val hargaLama = cartItem.item.hargaModal
            val hargaMasuk = cartItem.hargaModal
            if (stokAda > 0 && hargaLama > 0.0 && hargaMasuk > 0.0) {
                val weightedAvg = ((stokAda * hargaLama) + (masukQty * hargaMasuk)) / (stokAda + masukQty)
                current[index] = cartItem.copy(hargaModal = weightedAvg)
                _incomingCart.value = current
            }
        }
    }

    fun clearIncomingCart() {
        _incomingCart.value = emptyList()
    }

    suspend fun recordTransactionLogDirect(
        type: String,
        txId: Long,
        ref: String,
        action: String,
        prevStatus: String,
        newStatus: String,
        nominal: Double,
        accountType: String,
        balBefore: Double,
        balAfter: Double,
        note: String,
        syncStatus: String = "SYNCED",
        date: String = Formatters.getCurrentDateFormatted()
    ) {
        try {
            val log = com.example.data.entity.TransactionHistoryLogEntity(
                tanggal = date,
                transactionType = type,
                transactionId = txId,
                referenceNumber = ref,
                actionType = action,
                previousStatus = prevStatus,
                newStatus = newStatus,
                nominal = nominal,
                accountType = accountType,
                balanceBefore = balBefore,
                balanceAfter = balAfter,
                keterangan = note,
                syncStatus = syncStatus
            )
            transactionHistoryDao.insertLog(log)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
                val existingInDb = if (scanned.matchedItemId != null) {
                    allDbItems.find { it.id == scanned.matchedItemId }
                } else {
                    allDbItems.find {
                        it.namaBarang.equals(scanned.namaBarang, ignoreCase = true) ||
                                (it.kodeBarang.isNotBlank() && it.kodeBarang.equals(scanned.namaBarang, ignoreCase = true))
                    } ?: com.example.util.ItemMatcher.findBestMatch(scanned.namaBarang, allDbItems)
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
        nominalTunaiSplit: Double = 0.0,
        nominalTransferSplit: Double = 0.0,
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

                enqueueSyncRecord(
                    type = "INCOMING",
                    summary = "Barang Masuk $supplierName - ${Formatters.formatRupiah(totalNilai)}",
                    payloadJson = "{\"txId\":$txId,\"supplier\":\"$supplierName\",\"faktur\":\"$fakturNumber\",\"total\":$totalNilai}"
                )

                // Update item stock & history
                cart.forEach { cartItem ->
                    val currentItem = itemDao.getItemById(cartItem.item.id)
                    if (currentItem != null) {
                        val stokAwal = currentItem.totalStokCombined
                        val newUtama = currentItem.actualStokUtama + cartItem.jumlahMasuk
                        val newCabang = currentItem.actualStokCabang
                        val totalS = newUtama + newCabang
                        // Calculate weighted average cost if incoming price differs from master data
                        val stokAda = maxOf(0, stokAwal)
                        val masukQty = maxOf(1, cartItem.jumlahMasuk)
                        val totalGabungan = stokAda + masukQty
                        val finalHargaModal = if (stokAda > 0 && currentItem.hargaModal > 0.0 && cartItem.hargaModal > 0.0 && cartItem.hargaModal != currentItem.hargaModal) {
                            kotlin.math.round(((stokAda * currentItem.hargaModal) + (masukQty * cartItem.hargaModal)) / totalGabungan)
                        } else if (cartItem.hargaModal > 0.0) {
                            cartItem.hargaModal
                        } else {
                            currentItem.hargaModal
                        }

                        itemDao.updateStoreStocksAndPrice(
                            id = currentItem.id,
                            stokUtama = newUtama,
                            stokCabang = newCabang,
                            totalStok = totalS,
                            hargaModal = finalHargaModal
                        )

                        itemDao.updateItem(
                            currentItem.copy(
                                stokTokoUtama = newUtama,
                                stokTokoCabang = newCabang,
                                stok = totalS,
                                hargaModal = finalHargaModal,
                                updatedAt = System.currentTimeMillis()
                            )
                        )

                        val priceChangeNote = if (stokAda > 0 && currentItem.hargaModal > 0.0 && cartItem.hargaModal > 0.0 && cartItem.hargaModal != currentItem.hargaModal) {
                            " (HPP Rata-rata: Rp ${finalHargaModal.toInt()} [Stok Lama: $stokAda @ Rp ${currentItem.hargaModal.toInt()} + Masuk: $masukQty @ Rp ${cartItem.hargaModal.toInt()}])"
                        } else if (currentItem.hargaModal > 0.0 && cartItem.hargaModal > 0.0 && cartItem.hargaModal != currentItem.hargaModal) {
                            " (Harga Modal baru: Rp ${cartItem.hargaModal.toInt()} dari Rp ${currentItem.hargaModal.toInt()})"
                        } else ""

                        stockHistoryDao.insertHistory(
                            StockHistoryEntity(
                                itemId = cartItem.item.id,
                                kodeBarang = cartItem.item.kodeBarang,
                                namaBarang = cartItem.item.namaBarang,
                                jumlahPerubahan = cartItem.jumlahMasuk,
                                stokAwal = stokAwal,
                                stokAkhir = totalS,
                                jenis = "Barang Masuk",
                                keterangan = "Faktur: ${fakturNumber.ifBlank { "Supplier $supplierName" }}$priceChangeNote"
                            )
                        )
                    }
                }

                // Handle Cash Deduction or Supplier Payable & Track Balances
                val primaryAccCode = if (statusPembayaran == "Tunai") "TUNAI"
                else if (targetAccountCode.isNotBlank() && targetAccountCode != "TUNAI") targetAccountCode
                else "BANK"

                val accBefore = cashDao.getAccountDirect(primaryAccCode)?.saldo ?: 0.0

                if (statusPembayaran.contains("Tunai & Transfer") || (nominalTunaiSplit > 0 && nominalTransferSplit > 0)) {
                    val bankAccount = if (targetAccountCode.isNotBlank() && targetAccountCode != "TUNAI") targetAccountCode else "BANK"
                    val bankName = com.example.data.entity.CashAccountDefaults.getAccountName(bankAccount)
                    if (nominalTunaiSplit > 0) {
                        recordCashOutDirect(
                            accountType = "TUNAI",
                            amount = nominalTunaiSplit,
                            category = "Pembelian Barang (Tunai)",
                            note = "Barang Masuk #${txId} - Supplier: ${supplierName.trim()} | Faktur: ${fakturNumber.ifBlank { "-" }} (Bagian Tunai)",
                            date = tanggal
                        )
                    }
                    if (nominalTransferSplit > 0) {
                        recordCashOutDirect(
                            accountType = bankAccount,
                            amount = nominalTransferSplit,
                            category = "Pembelian Barang (Transfer)",
                            note = "Barang Masuk #${txId} - Supplier: ${supplierName.trim()} | Faktur: ${fakturNumber.ifBlank { "-" }} (Bagian Transfer $bankName)",
                            date = tanggal
                        )
                    }
                } else if (statusPembayaran == "Tunai") {
                    recordCashOutDirect(
                        accountType = "TUNAI",
                        amount = totalNilai,
                        category = "Pembelian Barang (Tunai)",
                        note = "Barang Masuk #${txId} - Supplier: ${supplierName.trim()} | Faktur: ${fakturNumber.ifBlank { "-" }}",
                        date = tanggal
                    )
                } else if (statusPembayaran.startsWith("Transfer") || statusPembayaran == "Transfer") {
                    val accType = if (targetAccountCode.isNotBlank() && targetAccountCode != "TUNAI") targetAccountCode else "BANK"
                    recordCashOutDirect(
                        accountType = accType,
                        amount = totalNilai,
                        category = "Pembelian Barang (Transfer)",
                        note = "Barang Masuk #${txId} - Supplier: ${supplierName.trim()} | Faktur: ${fakturNumber.ifBlank { "-" }}",
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

                val accAfter = cashDao.getAccountDirect(primaryAccCode)?.saldo ?: 0.0

                // Record Transaction Status History Log
                recordTransactionLogDirect(
                    type = "Barang Masuk",
                    txId = txId,
                    ref = if (fakturNumber.isNotBlank()) fakturNumber.trim() else "BM-#$txId",
                    action = "CREATED",
                    prevStatus = "Draft",
                    newStatus = if (statusPembayaran == "Hutang") "Belum Lunas" else "Completed",
                    nominal = totalNilai,
                    accountType = if (statusPembayaran == "Hutang") "HUTANG" else primaryAccCode,
                    balBefore = accBefore,
                    balAfter = accAfter,
                    note = "Barang Masuk #$txId dari $supplierName ($statusPembayaran)",
                    date = tanggal
                )

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

    fun addSalesCartItem(item: ItemEntity, qty: Int = 1) {
        val current = _salesCart.value.toMutableList()
        val index = current.indexOfFirst { it.item.id == item.id }
        val addQty = qty.coerceAtLeast(1)
        val maxStock = maxOf(1, item.totalStokCombined)
        if (index != -1) {
            val existing = current[index]
            val newQty = (existing.jumlahTerjual + addQty).coerceAtMost(maxStock)
            current[index] = existing.copy(jumlahTerjual = newQty)
        } else {
            val initialQty = addQty.coerceAtMost(maxStock)
            current.add(CartItemSales(item = item, jumlahTerjual = initialQty, hargaSatuan = item.hargaModal))
        }
        _salesCart.value = current
    }

    fun updateSalesCartQuantity(itemId: Long, qty: Int) {
        val current = _salesCart.value.toMutableList()
        val index = current.indexOfFirst { it.item.id == itemId }
        if (index != -1) {
            val maxStock = current[index].item.totalStokCombined
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
        metodePembayaran: String = "Tunai", // "Tunai", "Transfer", "Tunai & Transfer", "Piutang", etc.
        targetAccountCode: String = "TUNAI", // "TUNAI", "BCA", "MANDIRI", "BRI", "BNI", "BANK_LAIN", "GOPAY", "OVO", "DANA", "SHOPEEPAY", "LINKAJA"
        selectedStore: String = _selectedStoreForSales.value,
        nominalTunaiSplit: Double = 0.0,
        nominalTransferSplit: Double = 0.0,
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

                enqueueSyncRecord(
                    type = "SALES",
                    summary = "Penjualan $selectedStore Nota #$txId - ${Formatters.formatRupiah(finalMoney)}",
                    payloadJson = "{\"txId\":$txId,\"store\":\"$selectedStore\",\"total\":$finalMoney,\"itemsCount\":$totalItemTerjual}"
                )

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
                    if (metodePembayaran.contains("Tunai & Transfer") || (nominalTunaiSplit > 0 && nominalTransferSplit > 0)) {
                        val bankAccount = if (targetAccountCode.isNotBlank() && targetAccountCode != "TUNAI") targetAccountCode else "BANK"
                        val bankName = com.example.data.entity.CashAccountDefaults.getAccountName(bankAccount)
                        if (nominalTunaiSplit > 0) {
                            recordCashInDirect(
                                accountType = "TUNAI",
                                amount = nominalTunaiSplit,
                                category = "Penjualan $selectedStore (Tunai)",
                                note = "Nota #${txId} - $fullCatatan (Bagian Tunai)",
                                date = tanggal
                            )
                        }
                        if (nominalTransferSplit > 0) {
                            recordCashInDirect(
                                accountType = bankAccount,
                                amount = nominalTransferSplit,
                                category = "Penjualan $selectedStore (Transfer)",
                                note = "Nota #${txId} - $fullCatatan (Bagian Transfer $bankName)",
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
                            note = "Nota #${txId} - $fullCatatan",
                            date = tanggal
                        )
                    }
                }

                // Deduct store stock & log stock history with auto-fallback to other store/warehouse
                cart.forEach { cartItem ->
                    val currentItem = itemDao.getItemById(cartItem.item.id)
                    if (currentItem != null) {
                        val oldUtama = currentItem.actualStokUtama
                        val oldCabang = currentItem.actualStokCabang
                        val qtyDeducted = cartItem.jumlahTerjual

                        var newUtama = oldUtama
                        var newCabang = oldCabang
                        var deductedFromUtama = 0
                        var deductedFromCabang = 0

                        val isCabang = selectedStore == "Toko Cabang" || selectedStore == "Stok Toko" || selectedStore.contains("Cabang", ignoreCase = true) || selectedStore.contains("Toko", ignoreCase = true)
                        
                        if (isCabang) {
                            if (oldCabang >= qtyDeducted) {
                                newCabang = oldCabang - qtyDeducted
                                deductedFromCabang = qtyDeducted
                            } else {
                                // Stok toko 0 atau kurang dari jumlah -> potong semua dari cabang, sisanya otomatis dari Gudang
                                deductedFromCabang = oldCabang
                                val shortfall = qtyDeducted - oldCabang
                                val fromUtama = shortfall.coerceAtMost(oldUtama)
                                newCabang = 0
                                newUtama = (oldUtama - fromUtama).coerceAtLeast(0)
                                deductedFromUtama = fromUtama
                            }
                        } else {
                            if (oldUtama >= qtyDeducted) {
                                newUtama = oldUtama - qtyDeducted
                                deductedFromUtama = qtyDeducted
                            } else {
                                // Stok gudang 0 atau kurang dari jumlah -> potong semua dari gudang, sisanya otomatis dari Stok Toko
                                deductedFromUtama = oldUtama
                                val shortfall = qtyDeducted - oldUtama
                                val fromCabang = shortfall.coerceAtMost(oldCabang)
                                newUtama = 0
                                newCabang = (oldCabang - fromCabang).coerceAtLeast(0)
                                deductedFromCabang = fromCabang
                            }
                        }

                        val totalS = (newUtama + newCabang).coerceAtLeast(0)

                        itemDao.updateStoreStocks(
                            id = cartItem.item.id,
                            stokUtama = newUtama,
                            stokCabang = newCabang,
                            totalStok = totalS
                        )

                        val autoFallbackNote = if (isCabang && oldCabang < qtyDeducted && deductedFromUtama > 0) {
                            if (oldCabang <= 0) " [Stok Toko 0 -> Dialihkan ke Gudang: $deductedFromUtama unit]"
                            else " [Stok Toko: $deductedFromCabang unit, Sisa potong Gudang: $deductedFromUtama unit]"
                        } else if (!isCabang && oldUtama < qtyDeducted && deductedFromCabang > 0) {
                            if (oldUtama <= 0) " [Stok Gudang 0 -> Dialihkan ke Toko: $deductedFromCabang unit]"
                            else " [Stok Gudang: $deductedFromUtama unit, Sisa potong Toko: $deductedFromCabang unit]"
                        } else ""

                        stockHistoryDao.insertHistory(
                            StockHistoryEntity(
                                itemId = cartItem.item.id,
                                kodeBarang = cartItem.item.kodeBarang,
                                namaBarang = cartItem.item.namaBarang,
                                jumlahPerubahan = -qtyDeducted,
                                stokAwal = currentItem.totalStokCombined,
                                stokAkhir = totalS,
                                jenis = "Penjualan Harian",
                                keterangan = "Penjualan $selectedStore Tgl: $tanggal ${if (namaPelanggan.isNotBlank()) "($namaPelanggan)" else ""}$autoFallbackNote".trim(),
                                namaToko = selectedStore
                            )
                        )
                    }
                }

                recordTransactionLogDirect(
                    type = "Penjualan ($selectedStore)",
                    txId = txId,
                    ref = "PJ-#$txId",
                    action = "CREATED",
                    prevStatus = "Draft",
                    newStatus = if (isPiutang && finalMoney > uangMuka) "Belum Lunas" else "Completed",
                    nominal = finalMoney,
                    accountType = if (isPiutang) "PIUTANG" else if (metodePembayaran.startsWith("Transfer")) "BANK" else "TUNAI",
                    balBefore = 0.0,
                    balAfter = finalMoney,
                    note = "Nota Penjualan #$txId $selectedStore ($metodePembayaran) - $fullCatatan",
                    date = tanggal
                )

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

    fun cancelCustomerReceivable(piutangId: Long, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            customerReceivableDao.deletePaymentsByReceivable(piutangId)
            customerReceivableDao.deleteReceivable(piutangId)
            onSuccess?.invoke()
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

            val targetAccount = when {
                metodePembayaran == "Tunai" -> "TUNAI"
                metodePembayaran == "Transfer" -> "BANK"
                else -> metodePembayaran
            }
            recordCashInDirect(
                accountType = targetAccount,
                amount = nominalBayar,
                category = "Bayar Piutang (${if (metodePembayaran == "Tunai") "Tunai" else "Transfer/Bank"})",
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

    fun cancelCustomerPayment(paymentId: Long, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            val payment = customerReceivableDao.getPaymentById(paymentId) ?: return@launch
            val receivable = customerReceivableDao.getReceivableById(payment.piutangId)
            if (receivable != null) {
                val newSisa = receivable.nominalSisa + payment.nominalBayar
                val updated = receivable.copy(
                    nominalSisa = newSisa,
                    status = if (newSisa > 0) "Belum Lunas" else "Lunas"
                )
                customerReceivableDao.updateReceivable(updated)
            }
            val targetAccount = when {
                payment.metodePembayaran == "Tunai" -> "TUNAI"
                payment.metodePembayaran == "Transfer" -> "BANK"
                else -> payment.metodePembayaran.ifBlank { "TUNAI" }
            }
            recordCashOutDirect(
                accountType = targetAccount,
                amount = payment.nominalBayar,
                category = "Pembatalan Pelunasan Piutang",
                note = "Pembatalan pembayaran Rp ${payment.nominalBayar.toInt()} untuk ${receivable?.namaPelanggan ?: "Piutang"}",
                date = Formatters.getCurrentDateFormatted()
            )
            customerReceivableDao.deletePayment(paymentId)
            onSuccess?.invoke()
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

            val targetAccount = when {
                metodePembayaran == "Tunai" -> "TUNAI"
                metodePembayaran == "Transfer" -> "BANK"
                else -> metodePembayaran
            }
            recordCashOutDirect(
                accountType = targetAccount,
                amount = nominalBayar,
                category = "Bayar Hutang (${if (metodePembayaran == "Tunai") "Tunai" else "Transfer/Bank"})",
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

    fun cancelSupplierPayment(paymentId: Long, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            val payment = supplierPayableDao.getPaymentById(paymentId) ?: return@launch
            val payable = supplierPayableDao.getPayableById(payment.hutangId)
            if (payable != null) {
                val newSisa = payable.nominalSisa + payment.nominalBayar
                val updated = payable.copy(
                    nominalSisa = newSisa,
                    status = if (newSisa > 0) "Belum Lunas" else "Lunas"
                )
                supplierPayableDao.updatePayable(updated)
            }
            val targetAccount = when {
                payment.metodePembayaran == "Tunai" -> "TUNAI"
                payment.metodePembayaran == "Transfer" -> "BANK"
                else -> payment.metodePembayaran.ifBlank { "TUNAI" }
            }
            recordCashInDirect(
                accountType = targetAccount,
                amount = payment.nominalBayar,
                category = "Pembatalan Pelunasan Hutang",
                note = "Pembatalan pembayaran hutang Rp ${payment.nominalBayar.toInt()} untuk ${payable?.namaSupplier ?: "Supplier"}",
                date = Formatters.getCurrentDateFormatted()
            )
            supplierPayableDao.deletePayment(paymentId)
            onSuccess?.invoke()
        }
    }

    fun cancelSupplierPayable(hutangId: Long, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            supplierPayableDao.deletePaymentsByPayable(hutangId)
            supplierPayableDao.deletePayable(hutangId)
            onSuccess?.invoke()
        }
    }

    fun addSupplierPayable(
        namaSupplier: String,
        nomorFaktur: String = "",
        nominal: Double,
        tanggal: String,
        catatan: String = ""
    ) {
        if (namaSupplier.isBlank() || nominal <= 0) return
        val finalNote = if (nomorFaktur.isNotBlank()) {
            if (catatan.isNotBlank()) "Faktur: ${nomorFaktur.trim()} - ${catatan.trim()}" else "Faktur: ${nomorFaktur.trim()}"
        } else catatan.trim()

        viewModelScope.launch {
            val payable = SupplierPayableEntity(
                namaSupplier = namaSupplier.trim(),
                nominalAwal = nominal,
                nominalSisa = nominal,
                tanggal = tanggal,
                catatan = finalNote,
                status = "Belum Lunas"
            )
            supplierPayableDao.insertPayable(payable)
        }
    }

    fun cancelCashMutation(mutationId: Long, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            val mutation = cashDao.getMutationById(mutationId) ?: return@launch
            val currentAccount = cashDao.getAccountDirect(mutation.accountType)
            if (currentAccount != null) {
                val newSaldo = if (mutation.jenis == "MASUK") {
                    currentAccount.saldo - mutation.nominal
                } else {
                    currentAccount.saldo + mutation.nominal
                }
                cashDao.insertOrUpdateAccount(currentAccount.copy(saldo = newSaldo, lastUpdated = System.currentTimeMillis()))
            }
            cashDao.deleteMutation(mutationId)
            onSuccess?.invoke()
        }
    }

    fun updateCashMutation(
        mutationId: Long,
        newAccountType: String,
        newNominal: Double,
        newKategori: String,
        newKeterangan: String,
        newTanggal: String,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val oldMutation = cashDao.getMutationById(mutationId) ?: return@launch

            // 1. Revert old mutation effect on old account
            val oldAccount = cashDao.getAccountDirect(oldMutation.accountType)
            if (oldAccount != null) {
                val oldRevertedSaldo = if (oldMutation.jenis == "MASUK") {
                    oldAccount.saldo - oldMutation.nominal
                } else {
                    oldAccount.saldo + oldMutation.nominal
                }
                cashDao.insertOrUpdateAccount(oldAccount.copy(saldo = oldRevertedSaldo, lastUpdated = System.currentTimeMillis()))
            }

            // 2. Apply new mutation effect on new account
            val targetAccount = cashDao.getAccountDirect(newAccountType) ?: com.example.data.entity.CashAccountEntity(
                accountType = newAccountType,
                accountName = com.example.data.entity.CashAccountDefaults.getAccountName(newAccountType),
                saldo = 0.0
            )
            val newSaldo = if (oldMutation.jenis == "MASUK") {
                targetAccount.saldo + newNominal
            } else {
                targetAccount.saldo - newNominal
            }
            cashDao.insertOrUpdateAccount(targetAccount.copy(saldo = newSaldo, lastUpdated = System.currentTimeMillis()))

            // 3. Save updated mutation
            val updatedMutation = oldMutation.copy(
                accountType = newAccountType,
                nominal = newNominal,
                kategori = newKategori,
                keterangan = newKeterangan,
                tanggal = newTanggal,
                saldoSesudah = newSaldo
            )
            cashDao.insertMutationDirect(updatedMutation)
            onSuccess?.invoke()
        }
    }

    fun cancelSalesTransaction(transactionId: Long, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            val transaction = salesDao.getTransactionById(transactionId) ?: return@launch
            val items = salesDao.getItemsForTransaction(transactionId)

            // 1. Restore item stock
            items.forEach { salesItem ->
                val product = itemDao.getItemById(salesItem.itemId)
                if (product != null) {
                    val isCabang = transaction.namaToko.contains("Cabang", ignoreCase = true)
                    val oldUtama = product.actualStokUtama
                    val oldCabang = product.actualStokCabang
                    var newUtama = oldUtama
                    var newCabang = oldCabang
                    if (isCabang) {
                        newCabang += salesItem.jumlahTerjual
                    } else {
                        newUtama += salesItem.jumlahTerjual
                    }
                    val totalS = newUtama + newCabang

                    itemDao.updateStoreStocks(
                        id = product.id,
                        stokUtama = newUtama,
                        stokCabang = newCabang,
                        totalStok = totalS
                    )

                    stockHistoryDao.insertHistory(
                        com.example.data.entity.StockHistoryEntity(
                            itemId = product.id,
                            kodeBarang = product.kodeBarang,
                            namaBarang = product.namaBarang,
                            jumlahPerubahan = salesItem.jumlahTerjual,
                            stokAwal = product.totalStokCombined,
                            stokAkhir = totalS,
                            jenis = "Pembatalan Penjualan",
                            keterangan = "Pembatalan Transaksi Penjualan #${transaction.id}",
                            namaToko = transaction.namaToko
                        )
                    )
                }
            }

            // 2. Reverse all money into bank/cash
            val allMutations = cashDao.getAllMutationsList()
            val matchedMutations = allMutations.filter {
                it.keterangan.contains("Nota #${transaction.id}") || it.keterangan.contains("Nota #${transactionId}")
            }

            if (matchedMutations.isNotEmpty()) {
                matchedMutations.forEach { mut ->
                    val acc = cashDao.getAccountDirect(mut.accountType)
                    if (acc != null) {
                        val newSaldo = if (mut.jenis == "MASUK") {
                            acc.saldo - mut.nominal
                        } else {
                            acc.saldo + mut.nominal
                        }
                        cashDao.insertOrUpdateAccount(acc.copy(saldo = newSaldo, lastUpdated = System.currentTimeMillis()))
                    }
                    cashDao.deleteMutation(mut.id)
                }
            } else {
                // Fallback for legacy transactions
                if (transaction.metodePembayaran != "Piutang") {
                    val allAccounts = cashDao.getAllAccountsList()
                    val targetAccType = when {
                        transaction.metodePembayaran == "Tunai" -> "TUNAI"
                        else -> {
                            val matched = allAccounts.find { 
                                it.accountType.equals(transaction.metodePembayaran, ignoreCase = true) || 
                                it.accountName.equals(transaction.metodePembayaran, ignoreCase = true) 
                            }
                            matched?.accountType ?: "BANK"
                        }
                    }
                    val acc = cashDao.getAccountDirect(targetAccType)
                    if (acc != null) {
                        val newSaldo = acc.saldo - transaction.totalUangPenjualan
                        cashDao.insertOrUpdateAccount(acc.copy(saldo = newSaldo, lastUpdated = System.currentTimeMillis()))
                    }
                }
            }

            // 3. Remove customer receivables created for this transaction, if any
            val receivables = customerReceivableDao.getAllReceivablesList()
            val matchingReceivable = receivables.find { it.catatan.contains("Nota #${transaction.id}") }
            if (matchingReceivable != null) {
                customerReceivableDao.deletePaymentsByReceivable(matchingReceivable.id)
                customerReceivableDao.deleteReceivable(matchingReceivable.id)
            }

            salesDao.deleteItemsForTransaction(transactionId)
            salesDao.deleteTransaction(transactionId)
            onSuccess?.invoke()
        }
    }

    fun updateSalesTransaction(
        transactionId: Long,
        newTanggal: String,
        newTotalUangPenjualan: Double,
        newMetodePembayaran: String,
        newTargetAccountCode: String,
        newCatatan: String,
        newNamaPelanggan: String = "",
        updatedItems: List<SalesItemEntity>? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val oldTx = salesDao.getTransactionById(transactionId) ?: return@launch
            val oldItems = salesDao.getItemsForTransaction(transactionId)

            // 1. Revert previous item stock changes (add back old sold quantities)
            oldItems.forEach { oldItem ->
                val product = itemDao.getItemById(oldItem.itemId)
                if (product != null) {
                    val isCabang = oldTx.namaToko.contains("Cabang", ignoreCase = true)
                    val oldUtama = product.actualStokUtama
                    val oldCabang = product.actualStokCabang
                    var newUtama = oldUtama
                    var newCabang = oldCabang
                    if (isCabang) {
                        newCabang += oldItem.jumlahTerjual
                    } else {
                        newUtama += oldItem.jumlahTerjual
                    }
                    val totalS = newUtama + newCabang
                    itemDao.updateStoreStocks(
                        id = product.id,
                        stokUtama = newUtama,
                        stokCabang = newCabang,
                        totalStok = totalS
                    )
                }
            }

            // 2. Process new items (if provided) & deduct stock
            val itemsToSave = updatedItems ?: oldItems
            var calculatedTotalModal = 0.0

            itemsToSave.forEach { newItem ->
                val product = itemDao.getItemById(newItem.itemId)
                if (product != null) {
                    val isCabang = oldTx.namaToko.contains("Cabang", ignoreCase = true) || oldTx.namaToko.contains("Toko", ignoreCase = true)
                    val oldUtama = product.actualStokUtama
                    val oldCabang = product.actualStokCabang
                    val qtyDeducted = newItem.jumlahTerjual
                    var newUtama = oldUtama
                    var newCabang = oldCabang
                    var deductedFromUtama = 0
                    var deductedFromCabang = 0

                    if (isCabang) {
                        if (oldCabang >= qtyDeducted) {
                            newCabang = oldCabang - qtyDeducted
                            deductedFromCabang = qtyDeducted
                        } else {
                            deductedFromCabang = oldCabang
                            val shortfall = qtyDeducted - oldCabang
                            val fromUtama = shortfall.coerceAtMost(oldUtama)
                            newCabang = 0
                            newUtama = (oldUtama - fromUtama).coerceAtLeast(0)
                            deductedFromUtama = fromUtama
                        }
                    } else {
                        if (oldUtama >= qtyDeducted) {
                            newUtama = oldUtama - qtyDeducted
                            deductedFromUtama = qtyDeducted
                        } else {
                            deductedFromUtama = oldUtama
                            val shortfall = qtyDeducted - oldUtama
                            val fromCabang = shortfall.coerceAtMost(oldCabang)
                            newUtama = 0
                            newCabang = (oldCabang - fromCabang).coerceAtLeast(0)
                            deductedFromCabang = fromCabang
                        }
                    }
                    val totalS = (newUtama + newCabang).coerceAtLeast(0)
                    itemDao.updateStoreStocks(
                        id = product.id,
                        stokUtama = newUtama,
                        stokCabang = newCabang,
                        totalStok = totalS
                    )

                    calculatedTotalModal += (product.hargaModal * newItem.jumlahTerjual)

                    val autoFallbackNote = if (isCabang && oldCabang < qtyDeducted && deductedFromUtama > 0) {
                        if (oldCabang <= 0) " [Stok Toko 0 -> Dialihkan ke Gudang: $deductedFromUtama unit]"
                        else " [Stok Toko: $deductedFromCabang unit, Sisa potong Gudang: $deductedFromUtama unit]"
                    } else if (!isCabang && oldUtama < qtyDeducted && deductedFromCabang > 0) {
                        if (oldUtama <= 0) " [Stok Gudang 0 -> Dialihkan ke Toko: $deductedFromCabang unit]"
                        else " [Stok Gudang: $deductedFromUtama unit, Sisa potong Toko: $deductedFromCabang unit]"
                    } else ""

                    // Add stock history log for edit
                    stockHistoryDao.insertHistory(
                        com.example.data.entity.StockHistoryEntity(
                            itemId = product.id,
                            kodeBarang = product.kodeBarang,
                            namaBarang = newItem.namaBarang.ifBlank { product.namaBarang },
                            jumlahPerubahan = -newItem.jumlahTerjual,
                            stokAwal = product.totalStokCombined,
                            stokAkhir = totalS,
                            jenis = "Edit Penjualan",
                            keterangan = "Perubahan Transaksi Penjualan #${oldTx.id}$autoFallbackNote".trim(),
                            namaToko = oldTx.namaToko
                        )
                    )
                } else {
                    calculatedTotalModal += (newItem.hargaSatuan * 0.7 * newItem.jumlahTerjual)
                }
            }

            // Update sales_items table
            if (updatedItems != null) {
                salesDao.deleteItemsForTransaction(transactionId)
                salesDao.insertItems(updatedItems.map { 
                    it.copy(
                        id = 0,
                        transactionId = transactionId,
                        totalHarga = it.jumlahTerjual * it.hargaSatuan
                    ) 
                })
            }

            // 3. Revert previous cash mutation / bank balance impact
            val allMutations = cashDao.getAllMutationsList()
            val matchedMutations = allMutations.filter {
                it.keterangan.contains("Nota #${oldTx.id}")
            }

            if (matchedMutations.isNotEmpty()) {
                matchedMutations.forEach { mut ->
                    val acc = cashDao.getAccountDirect(mut.accountType)
                    if (acc != null) {
                        val revertedSaldo = if (mut.jenis == "MASUK") acc.saldo - mut.nominal else acc.saldo + mut.nominal
                        cashDao.insertOrUpdateAccount(acc.copy(saldo = revertedSaldo, lastUpdated = System.currentTimeMillis()))
                    }
                    cashDao.deleteMutation(mut.id)
                }
            } else if (oldTx.metodePembayaran != "Piutang") {
                val oldTargetAcc = if (oldTx.metodePembayaran == "Tunai") "TUNAI" else "BANK"
                val acc = cashDao.getAccountDirect(oldTargetAcc)
                if (acc != null) {
                    val revertedSaldo = acc.saldo - oldTx.totalUangPenjualan
                    cashDao.insertOrUpdateAccount(acc.copy(saldo = revertedSaldo, lastUpdated = System.currentTimeMillis()))
                }
            }

            // 4. Handle Customer Receivables (Piutang)
            val receivables = customerReceivableDao.getAllReceivablesList()
            val matchingReceivable = receivables.find { it.catatan.contains("Nota #${oldTx.id}") }
            val isPiutang = newMetodePembayaran.contains("Piutang", ignoreCase = true)

            if (isPiutang) {
                val finalNamaPelanggan = newNamaPelanggan.trim().ifBlank {
                    matchingReceivable?.namaPelanggan ?: "Pelanggan Nota #${oldTx.id}"
                }
                if (matchingReceivable != null) {
                    val payments = customerReceivableDao.getPaymentsByReceivableList(matchingReceivable.id)
                    val totalBayar = payments.sumOf { it.nominalBayar }
                    val newSisa = (newTotalUangPenjualan - totalBayar).coerceAtLeast(0.0)
                    val newStatus = if (newSisa <= 0) "Lunas" else "Belum Lunas"

                    val updatedReceivable = matchingReceivable.copy(
                        namaPelanggan = finalNamaPelanggan,
                        nominalAwal = newTotalUangPenjualan,
                        nominalSisa = newSisa,
                        tanggal = newTanggal,
                        catatan = "Penjualan ${oldTx.namaToko} Nota #${oldTx.id} (${newCatatan.ifBlank { "Diperbarui" }})",
                        status = newStatus
                    )
                    customerReceivableDao.insertReceivable(updatedReceivable)
                } else {
                    val newReceivable = CustomerReceivableEntity(
                        namaPelanggan = finalNamaPelanggan,
                        nominalAwal = newTotalUangPenjualan,
                        nominalSisa = newTotalUangPenjualan,
                        tanggal = newTanggal,
                        catatan = "Penjualan ${oldTx.namaToko} Nota #${oldTx.id} (${newCatatan.ifBlank { "Diperbarui" }})",
                        status = if (newTotalUangPenjualan <= 0) "Lunas" else "Belum Lunas"
                    )
                    customerReceivableDao.insertReceivable(newReceivable)
                }
            } else {
                // If previously was Piutang, remove it from receivables
                if (matchingReceivable != null) {
                    customerReceivableDao.deletePaymentsByReceivable(matchingReceivable.id)
                    customerReceivableDao.deleteReceivable(matchingReceivable.id)
                }

                // Record cash / bank entry if not Piutang
                val targetAccount = if (newMetodePembayaran == "Tunai") "TUNAI"
                else if (newTargetAccountCode.isNotBlank()) newTargetAccountCode
                else "BANK"

                recordCashInDirect(
                    accountType = targetAccount,
                    amount = newTotalUangPenjualan,
                    category = "Penjualan ${oldTx.namaToko} ($newMetodePembayaran)",
                    note = "Nota #${oldTx.id} - ${newCatatan.ifBlank { "Penjualan ${oldTx.namaToko}" }}",
                    date = newTanggal
                )
            }

            // 5. Update SalesTransaction entity
            val newTotalItemsCount = itemsToSave.sumOf { it.jumlahTerjual }
            val newProfit = (newTotalUangPenjualan - calculatedTotalModal).coerceAtLeast(0.0)
            val updatedTx = oldTx.copy(
                tanggal = newTanggal,
                totalUangPenjualan = newTotalUangPenjualan,
                totalModal = calculatedTotalModal,
                keuntungan = newProfit,
                totalItemTerjual = newTotalItemsCount,
                metodePembayaran = newMetodePembayaran,
                catatan = newCatatan
            )
            salesDao.insertTransactionDirect(updatedTx)
            onSuccess?.invoke()
        }
    }

    fun cancelIncomingTransaction(transactionId: Long, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            val transaction = incomingDao.getTransactionById(transactionId) ?: return@launch
            val items = incomingDao.getItemsForTransaction(transactionId)

            // 1. Revert item stock
            items.forEach { incItem ->
                val product = itemDao.getItemById(incItem.itemId)
                if (product != null) {
                    val oldUtama = product.actualStokUtama
                    val oldCabang = product.actualStokCabang
                    val newUtama = (oldUtama - incItem.jumlahMasuk).coerceAtLeast(0)
                    val totalS = newUtama + oldCabang

                    itemDao.updateStoreStocks(
                        id = product.id,
                        stokUtama = newUtama,
                        stokCabang = oldCabang,
                        totalStok = totalS
                    )

                    stockHistoryDao.insertHistory(
                        com.example.data.entity.StockHistoryEntity(
                            itemId = product.id,
                            kodeBarang = product.kodeBarang,
                            namaBarang = product.namaBarang,
                            jumlahPerubahan = -incItem.jumlahMasuk,
                            stokAwal = product.totalStokCombined,
                            stokAkhir = totalS,
                            jenis = "Pembatalan Barang Masuk",
                            keterangan = "Pembatalan Barang Masuk #${transaction.id} (${transaction.namaSupplier})",
                            namaToko = "Toko Utama"
                        )
                    )
                }
            }

            // 2. Reverse cash mutations & bank entries
            val allMutations = cashDao.getAllMutationsList()
            val matchedMutations = allMutations.filter {
                it.keterangan.contains("#${transaction.id}") ||
                it.keterangan.contains("Barang Masuk #${transaction.id}") ||
                it.keterangan.contains("Nota Masuk #${transaction.id}") ||
                (transaction.nomorFaktur.isNotBlank() && it.keterangan.contains(transaction.nomorFaktur))
            }

            if (matchedMutations.isNotEmpty()) {
                matchedMutations.forEach { mut ->
                    val acc = cashDao.getAccountDirect(mut.accountType) ?: CashAccountEntity(
                        accountType = mut.accountType,
                        accountName = com.example.data.entity.CashAccountDefaults.getAccountName(mut.accountType),
                        saldo = 0.0
                    )
                    val newSaldo = if (mut.jenis == "KELUAR") {
                        acc.saldo + mut.nominal
                    } else {
                        acc.saldo - mut.nominal
                    }
                    cashDao.insertOrUpdateAccount(acc.copy(saldo = newSaldo, lastUpdated = System.currentTimeMillis()))
                    cashDao.deleteMutation(mut.id)
                }
            } else if (transaction.statusPembayaran != "Hutang") {
                val targetAccount = when {
                    transaction.statusPembayaran.contains("Tunai", ignoreCase = true) -> "TUNAI"
                    else -> {
                        val allAccs = cashDao.getAllAccountsList()
                        val matchedAcc = allAccs.find { it.accountType.equals(transaction.statusPembayaran, ignoreCase = true) || it.accountName.equals(transaction.statusPembayaran, ignoreCase = true) }
                        matchedAcc?.accountType ?: "BANK"
                    }
                }
                val currentAccount = cashDao.getAccountDirect(targetAccount) ?: CashAccountEntity(
                    accountType = targetAccount,
                    accountName = com.example.data.entity.CashAccountDefaults.getAccountName(targetAccount),
                    saldo = 0.0
                )
                val newSaldo = currentAccount.saldo + transaction.totalNilai
                cashDao.insertOrUpdateAccount(currentAccount.copy(saldo = newSaldo, lastUpdated = System.currentTimeMillis()))

                // Also attempt to delete any orphan cash mutation for this transaction if supplier name matches
                if (transaction.namaSupplier.isNotBlank() && transaction.namaSupplier != "Supplier Umum") {
                    val orphanMutations = allMutations.filter {
                        it.jenis == "KELUAR" && it.keterangan.contains(transaction.namaSupplier) && it.kategori.contains("Pembelian")
                    }
                    orphanMutations.forEach { orphan ->
                        cashDao.deleteMutation(orphan.id)
                    }
                }
            }

            // 3. Remove supplier payables created for this transaction and revert payments made to cash
            val payables = supplierPayableDao.getAllPayablesList()
            val matchingPayable = payables.find {
                it.incomingTransactionId == transaction.id ||
                it.catatan.contains("#${transaction.id}") ||
                it.catatan.contains("Barang Masuk #${transaction.id}") ||
                it.catatan.contains("Nota Masuk #${transaction.id}") ||
                (transaction.nomorFaktur.isNotBlank() && it.catatan.contains(transaction.nomorFaktur))
            }
            if (matchingPayable != null) {
                val payments = supplierPayableDao.getPaymentsByPayableList(matchingPayable.id)
                payments.forEach { p ->
                    val pAccType = when {
                        p.metodePembayaran.equals("Tunai", ignoreCase = true) -> "TUNAI"
                        p.metodePembayaran.equals("Transfer", ignoreCase = true) -> "BANK"
                        else -> p.metodePembayaran.ifBlank { "TUNAI" }
                    }
                    val pAcc = cashDao.getAccountDirect(pAccType) ?: CashAccountEntity(
                        accountType = pAccType,
                        accountName = com.example.data.entity.CashAccountDefaults.getAccountName(pAccType),
                        saldo = 0.0
                    )
                    cashDao.insertOrUpdateAccount(pAcc.copy(saldo = pAcc.saldo + p.nominalBayar, lastUpdated = System.currentTimeMillis()))

                    val paymentMutations = allMutations.filter {
                        it.jenis == "KELUAR" && (it.keterangan.contains(matchingPayable.namaSupplier) || it.keterangan.contains("Bayar Hutang"))
                    }
                    paymentMutations.forEach { pm ->
                        cashDao.deleteMutation(pm.id)
                    }
                }
                supplierPayableDao.deletePaymentsByPayable(matchingPayable.id)
                supplierPayableDao.deletePayable(matchingPayable.id)
            }

            incomingDao.deleteItemsForTransaction(transactionId)
            incomingDao.deleteTransaction(transactionId)

            // Record cancellation in transaction history log
            recordTransactionLogDirect(
                type = "Barang Masuk",
                txId = transaction.id,
                ref = if (transaction.nomorFaktur.isNotBlank()) transaction.nomorFaktur else "BM-#${transaction.id}",
                action = "CANCELLED",
                prevStatus = transaction.statusPembayaran,
                newStatus = "Dibatalkan",
                nominal = transaction.totalNilai,
                accountType = if (transaction.statusPembayaran == "Hutang") "HUTANG" else "KAS/BANK",
                balBefore = transaction.totalNilai,
                balAfter = 0.0,
                note = "Pembatalan transaksi barang masuk #${transaction.id} (${transaction.namaSupplier}) - Saldo & Stok disinkronkan kembali",
                date = transaction.tanggal
            )
            onSuccess?.invoke()
        }
    }

    fun updateIncomingTransaction(
        transactionId: Long,
        newSupplier: String,
        newFakturNumber: String = "",
        newTanggal: String,
        newTotalNilai: Double,
        newStatusPembayaran: String,
        newTargetAccountCode: String,
        newCatatan: String,
        updatedItems: List<com.example.data.entity.IncomingItemEntity>? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val oldTx = incomingDao.getTransactionById(transactionId) ?: return@launch
            val oldItems = incomingDao.getItemsForTransaction(transactionId)

            // 1. Revert stock for old items
            oldItems.forEach { oldItem ->
                val product = itemDao.getItemById(oldItem.itemId)
                if (product != null) {
                    val oldUtama = product.actualStokUtama
                    val oldCabang = product.actualStokCabang
                    val newUtama = (oldUtama - oldItem.jumlahMasuk).coerceAtLeast(0)
                    val totalS = newUtama + oldCabang
                    itemDao.updateStoreStocks(
                        id = product.id,
                        stokUtama = newUtama,
                        stokCabang = oldCabang,
                        totalStok = totalS
                    )
                }
            }

            // 2. Apply stock for new/updated items and calculate weighted/average price if changed
            val itemsToSave = updatedItems ?: oldItems
            itemsToSave.forEach { newItem ->
                val product = itemDao.getItemById(newItem.itemId)
                if (product != null) {
                    val oldUtama = product.actualStokUtama
                    val oldCabang = product.actualStokCabang
                    val newUtama = oldUtama + newItem.jumlahMasuk
                    val totalS = newUtama + oldCabang

                    val stokAda = maxOf(0, product.totalStokCombined)
                    val masukQty = maxOf(1, newItem.jumlahMasuk)
                    val totalGabungan = stokAda + masukQty
                    val finalHargaModal = if (stokAda > 0 && product.hargaModal > 0.0 && newItem.hargaModal > 0.0 && newItem.hargaModal != product.hargaModal) {
                        kotlin.math.round(((stokAda * product.hargaModal) + (masukQty * newItem.hargaModal)) / totalGabungan)
                    } else if (newItem.hargaModal > 0.0) {
                        newItem.hargaModal
                    } else {
                        product.hargaModal
                    }

                    itemDao.updateStoreStocksAndPrice(
                        id = product.id,
                        stokUtama = newUtama,
                        stokCabang = oldCabang,
                        totalStok = totalS,
                        hargaModal = finalHargaModal
                    )

                    itemDao.updateItem(
                        product.copy(
                            stokTokoUtama = newUtama,
                            stokTokoCabang = oldCabang,
                            stok = totalS,
                            hargaModal = finalHargaModal,
                            updatedAt = System.currentTimeMillis()
                        )
                    )

                    val priceChangeNote = if (stokAda > 0 && product.hargaModal > 0.0 && newItem.hargaModal > 0.0 && newItem.hargaModal != product.hargaModal) {
                        " (HPP Rata-rata: Rp ${finalHargaModal.toInt()} [Stok Lama: $stokAda @ Rp ${product.hargaModal.toInt()} + Masuk: $masukQty @ Rp ${newItem.hargaModal.toInt()}])"
                    } else if (product.hargaModal > 0.0 && newItem.hargaModal > 0.0 && newItem.hargaModal != product.hargaModal) {
                        " (Harga Modal baru: Rp ${newItem.hargaModal.toInt()} dari Rp ${product.hargaModal.toInt()})"
                    } else ""

                    stockHistoryDao.insertHistory(
                        com.example.data.entity.StockHistoryEntity(
                            itemId = product.id,
                            kodeBarang = product.kodeBarang,
                            namaBarang = newItem.namaBarang.ifBlank { product.namaBarang },
                            jumlahPerubahan = newItem.jumlahMasuk,
                            stokAwal = product.totalStokCombined,
                            stokAkhir = totalS,
                            jenis = "Edit Barang Masuk",
                            keterangan = "Perubahan Transaksi Barang Masuk #${oldTx.id}$priceChangeNote",
                            namaToko = "Toko Utama"
                        )
                    )
                }
            }

            if (updatedItems != null) {
                incomingDao.deleteItemsForTransaction(transactionId)
                incomingDao.insertItems(updatedItems.map {
                    it.copy(
                        id = 0,
                        transactionId = transactionId
                    )
                })
            }

            // 3. Revert previous cash mutation / bank balance impact
            val allMutations = cashDao.getAllMutationsList()
            val matchedMutations = allMutations.filter {
                it.keterangan.contains("#${oldTx.id}") ||
                it.keterangan.contains("Barang Masuk #${oldTx.id}") ||
                it.keterangan.contains("Nota Masuk #${oldTx.id}") ||
                (oldTx.nomorFaktur.isNotBlank() && it.keterangan.contains(oldTx.nomorFaktur))
            }

            if (matchedMutations.isNotEmpty()) {
                matchedMutations.forEach { mut ->
                    val acc = cashDao.getAccountDirect(mut.accountType) ?: CashAccountEntity(
                        accountType = mut.accountType,
                        accountName = com.example.data.entity.CashAccountDefaults.getAccountName(mut.accountType),
                        saldo = 0.0
                    )
                    val revertedSaldo = if (mut.jenis == "KELUAR") acc.saldo + mut.nominal else acc.saldo - mut.nominal
                    cashDao.insertOrUpdateAccount(acc.copy(saldo = revertedSaldo, lastUpdated = System.currentTimeMillis()))
                    cashDao.deleteMutation(mut.id)
                }
            } else if (oldTx.statusPembayaran != "Hutang") {
                val oldTargetAcc = if (oldTx.statusPembayaran.contains("Tunai", ignoreCase = true)) "TUNAI" else "BANK"
                val acc = cashDao.getAccountDirect(oldTargetAcc) ?: CashAccountEntity(
                    accountType = oldTargetAcc,
                    accountName = com.example.data.entity.CashAccountDefaults.getAccountName(oldTargetAcc),
                    saldo = 0.0
                )
                val revertedSaldo = acc.saldo + oldTx.totalNilai
                cashDao.insertOrUpdateAccount(acc.copy(saldo = revertedSaldo, lastUpdated = System.currentTimeMillis()))
            }

            // 4. Update or Record Supplier Payables or Cash Deduction
            val payables = supplierPayableDao.getAllPayablesList()
            val matchingPayable = payables.find {
                it.catatan.contains("Barang Masuk #${oldTx.id}") || it.catatan.contains("Nota Masuk #${oldTx.id}")
            }
            val isHutang = newStatusPembayaran.equals("Hutang", ignoreCase = true)

            if (isHutang) {
                val finalSupplier = newSupplier.trim().ifBlank { oldTx.namaSupplier }
                if (matchingPayable != null) {
                    val payments = supplierPayableDao.getPaymentsByPayableList(matchingPayable.id)
                    val totalBayar = payments.sumOf { it.nominalBayar }
                    val newSisa = (newTotalNilai - totalBayar).coerceAtLeast(0.0)
                    val newStatus = if (newSisa <= 0) "Lunas" else "Belum Lunas"

                    val updatedPayable = matchingPayable.copy(
                        namaSupplier = finalSupplier,
                        nominalAwal = newTotalNilai,
                        nominalSisa = newSisa,
                        tanggal = newTanggal,
                        catatan = "Barang Masuk #${oldTx.id} - ${newCatatan.ifBlank { "Diperbarui" }}",
                        status = newStatus
                    )
                    supplierPayableDao.insertPayable(updatedPayable)
                } else {
                    val newPayable = SupplierPayableEntity(
                        namaSupplier = finalSupplier,
                        nominalAwal = newTotalNilai,
                        nominalSisa = newTotalNilai,
                        tanggal = newTanggal,
                        catatan = "Barang Masuk #${oldTx.id} - ${newCatatan.ifBlank { "Diperbarui" }}",
                        status = if (newTotalNilai <= 0) "Lunas" else "Belum Lunas"
                    )
                    supplierPayableDao.insertPayable(newPayable)
                }
            } else {
                if (matchingPayable != null) {
                    supplierPayableDao.deletePaymentsByPayable(matchingPayable.id)
                    supplierPayableDao.deletePayable(matchingPayable.id)
                }

                val targetAccount = if (newStatusPembayaran == "Tunai") "TUNAI"
                else if (newTargetAccountCode.isNotBlank()) newTargetAccountCode
                else "BANK"

                recordCashOutDirect(
                    accountType = targetAccount,
                    amount = newTotalNilai,
                    category = "Pembelian Barang Masuk",
                    note = "Barang Masuk #${oldTx.id} (${newSupplier.ifBlank { "Supplier Umum" }}) - ${newCatatan.ifBlank { "Pembelian Barang" }}",
                    date = newTanggal
                )
            }

            // 5. Update IncomingTransaction entity
            val newTotalItem = itemsToSave.sumOf { it.jumlahMasuk }
            val updatedTx = oldTx.copy(
                namaSupplier = newSupplier,
                nomorFaktur = if (newFakturNumber.isNotBlank()) newFakturNumber else oldTx.nomorFaktur,
                tanggal = newTanggal,
                totalNilai = newTotalNilai,
                totalItem = newTotalItem,
                statusPembayaran = newStatusPembayaran,
                catatan = newCatatan
            )
            incomingDao.insertTransactionDirect(updatedTx)

            // Record update in transaction history log
            recordTransactionLogDirect(
                type = "Barang Masuk",
                txId = oldTx.id,
                ref = if (newFakturNumber.isNotBlank()) newFakturNumber else oldTx.nomorFaktur.ifBlank { "BM-#${oldTx.id}" },
                action = "UPDATED",
                prevStatus = oldTx.statusPembayaran,
                newStatus = newStatusPembayaran,
                nominal = newTotalNilai,
                accountType = if (newStatusPembayaran == "Hutang") "HUTANG" else newTargetAccountCode.ifBlank { "KAS/BANK" },
                balBefore = oldTx.totalNilai,
                balAfter = newTotalNilai,
                note = "Pembaruan data transaksi barang masuk #${oldTx.id} dari $newSupplier ($newStatusPembayaran)",
                date = newTanggal
            )
            onSuccess?.invoke()
        }
    }

    fun synchronizeAndVerifyBalances(onComplete: (String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                var fixedPayables = 0
                val payables = supplierPayableDao.getAllPayablesList()
                for (p in payables) {
                    val payments = supplierPayableDao.getPaymentsByPayableList(p.id)
                    val totalPaid = payments.sumOf { it.nominalBayar }
                    val calculatedSisa = (p.nominalAwal - totalPaid).coerceAtLeast(0.0)
                    val expectedStatus = if (calculatedSisa <= 0.0) "Lunas" else "Belum Lunas"
                    if (p.nominalSisa != calculatedSisa || p.status != expectedStatus) {
                        supplierPayableDao.insertPayable(p.copy(nominalSisa = calculatedSisa, status = expectedStatus))
                        fixedPayables++
                        recordTransactionLogDirect(
                            type = "Hutang Supplier",
                            txId = p.id,
                            ref = "HUTANG-#${p.id}",
                            action = "SYNC_BALANCE",
                            prevStatus = p.status,
                            newStatus = expectedStatus,
                            nominal = calculatedSisa,
                            accountType = "HUTANG",
                            balBefore = p.nominalSisa,
                            balAfter = calculatedSisa,
                            note = "Sinkronisasi saldo sisa hutang ${p.namaSupplier} (Total Bayar: Rp ${totalPaid.toInt()})"
                        )
                    }
                }

                var fixedReceivables = 0
                val receivables = customerReceivableDao.getAllReceivablesList()
                for (r in receivables) {
                    val payments = customerReceivableDao.getPaymentsByReceivableList(r.id)
                    val totalPaid = payments.sumOf { it.nominalBayar }
                    val calculatedSisa = (r.nominalAwal - totalPaid).coerceAtLeast(0.0)
                    val expectedStatus = if (calculatedSisa <= 0.0) "Lunas" else "Belum Lunas"
                    if (r.nominalSisa != calculatedSisa || r.status != expectedStatus) {
                        customerReceivableDao.insertReceivable(r.copy(nominalSisa = calculatedSisa, status = expectedStatus))
                        fixedReceivables++
                        recordTransactionLogDirect(
                            type = "Piutang Pelanggan",
                            txId = r.id,
                            ref = "PIUTANG-#${r.id}",
                            action = "SYNC_BALANCE",
                            prevStatus = r.status,
                            newStatus = expectedStatus,
                            nominal = calculatedSisa,
                            accountType = "PIUTANG",
                            balBefore = r.nominalSisa,
                            balAfter = calculatedSisa,
                            note = "Sinkronisasi saldo sisa piutang ${r.namaPelanggan} (Total Bayar: Rp ${totalPaid.toInt()})"
                        )
                    }
                }

                val currentCash = cashDao.getAccountDirect("TUNAI")?.saldo ?: 0.0
                val currentBank = cashDao.getAccountDirect("BANK")?.saldo ?: 0.0
                recordTransactionLogDirect(
                    type = "Kas & Bank",
                    txId = 0L,
                    ref = "SYNC-ALL",
                    action = "SYNC_BALANCE",
                    prevStatus = "Audit",
                    newStatus = "Sinkron",
                    nominal = currentCash + currentBank,
                    accountType = "ALL",
                    balBefore = currentCash,
                    balAfter = currentBank,
                    note = "Audit & Sinkronisasi Saldo Sukses. Saldo Kas: Rp ${currentCash.toInt()}, Saldo Bank: Rp ${currentBank.toInt()}"
                )

                val summaryMsg = "Sinkronisasi selesai! $fixedPayables hutang & $fixedReceivables piutang telah diverifikasi 100% akurat."
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete(summaryMsg)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onComplete("Sinkronisasi gagal: ${e.message}")
                }
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
            if (result.isSuccess) {
                com.example.util.AppBackupUtils.saveContinuousSnapshot(getApplication(), db)
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                result.fold(
                    onSuccess = { msg -> onResult(true, msg) },
                    onFailure = { err -> onResult(false, err.message ?: "Gagal restore") }
                )
            }
        }
    }

    fun restoreFromLatestAutoSnapshot(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val result = com.example.util.AppBackupUtils.restoreFromLatestAutoSnapshot(getApplication(), db)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    result.fold(
                        onSuccess = { msg -> onResult(true, msg) },
                        onFailure = { err -> onResult(false, err.message ?: "Gagal restore") }
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, "Gagal memulihkan snapshot: ${e.localizedMessage ?: "Terjadi kesalahan"}")
                }
            }
        }
    }

    fun createSnapshotNow(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                com.example.util.AppBackupUtils.saveContinuousSnapshot(getApplication(), db)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(true, "Snapshot cadangan berhasil dibuat dan disimpan.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, "Gagal membuat snapshot: ${e.message}")
                }
            }
        }
    }

    suspend fun loadRecoveryFilesList(): List<java.io.File> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            (com.example.util.AppBackupUtils.getAllRecoveryFiles(getApplication()) +
             com.example.util.AutoBackupManager.getLocalAutoBackupFiles(getApplication()))
                .distinctBy { it.absolutePath }
                .sortedByDescending { it.lastModified() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun loadStandardSampleStoreData(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val result = com.example.util.AppBackupUtils.loadStandardSampleStoreData(db)
            if (result.isSuccess) {
                com.example.util.AppBackupUtils.saveContinuousSnapshot(getApplication(), db)
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                result.fold(
                    onSuccess = { msg -> onResult(true, msg) },
                    onFailure = { err -> onResult(false, err.message ?: "Gagal memuat") }
                )
            }
        }
    }

    fun getAllRecoveryFiles(): List<java.io.File> {
        return com.example.util.AppBackupUtils.getAllRecoveryFiles(getApplication())
    }

    fun saveCurrentContinuousSnapshot() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            com.example.util.AppBackupUtils.saveContinuousSnapshot(getApplication(), db)
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
