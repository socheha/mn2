package com.example.util

import android.content.Context
import android.content.SharedPreferences

object ThemePreferenceManager {
    private const val PREFS_NAME = "smartstock_theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    const val MODE_SYSTEM = "SYSTEM"
    const val MODE_LIGHT = "LIGHT"
    const val MODE_DARK = "DARK"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getThemeMode(context: Context): String {
        return getPrefs(context).getString(KEY_THEME_MODE, MODE_SYSTEM) ?: MODE_SYSTEM
    }

    fun setThemeMode(context: Context, mode: String) {
        getPrefs(context).edit().putString(KEY_THEME_MODE, mode).apply()
    }
}
