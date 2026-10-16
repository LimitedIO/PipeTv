package dev.limitedio.pipetv.ui

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.abs
import kotlin.math.sin

/** Five little bars that dance while a video plays (and rest while it does not). */
object Equaliser {
    private val eq = Paint(Paint.ANTI_ALIAS_FLAG)
    private val r = RectF()

    fun draw(c: Canvas, x: Float, y: Float, playing: Boolean) {
        val t = System.nanoTime() / 1e9f
        eq.shader = LinearGradient(0f, y - Ui.u(28), 0f, y, Ui.ACCENT2, Ui.ACCENT, Shader.TileMode.CLAMP)
        for (i in 0 until 5) {
            val h = Ui.u(6) + if (playing) Ui.u(22) * abs(sin(t * (2.2f + i * .7f) + i * 1.3f)) else Ui.u(3)
            r.set(x + i * Ui.u(11), y - h, x + i * Ui.u(11) + Ui.u(7), y); c.drawRoundRect(r, Ui.u(3), Ui.u(3), eq)
        }
    }
}
