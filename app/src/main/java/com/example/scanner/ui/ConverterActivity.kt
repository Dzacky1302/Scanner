package com.example.scanner.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.scanner.R
import com.example.scanner.databinding.ActivityConverterBinding
import com.example.scanner.utils.DocConverter
import com.example.scanner.utils.FileUtils
import com.example.scanner.utils.PdfUtils
import kotlinx.coroutines.launch

class ConverterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConverterBinding
    private var selectedPdfUri: Uri? = null

    private val selectPdfLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedPdfUri = it
            val name = FileUtils.getFileNameFromUri(this, it)
            binding.tvSelectedFile.text = "File dipilih: $name"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConverterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnSelectPdf.setOnClickListener {
            selectPdfLauncher.launch("application/pdf")
        }

        binding.btnConvert.setOnClickListener {
            val uri = selectedPdfUri
            if (uri == null) {
                Toast.makeText(this, "Pilih file PDF yang akan dikonversi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startConversion(uri)
        }
    }

    private fun startConversion(pdfUri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnConvert.isEnabled = false

        var outputName = binding.etOutputName.text.toString().trim()
        if (outputName.isBlank()) {
            outputName = "Converted_${System.currentTimeMillis()}"
        }

        lifecycleScope.launch {
            try {
                when {
                    binding.rbPdfToDocx.isChecked -> {
                        val file = DocConverter.pdfToDocx(this@ConverterActivity, pdfUri, "$outputName.docx")
                        onSuccess(file, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    }
                    binding.rbPdfToXlsx.isChecked -> {
                        val file = DocConverter.pdfToXlsx(this@ConverterActivity, pdfUri, "$outputName.xlsx")
                        onSuccess(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    }
                    binding.rbPdfToPptx.isChecked -> {
                        val file = DocConverter.pdfToPptx(this@ConverterActivity, pdfUri, "$outputName.pptx")
                        onSuccess(file, "application/vnd.openxmlformats-officedocument.presentationml.presentation")
                    }
                    binding.rbPdfToImage.isChecked -> {
                        val imgFiles = PdfUtils.pdfToImages(this@ConverterActivity, pdfUri, outputName)
                        binding.progressBar.visibility = View.GONE
                        binding.btnConvert.isEnabled = true
                        if (imgFiles.isNotEmpty()) {
                            Toast.makeText(this@ConverterActivity, "Berhasil konversi ${imgFiles.size} halaman ke Gambar!", Toast.LENGTH_LONG).show()
                            FileUtils.shareFile(this@ConverterActivity, imgFiles.first(), "image/jpeg")
                        } else {
                            Toast.makeText(this@ConverterActivity, "Gagal mengkonversi PDF ke Gambar", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnConvert.isEnabled = true
                Toast.makeText(this@ConverterActivity, "Gagal konversi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onSuccess(outputFile: java.io.File, mimeType: String) {
        binding.progressBar.visibility = View.GONE
        binding.btnConvert.isEnabled = true
        Toast.makeText(this, "Konversi berhasil: ${outputFile.name}", Toast.LENGTH_LONG).show()
        FileUtils.shareFile(this, outputFile, mimeType)
    }
}
