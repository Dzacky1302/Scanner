package com.example.scanner.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class CropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    val cropRect = RectF(0.1f, 0.1f, 0.9f, 0.9f) // Normalized 0..1 coordinates

    private val borderPaint = Paint().apply {
        color = Color.parseColor("#00E5FF")
        strokeWidth = 5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val handlePaint = Paint().apply {
        color = Color.parseColor("#0D6EFD")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val maskPaint = Paint().apply {
        color = Color.parseColor("#66000000")
        style = Paint.Style.FILL
    }

    private var activeHandle = -1

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        val left = cropRect.left * w
        val top = cropRect.top * h
        val right = cropRect.right * w
        val bottom = cropRect.bottom * h

        // Draw Translucent Outer Mask
        canvas.drawRect(0f, 0f, w, top, maskPaint)
        canvas.drawRect(0f, top, left, bottom, maskPaint)
        canvas.drawRect(right, top, w, bottom, maskPaint)
        canvas.drawRect(0f, bottom, w, h, maskPaint)

        // Draw Crop Rect Border
        canvas.drawRect(left, top, right, bottom, borderPaint)

        // Draw 4 Corner Handles
        canvas.drawCircle(left, top, 20f, handlePaint)
        canvas.drawCircle(right, top, 20f, handlePaint)
        canvas.drawCircle(left, bottom, 20f, handlePaint)
        canvas.drawCircle(right, bottom, 20f, handlePaint)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val w = width.toFloat()
        val h = height.toFloat()

        val x = event.x / w
        val y = event.y / h

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                activeHandle = findNearestHandle(x, y)
                performClick()
            }
            MotionEvent.ACTION_MOVE -> {
                if (activeHandle != -1) {
                    when (activeHandle) {
                        0 -> { cropRect.left = x.coerceIn(0f, cropRect.right - 0.1f); cropRect.top = y.coerceIn(0f, cropRect.bottom - 0.1f) }
                        1 -> { cropRect.right = x.coerceIn(cropRect.left + 0.1f, 1f); cropRect.top = y.coerceIn(0f, cropRect.bottom - 0.1f) }
                        2 -> { cropRect.left = x.coerceIn(0f, cropRect.right - 0.1f); cropRect.bottom = y.coerceIn(cropRect.top + 0.1f, 1f) }
                        3 -> { cropRect.right = x.coerceIn(cropRect.left + 0.1f, 1f); cropRect.bottom = y.coerceIn(cropRect.top + 0.1f, 1f) }
                    }
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activeHandle = -1
            }
        }
        return true
    }

    private fun findNearestHandle(x: Float, y: Float): Int {
        val handles = arrayOf(
            Pair(cropRect.left, cropRect.top),
            Pair(cropRect.right, cropRect.top),
            Pair(cropRect.left, cropRect.bottom),
            Pair(cropRect.right, cropRect.bottom)
        )

        for (i in handles.indices) {
            val dx = handles[i].first - x
            val dy = handles[i].second - y
            if (dx * dx + dy * dy < 0.05f * 0.05f) {
                return i
            }
        }
        return -1
    }
}
