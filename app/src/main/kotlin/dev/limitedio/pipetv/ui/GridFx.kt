package dev.limitedio.pipetv.ui

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader

/** Focus ring and loading shimmer for [Grid]. */
class GridFx {
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val shade = Paint(Paint.ANTI_ALIAS_FLAG)
    private val r = RectF()

    /** Gradient ring (with a soft glow) around the thumbnail at x, y (already scroll-adjusted). */
    fun ring(c: Canvas, x: Float, y: Float, w: Float, pop: Float) {
        val sc = 1f + .05f * pop
        val th = w * 9 / 16
        val hw = w * sc / 2 + Ui.u(5); val hh = th * sc / 2 + Ui.u(5)
        r.set(x + w / 2 - hw, y + th / 2 - hh, x + w / 2 + hw, y + th / 2 + hh)
        stroke.shader = LinearGradient(r.left, r.top, r.right, r.bottom, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP)
        stroke.alpha = 70; stroke.strokeWidth = Ui.u(14); c.drawRoundRect(r, Ui.u(20), Ui.u(20), stroke)
        stroke.alpha = 255; stroke.strokeWidth = Ui.u(4); c.drawRoundRect(r, Ui.u(18), Ui.u(18), stroke)
    }

    /** Pulsing placeholder cards shown while a page loads. */
    fun skeleton(c: Canvas, pad: Float, top: Float, w: Float, gap: Float, rowH: Float, cols: Int, busy: Boolean) {
        if (!busy) return
        val ph = (System.nanoTime() / 1_000_000 % 1200) / 1200f
        shade.shader = null
        for (i in 0 until cols * 2) {
            val x = pad + (i % cols) * (w + gap); val y = top + (i / cols) * rowH
            shade.color = Ui.SURFACE
            shade.alpha = (150 + 90 * Math.sin((ph * 2 - i * .2) * Math.PI)).toInt().coerceIn(60, 255)
            c.drawRoundRect(x, y, x + w, y + w * 9 / 16, Ui.u(14), Ui.u(14), shade)
        }
    }
}
