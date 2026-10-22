package com.example.scanner.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.scanner.R
import com.example.scanner.databinding.ActivityScanReviewBinding
import com.example.scanner.ui.adapters.SelectedImageAdapter
import com.example.scanner.ui.views.CropOverlayView
import com.example.scanner.utils.DocumentCropUtils
import com.example.scanner.utils.FileUtils
import com.example.scanner.utils.PdfUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ScanReviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanReviewBinding
    private val pageUris = mutableListOf<Uri>()
    private var currentPageIndex = 0
    private lateinit var thumbnailAdapter: SelectedImageAdapter

    private val retakeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                if (currentPageIndex in 0 until pageUris.size) {
                    pageUris[currentPageIndex] = it
                    updatePageDisplay()
                }
            }
        }
    }

    private val addPageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                pageUris.add(it)
                currentPageIndex = pageUris.size - 1
                updatePageDisplay()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanReviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        val urisFromIntent = intent.getParcelableArrayListExtra<Uri>("EXTRA_URIS")
        if (!urisFromIntent.isNullOrEmpty()) {
            pageUris.addAll(urisFromIntent)
        }

        setupRecyclerView()

        binding.btnCropManual.setOnClickListener { showManualCropDialog() }
        binding.btnRotate.setOnClickListener { rotateCurrentPage() }
        binding.btnRetake.setOnClickListener { retakeCurrentPage() }
        binding.btnDelete.setOnClickListener { deleteCurrentPage() }
        binding.btnAddMore.setOnClickListener { addMorePages() }
        binding.btnSavePdf.setOnClickListener { saveAndGeneratePdf() }

        updatePageDisplay()
    }

    private fun setupRecyclerView() {
        thumbnailAdapter = SelectedImageAdapter(pageUris) { index ->
            if (index in 0 until pageUris.size) {
                pageUris.removeAt(index)
                if (currentPageIndex >= pageUris.size) {
                    currentPageIndex = (pageUris.size - 1).coerceAtLeast(0)
                }
                updatePageDisplay()
            }
        }
        binding.rvPageThumbnails.adapter = thumbnailAdapter
    }

    private fun updatePageDisplay() {
        if (pageUris.isEmpty()) {
            Toast.makeText(this, "Tidak ada halaman tersisa", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentPageIndex = currentPageIndex.coerceIn(0, pageUris.size - 1)
        val currentUri = pageUris[currentPageIndex]

        binding.tvPageIndicator.text = "Halaman ${currentPageIndex + 1} / ${pageUris.size}"
        binding.imgPagePreview.setImageURI(currentUri)

        thumbnailAdapter.notifyDataSetChanged()
    }

    private fun showManualCropDialog() {
        if (pageUris.isEmpty()) return
        val currentUri = pageUris[currentPageIndex]

        val dialogView = layoutInflater.inflate(R.layout.dialog_manual_crop, null)
        val imgSource = dialogView.findViewById<ImageView>(R.id.imgCropSource)
        val cropOverlay = dialogView.findViewById<CropOverlayView>(R.id.cropOverlay)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnApply = dialogView.findViewById<Button>(R.id.btnApply)

        imgSource.setImageURI(currentUri)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnApply.setOnClickListener {
            dialog.dismiss()
            applyCropRect(currentUri, cropOverlay.cropRect)
        }

        dialog.show()
    }

    private fun applyCropRect(currentUri: Uri, rectF: RectF) {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(currentUri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap != null) {
                    val croppedBitmap = DocumentCropUtils.cropBitmapByRect(originalBitmap, rectF)
                    val tempFile = File.createTempFile("crop_manual_", ".jpg", cacheDir)
                    FileOutputStream(tempFile).use { out ->
                        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    val newUri = FileUtils.getUriForFile(this@ScanReviewActivity, tempFile)

                    withContext(Dispatchers.Main) {
                        pageUris[currentPageIndex] = newUri
                        binding.progressBar.visibility = View.GONE
                        updatePageDisplay()
                        Toast.makeText(this@ScanReviewActivity, "Potongan berhasil diterapkan!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@ScanReviewActivity, "Gagal memotong: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun rotateCurrentPage() {
        if (pageUris.isEmpty()) return
        val currentUri = pageUris[currentPageIndex]

        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(currentUri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap != null) {
                    val rotatedBitmap = DocumentCropUtils.rotateBitmap(originalBitmap, 90f)
                    val tempFile = File.createTempFile("rotate_page_", ".jpg", cacheDir)
                    FileOutputStream(tempFile).use { out ->
                        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                    val newUri = FileUtils.getUriForFile(this@ScanReviewActivity, tempFile)

                    withContext(Dispatchers.Main) {
                        pageUris[currentPageIndex] = newUri
                        binding.progressBar.visibility = View.GONE
                        updatePageDisplay()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@ScanReviewActivity, "Gagal memutar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun retakeCurrentPage() {
        val intent = Intent(this, CameraScannerActivity::class.java)
        retakeLauncher.launch(intent)
    }

    private fun deleteCurrentPage() {
        if (pageUris.isNotEmpty()) {
            pageUris.removeAt(currentPageIndex)
            updatePageDisplay()
        }
    }

    private fun addMorePages() {
        val intent = Intent(this, CameraScannerActivity::class.java)
        addPageLauncher.launch(intent)
    }

    private fun saveAndGeneratePdf() {
        if (pageUris.isEmpty()) return

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSavePdf.isEnabled = false

        lifecycleScope.launch {
            try {
                val outputFile = PdfUtils.imagesToPdf(
                    context = this@ScanReviewActivity,
                    imageUris = pageUris,
                    outputFileName = "Document_Scan_${System.currentTimeMillis()}.pdf"
                )

                binding.progressBar.visibility = View.GONE
                binding.btnSavePdf.isEnabled = true

                Toast.makeText(this@ScanReviewActivity, "PDF Berhasil Disimpan: ${outputFile.name}", Toast.LENGTH_LONG).show()
                FileUtils.shareFile(this@ScanReviewActivity, outputFile, "application/pdf")
                finish()
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnSavePdf.isEnabled = true
                Toast.makeText(this@ScanReviewActivity, "Gagal menyimpan PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
