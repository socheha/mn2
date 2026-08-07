package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.room.withTransaction
import com.example.data.AppDatabase
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
import com.example.data.entity.TransactionHistoryLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object AppBackupUtils {

    /**
     * Export list of items to a CSV / Excel format string
     */
    fun generateItemsCsvString(items: List<ItemEntity>): String {
        val sb = StringBuilder()
        sb.append("Kode Barang,Nama Barang,Total Stok,Stok Gudang,Stok Toko,Harga Modal (Rp),Keterangan\n")
        for (item in items) {
            val kode = sanitizeCsvField(item.kodeBarang)
            val nama = sanitizeCsvField(item.namaBarang)
            val totalStok = item.totalStokCombined
            val stokGudang = item.actualStokUtama
            val stokToko = item.actualStokCabang
            val hargaModal = item.hargaModal
            val ket = sanitizeCsvField(item.keterangan)

            sb.append("\"$kode\",\"$nama\",$totalStok,$stokGudang,$stokToko,$hargaModal,\"$ket\"\n")
        }
        return sb.toString()
    }

    private fun sanitizeCsvField(field: String): String {
        return field.replace("\"", "\"\"")
    }

    /**
     * Save CSV/Excel backup file of Items directly into the device's preferred storage location
     */
    suspend fun saveCsvToPreferredStorage(context: Context, items: List<ItemEntity>): String = withContext(Dispatchers.IO) {
        val csvContent = generateItemsCsvString(items)
        val dateStamp = Formatters.getCurrentDateFormatted().replace("-", "")
        val timeStamp = (System.currentTimeMillis() % 10000).toString()
        val fileName = "Backup_Data_Barang_SmartStock_${dateStamp}_${timeStamp}.csv"
        val bytes = csvContent.toByteArray(Charsets.UTF_8)

        val result = StorageLocationManager.writeBytesToPreferredStorage(context, fileName, "text/csv", bytes)
        result.second
    }

    /**
     * Save CSV/Excel backup file of Items directly into the device's public Downloads directory
     */
    fun saveCsvToDownloads(context: Context, items: List<ItemEntity>): String {
        val csvContent = generateItemsCsvString(items)
        val dateStamp = Formatters.getCurrentDateFormatted().replace("-", "")
        val timeStamp = (System.currentTimeMillis() % 10000).toString()
        val fileName = "Backup_Data_Barang_SmartStock_${dateStamp}_${timeStamp}.csv"

        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        os.write(csvContent.toByteArray(Charsets.UTF_8))
                    }
                    "File Excel/CSV berhasil disimpan ke Folder Download ($fileName)"
                } else {
                    throw Exception("Gagal membuat file di folder Downloads")
                }
            } else {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { os ->
                    os.write(csvContent.toByteArray(Charsets.UTF_8))
                }
                "File Excel/CSV berhasil disimpan di Folder Download (${file.name})"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            shareItemsCsvFile(context, items)
            "Menampilkan menu simpan/bagikan file Excel: ${e.message}"
        }
    }

    /**
     * Share or save CSV file of Items
     */
    fun shareItemsCsvFile(context: Context, items: List<ItemEntity>) {
        try {
            val csvContent = generateItemsCsvString(items)
            val fileName = "Backup_Data_Barang_${Formatters.getCurrentDateFormatted().replace("-", "")}.csv"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use {
                it.write(csvContent.toByteArray(Charsets.UTF_8))
            }

            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Backup Data Barang SmartStock")
                putExtra(Intent.EXTRA_TEXT, "Berikut file backup data barang format CSV/Excel (${items.size} item):")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Bagikan / Simpan Backup Excel Barang"))
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuat file backup: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Full Database JSON Export
     */
    suspend fun exportDatabaseToJson(db: AppDatabase): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "SmartStock")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("exportedDate", Formatters.getCurrentDateFormatted())

        // 1. Items
        val itemsArr = JSONArray()
        val items = db.itemDao().getAllItemsList()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("kodeBarang", item.kodeBarang)
                put("namaBarang", item.namaBarang)
                put("stok", item.stok)
                put("stokTokoUtama", item.stokTokoUtama)
                put("stokTokoCabang", item.stokTokoCabang)
                put("hargaModal", item.hargaModal)
                put("keterangan", item.keterangan)
                put("updatedAt", item.updatedAt)
            }
            itemsArr.put(obj)
        }
        root.put("items", itemsArr)

        // 2. Sales Transactions & Items
        val salesTxArr = JSONArray()
        val salesItemsArr = JSONArray()
        val salesTxs = db.salesDao().getAllTransactionsList()
        for (tx in salesTxs) {
            val obj = JSONObject().apply {
                put("id", tx.id)
                put("tanggal", tx.tanggal)
                put("totalUangPenjualan", tx.totalUangPenjualan)
                put("totalItemTerjual", tx.totalItemTerjual)
                put("catatan", tx.catatan)
                put("metodePembayaran", tx.metodePembayaran)
                put("namaToko", tx.namaToko)
                put("timestamp", tx.timestamp)
            }
            salesTxArr.put(obj)

            val sItems = db.salesDao().getItemsForTransaction(tx.id)
            for (si in sItems) {
                val siObj = JSONObject().apply {
                    put("id", si.id)
                    put("transactionId", si.transactionId)
                    put("itemId", si.itemId)
                    put("kodeBarang", si.kodeBarang)
                    put("namaBarang", si.namaBarang)
                    put("jumlahTerjual", si.jumlahTerjual)
                    put("hargaSatuan", si.hargaSatuan)
                    put("totalHarga", si.totalHarga)
                }
                salesItemsArr.put(siObj)
            }
        }
        root.put("salesTransactions", salesTxArr)
        root.put("salesItems", salesItemsArr)

        // 3. Incoming Transactions & Items
        val incTxArr = JSONArray()
        val incItemsArr = JSONArray()
        val incTxs = db.incomingDao().getAllTransactionsList()
        for (tx in incTxs) {
            val obj = JSONObject().apply {
                put("id", tx.id)
                put("tanggal", tx.tanggal)
                put("namaSupplier", tx.namaSupplier)
                put("nomorFaktur", tx.nomorFaktur)
                put("catatan", tx.catatan)
                put("totalNilai", tx.totalNilai)
                put("totalItem", tx.totalItem)
                put("statusPembayaran", tx.statusPembayaran)
                put("timestamp", tx.timestamp)
            }
            incTxArr.put(obj)

            val iiList = db.incomingDao().getItemsForTransaction(tx.id)
            for (ii in iiList) {
                val iiObj = JSONObject().apply {
                    put("id", ii.id)
                    put("transactionId", ii.transactionId)
                    put("itemId", ii.itemId)
                    put("kodeBarang", ii.kodeBarang)
                    put("namaBarang", ii.namaBarang)
                    put("jumlahMasuk", ii.jumlahMasuk)
                    put("hargaModal", ii.hargaModal)
                }
                incItemsArr.put(iiObj)
            }
        }
        root.put("incomingTransactions", incTxArr)
        root.put("incomingItems", incItemsArr)

        // 4. Receivables & Payments
        val recArr = JSONArray()
        val recPayArr = JSONArray()
        val recs = db.customerReceivableDao().getAllReceivablesList()
        for (r in recs) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("namaPelanggan", r.namaPelanggan)
                put("nomorHp", r.nomorHp)
                put("nominalAwal", r.nominalAwal)
                put("nominalSisa", r.nominalSisa)
                put("tanggal", r.tanggal)
                put("jatuhTempo", r.jatuhTempo)
                put("catatan", r.catatan)
                put("status", r.status)
                put("timestamp", r.timestamp)
            }
            recArr.put(obj)

            val pList = db.customerReceivableDao().getPaymentsByReceivableList(r.id)
            for (p in pList) {
                val pObj = JSONObject().apply {
                    put("id", p.id)
                    put("piutangId", p.piutangId)
                    put("nominalBayar", p.nominalBayar)
                    put("tanggal", p.tanggal)
                    put("catatan", p.catatan)
                    put("metodePembayaran", p.metodePembayaran)
                    put("timestamp", p.timestamp)
                }
                recPayArr.put(pObj)
            }
        }
        root.put("customerReceivables", recArr)
        root.put("customerPayments", recPayArr)

        // 5. Payables & Payments
        val payArr = JSONArray()
        val payPayArr = JSONArray()
        val pays = db.supplierPayableDao().getAllPayablesList()
        for (p in pays) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("namaSupplier", p.namaSupplier)
                put("nominalAwal", p.nominalAwal)
                put("nominalSisa", p.nominalSisa)
                put("tanggal", p.tanggal)
                put("catatan", p.catatan)
                put("status", p.status)
                put("incomingTransactionId", p.incomingTransactionId)
                put("timestamp", p.timestamp)
            }
            payArr.put(obj)

            val pList = db.supplierPayableDao().getPaymentsByPayableList(p.id)
            for (sp in pList) {
                val pObj = JSONObject().apply {
                    put("id", sp.id)
                    put("hutangId", sp.hutangId)
                    put("nominalBayar", sp.nominalBayar)
                    put("tanggal", sp.tanggal)
                    put("catatan", sp.catatan)
                    put("metodePembayaran", sp.metodePembayaran)
                    put("timestamp", sp.timestamp)
                }
                payPayArr.put(pObj)
            }
        }
        root.put("supplierPayables", payArr)
        root.put("supplierPayments", payPayArr)

        // 6. Cash Accounts & Mutations
        val cashAccArr = JSONArray()
        val cashMutArr = JSONArray()
        val cashAccounts = db.cashDao().getAllAccountsList()
        for (acc in cashAccounts) {
            val obj = JSONObject().apply {
                put("accountType", acc.accountType)
                put("accountName", acc.accountName)
                put("saldo", acc.saldo)
                put("lastUpdated", acc.lastUpdated)
            }
            cashAccArr.put(obj)
        }
        val cashMutations = db.cashDao().getAllMutationsList()
        for (m in cashMutations) {
            val obj = JSONObject().apply {
                put("id", m.id)
                put("tanggal", m.tanggal)
                put("accountType", m.accountType)
                put("jenis", m.jenis)
                put("nominal", m.nominal)
                put("saldoSesudah", m.saldoSesudah)
                put("kategori", m.kategori)
                put("keterangan", m.keterangan)
                put("timestamp", m.timestamp)
            }
            cashMutArr.put(obj)
        }
        root.put("cashAccounts", cashAccArr)
        root.put("cashMutations", cashMutArr)

        // 7. Stock Histories
        val stockHistArr = JSONArray()
        val histories = db.stockHistoryDao().getAllHistoryList()
        for (h in histories) {
            val obj = JSONObject().apply {
                put("id", h.id)
                put("itemId", h.itemId)
                put("kodeBarang", h.kodeBarang)
                put("namaBarang", h.namaBarang)
                put("jumlahPerubahan", h.jumlahPerubahan)
                put("stokAwal", h.stokAwal)
                put("stokAkhir", h.stokAkhir)
                put("jenis", h.jenis)
                put("keterangan", h.keterangan)
                put("namaToko", h.namaToko)
                put("timestamp", h.timestamp)
            }
            stockHistArr.put(obj)
        }
        root.put("stockHistory", stockHistArr)

        // 8. Transaction History Logs
        val logArr = JSONArray()
        val logs = db.transactionHistoryDao().getAllLogsList()
        for (l in logs) {
            val obj = JSONObject().apply {
                put("id", l.id)
                put("timestamp", l.timestamp)
                put("tanggal", l.tanggal)
                put("transactionType", l.transactionType)
                put("transactionId", l.transactionId)
                put("referenceNumber", l.referenceNumber)
                put("actionType", l.actionType)
                put("previousStatus", l.previousStatus)
                put("newStatus", l.newStatus)
                put("nominal", l.nominal)
                put("accountType", l.accountType)
                put("balanceBefore", l.balanceBefore)
                put("balanceAfter", l.balanceAfter)
                put("keterangan", l.keterangan)
                put("syncStatus", l.syncStatus)
            }
            logArr.put(obj)
        }
        root.put("transactionHistoryLogs", logArr)

        root.toString(2)
    }

    /**
     * Restore database from JSON string
     */
    suspend fun restoreDatabaseFromJson(db: AppDatabase, jsonString: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            if (!root.has("app") || root.getString("app") != "SmartStock") {
                return@withContext Result.failure(Exception("Format file backup tidak valid (Bukan SmartStock backup)."))
            }

            // Perform clear and restore inside a single database transaction
            db.withTransaction {
                db.clearAllTables()

                // 1. Items
                if (root.has("items")) {
                    val arr = root.getJSONArray("items")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val entity = ItemEntity(
                            id = obj.optLong("id", 0L),
                            kodeBarang = obj.optString("kodeBarang", ""),
                            namaBarang = obj.optString("namaBarang", ""),
                            stok = obj.optInt("stok", 0),
                            stokTokoUtama = obj.optInt("stokTokoUtama", 0),
                            stokTokoCabang = obj.optInt("stokTokoCabang", 0),
                            hargaModal = obj.optDouble("hargaModal", 0.0),
                            keterangan = obj.optString("keterangan", ""),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                        )
                        db.itemDao().insertItemDirect(entity)
                    }
                }

                // 2. Sales
                if (root.has("salesTransactions")) {
                    val arr = root.getJSONArray("salesTransactions")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val entity = SalesTransactionEntity(
                            id = obj.optLong("id", 0L),
                            tanggal = obj.optString("tanggal", ""),
                            totalUangPenjualan = obj.optDouble("totalUangPenjualan", 0.0),
                            totalItemTerjual = obj.optInt("totalItemTerjual", 0),
                            catatan = obj.optString("catatan", ""),
                            metodePembayaran = obj.optString("metodePembayaran", "Tunai"),
                            namaToko = obj.optString("namaToko", "Gudang"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                        db.salesDao().insertTransactionDirect(entity)
                    }
                }
                if (root.has("salesItems")) {
                    val arr = root.getJSONArray("salesItems")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val entity = SalesItemEntity(
                            id = obj.optLong("id", 0L),
                            transactionId = obj.optLong("transactionId", 0L),
                            itemId = obj.optLong("itemId", 0L),
                            kodeBarang = obj.optString("kodeBarang", ""),
                            namaBarang = obj.optString("namaBarang", ""),
                            jumlahTerjual = obj.optInt("jumlahTerjual", 0),
                            hargaSatuan = obj.optDouble("hargaSatuan", 0.0),
                            totalHarga = obj.optDouble("totalHarga", 0.0)
                        )
                        db.salesDao().insertItemDirect(entity)
                    }
                }

                // 3. Incoming
                if (root.has("incomingTransactions")) {
                    val arr = root.getJSONArray("incomingTransactions")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val entity = IncomingTransactionEntity(
                            id = obj.optLong("id", 0L),
                            tanggal = obj.optString("tanggal", ""),
                            namaSupplier = obj.optString("namaSupplier", ""),
                            nomorFaktur = obj.optString("nomorFaktur", ""),
                            catatan = obj.optString("catatan", ""),
                            totalNilai = obj.optDouble("totalNilai", 0.0),
                            totalItem = obj.optInt("totalItem", 0),
                            statusPembayaran = obj.optString("statusPembayaran", "Tunai"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                        db.incomingDao().insertTransactionDirect(entity)
                    }
                }
                if (root.has("incomingItems")) {
                    val arr = root.getJSONArray("incomingItems")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val entity = IncomingItemEntity(
                            id = obj.optLong("id", 0L),
                            transactionId = obj.optLong("transactionId", 0L),
                            itemId = obj.optLong("itemId", 0L),
                            kodeBarang = obj.optString("kodeBarang", ""),
                            namaBarang = obj.optString("namaBarang", ""),
                            jumlahMasuk = obj.optInt("jumlahMasuk", 0),
                            hargaModal = obj.optDouble("hargaModal", 0.0)
                        )
                        db.incomingDao().insertItemDirect(entity)
                    }
                }

                // 4. Receivables
                if (root.has("customerReceivables")) {
                    val arr = root.getJSONArray("customerReceivables")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val entity = CustomerReceivableEntity(
                            id = obj.optLong("id", 0L),
                            namaPelanggan = obj.optString("namaPelanggan", ""),
                            nomorHp = obj.optString("nomorHp", ""),
                            nominalAwal = obj.optDouble("nominalAwal", 0.0),
                            nominalSisa = obj.optDouble("nominalSisa", 0.0),
                            tanggal = obj.optString("tanggal", ""),
                            jatuhTempo = obj.optString("jatuhTempo", ""),
                            catatan = obj.optString("catatan", ""),
                            status = obj.optString("status", "Belum Lunas"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                        db.customerReceivableDao().insertReceivableDirect(entity)
                    }
                }
                if (root.has("customerPayments")) {
                    val arr = root.getJSONArray("customerPayments")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val entity = CustomerPaymentEntity(
                            id = obj.optLong("id", 0L),
                            piutangId = obj.optLong("piutangId", 0L),
                            nominalBayar = obj.optDouble("nominalBayar", 0.0),
                            tanggal = obj.optString("tanggal", ""),
                            catatan = obj.optString("catatan", ""),
                            metodePembayaran = obj.optString("metodePembayaran", "Tunai"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                        db.customerReceivableDao().insertPaymentDirect(entity)
                    }
                }

                // 5. Payables
                if (root.has("supplierPayables")) {
                    val arr = root.getJSONArray("supplierPayables")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val entity = SupplierPayableEntity(
                            id = obj.optLong("id", 0L),
                            namaSupplier = obj.optString("namaSupplier", ""),
                            nominalAwal = obj.optDouble("nominalAwal", 0.0),
                            nominalSisa = obj.optDouble("nominalSisa", 0.0),
                            tanggal = obj.optString("tanggal", ""),
                            catatan = obj.optString("catatan", ""),
                            status = obj.optString("status", "Belum Lunas"),
                            incomingTransactionId = obj.optLong("incomingTransactionId", 0L),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                        db.supplierPayableDao().insertPayableDirect(entity)
                    }
                }
                if (root.has("supplierPayments")) {
                    val arr = root.getJSONArray("supplierPayments")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val entity = SupplierPaymentEntity(
                            id = obj.optLong("id", 0L),
                            hutangId = obj.optLong("hutangId", 0L),
                            nominalBayar = obj.optDouble("nominalBayar", 0.0),
                            tanggal = obj.optString("tanggal", ""),
                            catatan = obj.optString("catatan", ""),
                            metodePembayaran = obj.optString("metodePembayaran", "Tunai"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                        db.supplierPayableDao().insertPaymentDirect(entity)
                    }
                }

                // 6. Cash
                if (root.has("cashAccounts")) {
                    val arr = root.getJSONArray("cashAccounts")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val entity = CashAccountEntity(
                            accountType = obj.optString("accountType", "TUNAI"),
                            accountName = obj.optString("accountName", ""),
                            saldo = obj.optDouble("saldo", 0.0),
                            lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis())
                        )
                        db.cashDao().insertOrUpdateAccount(entity)
                    }
                }
                if (root.has("cashMutations")) {
                    val arr = root.getJSONArray("cashMutations")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val entity = CashMutationEntity(
                            id = obj.optLong("id", 0L),
                            tanggal = obj.optString("tanggal", ""),
                            accountType = obj.optString("accountType", "TUNAI"),
                            jenis = obj.optString("jenis", "MASUK"),
                            nominal = obj.optDouble("nominal", 0.0),
                            saldoSesudah = obj.optDouble("saldoSesudah", 0.0),
                            kategori = obj.optString("kategori", ""),
                            keterangan = obj.optString("keterangan", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                        db.cashDao().insertMutationDirect(entity)
                    }
                }

                // 7. Stock Histories
                if (root.has("stockHistory")) {
                    val arr = root.getJSONArray("stockHistory")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val entity = StockHistoryEntity(
                            id = obj.optLong("id", 0L),
                            itemId = obj.optLong("itemId", 0L),
                            kodeBarang = obj.optString("kodeBarang", ""),
                            namaBarang = obj.optString("namaBarang", ""),
                            jumlahPerubahan = obj.optInt("jumlahPerubahan", 0),
                            stokAwal = obj.optInt("stokAwal", 0),
                            stokAkhir = obj.optInt("stokAkhir", 0),
                            jenis = obj.optString("jenis", ""),
                            keterangan = obj.optString("keterangan", ""),
                            namaToko = obj.optString("namaToko", "Gudang"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                        db.stockHistoryDao().insertHistoryDirect(entity)
                    }
                }

                // 8. Transaction History Logs
                if (root.has("transactionHistoryLogs")) {
                    val arr = root.getJSONArray("transactionHistoryLogs")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val entity = TransactionHistoryLogEntity(
                            id = obj.optLong("id", 0L),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            tanggal = obj.optString("tanggal", ""),
                            transactionType = obj.optString("transactionType", ""),
                            transactionId = obj.optLong("transactionId", 0L),
                            referenceNumber = obj.optString("referenceNumber", ""),
                            actionType = obj.optString("actionType", ""),
                            previousStatus = obj.optString("previousStatus", ""),
                            newStatus = obj.optString("newStatus", ""),
                            nominal = obj.optDouble("nominal", 0.0),
                            accountType = obj.optString("accountType", ""),
                            balanceBefore = obj.optDouble("balanceBefore", 0.0),
                            balanceAfter = obj.optDouble("balanceAfter", 0.0),
                            keterangan = obj.optString("keterangan", ""),
                            syncStatus = obj.optString("syncStatus", "SYNCED")
                        )
                        db.transactionHistoryDao().insertLog(entity)
                    }
                }
            }
            Result.success("Berhasil memulihkan data aplikasi dari backup.")
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Gagal me-restore data: ${e.message}"))
        }
    }

    /**
     * Save JSON backup file directly into the device's preferred storage location (Downloads, Documents, Custom Folder, or Internal)
     */
    suspend fun saveJsonBackupToPreferredStorage(context: Context, jsonString: String, customFileName: String? = null): String = withContext(Dispatchers.IO) {
        val dateStamp = Formatters.getCurrentDateFormatted().replace("-", "")
        val timeStamp = (System.currentTimeMillis() % 10000).toString()
        val fileName = customFileName ?: "Backup_SmartStock_Full_${dateStamp}_${timeStamp}.json"
        val bytes = jsonString.toByteArray(Charsets.UTF_8)

        val result = StorageLocationManager.writeBytesToPreferredStorage(context, fileName, "application/json", bytes)
        result.second
    }

    /**
     * Save JSON backup file directly into the device's public Downloads directory
     */
    fun saveJsonBackupToDownloads(context: Context, jsonString: String): String {
        val dateStamp = Formatters.getCurrentDateFormatted().replace("-", "")
        val timeStamp = (System.currentTimeMillis() % 10000).toString()
        val fileName = "Backup_SmartStock_Full_${dateStamp}_${timeStamp}.json"

        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        os.write(jsonString.toByteArray(Charsets.UTF_8))
                    }
                    "File backup berhasil disimpan ke folder Download ($fileName)"
                } else {
                    throw Exception("Gagal membuat file di folder Downloads")
                }
            } else {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { os ->
                    os.write(jsonString.toByteArray(Charsets.UTF_8))
                }
                "File backup berhasil disimpan di folder Download (${file.name})"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            shareJsonBackupFile(context, jsonString)
            "Menampilkan menu simpan/bagikan file: ${e.message}"
        }
    }

    /**
     * Share full database JSON backup file
     */
    fun shareJsonBackupFile(context: Context, jsonString: String) {
        try {
            val fileName = "Backup_SmartStock_Full_${Formatters.getCurrentDateFormatted().replace("-", "")}.json"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use {
                it.write(jsonString.toByteArray(Charsets.UTF_8))
            }

            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_SUBJECT, "Backup Database SmartStock")
                putExtra(Intent.EXTRA_TEXT, "Berikut file backup database lengkap aplikasi SmartStock:")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Bagikan / Simpan File Backup JSON"))
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan file backup: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Save continuous snapshot to phone internal storage so data is never lost during updates
     */
    suspend fun saveContinuousSnapshot(context: Context, db: AppDatabase): Unit = withContext(Dispatchers.IO) {
        try {
            val json = exportDatabaseToJson(db)
            val file1 = File(context.filesDir, "pre_update_snapshot.json")
            val file2 = File(context.filesDir, "auto_backup_latest.json")
            FileOutputStream(file1).use { it.write(json.toByteArray(Charsets.UTF_8)) }
            FileOutputStream(file2).use { it.write(json.toByteArray(Charsets.UTF_8)) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Retrieve all available local snapshot and auto backup files
     */
    fun getAllRecoveryFiles(context: Context): List<File> {
        val filesList = mutableListOf<File>()
        val file1 = File(context.filesDir, "pre_update_snapshot.json")
        if (file1.exists() && file1.length() > 20) filesList.add(file1)

        val file2 = File(context.filesDir, "auto_backup_latest.json")
        if (file2.exists() && file2.length() > 20 && !filesList.contains(file2)) filesList.add(file2)

        val autoDir = File(context.filesDir, "auto_backups")
        if (autoDir.exists()) {
            autoDir.listFiles { _, name -> name.endsWith(".json") }?.let {
                filesList.addAll(it)
            }
        }
        return filesList.sortedByDescending { it.lastModified() }
    }

    /**
     * Automatically restore the most recent auto-recovery snapshot
     */
    suspend fun restoreFromLatestAutoSnapshot(context: Context, db: AppDatabase): Result<String> = withContext(Dispatchers.IO) {
        val recoveryFiles = getAllRecoveryFiles(context)
        if (recoveryFiles.isEmpty()) {
            return@withContext Result.failure(Exception("Tidak ditemukan file cadangan otomatis di memori internal."))
        }

        for (file in recoveryFiles) {
            try {
                val jsonString = file.readText(Charsets.UTF_8)
                if (jsonString.isNotBlank() && jsonString.contains("SmartStock")) {
                    val res = restoreDatabaseFromJson(db, jsonString)
                    if (res.isSuccess) {
                        return@withContext Result.success("Berhasil memulihkan data dari cadangan: ${file.name}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        Result.failure(Exception("Gagal memulihkan dari cadangan otomatis."))
    }

    /**
     * Load complete standard starter sample store items & initial cash accounts
     */
    suspend fun loadStandardSampleStoreData(db: AppDatabase): Result<String> = withContext(Dispatchers.IO) {
        try {
            db.withTransaction {
                val existingItems = db.itemDao().getAllItemsList()
                if (existingItems.isEmpty()) {
                    val sampleItems = listOf(
                        ItemEntity(
                            kodeBarang = "BRG001",
                            namaBarang = "Beras Premium Ramos 5kg",
                            stok = 50,
                            stokTokoUtama = 30,
                            stokTokoCabang = 20,
                            hargaModal = 68000.0,
                            keterangan = "Gudang: 30, Toko: 20",
                            updatedAt = System.currentTimeMillis()
                        ),
                        ItemEntity(
                            kodeBarang = "BRG002",
                            namaBarang = "Minyak Goreng Bimoli 2 Liter",
                            stok = 40,
                            stokTokoUtama = 25,
                            stokTokoCabang = 15,
                            hargaModal = 34000.0,
                            keterangan = "Gudang: 25, Toko: 15",
                            updatedAt = System.currentTimeMillis()
                        ),
                        ItemEntity(
                            kodeBarang = "BRG003",
                            namaBarang = "Gula Pasir Gulaku 1kg",
                            stok = 60,
                            stokTokoUtama = 40,
                            stokTokoCabang = 20,
                            hargaModal = 15500.0,
                            keterangan = "Gudang: 40, Toko: 20",
                            updatedAt = System.currentTimeMillis()
                        ),
                        ItemEntity(
                            kodeBarang = "BRG004",
                            namaBarang = "Telur Ayam Negeri 1kg",
                            stok = 35,
                            stokTokoUtama = 20,
                            stokTokoCabang = 15,
                            hargaModal = 26000.0,
                            keterangan = "Gudang: 20, Toko: 15",
                            updatedAt = System.currentTimeMillis()
                        ),
                        ItemEntity(
                            kodeBarang = "BRG005",
                            namaBarang = "Indomie Goreng Spesial (Dus)",
                            stok = 30,
                            stokTokoUtama = 20,
                            stokTokoCabang = 10,
                            hargaModal = 112000.0,
                            keterangan = "Gudang: 20, Toko: 10",
                            updatedAt = System.currentTimeMillis()
                        ),
                        ItemEntity(
                            kodeBarang = "BRG006",
                            namaBarang = "Kopi Kapal Api Special Mix 1 Renceng",
                            stok = 45,
                            stokTokoUtama = 30,
                            stokTokoCabang = 15,
                            hargaModal = 14500.0,
                            keterangan = "Gudang: 30, Toko: 15",
                            updatedAt = System.currentTimeMillis()
                        ),
                        ItemEntity(
                            kodeBarang = "BRG007",
                            namaBarang = "Sabun Mandi Lifebuoy Total 10 (Pack)",
                            stok = 50,
                            stokTokoUtama = 35,
                            stokTokoCabang = 15,
                            hargaModal = 18000.0,
                            keterangan = "Gudang: 35, Toko: 15",
                            updatedAt = System.currentTimeMillis()
                        )
                    )

                    for (item in sampleItems) {
                        db.itemDao().insertItem(item)
                    }

                    // Setup initial cash account balances
                    db.cashDao().insertOrUpdateAccount(
                        CashAccountEntity(
                            accountType = "TUNAI",
                            accountName = "Kas Tunai Toko",
                            saldo = 1500000.0,
                            lastUpdated = System.currentTimeMillis()
                        )
                    )
                    db.cashDao().insertOrUpdateAccount(
                        CashAccountEntity(
                            accountType = "BANK",
                            accountName = "Kas Rekening Bank",
                            saldo = 5000000.0,
                            lastUpdated = System.currentTimeMillis()
                        )
                    )
                }
            }
            Result.success("Berhasil memuat 7 produk standar toko dan saldo kas awal.")
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Gagal memuat data standar: ${e.message}"))
        }
    }

    /**
     * Copy text to clipboard
     */
    fun copyToClipboard(context: Context, text: String, label: String = "Backup Teks") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Teks $label berhasil disalin ke clipboard", Toast.LENGTH_SHORT).show()
    }
}

