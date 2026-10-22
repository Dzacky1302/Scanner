package com.example.scanner

import android.app.Application
import com.example.scanner.utils.ThemeManager

class ScannerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemeManager.applyTheme(this)
    }
}
