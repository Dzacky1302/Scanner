package com.example.scanner

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.scanner.databinding.ActivityMainBinding
import com.example.scanner.ui.BatchScannerActivity
import com.example.scanner.ui.CompressorActivity
import com.example.scanner.ui.ConverterActivity
import com.example.scanner.ui.ImageToPdfActivity
import com.example.scanner.ui.OcrActivity
import com.example.scanner.ui.PdfMergerActivity
import com.example.scanner.ui.SavedFilesActivity
import com.example.scanner.ui.TranslatorActivity
import com.example.scanner.utils.ThemeManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnThemeToggle.setOnClickListener {
            showThemeSelectionDialog()
        }

        binding.fabQuickScan.setOnClickListener {
            startActivity(Intent(this, OcrActivity::class.java))
        }

        binding.cardBatchScan.setOnClickListener {
            startActivity(Intent(this, BatchScannerActivity::class.java))
        }

        binding.cardOcr.setOnClickListener {
            startActivity(Intent(this, OcrActivity::class.java))
        }

        binding.cardTranslator.setOnClickListener {
            startActivity(Intent(this, TranslatorActivity::class.java))
        }

        binding.cardImgToPdf.setOnClickListener {
            startActivity(Intent(this, ImageToPdfActivity::class.java))
        }

        binding.cardPdfMerger.setOnClickListener {
            startActivity(Intent(this, PdfMergerActivity::class.java))
        }

        binding.cardConverter.setOnClickListener {
            startActivity(Intent(this, ConverterActivity::class.java))
        }

        binding.cardCompressor.setOnClickListener {
            startActivity(Intent(this, CompressorActivity::class.java))
        }

        binding.cardSavedFiles.setOnClickListener {
            startActivity(Intent(this, SavedFilesActivity::class.java))
        }
    }

    private fun showThemeSelectionDialog() {
        val options = arrayOf("Mode Terang (Light)", "Mode Gelap (Dark)", "Mengikuti Sistem")
        val currentMode = ThemeManager.getSavedThemeMode(this)

        AlertDialog.Builder(this)
            .setTitle("Pilih Tema Aplikasi")
            .setSingleChoiceItems(options, currentMode) { dialog, which ->
                ThemeManager.saveAndApplyThemeMode(this, which)
                dialog.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
