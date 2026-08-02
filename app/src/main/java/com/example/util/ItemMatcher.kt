package com.example.util

import com.example.data.entity.ItemEntity
import kotlin.math.max
import kotlin.math.min

object ItemMatcher {

    /**
     * Finds the best matching ItemEntity from database based on scanned text.
     * Returns null if no item reaches a match threshold.
     */
    fun findBestMatch(queryText: String, databaseItems: List<ItemEntity>): ItemEntity? {
        if (queryText.isBlank() || databaseItems.isEmpty()) return null

        val cleanQuery = queryText.lowercase().replace(Regex("[^a-z0-9\\s]"), " ").trim()
        val queryTokens = cleanQuery.split("\\s+".toRegex()).filter { it.length >= 2 }

        if (queryTokens.isEmpty()) return null

        var bestItem: ItemEntity? = null
        var highestScore = 0.0

        for (item in databaseItems) {
            val itemCode = item.kodeBarang.lowercase().trim()
            val itemName = item.namaBarang.lowercase().replace(Regex("[^a-z0-9\\s]"), " ").trim()
            val itemTokens = itemName.split("\\s+".toRegex()).filter { it.length >= 2 }

            var score = 0.0

            // 1. Check exact item code match (Highest priority)
            if (itemCode.isNotBlank()) {
                if (cleanQuery == itemCode) {
                    score += 100.0
                } else if (queryTokens.contains(itemCode)) {
                    score += 85.0
                } else if (cleanQuery.contains(itemCode) && itemCode.length >= 3) {
                    score += 65.0
                }
            }

            // 2. Token overlap score
            if (itemTokens.isNotEmpty()) {
                val matchedTokenCount = queryTokens.count { qToken ->
                    itemTokens.any { iToken ->
                        iToken == qToken ||
                        (qToken.length >= 3 && iToken.contains(qToken)) ||
                        (iToken.length >= 3 && qToken.contains(iToken))
                    }
                }

                val tokenOverlapRatio = matchedTokenCount.toDouble() / max(queryTokens.size, itemTokens.size).toDouble()
                score += tokenOverlapRatio * 50.0
            }

            // 3. String similarity (Levenshtein distance ratio)
            val similarity = calculateSimilarity(cleanQuery, itemName)
            score += similarity * 40.0

            // Adjusted match threshold requirement (score >= 35.0)
            if (score > highestScore && score >= 35.0) {
                highestScore = score
                bestItem = item
            }
        }

        return bestItem
    }

    /**
     * Cleans OCR raw text by removing leading numbers, prices, and common unit/noise words.
     */
    fun cleanOcrQuery(rawText: String): String {
        if (rawText.isBlank()) return ""
        var cleaned = rawText.lowercase()
            .replace(Regex("^[0-9]+[\\.\\)\\s]+"), "") // Remove leading numbers like "1.", "01)", "1 "
            .replace(Regex("\\b(pcs|unt|unit|pack|dus|ctn|kg|gr|pck|lsn|lusin|btg|sak|roll|set|box|rp|harga|total)\\b"), " ")
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .trim()
        
        // Remove trailing pure numbers (like price 15000)
        cleaned = cleaned.replace(Regex("\\b[0-9]{4,}\\b"), "").trim()
        return cleaned.ifBlank { rawText.replace(Regex("[^a-zA-Z0-9\\s]"), " ").trim() }
    }

    /**
     * Search and rank database items based on user query or draft text.
     * Splits into keywords/tokens and supports matching both kodeBarang and namaBarang.
     */
    fun searchItems(queryText: String, databaseItems: List<ItemEntity>): List<ItemEntity> {
        if (databaseItems.isEmpty()) return emptyList()
        if (queryText.isBlank()) return databaseItems

        val cleanedQuery = cleanOcrQuery(queryText)
        val cleanQuery = cleanedQuery.lowercase().trim()
        val tokens = cleanQuery.split("\\s+".toRegex()).filter { it.isNotBlank() && it.length >= 2 }

        if (tokens.isEmpty()) return databaseItems

        val scored = databaseItems.mapNotNull { item ->
            val code = item.kodeBarang.lowercase().trim()
            val name = item.namaBarang.lowercase().replace(Regex("[^a-z0-9\\s]"), " ").trim()

            var score = 0.0

            // Exact or substring match in code
            if (code.isNotBlank()) {
                if (code == cleanQuery) score += 100.0
                else if (code.contains(cleanQuery)) score += 80.0
                else if (tokens.any { code.contains(it) }) score += 40.0
            }

            // Exact or substring match in name
            if (name == cleanQuery) {
                score += 90.0
            } else if (name.startsWith(cleanQuery)) {
                score += 70.0
            } else if (name.contains(cleanQuery)) {
                score += 50.0
            }

            // Token level matching
            var matchedTokenCount = 0
            for (token in tokens) {
                if (token.length >= 2) {
                    if (name.contains(token) || (code.isNotBlank() && code.contains(token))) {
                        matchedTokenCount++
                        score += 25.0
                    }
                }
            }

            if (score > 0.0) {
                Pair(item, score)
            } else {
                null
            }
        }

        if (scored.isEmpty()) {
            // Fallback: try raw query substring search before returning empty
            val rawSubMatches = databaseItems.filter { item ->
                item.namaBarang.contains(queryText, ignoreCase = true) ||
                item.kodeBarang.contains(queryText, ignoreCase = true)
            }
            if (rawSubMatches.isNotEmpty()) return rawSubMatches
            return emptyList()
        }

        return scored.sortedByDescending { it.second }.map { it.first }
    }

    private fun calculateSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0

        val maxLen = max(s1.length, s2.length)
        if (maxLen == 0) return 1.0

        val dist = levenshteinDistance(s1, s2)
        return (maxLen - dist).toDouble() / maxLen.toDouble()
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    dp[i - 1][j] + 1,
                    min(dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
                )
            }
        }

        return dp[s1.length][s2.length]
    }
}
