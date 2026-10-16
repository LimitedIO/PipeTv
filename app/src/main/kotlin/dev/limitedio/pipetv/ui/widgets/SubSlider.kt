package dev.limitedio.pipetv.ui.widgets

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import dev.limitedio.pipetv.ui.Txt
import dev.limitedio.pipetv.ui.Ui

/** The big slider shown inside a [SubMenu]. */
class SubSlider(val min: Float, val max: Float, val step: Float, val get: () -> Float, val set: (Float) -> Unit, val fmt: (Float) -> String) {
    private val big = Ui.paint(Ui.TEXT, 64f, true)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val r = RectF()

    fun nudge(dir: Int, fast: Boolean) = set((Math.round((get() + dir * step * (if (fast) 4 else 1)) / step) * step).coerceIn(min, max))

    fun draw(c: Canvas, x: Float, y: Float, w: Float) {
        val f = ((get() - min) / (max - min)).coerceIn(0f, 1f)
        Txt.draw(c, fmt(get()), x, y + Ui.u(40), w, big, true)
        val ty = y + Ui.u(110)
        r.set(x, ty - Ui.u(7), x + w, ty + Ui.u(7)); c.drawRoundRect(r, Ui.u(7), Ui.u(7), RowStyle.track)
        fill.shader = LinearGradient(x, 0f, x + w, 0f, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP)
        r.right = x + w * f; c.drawRoundRect(r, Ui.u(7), Ui.u(7), fill)
        c.drawCircle(r.right, ty, Ui.u(20), RowStyle.knob)
    }
}
