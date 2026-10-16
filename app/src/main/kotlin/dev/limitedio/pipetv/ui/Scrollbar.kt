package dev.limitedio.pipetv.ui

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.max

/** A slim scrollbar on the trailing edge: faint when the list is idle, bright and wider when it has focus. */
object Scrollbar {
    private val track = Ui.paint(0x24FFFFFF)
    private val thumb = Paint(Paint.ANTI_ALIAS_FLAG)
    private val r = RectF()

    fun draw(c: Canvas, viewW: Float, y0: Float, y1: Float, content: Float, view: Float, offset: Float, lit: Boolean) {
        if (content <= view + 1f) return
        val w = Ui.u(if (lit) 11 else 6)
        val x = if (Ui.rtl) Ui.u(14) else viewW - Ui.u(14) - w
        r.set(x, y0, x + w, y1); c.drawRoundRect(r, w / 2, w / 2, track)
        val th = max(Ui.u(64), (y1 - y0) * view / content)
        val ty = y0 + (y1 - y0 - th) * (offset / (content - view)).coerceIn(0f, 1f)
        r.set(x, ty, x + w, ty + th)
        thumb.shader = LinearGradient(0f, ty, 0f, ty + th, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP)
        thumb.alpha = if (lit) 255 else 110
        c.drawRoundRect(r, w / 2, w / 2, thumb)
    }
}
