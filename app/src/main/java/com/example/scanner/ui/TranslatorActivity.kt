package com.example.scanner.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.scanner.databinding.ActivityTranslatorBinding
import com.example.scanner.utils.OcrManager
import com.example.scanner.utils.TranslatorManager
import kotlinx.coroutines.launch

class TranslatorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTranslatorBinding

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { processImageUri(it) }
    }

    private val liveCameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            uri?.let { processImageUri(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTranslatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        setupSpinners()

        binding.btnGallery.setOnClickListener { galleryLauncher.launch("image/*") }
        binding.btnCamera.setOnClickListener {
            liveCameraLauncher.launch(Intent(this, CameraScannerActivity::class.java))
        }

        binding.btnTranslate.setOnClickListener {
            translateCurrentText()
        }

        binding.btnCopy.setOnClickListener {
            val text = binding.tvTranslatedResult.text.toString()
            if (text.isNotBlank()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Translated Text", text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Teks terjemahan disalin!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnShare.setOnClickListener {
            val text = binding.tvTranslatedResult.text.toString()
            if (text.isNotBlank()) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                startActivity(Intent.createChooser(intent, "Bagikan Hasil Terjemahan"))
            }
        }
    }

    private fun setupSpinners() {
        val languageNames = TranslatorManager.supportedLanguages.map { it.displayName }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languageNames).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.spSourceLang.adapter = adapter
        binding.spTargetLang.adapter = adapter

        binding.spSourceLang.setSelection(1) // Default English
        binding.spTargetLang.setSelection(0) // Default Indonesian
    }

    private fun processImageUri(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val text = OcrManager.recognizeTextFromUri(this@TranslatorActivity, uri)
                binding.progressBar.visibility = View.GONE
                if (text.isNotBlank()) {
                    binding.etOriginalText.setText(text)
                    translateCurrentText()
                } else {
                    Toast.makeText(this@TranslatorActivity, "Tidak ada teks yang terdeteksi pada gambar.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@TranslatorActivity, "Gagal membaca teks: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun translateCurrentText() {
        val originalText = binding.etOriginalText.text.toString()
        if (originalText.isBlank()) {
            Toast.makeText(this, "Masukkan atau pilih foto teks terlebih dahulu.", Toast.LENGTH_SHORT).show()
            return
        }

        val sourceLangIndex = binding.spSourceLang.selectedItemPosition
        val targetLangIndex = binding.spTargetLang.selectedItemPosition

        val sourceCode = TranslatorManager.supportedLanguages[sourceLangIndex].code
        val targetCode = TranslatorManager.supportedLanguages[targetLangIndex].code

        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val translated = TranslatorManager.translateText(originalText, sourceCode, targetCode)
                binding.progressBar.visibility = View.GONE
                binding.tvTranslatedResult.text = translated
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@TranslatorActivity, "Gagal menerjemahkan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
