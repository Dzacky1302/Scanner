package com.example.scanner.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.scanner.databinding.ActivityPdfMergerBinding
import com.example.scanner.model.SelectedPdfItem
import com.example.scanner.ui.adapters.SelectedPdfAdapter
import com.example.scanner.utils.FileUtils
import com.example.scanner.utils.PdfUtils
import kotlinx.coroutines.launch

class PdfMergerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfMergerBinding
    private val selectedPdfs = mutableListOf<SelectedPdfItem>()
    private lateinit var adapter: SelectedPdfAdapter

    private val selectPdfLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            for (uri in uris) {
                val name = FileUtils.getFileNameFromUri(this, uri)
                selectedPdfs.add(SelectedPdfItem(uri, name, 0L))
            }
            updatePdfList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfMergerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()

        binding.btnAddPdfs.setOnClickListener {
            selectPdfLauncher.launch("application/pdf")
        }

        binding.btnMerge.setOnClickListener {
            if (selectedPdfs.size < 2) {
                Toast.makeText(this, "Pilih minimal 2 file PDF untuk digabungkan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            mergePdfs()
        }
    }

    private fun setupRecyclerView() {
        adapter = SelectedPdfAdapter(selectedPdfs) { position ->
            if (position in 0 until selectedPdfs.size) {
                selectedPdfs.removeAt(position)
                updatePdfList()
            }
        }
        binding.rvPdfItems.layoutManager = LinearLayoutManager(this)
        binding.rvPdfItems.adapter = adapter
    }

    private fun updatePdfList() {
        adapter.notifyDataSetChanged()
        binding.tvCount.text = "File PDF terpilih: ${selectedPdfs.size}"
    }

    private fun mergePdfs() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnMerge.isEnabled = false

        var outputName = binding.etOutputName.text.toString().trim()
        if (outputName.isBlank()) {
            outputName = "Merged_${System.currentTimeMillis()}.pdf"
        }

        val pdfUris = selectedPdfs.map { it.uri }

        lifecycleScope.launch {
            try {
                val mergedFile = PdfUtils.mergePdfs(
                    context = this@PdfMergerActivity,
                    pdfUris = pdfUris,
                    outputFileName = outputName
                )
                binding.progressBar.visibility = View.GONE
                binding.btnMerge.isEnabled = true

                Toast.makeText(this@PdfMergerActivity, "PDF Berhasil digabungkan: ${mergedFile.name}", Toast.LENGTH_LONG).show()
                FileUtils.shareFile(this@PdfMergerActivity, mergedFile, "application/pdf")
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnMerge.isEnabled = true
                Toast.makeText(this@PdfMergerActivity, "Gagal menggabungkan PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
