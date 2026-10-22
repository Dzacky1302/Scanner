package com.example.scanner.ui

import android.Manifest
import android.app.Activity
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
import com.example.scanner.databinding.ActivityCameraScannerBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraScannerBinding
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var isFlashOn = false
    private lateinit var cameraExecutor: ExecutorService

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
        binding = ActivityCameraScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.btnBack.setOnClickListener { finish() }

        binding.btnFlash.setOnClickListener {
            toggleFlash()
        }

        binding.btnCapture.setOnClickListener {
            takePhoto()
        }

        checkPermissionAndStart()
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
            Toast.makeText(this, if (isFlashOn) "Lampu Flash Aktif" else "Lampu Flash Mati", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Perangkat tidak memiliki lampu flash", Toast.LENGTH_SHORT).show()
        }
    }

    private fun takePhoto() {
        val capture = imageCapture ?: return

        val storageDir = externalCacheDir ?: cacheDir
        val photoFile = File.createTempFile("live_scan_", ".jpg", storageDir)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = FileProvider.getUriForFile(
                        this@CameraScannerActivity,
                        "$packageName.fileprovider",
                        photoFile
                    )
                    val intent = Intent().apply {
                        data = savedUri
                    }
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                    Toast.makeText(this@CameraScannerActivity, "Gagal mengambil foto: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
