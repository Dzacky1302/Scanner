package com.example.scanner.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.scanner.databinding.ActivityImageToPdfBinding
import com.example.scanner.ui.adapters.SelectedImageAdapter
import com.example.scanner.utils.FileUtils
import com.example.scanner.utils.PdfUtils
import kotlinx.coroutines.launch

class ImageToPdfActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageToPdfBinding
    private val selectedImages = mutableListOf<Uri>()
    private lateinit var adapter: SelectedImageAdapter

    private val selectImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            selectedImages.addAll(uris)
            updateImagesList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageToPdfBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()

        binding.btnAddImages.setOnClickListener {
            selectImagesLauncher.launch("image/*")
        }

        binding.btnCreatePdf.setOnClickListener {
            if (selectedImages.isEmpty()) {
                Toast.makeText(this, "Pilih minimal 1 foto terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            createPdf()
        }
    }

    private fun setupRecyclerView() {
        adapter = SelectedImageAdapter(selectedImages) { position ->
            if (position in 0 until selectedImages.size) {
                selectedImages.removeAt(position)
                updateImagesList()
            }
        }
        binding.rvImages.layoutManager = GridLayoutManager(this, 3)
        binding.rvImages.adapter = adapter
    }

    private fun updateImagesList() {
        adapter.notifyDataSetChanged()
        binding.tvCount.text = "Jumlah foto terpilih: ${selectedImages.size}"
    }

    private fun createPdf() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnCreatePdf.isEnabled = false

        val quality = binding.sliderQuality.value.toInt()

        lifecycleScope.launch {
            try {
                val outputFile = PdfUtils.imagesToPdf(
                    context = this@ImageToPdfActivity,
                    imageUris = selectedImages,
                    quality = quality
                )
                binding.progressBar.visibility = View.GONE
                binding.btnCreatePdf.isEnabled = true

                Toast.makeText(this@ImageToPdfActivity, "PDF Berhasil dibuat: ${outputFile.name}", Toast.LENGTH_LONG).show()
                FileUtils.shareFile(this@ImageToPdfActivity, outputFile, "application/pdf")
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnCreatePdf.isEnabled = true
                Toast.makeText(this@ImageToPdfActivity, "Gagal membuat PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
