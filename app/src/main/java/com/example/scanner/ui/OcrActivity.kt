package com.example.scanner.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.scanner.databinding.ActivityOcrBinding
import com.example.scanner.utils.OcrManager
import kotlinx.coroutines.launch

class OcrActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOcrBinding

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
        binding = ActivityOcrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.btnCamera.setOnClickListener {
            liveCameraLauncher.launch(Intent(this, CameraScannerActivity::class.java))
        }

        binding.btnCopy.setOnClickListener {
            val text = binding.etResultText.text.toString()
            if (text.isNotBlank()) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Extracted Text", text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Teks berhasil disalin!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnShare.setOnClickListener {
            val text = binding.etResultText.text.toString()
            if (text.isNotBlank()) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                startActivity(Intent.createChooser(intent, "Bagikan Teks"))
            }
        }
    }

    private fun processImageUri(uri: Uri) {
        binding.layoutPlaceholder.visibility = View.GONE
        binding.imgPreview.visibility = View.VISIBLE
        binding.imgPreview.setImageURI(uri)
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val text = OcrManager.recognizeTextFromUri(this@OcrActivity, uri)
                binding.progressBar.visibility = View.GONE
                if (text.isNotBlank()) {
                    binding.etResultText.setText(text)
                } else {
                    binding.etResultText.setText("Tidak ada teks yang terdeteksi pada gambar.")
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@OcrActivity, "Gagal ekstrak teks: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
