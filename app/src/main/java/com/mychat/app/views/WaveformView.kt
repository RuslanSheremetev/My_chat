package com.mychat.app.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    var isAnimating: Boolean = false
    
    private val paint = Paint().apply {
        color = Color.parseColor("#ff5e8e")
        isAntiAlias = true
    }
    
    private val barCount = 12
    private val barWidth = 4f
    private val gap = 2f
    private val rand = Random(42)
    private val barHeights = FloatArray(barCount) { rand.nextFloat() }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val h = height.toFloat()
        val density = resources.displayMetrics.density
        
        for (i in 0 until barCount) {
            val barHeight = (h * 0.2f + barHeights[i] * h * 0.6f)
            val left = i * (barWidth + gap) * density
            val top = (h - barHeight) / 2
            val right = left + barWidth * density
            val bottom = top + barHeight
            canvas.drawRoundRect(left, top, right, bottom, 2f, 2f, paint)
        }
        
        if (isAnimating) {
            for (i in 0 until barCount) {
                barHeights[i] = rand.nextFloat()
            }
            postInvalidateDelayed(300)
        }
    }
    
    fun startAnimation() {
        isAnimating = true
        postInvalidate()
    }
    
    fun stopAnimation() {
        isAnimating = false
        postInvalidate()
    }
}
