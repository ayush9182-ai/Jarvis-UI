package com.ayush9182.jarvis

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class JarvisOrbView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var rotation = 0f
    private val animator = object : Runnable {
        override fun run() {
            rotation = (rotation + 0.8f) % 360f
            invalidate()
            postDelayed(this, 32L)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        post(animator)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(animator)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = (minOf(width, height) * 0.39f).coerceAtLeast(1f)

        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            cx,
            cy,
            radius * 1.5f,
            intArrayOf(
                Color.rgb(25, 101, 137),
                Color.rgb(7, 29, 45),
                Color.rgb(6, 16, 29)
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radius * 1.35f, paint)

        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.rgb(98, 230, 255)
        canvas.drawCircle(cx, cy, radius, paint)

        paint.color = Color.argb(170, 189, 247, 255)
        canvas.drawCircle(cx, cy, radius * 0.76f, paint)

        canvas.save()
        canvas.rotate(rotation, cx, cy)
        paint.strokeWidth = 3f
        paint.color = Color.argb(210, 98, 230, 255)
        canvas.drawArc(
            cx - radius * 1.18f,
            cy - radius * 1.18f,
            cx + radius * 1.18f,
            cy + radius * 1.18f,
            12f,
            94f,
            false,
            paint
        )

        paint.color = Color.rgb(226, 255, 120)
        paint.style = Paint.Style.FILL
        for (i in 0 until 12) {
            val a = Math.toRadians((i * 30).toDouble())
            val px = cx + cos(a).toFloat() * radius * 1.08f
            val py = cy + sin(a).toFloat() * radius * 1.08f
            canvas.drawCircle(px, py, 3.5f, paint)
        }
        canvas.restore()

        paint.color = Color.rgb(221, 249, 255)
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = radius * 0.68f
        paint.style = Paint.Style.FILL
        canvas.drawText("J", cx, cy - (paint.ascent() + paint.descent()) / 2f, paint)
    }
}
