package dev.limitedio.pipetv.ui

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.cos
import kotlin.math.sin

/** One loading animation for the whole app: twelve soft bars with a smooth fading tail. */
object Spinner {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }

    fun draw(c: Canvas, cx: Float, cy: Float, r: Float, alpha: Float, color: Int = Ui.ACCENT2) {
        if (alpha <= .01f) return
        val t = System.nanoTime() / 1e9f * 1.1f
        p.color = color; p.strokeWidth = r * .17f
        for (i in 0 until 12) {
            val a = i * Math.PI.toFloat() / 6f
            val tail = 1f - ((t - i / 12f) % 1f + 1f) % 1f
            p.alpha = (255 * alpha * (.12f + .88f * tail * tail)).toInt().coerceIn(0, 255)
            c.drawLine(cx + sin(a) * r * .55f, cy - cos(a) * r * .55f, cx + sin(a) * r, cy - cos(a) * r, p)
        }
    }
}
