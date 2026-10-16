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
    private val note = Ui.paint(Ui.DIM, 26f); private val btn = Ui.paint(0xFF0A0B14.toInt(), 28f, true)

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

    /** The "Retry" pill of an empty grid that failed to load: always focused, OK triggers it. */
    private fun button(c: Canvas, pad: Float, y: Float, total: Float, lit: Boolean) {
        val label = dev.limitedio.pipetv.i18n.S.t(dev.limitedio.pipetv.i18n.K.RETRY); val bw = btn.measureText(label) + Ui.u(100)
        val x = Ui.mx(pad, bw, total); r.set(x, y, x + bw, y + Ui.u(76))
        shade.shader = LinearGradient(r.left, 0f, r.right, 0f, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP); shade.alpha = if (lit) 255 else 120
        c.drawRoundRect(r, Ui.u(38), Ui.u(38), shade); shade.shader = null
        Txt.draw(c, label, r.left, r.centerY() + Ui.u(10), bw, btn, true)
    }

    /** Loading placeholders and the status message of an empty grid. */
    fun empty(c: Canvas, pad: Float, top: Float, w: Float, gap: Float, rowH: Float, cols: Int, busy: Boolean, total: Float, message: String?, retry: Boolean = false, lit: Boolean = false) {
        message?.let { Txt.draw(c, it, pad, top + Ui.u(40), total - pad * 2, note) }
        if (retry && !busy && message != null) button(c, pad, top + Ui.u(80), total, lit)
        if (!busy) return
        val top = if (message != null) top + Ui.u(70) else top
        val ph = (System.nanoTime() / 1_000_000 % 1200) / 1200f
        shade.shader = null
        for (i in 0 until cols * 2) {
            val x = Ui.mx(pad + (i % cols) * (w + gap), w, total); val y = top + (i / cols) * rowH
            shade.color = Ui.SURFACE
            shade.alpha = (150 + 90 * Math.sin((ph * 2 - i * .2) * Math.PI)).toInt().coerceIn(60, 255)
            c.drawRoundRect(x, y, x + w, y + w * 9 / 16, Ui.u(14), Ui.u(14), shade)
        }
    }
}
