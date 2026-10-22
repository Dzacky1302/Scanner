package com.example.scanner.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageCompressor {

    suspend fun compressImage(
        context: Context,
        imageUri: Uri,
        quality: Int = 60,
        maxWidth: Int = 1200,
        maxHeight: Int = 1600,
        outputFileName: String = "Compress_${System.currentTimeMillis()}.jpg"
    ): File = withContext(Dispatchers.IO) {
        val outputDir = FileUtils.getOutputDir(context)
        val outputFile = File(outputDir, if (outputFileName.endsWith(".jpg")) outputFileName else "$outputFileName.jpg")

        val inputStream = context.contentResolver.openInputStream(imageUri)
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream?.close()

        val sampleSize = calculateInSampleSize(options, maxWidth, maxHeight)

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }

        val inputStream2 = context.contentResolver.openInputStream(imageUri)
        val decodedBitmap = BitmapFactory.decodeStream(inputStream2, null, decodeOptions)
        inputStream2?.close()

        if (decodedBitmap != null) {
            val stream = ByteArrayOutputStream()
            decodedBitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(10, 100), stream)
            val byteArray = stream.toByteArray()

            FileOutputStream(outputFile).use { out ->
                out.write(byteArray)
            }
            decodedBitmap.recycle()
        }

        return@withContext outputFile
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}
