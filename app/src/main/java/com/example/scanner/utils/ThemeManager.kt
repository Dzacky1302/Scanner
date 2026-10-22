package com.example.scanner.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {

    private const val PREFS_NAME = "scanner_theme_prefs"
    private const val KEY_THEME_MODE = "key_theme_mode"

    const val MODE_LIGHT = 0
    const val MODE_DARK = 1
    const val MODE_SYSTEM = 2

    fun applyTheme(context: Context) {
        val mode = getSavedThemeMode(context)
        when (mode) {
            MODE_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            MODE_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    fun saveAndApplyThemeMode(context: Context, mode: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply()
        applyTheme(context)
    }

    fun getSavedThemeMode(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_THEME_MODE, MODE_SYSTEM)
    }
}
