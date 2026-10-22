package com.example.scanner.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object IdCardUtils {

    suspend fun createIdCardPdf(
        context: Context,
        frontUri: Uri,
        backUri: Uri?,
        outputFileName: String = "KTP_Scan_${System.currentTimeMillis()}.pdf"
    ): File = withContext(Dispatchers.IO) {
        val outputDir = FileUtils.getOutputDir(context)
        val outputFile = File(outputDir, if (outputFileName.endsWith(".pdf")) outputFileName else "$outputFileName.pdf")

        val pdfDocument = PdfDocument()
        val pageWidth = 595 // Standard A4 width in points
        val pageHeight = 842 // Standard A4 height in points

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)

        val titlePaint = Paint().apply {
            color = Color.parseColor("#1A1C1E")
            textSize = 18f
            isFakeBoldText = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#44474F")
            textSize = 12f
        }

        canvas.drawText("DOKUMEN KARTU IDENTITAS (KTP)", 40f, 50f, titlePaint)
        canvas.drawText("Hasil Pindai Resmi Scanner Pro", 40f, 70f, subtitlePaint)

        // Draw Front Image
        val frontBitmap = loadBitmap(context, frontUri)
        if (frontBitmap != null) {
            val cardWidth = 480f
            val cardHeight = 280f
            val left = (pageWidth - cardWidth) / 2f
            val top = 100f

            val rect = RectF(left, top, left + cardWidth, top + cardHeight)
            canvas.drawBitmap(frontBitmap, null, rect, null)

            val labelPaint = Paint().apply {
                color = Color.parseColor("#0D6EFD")
                textSize = 11f
                isFakeBoldText = true
            }
            canvas.drawText("SISI DEPAN (FRONT)", left, top + cardHeight + 20f, labelPaint)
            frontBitmap.recycle()
        }

        // Draw Back Image if available
        if (backUri != null) {
            val backBitmap = loadBitmap(context, backUri)
            if (backBitmap != null) {
                val cardWidth = 480f
                val cardHeight = 280f
                val left = (pageWidth - cardWidth) / 2f
                val top = 440f

                val rect = RectF(left, top, left + cardWidth, top + cardHeight)
                canvas.drawBitmap(backBitmap, null, rect, null)

                val labelPaint = Paint().apply {
                    color = Color.parseColor("#0D6EFD")
                    textSize = 11f
                    isFakeBoldText = true
                }
                canvas.drawText("SISI BELAKANG (BACK)", left, top + cardHeight + 20f, labelPaint)
                backBitmap.recycle()
            }
        }

        pdfDocument.finishPage(page)

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return@withContext outputFile
    }

    private fun loadBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
