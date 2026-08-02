package com.example.util

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.math.max

object SecurityManager {
    private const val PREFS_NAME = "smartstock_security_prefs"
    private const val KEY_IS_PIN_ENABLED = "is_pin_enabled"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_SALT = "salt_value"
    private const val KEY_SECURITY_QUESTION = "security_question"
    private const val KEY_SECURITY_ANSWER_HASH = "security_answer_hash"
    private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
    private const val KEY_LOCKOUT_UNTIL = "lockout_until_ms"
    private const val KEY_LAST_UNLOCK_TIME = "last_unlock_time_ms"
    private const val KEY_PIN_TIMEOUT_MINUTES = "pin_timeout_minutes"

    private const val MAX_FAILED_ATTEMPTS = 5
    private const val LOCKOUT_DURATION_MS = 30_000L // 30 seconds lockout

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getOrCreateSalt(context: Context): String {
        val prefs = getPrefs(context)
        var salt = prefs.getString(KEY_SALT, "") ?: ""
        if (salt.isBlank()) {
            val randomBytes = ByteArray(16)
            SecureRandom().nextBytes(randomBytes)
            salt = randomBytes.joinToString("") { "%02x".format(it) }
            prefs.edit().putString(KEY_SALT, salt).apply()
        }
        return salt
    }

    private fun hashWithSalt(context: Context, input: String): String {
        if (input.isBlank()) return ""
        val salt = getOrCreateSalt(context)
        val combined = "$salt:$input"
        val bytes = MessageDigest.getInstance("SHA-256").digest(combined.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun isPinEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_PIN_ENABLED, false) && getPrefs(context).getString(KEY_PIN_HASH, "")!!.isNotBlank()
    }

    fun getRemainingLockoutSeconds(context: Context): Int {
        val lockoutUntil = getPrefs(context).getLong(KEY_LOCKOUT_UNTIL, 0L)
        val now = System.currentTimeMillis()
        return if (lockoutUntil > now) {
            max(1, ((lockoutUntil - now) / 1000).toInt())
        } else {
            0
        }
    }

    fun setPin(context: Context, pin: String, question: String, answer: String): Boolean {
        if (pin.length < 4) return false
        val pinHash = hashWithSalt(context, pin)
        val answerHash = hashWithSalt(context, answer.trim().lowercase())

        getPrefs(context).edit()
            .putBoolean(KEY_IS_PIN_ENABLED, true)
            .putString(KEY_PIN_HASH, pinHash)
            .putString(KEY_SECURITY_QUESTION, question.ifBlank { "Siapa nama toko Anda?" })
            .putString(KEY_SECURITY_ANSWER_HASH, answerHash)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
        return true
    }

    fun verifyPin(context: Context, pinInput: String): Boolean {
        val prefs = getPrefs(context)
        val storedHash = prefs.getString(KEY_PIN_HASH, "") ?: ""
        if (storedHash.isBlank()) return true // No PIN set

        val remainingLockout = getRemainingLockoutSeconds(context)
        if (remainingLockout > 0) {
            return false // Currently locked out
        }

        val inputHash = hashWithSalt(context, pinInput)
        val isCorrect = (inputHash == storedHash)

        if (isCorrect) {
            // Reset failure counters
            prefs.edit()
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL, 0L)
                .apply()
            return true
        } else {
            val failed = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
            val editor = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, failed)
            if (failed >= MAX_FAILED_ATTEMPTS) {
                val lockoutUntil = System.currentTimeMillis() + LOCKOUT_DURATION_MS
                editor.putLong(KEY_LOCKOUT_UNTIL, lockoutUntil)
                editor.putInt(KEY_FAILED_ATTEMPTS, 0)
            }
            editor.apply()
            return false
        }
    }

    fun disablePin(context: Context, pinInput: String): Boolean {
        if (!verifyPin(context, pinInput)) return false
        getPrefs(context).edit().putBoolean(KEY_IS_PIN_ENABLED, false).apply()
        return true
    }

    fun getSecurityQuestion(context: Context): String {
        return getPrefs(context).getString(KEY_SECURITY_QUESTION, "Siapa nama toko Anda?") ?: "Siapa nama toko Anda?"
    }

    fun verifySecurityAnswer(context: Context, answerInput: String): Boolean {
        val storedHash = getPrefs(context).getString(KEY_SECURITY_ANSWER_HASH, "") ?: ""
        if (storedHash.isBlank()) return false
        return hashWithSalt(context, answerInput.trim().lowercase()) == storedHash
    }

    fun resetPinWithAnswer(context: Context, answerInput: String, newPin: String): Boolean {
        if (!verifySecurityAnswer(context, answerInput)) return false
        if (newPin.length < 4) return false
        val newPinHash = hashWithSalt(context, newPin)
        getPrefs(context).edit()
            .putBoolean(KEY_IS_PIN_ENABLED, true)
            .putString(KEY_PIN_HASH, newPinHash)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
        return true
    }

    fun recordUnlock(context: Context) {
        getPrefs(context).edit().putLong(KEY_LAST_UNLOCK_TIME, System.currentTimeMillis()).apply()
    }

    fun clearUnlockSession(context: Context) {
        getPrefs(context).edit().putLong(KEY_LAST_UNLOCK_TIME, 0L).apply()
    }

    fun getPinTimeoutMinutes(context: Context): Int {
        return getPrefs(context).getInt(KEY_PIN_TIMEOUT_MINUTES, 5) // default 5 minutes
    }

    fun setPinTimeoutMinutes(context: Context, minutes: Int) {
        getPrefs(context).edit().putInt(KEY_PIN_TIMEOUT_MINUTES, minutes).apply()
    }

    fun isSessionValid(context: Context): Boolean {
        if (!isPinEnabled(context)) return true
        val lastUnlock = getPrefs(context).getLong(KEY_LAST_UNLOCK_TIME, 0L)
        if (lastUnlock <= 0L) return false
        val timeoutMinutes = getPinTimeoutMinutes(context)
        if (timeoutMinutes <= 0) return false // 0 = immediate lock
        val elapsedMs = System.currentTimeMillis() - lastUnlock
        return elapsedMs < (timeoutMinutes * 60 * 1000L)
    }

    /**
     * Sanitizes user text input against script injection, dangerous characters, and control codes.
     */
    fun sanitizeInput(input: String): String {
        if (input.isEmpty()) return input
        return input
            .replace(Regex("<script[^>]*>.*?</script>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<[^>]*>"), "") // strip HTML tags
            .replace("'", "''") // escape single quotes for safety
            .replace(Regex("[\\r\\n]+"), " ") // sanitize newlines
            .trim()
    }
}

