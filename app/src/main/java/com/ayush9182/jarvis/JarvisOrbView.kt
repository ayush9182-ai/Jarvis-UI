package com.ayush9182.jarvis

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/** Lightweight, battery-friendly JARVIS HUD. It animates only while visible. */
class JarvisOrbView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var rotation = 0f
    private val runnable = object : Runnable {
        override fun run() {
            rotation = (rotation + 0.8f) % 360f
            invalidate()
            postDelayed(this, 32L)
        }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); post(runnable) }
    override fun onDetachedFromWindow() { removeCallbacks(runnable); super.onDetachedFromWindow() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = (minOf(width, height) * 0.39f).coerceAtLeast(1f)
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(cx, cy, radius * 1.5f, intArrayOf(Color.rgb(25, 101, 137), Color.rgb(7, 29, 45), Color.rgb(6, 16, 29)), floatArrayOf(0f, .55f, 1f), Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, radius * 1.35f, paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.rgb(98, 230, 255)
        canvas.drawCircle(cx, cy, radius, paint)
        paint.color = Color.argb(170, 189, 247, 255)
        canvas.drawCircle(cx, cy, radius * .76f, paint)
        canvas.save()
        canvas.rotate(rotation, cx, cy)
        paint.color = Color.argb(210, 98, 230, 255)
        paint.strokeWidth = 3f
        canvas.drawArc(cx - radius * 1.18f, cy - radius * 1.18f, cx + radius * 1.18f, cy + radius * 1.18f, 12f, 94f, false, paint)
        paint.color = Color.rgb(226, 255, 120)
        paint.style = Paint.Style.FILL
        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30).toDouble())
            canvas.drawCircle(cx + cos(angle).toFloat() * radius * 1.08f, cy + sin(angle).toFloat() * radius * 1.08f, 3.5f, paint)
        }
        canvas.restore()
        paint.color = Color.rgb(221, 249, 255)
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = radius * .65f
        paint.style = Paint.Style.FILL
        canvas.drawText("J", cx, cy - (paint.ascent() + paint.descent()) / 2f, paint)
    }
}
