package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

data class ImportedItemRaw(
    val kodeBarang: String = "",
    val namaBarang: String = "",
    val stok: Int = 0,
    val hargaModal: Double = 0.0,
    val keterangan: String = ""
)

object ExcelImportUtils {

    /**
     * Reads items from a Uri (XLSX, XLS, or CSV/TSV) or plain text.
     */
    fun parseItemsFromUri(context: Context, uri: Uri): List<ImportedItemRaw> {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: ""
        val fileName = uri.lastPathSegment?.lowercase() ?: ""

        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                if (fileName.endsWith(".xlsx") || mimeType.contains("spreadsheetml")) {
                    parseXlsxStream(inputStream)
                } else if (fileName.endsWith(".csv") || fileName.endsWith(".tsv") || fileName.endsWith(".txt") || mimeType.contains("csv") || mimeType.contains("plain")) {
                    parseCsvOrTextStream(inputStream)
                } else {
                    // Try parsing as XLSX first, fallback to CSV
                    try {
                        parseXlsxStream(inputStream)
                    } catch (e: Exception) {
                        contentResolver.openInputStream(uri)?.use { fallbackStream ->
                            parseCsvOrTextStream(fallbackStream)
                        } ?: emptyList()
                    }
                }
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Parses raw pasted text copied from Excel or Google Sheets (tab or comma separated).
     */
    fun parseItemsFromPastedText(pastedText: String): List<ImportedItemRaw> {
        if (pastedText.isBlank()) return emptyList()
        val lines = pastedText.split("\r\n", "\n", "\r").filter { it.isNotBlank() }
        val grid = lines.map { line ->
            when {
                line.contains("\t") -> line.split("\t")
                line.contains(";") -> parseCsvLine(line, ';')
                else -> parseCsvLine(line, ',')
            }
        }
        return processGridToItems(grid)
    }

    private fun parseCsvOrTextStream(inputStream: InputStream): List<ImportedItemRaw> {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val lines = reader.readLines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        // Determine separator
        val sampleLine = lines.firstOrNull() ?: ""
        val separator = when {
            sampleLine.contains("\t") -> '\t'
            sampleLine.contains(";") -> ';'
            else -> ','
        }

        val grid = lines.map { parseCsvLine(it, separator) }
        return processGridToItems(grid)
    }

    private fun parseCsvLine(line: String, separator: Char): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (ch in line) {
            if (ch == '"') {
                inQuotes = !inQuotes
            } else if (ch == separator && !inQuotes) {
                tokens.add(sb.toString().trim())
                sb.clear()
            } else {
                sb.append(ch)
            }
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    /**
     * Parses OOXML .xlsx files without external POI dependency by extracting zip contents.
     */
    private fun parseXlsxStream(inputStream: InputStream): List<ImportedItemRaw> {
        val sharedStrings = mutableListOf<String>()
        val sheetRows = mutableListOf<List<String>>()

        val zip = ZipInputStream(inputStream)
        var entry = zip.nextEntry

        // First pass: locate sharedStrings and sheet1
        var sheetBytes: ByteArray? = null
        var sharedStringsBytes: ByteArray? = null

        while (entry != null) {
            val name = entry.name.lowercase()
            if (name.endsWith("xl/sharedstrings.xml")) {
                sharedStringsBytes = zip.readBytes()
            } else if (name.endsWith("xl/worksheets/sheet1.xml") || (sheetBytes == null && name.contains("xl/worksheets/sheet"))) {
                sheetBytes = zip.readBytes()
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }

        // Parse sharedStrings if present
        if (sharedStringsBytes != null) {
            parseSharedStringsXml(sharedStringsBytes.inputStream(), sharedStrings)
        }

        // Parse sheet xml
        if (sheetBytes != null) {
            parseSheetXml(sheetBytes.inputStream(), sharedStrings, sheetRows)
        }

        return processGridToItems(sheetRows)
    }

    private fun parseSharedStringsXml(inputStream: InputStream, targetList: MutableList<String>) {
        try {
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, "UTF-8")
            var eventType = parser.eventType
            val currentText = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name == "t") {
                            currentText.clear()
                        }
                    }
                    XmlPullParser.TEXT -> {
                        currentText.append(parser.text)
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "t") {
                            targetList.add(currentText.toString())
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseSheetXml(
        inputStream: InputStream,
        sharedStrings: List<String>,
        targetGrid: MutableList<List<String>>
    ) {
        try {
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, "UTF-8")
            var eventType = parser.eventType

            val currentRow = mutableMapOf<Int, String>()
            var currentCellRef = ""
            var currentType = ""
            var isCellValue = false
            val cellText = StringBuilder()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name
                        if (name == "row") {
                            currentRow.clear()
                        } else if (name == "c") {
                            currentCellRef = parser.getAttributeValue(null, "r") ?: ""
                            currentType = parser.getAttributeValue(null, "t") ?: ""
                            cellText.clear()
                        } else if (name == "v" || name == "t") {
                            isCellValue = true
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (isCellValue) {
                            cellText.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val name = parser.name
                        if (name == "v" || name == "t") {
                            isCellValue = false
                        } else if (name == "c") {
                            val colIndex = getColumnIndexFromRef(currentCellRef)
                            val valStr = cellText.toString().trim()
                            val finalVal = if (currentType == "s") {
                                val sIndex = valStr.toIntOrNull() ?: -1
                                if (sIndex in sharedStrings.indices) sharedStrings[sIndex] else valStr
                            } else {
                                valStr
                            }
                            if (colIndex >= 0) {
                                currentRow[colIndex] = finalVal
                            }
                        } else if (name == "row") {
                            if (currentRow.isNotEmpty()) {
                                val maxCol = (currentRow.keys.maxOrNull() ?: 0)
                                val rowList = ArrayList<String>(maxCol + 1)
                                for (i in 0..maxCol) {
                                    rowList.add(currentRow[i] ?: "")
                                }
                                targetGrid.add(rowList)
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getColumnIndexFromRef(ref: String): Int {
        if (ref.isBlank()) return -1
        val colLetters = ref.takeWhile { it.isLetter() }.uppercase()
        if (colLetters.isEmpty()) return -1
        var col = 0
        for (ch in colLetters) {
            col = col * 26 + (ch - 'A' + 1)
        }
        return col - 1
    }

    private fun processGridToItems(grid: List<List<String>>): List<ImportedItemRaw> {
        if (grid.isEmpty()) return emptyList()

        // Column Index Mapping
        var colKode = -1
        var colNama = -1
        var colStok = -1
        var colHarga = -1
        var colKet = -1

        val firstRow = grid.firstOrNull() ?: emptyList()

        // Check if first row is header
        var isHeaderPresent = false
        firstRow.forEachIndexed { idx, cellVal ->
            val norm = cellVal.lowercase().trim()
            if (norm.contains("kode") || norm.contains("code") || norm.contains("sku")) {
                colKode = idx
                isHeaderPresent = true
            } else if (norm.contains("nama") || norm.contains("barang") || norm.contains("produk") || norm.contains("item")) {
                colNama = idx
                isHeaderPresent = true
            } else if (norm.contains("stok") || norm.contains("qty") || norm.contains("jumlah")) {
                colStok = idx
                isHeaderPresent = true
            } else if (norm.contains("harga") || norm.contains("modal") || norm.contains("beli") || norm.contains("cost") || norm.contains("price")) {
                colHarga = idx
                isHeaderPresent = true
            } else if (norm.contains("ket") || norm.contains("keterangan") || norm.contains("satuan") || norm.contains("note")) {
                colKet = idx
                isHeaderPresent = true
            }
        }

        // Fallback default index if headers not explicitly found
        if (!isHeaderPresent) {
            colKode = 0
            colNama = 1
            colStok = 2
            colHarga = 3
            colKet = 4
        } else {
            // Fill unmapped columns based on order
            if (colNama == -1) colNama = if (colKode != 0) 0 else 1
            if (colKode == -1 && colNama != 0) colKode = 0
            if (colStok == -1) colStok = 2
            if (colHarga == -1) colHarga = 3
            if (colKet == -1) colKet = 4
        }

        val dataRows = if (isHeaderPresent) grid.drop(1) else grid
        val result = mutableListOf<ImportedItemRaw>()

        for (row in dataRows) {
            if (row.all { it.isBlank() }) continue

            val kode = row.getOrNull(colKode)?.trim() ?: ""
            val nama = row.getOrNull(colNama)?.trim() ?: ""

            if (nama.isBlank() && kode.isBlank()) continue

            // PERBAIKAN: Gunakan fungsi khusus cleanAndParseStok & cleanAndParsePrice
            // yang membaca angka dari Excel persis apa adanya tanpa mengalikan, membagi,
            // atau mengubah digit desimal/ribuan.
            val rawStokStr = row.getOrNull(colStok)
            val stok = cleanAndParseStok(rawStokStr)

            val rawHargaStr = row.getOrNull(colHarga)
            val harga = cleanAndParsePrice(rawHargaStr)

            val ket = row.getOrNull(colKet)?.trim() ?: ""

            val finalNama = if (nama.isNotBlank()) nama else "Barang $kode"

            // Validasi akhir: Pastikan nilai hargaModal tidak mengalami pergeseran digit
            val itemRaw = ImportedItemRaw(
                kodeBarang = kode,
                namaBarang = finalNama,
                stok = stok,
                hargaModal = harga,
                keterangan = ket
            )

            result.add(itemRaw)
        }

        return result
    }

    /**
     * PERBAIKAN UTAMA PARSING HARGA EXCEL:
     * Membaca dan memparse nilai numerik harga dari sel Excel/CSV secara AKURAT & ABSOLUT.
     * Mencegah pergeseran digit (misal 100000 berubah menjadi 1000000) yang sebelumnya disebabkan oleh
     * penghapusan titik desimal standar XML Excel ("100000.0" -> "1000000").
     */
    fun cleanAndParsePrice(rawStr: String?): Double {
        if (rawStr.isNullOrBlank()) return 0.0

        // 1. Bersihkan karakter non-angka seperti 'Rp', 'rp', spasi, dan simbol mata uang
        var clean = rawStr.replace(Regex("(?i)rp"), "").trim()
        if (clean.isEmpty()) return 0.0

        // 2. Baca nilai numerik langsung jika berupa string angka standar (misal: "100000", "100000.0", "100000.00").
        // File .xlsx menyimpan nilai sel angka dalam tag XML <v> sebagai "100000" atau "100000.0".
        // Jika toDoubleOrNull() berhasil langsung, PAKAI LANGSUNG TANPA MENGEDIT TITIK/KOMA!
        val directDouble = clean.toDoubleOrNull()
        if (directDouble != null) {
            return directDouble
        }

        // 3. Tangani format teks angka bertanda pemisah ribuan / desimal (misal: "100.000", "100.000,00", "100,000.00")
        if (clean.contains(".") && clean.contains(",")) {
            val lastDot = clean.lastIndexOf('.')
            val lastComma = clean.lastIndexOf(',')
            clean = if (lastDot > lastComma) {
                // Format US/English: 100,000.00 -> Hapus koma pemisah ribuan
                clean.replace(",", "")
            } else {
                // Format Indonesia: 100.000,00 -> Hapus titik pemisah ribuan, ganti koma desimal dengan titik
                clean.replace(".", "").replace(",", ".")
            }
        } else if (clean.contains(".")) {
            val dotCount = clean.count { it == '.' }
            val parts = clean.split(".")

            if (dotCount > 1) {
                // Banyak titik (Misal: 1.000.000) -> pasti pemisah ribuan Rupiah
                clean = clean.replace(".", "")
            } else {
                // Hanya 1 titik. Cek jumlah digit di belakang titik:
                val integerPart = parts.getOrNull(0) ?: ""
                val decimalPart = parts.getOrNull(1) ?: ""

                // Jika di belakang titik terdapat 3 digit (misal "100.000" atau "65.000"),
                // ini adalah pemisah ribuan Rupiah -> hapus titik.
                // Jika di belakang titik terdapat 1 atau 2 digit (misal "100000.0" atau "100000.00"), JANGAN HAPUS TITIK!
                if (decimalPart.length == 3 && integerPart.length >= 1) {
                    clean = clean.replace(".", "")
                }
            }
        } else if (clean.contains(",")) {
            val commaCount = clean.count { it == ',' }
            if (commaCount > 1) {
                clean = clean.replace(",", "")
            } else {
                val parts = clean.split(",")
                val decimalPart = parts.getOrNull(1) ?: ""
                if (decimalPart.length == 3) {
                    clean = clean.replace(",", "")
                } else {
                    clean = clean.replace(",", ".")
                }
            }
        }

        // 4. Konversi ke Double dengan fallback 0.0
        val finalPrice = clean.toDoubleOrNull() ?: 0.0

        // 5. Validasi: Hasil tidak boleh negatif
        return if (finalPrice >= 0.0) finalPrice else 0.0
    }

    /**
     * PERBAIKAN PARSING STOK EXCEL:
     * Membaca angka stok dari Excel secara presisi.
     * Mencegah stok 20 berubah menjadi 200 akibat kesalahan baca float XML "20.0".
     */
    fun cleanAndParseStok(rawStr: String?): Int {
        if (rawStr.isNullOrBlank()) return 0
        val clean = rawStr.replace(Regex("(?i)rp"), "").trim()
        val directDouble = clean.toDoubleOrNull()
        if (directDouble != null) {
            return directDouble.toInt()
        }
        val priceVal = cleanAndParsePrice(rawStr)
        return priceVal.toInt()
    }

    /**
     * Sample template text for copying to Excel or pasting.
     */
    fun getSampleExcelTemplate(): String {
        return """
            Kode Barang	Nama Barang	Stok Awal	Harga Modal (Rp)	Keterangan / Satuan
            BRG-001	Beras Pandan Wangi 5kg	20	65000	Karung
            BRG-002	Minyak Goreng Sania 2L	50	32000	Pouch
            BRG-003	Gula Pasir Gulaku 1kg	40	16500	Bungkus
            BRG-004	Telur Ayam Negeri 1kg	30	28000	Kg
            BRG-005	Kopi Kapal Api 165g	25	14000	Pack
        """.trimIndent()
    }
}
