package com.example.tracker

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class ScanningOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val boxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val scanLinePaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 5f
        alpha = 150
    }

    private var boundingBoxes: List<Rect> = emptyList()
    private var scanLineY = 0f
    private var scanLineDirection = 1
    private var alignmentBox: RectF? = null
    private val tempRectF = RectF()
    private var isPaused = false

    init {
        // Start animation
        post(object : Runnable {
            override fun run() {
                if (!isPaused) {
                    updateScanLine()
                    invalidate()
                }
                postDelayed(this, 20)
            }
        })
    }

    fun setPaused(paused: Boolean) {
        isPaused = paused
        invalidate()
    }

    fun setBoundingBoxes(boxes: List<Rect>) {
        boundingBoxes = boxes
        invalidate()
    }

    fun setAlignmentBox(rect: RectF) {
        alignmentBox = rect
        invalidate()
    }

    private fun updateScanLine() {
        val rect = alignmentBox ?: return
        
        if (scanLineY < rect.top || scanLineY > rect.bottom) {
            scanLineY = rect.top
        }

        scanLineY += 5f * scanLineDirection
        if (scanLineY > rect.bottom) {
            scanLineY = rect.bottom
            scanLineDirection = -1
        } else if (scanLineY < rect.top) {
            scanLineY = rect.top
            scanLineDirection = 1
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw scan line within alignment box
        alignmentBox?.let { rect ->
            canvas.drawLine(rect.left, scanLineY, rect.right, scanLineY, scanLinePaint)
        }

        // Draw detected text boxes only if they are near or inside the alignment box
        alignmentBox?.let { alignment ->
            for (box in boundingBoxes) {
                tempRectF.set(box)
                if (RectF.intersects(alignment, tempRectF)) {
                    canvas.drawRect(box, boxPaint)
                }
            }
        }
    }
}