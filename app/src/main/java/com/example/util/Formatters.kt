package com.example.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatters {
    private val indonesianLocale = Locale("id", "ID")

    fun formatRupiah(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(indonesianLocale)
        formatter.maximumFractionDigits = 0
        return formatter.format(amount)
    }

    fun formatNumber(number: Int): String {
        val formatter = NumberFormat.getNumberInstance(indonesianLocale)
        return formatter.format(number)
    }

    fun getCurrentDateFormatted(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun formatDateToIndonesian(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = inputFormat.parse(dateString) ?: return dateString
            val outputFormat = SimpleDateFormat("dd MMM yyyy", indonesianLocale)
            outputFormat.format(date)
        } catch (e: Exception) {
            dateString
        }
    }

    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", indonesianLocale)
        return sdf.format(Date(timestamp))
    }

    fun formatTimestampToDateString(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun getAddDaysDate(fromDateString: String, days: Int): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(fromDateString) ?: Date()
            val calendar = java.util.Calendar.getInstance()
            calendar.time = date
            calendar.add(java.util.Calendar.DAY_OF_YEAR, days)
            sdf.format(calendar.time)
        } catch (e: Exception) {
            getCurrentDateFormatted()
        }
    }

    fun isOverdue(dueDateString: String): Boolean {
        if (dueDateString.isBlank()) return false
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dueDate = sdf.parse(dueDateString) ?: return false
            val todayStr = getCurrentDateFormatted()
            val todayDate = sdf.parse(todayStr) ?: return false
            todayDate.after(dueDate)
        } catch (e: Exception) {
            false
        }
    }

    fun getSevenDaysAgoDate(): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -7)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(calendar.time)
    }

    fun getStartOfMonthDate(): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(calendar.time)
    }

    fun getStartOfYearDate(): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_YEAR, 1)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(calendar.time)
    }
}
