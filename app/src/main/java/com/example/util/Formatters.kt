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

    fun formatTimeOnly(timestamp: Long): String {
        if (timestamp <= 0) return ""
        return try {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (e: Exception) {
            ""
        }
    }

    fun formatDateAndTime(dateString: String, timestamp: Long): String {
        val datePart = formatDateToIndonesian(dateString)
        val timePart = formatTimeOnly(timestamp)
        return if (timePart.isNotBlank()) "$datePart $timePart" else datePart
    }

    fun getTimestampForDate(dateString: String, baseTimestamp: Long = System.currentTimeMillis()): Long {
        if (dateString.isBlank()) return if (baseTimestamp > 0) baseTimestamp else System.currentTimeMillis()
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val parsedDate = sdf.parse(dateString) ?: return if (baseTimestamp > 0) baseTimestamp else System.currentTimeMillis()
            val calDate = java.util.Calendar.getInstance().apply { time = parsedDate }
            val calBase = java.util.Calendar.getInstance().apply { 
                timeInMillis = if (baseTimestamp > 0) baseTimestamp else System.currentTimeMillis() 
            }
            calDate.set(java.util.Calendar.HOUR_OF_DAY, calBase.get(java.util.Calendar.HOUR_OF_DAY))
            calDate.set(java.util.Calendar.MINUTE, calBase.get(java.util.Calendar.MINUTE))
            calDate.set(java.util.Calendar.SECOND, calBase.get(java.util.Calendar.SECOND))
            calDate.set(java.util.Calendar.MILLISECOND, calBase.get(java.util.Calendar.MILLISECOND))
            calDate.timeInMillis
        } catch (e: Exception) {
            if (baseTimestamp > 0) baseTimestamp else System.currentTimeMillis()
        }
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
