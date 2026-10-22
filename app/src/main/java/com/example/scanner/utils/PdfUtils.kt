package com.example.scanner.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object PdfUtils {

    suspend fun imagesToPdf(
        context: Context,
        imageUris: List<Uri>,
        outputFileName: String = "Scan_${System.currentTimeMillis()}.pdf",
        quality: Int = 80
    ): File = withContext(Dispatchers.IO) {
        val outputDir = FileUtils.getOutputDir(context)
        val outputFile = File(outputDir, if (outputFileName.endsWith(".pdf")) outputFileName else "$outputFileName.pdf")

        val pdfDocument = PdfDocument()

        for (i in imageUris.indices) {
            val uri = imageUris[i]
            val inputStream = context.contentResolver.openInputStream(uri) ?: continue
            var bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (bitmap != null) {
                if (quality < 100) {
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                    val byteArray = stream.toByteArray()
                    bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                }

                val pageWidth = bitmap.width
                val pageHeight = bitmap.height
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create()
                val page = pdfDocument.startPage(pageInfo)

                val canvas = page.canvas
                val paint = Paint()
                canvas.drawBitmap(bitmap, 0f, 0f, paint)

                pdfDocument.finishPage(page)
            }
        }

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return@withContext outputFile
    }

    suspend fun mergePdfs(
        context: Context,
        pdfUris: List<Uri>,
        outputFileName: String = "Merged_${System.currentTimeMillis()}.pdf"
    ): File = withContext(Dispatchers.IO) {
        val outputDir = FileUtils.getOutputDir(context)
        val outputFile = File(outputDir, if (outputFileName.endsWith(".pdf")) outputFileName else "$outputFileName.pdf")

        val pdfDocument = PdfDocument()
        var totalPageCount = 1

        for (uri in pdfUris) {
            val tempFile = FileUtils.copyUriToTempFile(context, uri, "merge_src_", ".pdf")
            try {
                val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)

                for (pageIndex in 0 until renderer.pageCount) {
                    val rendererPage = renderer.openPage(pageIndex)
                    val width = rendererPage.width
                    val height = rendererPage.height

                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    rendererPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    rendererPage.close()

                    val pageInfo = PdfDocument.PageInfo.Builder(width, height, totalPageCount++).create()
                    val pdfPage = pdfDocument.startPage(pageInfo)
                    val pdfCanvas = pdfPage.canvas
                    pdfCanvas.drawBitmap(bitmap, 0f, 0f, null)
                    pdfDocument.finishPage(pdfPage)

                    bitmap.recycle()
                }

                renderer.close()
                pfd.close()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                tempFile.delete()
            }
        }

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return@withContext outputFile
    }

    suspend fun pdfToImages(
        context: Context,
        pdfUri: Uri,
        outputPrefix: String = "PDFPage"
    ): List<File> = withContext(Dispatchers.IO) {
        val outputDir = FileUtils.getOutputDir(context)
        val tempFile = FileUtils.copyUriToTempFile(context, pdfUri, "pdf_to_img_", ".pdf")
        val imageFiles = mutableListOf<File>()

        try {
            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val imgFile = File(outputDir, "${outputPrefix}_${i + 1}_${System.currentTimeMillis()}.jpg")
                FileOutputStream(imgFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                bitmap.recycle()
                imageFiles.add(imgFile)
            }

            renderer.close()
            pfd.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            tempFile.delete()
        }

        return@withContext imageFiles
    }

    suspend fun compressPdf(
        context: Context,
        pdfUri: Uri,
        compressionRatio: Float = 0.5f,
        outputFileName: String = "Compressed_${System.currentTimeMillis()}.pdf"
    ): File = withContext(Dispatchers.IO) {
        val outputDir = FileUtils.getOutputDir(context)
        val outputFile = File(outputDir, if (outputFileName.endsWith(".pdf")) outputFileName else "$outputFileName.pdf")
        val tempFile = FileUtils.copyUriToTempFile(context, pdfUri, "compress_src_", ".pdf")

        val pdfDocument = PdfDocument()

        try {
            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)

            val quality = (compressionRatio * 100).toInt().coerceIn(20, 85)

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val scaledWidth = (page.width * compressionRatio).toInt().coerceAtLeast(100)
                val scaledHeight = (page.height * compressionRatio).toInt().coerceAtLeast(100)

                val bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                val compressedBytes = stream.toByteArray()
                val compressedBitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)

                val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, i + 1).create()
                val pdfPage = pdfDocument.startPage(pageInfo)
                val pdfCanvas = pdfPage.canvas

                val destRect = Rect(0, 0, page.width, page.height)
                pdfCanvas.drawBitmap(compressedBitmap, null, destRect, null)

                pdfDocument.finishPage(pdfPage)
                bitmap.recycle()
                compressedBitmap.recycle()
            }

            renderer.close()
            pfd.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            tempFile.delete()
        }

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return@withContext outputFile
    }
}
