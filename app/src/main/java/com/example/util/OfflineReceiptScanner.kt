package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.data.entity.ItemEntity
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

object OfflineReceiptScanner {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private suspend fun <T> Task<T>.awaitTask(): T =
        suspendCancellableCoroutine { continuation ->
            addOnSuccessListener { result ->
                if (continuation.isActive) continuation.resume(result)
            }
            addOnFailureListener { exception ->
                if (continuation.isActive) continuation.resumeWith(Result.failure(exception))
            }
        }

    /**
     * Performs offline text recognition on a Bitmap using MLKit.
     */
    suspend fun scanReceiptBitmapOffline(
        bitmap: Bitmap,
        databaseItems: List<ItemEntity> = emptyList()
    ): Result<ScannedReceiptResult> = withContext(Dispatchers.IO) {
        try {
            val preprocessed = ImagePreprocessor.preprocessForOcr(bitmap)
            val image = InputImage.fromBitmap(preprocessed, 0)
            val visionText = recognizer.process(image).awaitTask()
            val rawText = visionText.text

            if (rawText.isBlank()) {
                return@withContext Result.failure(Exception("Tidak ada teks terdeteksi dalam foto nota."))
            }

            val items = parseTextLinesToItems(rawText, databaseItems)
            val totalEstimate = items.sumOf { it.totalHarga }

            Result.success(
                ScannedReceiptResult(
                    namaSupplier = extractSupplierOffline(rawText),
                    nomorFaktur = extractFakturOffline(rawText),
                    tanggal = Formatters.getCurrentDateFormatted(),
                    totalNota = totalEstimate,
                    items = items,
                    rawTextResponse = rawText
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Gagal scan offline: ${e.message}"))
        }
    }

    /**
     * Performs offline text recognition on a Uri using MLKit.
     */
    suspend fun scanReceiptUriOffline(
        context: Context,
        imageUri: Uri,
        databaseItems: List<ItemEntity> = emptyList()
    ): Result<ScannedReceiptResult> = withContext(Dispatchers.IO) {
        try {
            val preprocessedBitmap = ImagePreprocessor.loadAndPreprocessUri(context, imageUri)
            val image = if (preprocessedBitmap != null) {
                InputImage.fromBitmap(preprocessedBitmap, 0)
            } else {
                InputImage.fromFilePath(context, imageUri)
            }
            val visionText = recognizer.process(image).awaitTask()
            val rawText = visionText.text

            if (rawText.isBlank()) {
                return@withContext Result.failure(Exception("Tidak ada teks terdeteksi dalam foto nota."))
            }

            val items = parseTextLinesToItems(rawText, databaseItems)
            val totalEstimate = items.sumOf { it.totalHarga }

            Result.success(
                ScannedReceiptResult(
                    namaSupplier = extractSupplierOffline(rawText),
                    nomorFaktur = extractFakturOffline(rawText),
                    tanggal = Formatters.getCurrentDateFormatted(),
                    totalNota = totalEstimate,
                    items = items,
                    rawTextResponse = rawText
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Gagal scan offline: ${e.message}"))
        }
    }

    fun parseTextLinesToItems(
        rawText: String,
        databaseItems: List<ItemEntity> = emptyList()
    ): List<ScannedReceiptItem> {
        val lines = rawText.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        val resultItems = mutableListOf<ScannedReceiptItem>()

        for (line in lines) {
            val upper = line.uppercase()
            if (upper.contains("TOTAL") || upper.contains("BAYAR") || upper.contains("KEMBALI") ||
                upper.contains("CASH") || upper.contains("TERIMA KASIH") || upper.contains("SUBTOTAL") ||
                upper.contains("TUNAI") || upper.startsWith("NO.") || upper.startsWith("NAMA BARANG")) {
                continue
            }

            // 1. Cek apakah ada nomor di awal baris (misal "2 Tplink wr840n 150.000 300.000" atau "1 Totolink...")
            var leadingQtyCandidate: Int? = null
            var lineClean = line.trim()
            val leadingNumMatch = Regex("""^\s*(\d{1,3})\s+(.*)$""").find(lineClean)
            if (leadingNumMatch != null) {
                val candidate = leadingNumMatch.groupValues[1].toIntOrNull()
                val restText = leadingNumMatch.groupValues[2]
                if (candidate != null && candidate in 1..999) {
                    leadingQtyCandidate = candidate
                    lineClean = restText
                }
            } else {
                // Hapus nomor urut dengan titik/titik dua (misal "1. ", "2) ")
                lineClean = line.replace(Regex("""^\s*\d{1,2}[\s.|)]+"""), "").trim()
            }
            if (lineClean.isBlank()) continue

            // 2. Ekstrak pola harga Rupiah atau angka besar di bagian kanan/akhir baris (misal Rp135.000 atau Rp1.350.000 atau 135.000)
            val priceRegex = Regex("""(?i)(?:Rp\s*)?(\d{1,3}(?:[.,]\d{3})+(?:[.,]\d{1,2})?|\d{4,9})""")
            val priceMatches = priceRegex.findAll(lineClean).toList()

            var hargaSatuan = 0.0
            var totalHarga = 0.0
            var jumlah = leadingQtyCandidate ?: 1

            if (priceMatches.isNotEmpty()) {
                val parsedPrices = priceMatches.mapNotNull { m ->
                    val numStr = m.groupValues[1].replace(".", "").replace(",", ".")
                    numStr.toDoubleOrNull()
                }.filter { it > 0 }

                if (parsedPrices.size >= 2) {
                    hargaSatuan = parsedPrices[parsedPrices.size - 2]
                    totalHarga = parsedPrices.last()
                } else if (parsedPrices.size == 1) {
                    totalHarga = parsedPrices[0]
                    hargaSatuan = totalHarga
                }
            }

            // Verify if leadingQtyCandidate matches math (hargaSatuan * leadingQtyCandidate == totalHarga)
            if (leadingQtyCandidate != null && hargaSatuan > 0 && totalHarga > 0) {
                if (Math.abs((hargaSatuan * leadingQtyCandidate) - totalHarga) < 100) {
                    jumlah = leadingQtyCandidate
                }
            }

            // 3. Ekstrak QTY (misal "10", "8", "5", "20" atau "10 pcs")
            val qtyRegex = Regex("""\b(\d{1,3})\s*(x|pcs|unit|bh|pack)?\b""", RegexOption.IGNORE_CASE)
            val qtyMatches = qtyRegex.findAll(lineClean).toList()
            for (m in qtyMatches) {
                val qCandidate = m.groupValues[1].toIntOrNull()
                if (qCandidate != null && qCandidate in 1..999) {
                    // Pastikan QTY sanity check vs harga
                    if (totalHarga > 0 && hargaSatuan > 0 && Math.abs((hargaSatuan * qCandidate) - totalHarga) < 100) {
                        jumlah = qCandidate
                        break
                    } else if (m.groupValues[2].isNotBlank()) {
                        jumlah = qCandidate
                        break
                    } else if (qCandidate != 1) {
                        jumlah = qCandidate
                    }
                }
            }

            // Hitung kalkulasi akhir jika salah satu 0
            if (hargaSatuan <= 0 && totalHarga > 0) {
                hargaSatuan = totalHarga / jumlah
            } else if (totalHarga <= 0 && hargaSatuan > 0) {
                totalHarga = hargaSatuan * jumlah
            }

            // 4. Dapatkan nama barang dengan menghapus HANYA harga/qty di bagian kanan baris,
            // SEHINGGA NAMA BARANG BERISI MODEL/ANGKA (N200RE v4, WR840N, 2F4E, 12v 1.5a) TETAP UTUH!
            var namaBarang = lineClean

            // Hapus blok harga Rupiah dari namaBarang
            for (pm in priceMatches) {
                namaBarang = namaBarang.replace(pm.value, "").trim()
            }

            // Jika ada QTY mandiri di akhir kata namaBarang, bersihkan
            namaBarang = namaBarang.replace(Regex("""\s+\d{1,3}\s*$"""), "").trim()

            // Jika nama barang menjadi kosong, gunakan teks asli
            if (namaBarang.length < 2) {
                namaBarang = lineClean
            }

            val matched = ItemMatcher.findBestMatch(namaBarang, databaseItems)

            resultItems.add(
                ScannedReceiptItem(
                    namaBarang = namaBarang,
                    jumlah = jumlah,
                    hargaSatuan = if (matched != null && hargaSatuan <= 0) matched.hargaModal else hargaSatuan,
                    totalHarga = if (matched != null && totalHarga <= 0) matched.hargaModal * jumlah else totalHarga,
                    matchedItemId = matched?.id,
                    matchedKodeBarang = matched?.kodeBarang,
                    matchedNamaBarang = matched?.namaBarang,
                    isMatchedWithDatabase = matched != null
                )
            )
        }

        return resultItems.take(30)
    }

    fun extractSupplierOffline(text: String): String {
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isNotEmpty()) {
            val firstLine = lines.first()
            if (!firstLine.uppercase().contains("NOTA") && !firstLine.uppercase().contains("FAKTUR")) {
                return firstLine
            }
        }
        return ""
    }

    fun extractFakturOffline(text: String): String {
        val match = Regex("""(?:NO|FAKTUR|NOTA|INV)[\s.:#]*([A-Z0-9\-/]+)""", RegexOption.IGNORE_CASE).find(text)
        return match?.groupValues?.getOrNull(1) ?: ""
    }
}
