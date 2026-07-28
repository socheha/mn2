package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.BuildConfig
import com.example.data.entity.ItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

data class ScannedReceiptItem(
    val namaBarang: String,
    val jumlah: Int,
    val hargaSatuan: Double,
    val totalHarga: Double,
    val matchedItemId: Long? = null,
    val matchedKodeBarang: String? = null,
    val matchedNamaBarang: String? = null,
    val isMatchedWithDatabase: Boolean = false
)

data class ScannedReceiptResult(
    val namaSupplier: String = "",
    val nomorFaktur: String = "",
    val tanggal: String = "",
    val totalNota: Double = 0.0,
    val items: List<ScannedReceiptItem> = emptyList(),
    val rawTextResponse: String = ""
)

object GeminiReceiptScanner {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Scans a receipt photo Uri using Gemini 3.5 Flash Vision API with strict exact transcription.
     */
    suspend fun scanReceiptUri(
        context: Context,
        imageUri: Uri,
        databaseItems: List<ItemEntity> = emptyList()
    ): Result<ScannedReceiptResult> = withContext(Dispatchers.IO) {
        try {
            val bitmap = ImagePreprocessor.loadAndPreprocessUri(context, imageUri)
                ?: return@withContext Result.failure(Exception("Gagal membaca file gambar nota."))
            scanReceiptBitmap(bitmap, databaseItems)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Scans a receipt Bitmap using Gemini 3.5 Flash Vision API with strict exact transcription.
     */
    suspend fun scanReceiptBitmap(
        bitmap: Bitmap,
        databaseItems: List<ItemEntity> = emptyList()
    ): Result<ScannedReceiptResult> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(Exception("Kunci API Gemini belum dikonfigurasi di Secrets Panel."))
        }

        try {
            val preprocessedBitmap = ImagePreprocessor.preprocessForOcr(bitmap)
            val base64Image = bitmapToBase64(preprocessedBitmap)

            val dbStockText = if (databaseItems.isNotEmpty()) {
                databaseItems.joinToString("\n") { "- ${it.namaBarang} (Kode: ${it.kodeBarang})" }
            } else {
                "Belum ada barang di database stok."
            }

            val prompt = """
                Anda adalah AI OCR tingkat tinggi yang sangat ahli dalam menganalisis dan membaca segala jenis nota/bon penjualan toko (termasuk struk kasir thermal, nota cetak komputer, faktur A4, maupun nota tulisan tangan manual).

                Tugas Utama:
                Membaca dan merekap seluruh data dari foto nota secara amat teliti PER BARIS SEJAJAR.

                PETUNJUK MEMBACA HEADER NOTA:
                1. NAMA BON / VENDOR:
                   - Toko/vendor penerbit nota di bagian paling atas/kiri (misal: "PHILIPS", "MULTI ELEKTRINDO", "TOKO ABADI") -> masukkan ke "nama_bon" atau "namaSupplier".
                2. PELANGGAN & ALAMAT:
                   - Teks di bagian "Kepada Yth" atau "Pelanggan" atau "Pembeli":
                     * Nama pelanggan/toko (sebelum koma) -> masukkan ke "nama_pelanggan".
                     * Alamat/lokasi (setelah koma) -> masukkan ke "alamat".
                3. TANGGAL & NOMOR NOTA:
                   - Nomor nota/faktur/invoice -> "nomorFaktur".
                   - Tanggal nota -> "tanggal" (format YYYY-MM-DD jika dapat terbaca).

                PETUNJUK MEMBACA TABEL PENJUALAN (PROSES PER BARIS SEJAJAR):
                Pahami format tata letak nota yang sedang dipindai:
                - VARIASI 1 (Nota Tulisan Tangan / Manual):
                  Format kolom: [QTY] | [NAMA BARANG] | [HARGA] | [JUMLAH]
                  * Kolom paling kiri adalah QTY -> "qty"
                  * Kolom kedua adalah Nama Barang -> "nama_barang"
                  * Kolom ketiga adalah Harga Satuan -> "harga_satuan"
                  * Kolom keempat adalah Total Nominal -> "total_harga"

                - VARIASI 2 (Nota Cetak Komputer / Faktur):
                  Format kolom: [NO] | [NAMA BARANG] | [QTY] | [HARGA] | [JUMLAH]
                  * ABAIKAN kolom "No" (DILARANG dimasukkan ke nama barang atau qty).
                  * Kolom Nama Barang -> "nama_barang"
                  * Kolom QTY -> "qty"
                  * Kolom Harga -> "harga_satuan"
                  * Kolom Jumlah -> "total_harga"

                - VARIASI 3 (Struk Thermal Kasir / Minimarket):
                  * Baris 1: Nama Barang
                  * Baris 2: [QTY] x [HARGA SATUAN] = [TOTAL HARGA]

                PETUNJUK PENCANGKOKAN STOK DATABASE:
                - Bandingkan "nama_barang" hasil OCR dengan DAFTAR DATABASE STOK di bawah.
                - Jika menemukan barang yang sangat cocok/mirip (misal typo tulisan tangan atau beda kapital), isi "nama_stok" dengan nama barang persis dari database.
                - Jika tidak ada yang cocok, isi "nama_stok" dengan null.

                Aturan Ketat:
                1. Pastikan nilai perkalian konsisten: QTY x harga_satuan = total_harga.
                2. DILARANG mengarang data. Jika angka atau kata tidak terbaca, perkirakan angka paling logis berdasarkan rumus matematis nota.
                3. Keluarkan HANYA JSON valid.

                Format JSON:
                {
                  "nama_bon": "NAMA TOKO VENDOR",
                  "nama_pelanggan": "NAMA PELANGGAN",
                  "alamat": "ALAMAT PELANGGAN",
                  "nomorFaktur": "NO NOTA",
                  "tanggal": "YYYY-MM-DD",
                  "items": [
                    {
                      "nama_barang": "Nama barang di nota",
                      "qty": 1,
                      "harga_satuan": 10000,
                      "total_harga": 10000,
                      "nama_stok": "Nama barang cocok di database / null",
                      "confidence": 0.95,
                      "catatan": ""
                    }
                  ],
                  "total_penjualan": 10000
                }

                DAFTAR DATABASE STOK:
                $dbStockText
            """.trimIndent()

            val jsonRequest = JSONObject().apply {
                val contents = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val parts = JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                            put(JSONObject().put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            }))
                        }
                        put("parts", parts)
                    }
                    put(contentObj)
                }
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("responseMimeType", "application/json")
                })
            }

            val modelsToTry = listOf("gemini-3.5-flash", "gemini-flash-latest", "gemini-2.5-flash")
            var responseString = ""
            var isSuccess = false
            var lastErrorCode = 0

            for (modelName in modelsToTry) {
                val requestUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                val body = jsonRequest.toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(requestUrl)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                responseString = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    isSuccess = true
                    break
                } else {
                    lastErrorCode = response.code
                }
            }

            if (!isSuccess) {
                return@withContext Result.failure(Exception("Gagal memanggil AI Gemini ($lastErrorCode): $responseString"))
            }

            val jsonResp = JSONObject(responseString)
            val candidates = jsonResp.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textPart = parts?.optJSONObject(0)?.optString("text") ?: ""

            if (textPart.isBlank()) {
                return@withContext Result.failure(Exception("AI Gemini tidak mengembalikan teks hasil analisis."))
            }

            val parsedResult = parseGeminiResponseText(textPart, databaseItems)
            Result.success(parsedResult)

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun parseGeminiResponseText(rawText: String, databaseItems: List<ItemEntity>): ScannedReceiptResult {
        var cleanJson = rawText.trim()
        if (cleanJson.startsWith("```json")) {
            cleanJson = cleanJson.removePrefix("```json")
        } else if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.removePrefix("```")
        }
        if (cleanJson.endsWith("```")) {
            cleanJson = cleanJson.removeSuffix("```")
        }
        cleanJson = cleanJson.trim()

        return try {
            val obj = JSONObject(cleanJson)
            val namaBon = obj.optString("nama_bon", "").ifBlank { obj.optString("namaSupplier", "") }
            val namaPelanggan = obj.optString("nama_pelanggan", "").ifBlank { obj.optString("nama_toko", "") }
            val alamat = obj.optString("alamat", "")

            val finalSupplier = when {
                namaPelanggan.isNotBlank() && alamat.isNotBlank() -> "$namaPelanggan, $alamat"
                namaPelanggan.isNotBlank() -> namaPelanggan
                namaBon.isNotBlank() -> namaBon
                else -> obj.optString("namaSupplier", "")
            }

            val nomorFaktur = obj.optString("nomorFaktur", "")
            val tanggal = obj.optString("tanggal", "")
            
            var totalNota = obj.optDouble("total_penjualan", -1.0)
            if (totalNota < 0) {
                totalNota = obj.optDouble("totalNota", 0.0)
            }

            val itemsList = mutableListOf<ScannedReceiptItem>()
            val itemsArr = obj.optJSONArray("items")
            if (itemsArr != null) {
                for (i in 0 until itemsArr.length()) {
                    val itemObj = itemsArr.optJSONObject(i) ?: continue

                    // Support both nama_barang & namaBarang
                    var namaBarangRaw = itemObj.optString("nama_barang", "").trim()
                    if (namaBarangRaw.isBlank()) {
                        namaBarangRaw = itemObj.optString("namaBarang", "").trim()
                    }
                    if (namaBarangRaw.isBlank()) continue

                    // Support qty & jumlah (as quantity)
                    val qtyVal = if (itemObj.has("qty")) itemObj.optInt("qty", 1) 
                        else itemObj.optInt("jumlah", 1)
                    val jumlahQty = qtyVal.coerceAtLeast(1)

                    // Support harga_satuan & hargaSatuan & harga
                    var hargaSatuan = when {
                        itemObj.has("harga_satuan") -> itemObj.optDouble("harga_satuan", 0.0)
                        itemObj.has("hargaSatuan") -> itemObj.optDouble("hargaSatuan", 0.0)
                        itemObj.has("harga") -> itemObj.optDouble("harga", 0.0)
                        else -> 0.0
                    }

                    // Support total_harga & totalHarga & jumlah (as total rupiah)
                    var totalHarga = when {
                        itemObj.has("total_harga") -> itemObj.optDouble("total_harga", 0.0)
                        itemObj.has("totalHarga") -> itemObj.optDouble("totalHarga", 0.0)
                        itemObj.has("jumlah") && itemObj.has("qty") -> itemObj.optDouble("jumlah", 0.0)
                        else -> itemObj.optDouble("jumlah", 0.0)
                    }

                    if (hargaSatuan <= 0 && totalHarga > 0) {
                        hargaSatuan = totalHarga / jumlahQty
                    } else if (totalHarga <= 0 && hargaSatuan > 0) {
                        totalHarga = hargaSatuan * jumlahQty
                    }

                    // Stock matching based on nama_stok or namaBarangRaw
                    val namaStokFromAi = if (itemObj.has("nama_stok") && !itemObj.isNull("nama_stok")) {
                        itemObj.optString("nama_stok", "").trim()
                    } else null

                    val matchedByMatcher = if (!namaStokFromAi.isNullAndBlank()) {
                        ItemMatcher.findBestMatch(namaStokFromAi!!, databaseItems)
                            ?: ItemMatcher.findBestMatch(namaBarangRaw, databaseItems)
                    } else {
                        ItemMatcher.findBestMatch(namaBarangRaw, databaseItems)
                    }

                    itemsList.add(
                        ScannedReceiptItem(
                            namaBarang = namaBarangRaw,
                            jumlah = jumlahQty,
                            hargaSatuan = if (matchedByMatcher != null && hargaSatuan <= 0) matchedByMatcher.hargaModal else hargaSatuan,
                            totalHarga = if (matchedByMatcher != null && totalHarga <= 0) (hargaSatuan * jumlahQty) else totalHarga,
                            matchedItemId = matchedByMatcher?.id,
                            matchedKodeBarang = matchedByMatcher?.kodeBarang,
                            matchedNamaBarang = matchedByMatcher?.namaBarang,
                            isMatchedWithDatabase = matchedByMatcher != null
                        )
                    )
                }
            }

            val calculatedTotal = if (totalNota > 0) totalNota else itemsList.sumOf { it.totalHarga }

            ScannedReceiptResult(
                namaSupplier = finalSupplier,
                nomorFaktur = nomorFaktur,
                tanggal = tanggal,
                totalNota = calculatedTotal,
                items = itemsList,
                rawTextResponse = rawText
            )
        } catch (e: Exception) {
            ScannedReceiptResult(rawTextResponse = rawText)
        }
    }

    private fun String?.isNullAndBlank(): Boolean {
        return this == null || this.isBlank() || this.equals("null", ignoreCase = true)
    }

    private fun loadAndScaleBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val original = BitmapFactory.decodeStream(inputStream) ?: return null

            val maxDimension = 1280
            val width = original.width
            val height = original.height

            if (width <= maxDimension && height <= maxDimension) {
                return original
            }

            val aspectRatio = width.toFloat() / height.toFloat()
            val newWidth: Int
            val newHeight: Int

            if (width > height) {
                newWidth = maxDimension
                newHeight = (maxDimension / aspectRatio).toInt()
            } else {
                newHeight = maxDimension
                newWidth = (maxDimension * aspectRatio).toInt()
            }

            Bitmap.createScaledBitmap(original, newWidth, newHeight, true)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
