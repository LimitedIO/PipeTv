package dev.limitedio.pipetv.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import dev.limitedio.pipetv.Bg

/** Small message pill that springs up from the bottom edge. */
class Toast(c: Context) : AnimView(c) {
    private val pop = spring(0f, 260f, 22f)
    private val fill = Ui.paint(0xF0222640.toInt())
    private val text = Ui.paint(Ui.TEXT, 28f, true)
    private val hide = Runnable { pop.to(0f) }
    private var msg = ""
    private val r = RectF()

    fun show(s: String) {
        msg = s; pop.to(1f)
        Bg.main.removeCallbacks(hide); Bg.main.postDelayed(hide, 2600)
    }

    override fun render(c: Canvas, dt: Float) {
        if (pop.value < .01f) return
        val w = text.measureText(msg) + Ui.u(64); val h = Ui.u(64)
        val x = (width - w) / 2; val y = height - Ui.u(60) - h * pop.value.coerceIn(0f, 1.1f) - Ui.u(20)
        r.set(x, y, x + w, y + h)
        fill.alpha = (240 * pop.value.coerceIn(0f, 1f)).toInt(); text.alpha = (255 * pop.value.coerceIn(0f, 1f)).toInt()
        c.drawRoundRect(r, h / 2, h / 2, fill)
        c.drawText(msg, x + Ui.u(32), y + h / 2 + Ui.u(10), text)
    }
}
