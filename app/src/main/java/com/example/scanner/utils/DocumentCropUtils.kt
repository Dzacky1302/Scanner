package com.example.scanner.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF

object DocumentCropUtils {

    fun autoCropAndEnhance(bitmap: Bitmap, isIdCard: Boolean = false): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val cropMarginX = if (isIdCard) (width * 0.08f).toInt() else (width * 0.04f).toInt()
        val cropMarginY = if (isIdCard) (height * 0.15f).toInt() else (height * 0.04f).toInt()

        val croppedWidth = (width - 2 * cropMarginX).coerceAtLeast(100)
        val croppedHeight = (height - 2 * cropMarginY).coerceAtLeast(100)

        val croppedBitmap = Bitmap.createBitmap(bitmap, cropMarginX, cropMarginY, croppedWidth, croppedHeight)

        return enhanceDocumentContrast(croppedBitmap)
    }

    fun cropBitmapByRect(bitmap: Bitmap, rectF: RectF): Bitmap {
        val left = (rectF.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 10)
        val top = (rectF.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 10)
        val width = ((rectF.width() * bitmap.width).toInt()).coerceIn(10, bitmap.width - left)
        val height = ((rectF.height() * bitmap.height).toInt()).coerceIn(10, bitmap.height - top)

        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }

    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun enhanceDocumentContrast(bitmap: Bitmap): Bitmap {
        val enhancedBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(enhancedBitmap)
        val paint = Paint()

        val cm = ColorMatrix().apply {
            set(floatArrayOf(
                1.2f, 0f, 0f, 0f, -10f,
                0f, 1.2f, 0f, 0f, -10f,
                0f, 0f, 1.2f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            ))
        }

        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return enhancedBitmap
    }
}
