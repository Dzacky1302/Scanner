package com.example.scanner.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.scanner.databinding.ActivityBatchScannerBinding
import com.example.scanner.ui.adapters.SelectedImageAdapter
import com.google.android.material.tabs.TabLayout
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BatchScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBatchScannerBinding
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var isFlashOn = false
    private lateinit var cameraExecutor: ExecutorService

    private val capturedUris = mutableListOf<Uri>()
    private lateinit var thumbnailAdapter: SelectedImageAdapter
    private var currentMode = 0 // 0: Books/Multi-page, 1: KTP/ID, 2: Passport

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Izin kamera dibutuhkan untuk memindai dokumen", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBatchScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.btnBack.setOnClickListener { finish() }

        binding.btnFlash.setOnClickListener { toggleFlash() }

        setupRecyclerView()
        setupTabs()

        binding.btnCapture.setOnClickListener { takePhoto() }

        binding.btnFinish.setOnClickListener { finishAndReview() }

        checkPermissionAndStart()
    }

    private fun setupRecyclerView() {
        thumbnailAdapter = SelectedImageAdapter(capturedUris) { position ->
            if (position in 0 until capturedUris.size) {
                capturedUris.removeAt(position)
                updateUI()
            }
        }
        binding.rvThumbnails.adapter = thumbnailAdapter
    }

    private fun setupTabs() {
        binding.tabModes.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentMode = tab?.position ?: 0
                capturedUris.clear()
                updateUI()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateUI() {
        thumbnailAdapter.notifyDataSetChanged()
        binding.btnFinish.text = "Review (${capturedUris.size})"

        when (currentMode) {
            0 -> {
                binding.tvTitle.text = "Pindai Buku & Banyak Halaman"
                binding.tvInstruction.text = "Foto halaman 1, 2, 3... berurutan lalu tekan Review"
            }
            1 -> {
                binding.tvTitle.text = "Pindai KTP / Kartu Identitas"
                if (capturedUris.isEmpty()) {
                    binding.tvInstruction.text = "1. Ambil Foto KTP Sisi DEPAN"
                } else if (capturedUris.size == 1) {
                    binding.tvInstruction.text = "2. Ambil Foto KTP Sisi BELAKANG"
                } else {
                    binding.tvInstruction.text = "KTP Sisi Depan & Belakang Siap. Tekan Review!"
                }
            }
            2 -> {
                binding.tvTitle.text = "Pindai Paspor & Berkas Resmi"
                binding.tvInstruction.text = "Posisikan paspor di dalam kotak lalu ambil foto"
            }
        }
    }

    private fun checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                exc.printStackTrace()
                Toast.makeText(this, "Gagal memulai kamera: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun toggleFlash() {
        val cam = camera ?: return
        if (cam.cameraInfo.hasFlashUnit()) {
            isFlashOn = !isFlashOn
            cam.cameraControl.enableTorch(isFlashOn)
        }
    }

    private fun takePhoto() {
        val capture = imageCapture ?: return

        val storageDir = externalCacheDir ?: cacheDir
        val photoFile = File.createTempFile("batch_page_", ".jpg", storageDir)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = FileProvider.getUriForFile(
                        this@BatchScannerActivity,
                        "$packageName.fileprovider",
                        photoFile
                    )
                    capturedUris.add(savedUri)
                    updateUI()
                    Toast.makeText(this@BatchScannerActivity, "Halaman ${capturedUris.size} difoto!", Toast.LENGTH_SHORT).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                    Toast.makeText(this@BatchScannerActivity, "Gagal foto: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun finishAndReview() {
        if (capturedUris.isEmpty()) {
            Toast.makeText(this, "Ambil minimal 1 foto terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, ScanReviewActivity::class.java).apply {
            putParcelableArrayListExtra("EXTRA_URIS", ArrayList(capturedUris))
        }
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
