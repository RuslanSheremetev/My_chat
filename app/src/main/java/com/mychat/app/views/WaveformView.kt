package com.mychat.app.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class WaveformView @android.view.ViewConstructor constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    
    private val paint = Paint().apply {
        color = 0xffff5e8e.toInt()
        isAntiAlias = true
    }
    
    private val barCount = 12
    private val barWidth = 4f
    private val gap = 2f
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val h = height.toFloat()
        val centerY = h / 2
        
        for (i in 0 until barCount) {
            val barHeight = (h * 0.3 + Math.random() * h * 0.5).toFloat()
            val left = i * (barWidth + gap) * density
            val top = centerY - barHeight / 2
            val right = left + barWidth * density
            val bottom = centerY + barHeight / 2
            canvas.drawRoundRect(left, top, right, bottom, 2f, 2f, paint)
        }
        invalidate()
    }
    
    private val density = resources.displayMetrics.density
}
