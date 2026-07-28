package com.example.util

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.ui.CartItemSales

object PrinterUtils {

    /**
     * Prints receipt HTML via Android System Print Manager (works with Wi-Fi, USB, Bluetooth, PDF printers).
     */
    fun printReceiptHtml(context: Context, jobName: String, htmlContent: String) {
        try {
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    if (printManager != null) {
                        val printAdapter: PrintDocumentAdapter = webView.createPrintDocumentAdapter(jobName)
                        val attributes = PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.UNKNOWN_PORTRAIT)
                            .setMinMargins(PrintAttributes.Margins(0, 0, 0, 0))
                            .build()
                        printManager.print(jobName, printAdapter, attributes)
                    } else {
                        Toast.makeText(context, "Layanan Cetak tidak tersedia di perangkat ini.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka driver printer: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shares plain text receipt to WhatsApp, Bluetooth thermal print apps, Email, or messaging apps.
     */
    fun shareReceiptText(context: Context, receiptText: String, subject: String = "Nota Penjualan") {
        try {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, receiptText)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Cetak / Bagikan Nota via App")
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membagikan nota: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Generates HTML layout tailored for thermal receipt printing (58mm/80mm or A4).
     */
    fun generateReceiptHtml(
        tanggal: String,
        namaPelanggan: String,
        nomorHp: String,
        catatan: String,
        cartItems: List<CartItemSales>,
        totalBelanja: Double,
        isPiutang: Boolean,
        uangMuka: Double,
        sisaPiutang: Double,
        jatuhTempo: String = ""
    ): String {
        val itemsHtml = StringBuilder()
        cartItems.forEach { item ->
            val subtotal = item.jumlahTerjual * item.hargaSatuan
            itemsHtml.append("""
                <tr>
                    <td style="padding: 4px 0;" colspan="2">
                        <strong>${item.item.namaBarang}</strong>
                    </td>
                </tr>
                <tr>
                    <td style="color: #555; padding-bottom: 6px;">
                        ${item.jumlahTerjual} x ${Formatters.formatRupiah(item.hargaSatuan)}
                    </td>
                    <td style="text-align: right; font-weight: bold; padding-bottom: 6px;">
                        ${Formatters.formatRupiah(subtotal)}
                    </td>
                </tr>
            """.trimIndent())
        }

        val pelangganHtml = if (namaPelanggan.isNotBlank()) {
            """
            <div style="margin-bottom: 4px;"><strong>Pelanggan:</strong> $namaPelanggan</div>
            ${if (nomorHp.isNotBlank()) "<div><strong>No HP:</strong> $nomorHp</div>" else ""}
            """.trimIndent()
        } else ""

        val catatanHtml = if (catatan.isNotBlank()) {
            """<div style="margin-top: 4px;"><strong>Catatan:</strong> $catatan</div>"""
        } else ""

        val statusBayarStr = if (isPiutang) "PIUTANG / HUTANG TOKO" else "TUNAI (LUNAS)"
        val statusBgColor = if (isPiutang) "#ffebee" else "#e8f5e9"
        val statusTextColor = if (isPiutang) "#c62828" else "#2e7d32"

        val piutangDetailsHtml = if (isPiutang) {
            val jtStr = if (jatuhTempo.isNotBlank()) "<tr><td style=\"padding: 4px 0;\">Jatuh Tempo:</td><td style=\"text-align: right; font-weight: bold; color: #d32f2f;\">${Formatters.formatDateToIndonesian(jatuhTempo)}</td></tr>" else ""
            """
            <tr>
                <td style="padding: 4px 0;">Uang Muka (DP):</td>
                <td style="text-align: right; font-weight: bold;">${Formatters.formatRupiah(uangMuka)}</td>
            </tr>
            <tr style="color: #c62828;">
                <td style="padding: 4px 0; font-weight: bold;">Sisa Piutang:</td>
                <td style="text-align: right; font-weight: bold;">${Formatters.formatRupiah(sisaPiutang)}</td>
            </tr>
            $jtStr
            """.trimIndent()
        } else ""

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body {
                        font-family: 'Courier New', Courier, monospace;
                        width: 280px;
                        margin: 0 auto;
                        padding: 10px;
                        color: #000;
                        font-size: 13px;
                    }
                    .text-center { text-align: center; }
                    .border-top { border-top: 1px dashed #000; margin-top: 8px; padding-top: 8px; }
                    .border-bottom { border-bottom: 1px dashed #000; margin-bottom: 8px; padding-bottom: 8px; }
                    table { width: 100%; border-collapse: collapse; }
                    .status-badge {
                        background-color: $statusBgColor;
                        color: $statusTextColor;
                        padding: 4px 8px;
                        border-radius: 4px;
                        display: inline-block;
                        font-weight: bold;
                        font-size: 11px;
                        margin-top: 6px;
                    }
                </style>
            </head>
            <body>
                <div class="text-center border-bottom">
                    <h2 style="margin: 0 0 4px 0; font-size: 18px;">NOTA PENJUALAN TOKO</h2>
                    <div style="font-size: 12px; color: #444;">Tanggal: $tanggal</div>
                </div>

                $pelangganHtml
                $catatanHtml

                <div class="border-top" style="margin-bottom: 6px;"></div>

                <table>
                    $itemsHtml
                </table>

                <div class="border-top">
                    <table>
                        <tr>
                            <td style="padding: 4px 0; font-size: 14px; font-weight: bold;">Total Belanja:</td>
                            <td style="text-align: right; font-size: 15px; font-weight: bold;">${Formatters.formatRupiah(totalBelanja)}</td>
                        </tr>
                        $piutangDetailsHtml
                    </table>
                </div>

                <div class="text-center" style="margin-top: 10px;">
                    <div class="status-badge">$statusBayarStr</div>
                </div>

                <div class="text-center border-top" style="margin-top: 16px; font-size: 11px;">
                    Terima Kasih Atas Kunjungan Anda!
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Generates HTML receipt for a saved sales transaction (used in Rekap).
     */
    fun generateReceiptHtmlForTransaction(
        transaction: com.example.data.entity.SalesTransactionEntity,
        items: List<com.example.data.entity.SalesItemEntity>
    ): String {
        val itemsHtml = StringBuilder()
        items.forEach { item ->
            itemsHtml.append("""
                <tr>
                    <td style="padding: 4px 0;" colspan="2">
                        <strong>${item.namaBarang}</strong>
                    </td>
                </tr>
                <tr>
                    <td style="color: #555; padding-bottom: 6px;">
                        ${item.jumlahTerjual} x ${Formatters.formatRupiah(item.hargaSatuan)}
                    </td>
                    <td style="text-align: right; font-weight: bold; padding-bottom: 6px;">
                        ${Formatters.formatRupiah(item.totalHarga)}
                    </td>
                </tr>
            """.trimIndent())
        }

        val catatanHtml = if (transaction.catatan.isNotBlank()) {
            """<div style="margin-top: 4px;"><strong>Catatan:</strong> ${transaction.catatan}</div>"""
        } else ""

        val isPiutang = transaction.metodePembayaran.contains("Piutang", ignoreCase = true)
        val statusBgColor = if (isPiutang) "#ffebee" else "#e8f5e9"
        val statusTextColor = if (isPiutang) "#c62828" else "#2e7d32"

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <style>
                    body {
                        font-family: 'Courier New', Courier, monospace;
                        width: 280px;
                        margin: 0 auto;
                        padding: 10px;
                        color: #000;
                        font-size: 13px;
                    }
                    .text-center { text-align: center; }
                    .border-top { border-top: 1px dashed #000; margin-top: 8px; padding-top: 8px; }
                    .border-bottom { border-bottom: 1px dashed #000; margin-bottom: 8px; padding-bottom: 8px; }
                    table { width: 100%; border-collapse: collapse; }
                    .status-badge {
                        background-color: $statusBgColor;
                        color: $statusTextColor;
                        padding: 4px 8px;
                        border-radius: 4px;
                        display: inline-block;
                        font-weight: bold;
                        font-size: 11px;
                        margin-top: 6px;
                    }
                </style>
            </head>
            <body>
                <div class="text-center border-bottom">
                    <h2 style="margin: 0 0 4px 0; font-size: 18px;">NOTA PENJUALAN #${transaction.id}</h2>
                    <div style="font-size: 12px; color: #444;">Tanggal: ${Formatters.formatDateToIndonesian(transaction.tanggal)}</div>
                    <div style="font-size: 11px; color: #666;">Lokasi: ${transaction.namaToko}</div>
                </div>

                $catatanHtml

                <div class="border-top" style="margin-bottom: 6px;"></div>

                <table>
                    $itemsHtml
                </table>

                <div class="border-top">
                    <table>
                        <tr>
                            <td style="padding: 4px 0; font-size: 14px; font-weight: bold;">Total Penjualan:</td>
                            <td style="text-align: right; font-size: 15px; font-weight: bold;">${Formatters.formatRupiah(transaction.totalUangPenjualan)}</td>
                        </tr>
                    </table>
                </div>

                <div class="text-center" style="margin-top: 10px;">
                    <div class="status-badge">METODE: ${transaction.metodePembayaran.uppercase()}</div>
                </div>

                <div class="text-center border-top" style="margin-top: 16px; font-size: 11px;">
                    Terima Kasih Atas Kunjungan Anda!
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    /**
     * Generates plain text receipt for sharing via WhatsApp or thermal bluetooth printing apps.
     */
    fun generateReceiptTextForTransaction(
        transaction: com.example.data.entity.SalesTransactionEntity,
        items: List<com.example.data.entity.SalesItemEntity>
    ): String {
        val sb = StringBuilder()
        sb.appendLine("===============================")
        sb.appendLine("     NOTA PENJUALAN #${transaction.id}")
        sb.appendLine("===============================")
        sb.appendLine("Tanggal : ${Formatters.formatDateToIndonesian(transaction.tanggal)}")
        sb.appendLine("Lokasi  : ${transaction.namaToko}")
        if (transaction.catatan.isNotBlank()) {
            sb.appendLine("Catatan : ${transaction.catatan}")
        }
        sb.appendLine("-------------------------------")
        items.forEach { item ->
            sb.appendLine(item.namaBarang)
            sb.appendLine("  ${item.jumlahTerjual} x ${Formatters.formatRupiah(item.hargaSatuan)} = ${Formatters.formatRupiah(item.totalHarga)}")
        }
        sb.appendLine("-------------------------------")
        sb.appendLine("TOTAL   : ${Formatters.formatRupiah(transaction.totalUangPenjualan)}")
        sb.appendLine("METODE  : ${transaction.metodePembayaran}")
        sb.appendLine("===============================")
        sb.appendLine("Terima Kasih!")
        return sb.toString()
    }
}
