package com.example.scanner.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.scanner.databinding.ActivitySavedFilesBinding
import com.example.scanner.model.ScannedDocument
import com.example.scanner.ui.adapters.SavedFileAdapter
import com.example.scanner.utils.FileUtils

class SavedFilesActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavedFilesBinding
    private val savedFilesList = mutableListOf<ScannedDocument>()
    private lateinit var adapter: SavedFileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedFilesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        setupRecyclerView()
        loadSavedFiles()
    }

    private fun setupRecyclerView() {
        adapter = SavedFileAdapter(
            files = savedFilesList,
            onItemClick = { doc -> openFile(doc) },
            onShareClick = { doc -> FileUtils.shareFile(this, doc.file) },
            onDeleteClick = { doc, position -> confirmDelete(doc, position) }
        )
        binding.rvSavedFiles.layoutManager = LinearLayoutManager(this)
        binding.rvSavedFiles.adapter = adapter
    }

    private fun loadSavedFiles() {
        savedFilesList.clear()
        savedFilesList.addAll(FileUtils.getAllSavedDocuments(this))
        adapter.notifyDataSetChanged()

        if (savedFilesList.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvSavedFiles.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvSavedFiles.visibility = View.VISIBLE
        }
    }

    private fun openFile(doc: ScannedDocument) {
        try {
            val uri = doc.uri
            val mimeType = when (doc.extension) {
                "pdf" -> "application/pdf"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                "zip" -> "application/zip"
                "jpg", "jpeg", "png" -> "image/*"
                else -> "*/*"
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Buka File"))
        } catch (e: Exception) {
            Toast.makeText(this, "Tidak ada aplikasi untuk membuka file ini: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDelete(doc: ScannedDocument, position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Hapus File")
            .setMessage("Apakah Anda yakin ingin menghapus file ${doc.name}?")
            .setPositiveButton("Hapus") { _, _ ->
                if (doc.file.exists()) {
                    doc.file.delete()
                }
                savedFilesList.removeAt(position)
                adapter.notifyItemRemoved(position)

                if (savedFilesList.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.rvSavedFiles.visibility = View.GONE
                }
                Toast.makeText(this, "File dihapus", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}
