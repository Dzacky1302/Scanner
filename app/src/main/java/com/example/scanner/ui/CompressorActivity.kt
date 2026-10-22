package com.example.scanner.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.scanner.databinding.ActivityCompressorBinding
import com.example.scanner.utils.FileUtils
import com.example.scanner.utils.ImageCompressor
import com.example.scanner.utils.PdfUtils
import com.example.scanner.utils.ZipUtils
import kotlinx.coroutines.launch

class CompressorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompressorBinding
    private val selectedUris = mutableListOf<Uri>()

    private val selectFileLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedUris.clear()
            selectedUris.addAll(uris)
            if (uris.size == 1) {
                val name = FileUtils.getFileNameFromUri(this, uris.first())
                binding.tvSelectedFile.text = "File dipilih: $name"
            } else {
                binding.tvSelectedFile.text = "${uris.size} File dipilih"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompressorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.rgCompressMode.setOnCheckedChangeListener { _, checkedId ->
            selectedUris.clear()
            binding.tvSelectedFile.text = "Belum ada file dipilih"
        }

        binding.btnSelectFile.setOnClickListener {
            val mimeType = when {
                binding.rbCompressImage.isChecked -> "image/*"
                binding.rbCompressPdf.isChecked -> "application/pdf"
                else -> "*/*"
            }
            selectFileLauncher.launch(mimeType)
        }

        binding.btnCompress.setOnClickListener {
            if (selectedUris.isEmpty()) {
                Toast.makeText(this, "Pilih file yang akan dikompres terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startCompression()
        }
    }

    private fun startCompression() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnCompress.isEnabled = false

        val quality = binding.sliderQuality.value.toInt()
        val ratio = quality / 100f

        lifecycleScope.launch {
            try {
                when {
                    binding.rbCompressImage.isChecked -> {
                        val uri = selectedUris.first()
                        val compressedFile = ImageCompressor.compressImage(
                            context = this@CompressorActivity,
                            imageUri = uri,
                            quality = quality
                        )
                        onSuccess(compressedFile, "image/jpeg")
                    }
                    binding.rbCompressPdf.isChecked -> {
                        val uri = selectedUris.first()
                        val compressedFile = PdfUtils.compressPdf(
                            context = this@CompressorActivity,
                            pdfUri = uri,
                            compressionRatio = ratio
                        )
                        onSuccess(compressedFile, "application/pdf")
                    }
                    binding.rbCompressZip.isChecked -> {
                        val zipFile = ZipUtils.compressFilesToZip(
                            context = this@CompressorActivity,
                            fileUris = selectedUris
                        )
                        onSuccess(zipFile, "application/zip")
                    }
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnCompress.isEnabled = true
                Toast.makeText(this@CompressorActivity, "Gagal mengkompres: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onSuccess(outputFile: java.io.File, mimeType: String) {
        binding.progressBar.visibility = View.GONE
        binding.btnCompress.isEnabled = true
        val sizeStr = FileUtils.formatFileSize(outputFile.length())
        Toast.makeText(this, "Kompresi Berhasil!\nUkuran baru: $sizeStr", Toast.LENGTH_LONG).show()
        FileUtils.shareFile(this, outputFile, mimeType)
    }
}
