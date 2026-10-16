package dev.limitedio.pipetv.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.KeyEvent
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.i18n.K

/** A scrolling row of topic chips (Trending, Music, News, ...) with a gliding highlight. */
class TopicChips(c: Context, private val names: List<K>) : AnimView(c) {
    var sel = 0; private set
    var lit = false
    var onChange: (Int) -> Unit = {}
    private val scroll = spring(0f, 200f, 26f)
    private val text = Ui.paint(Ui.TEXT, 28f, true); private val face = Ui.paint(Ui.SURFACE)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG); private val r = RectF()
    private val pad get() = Ui.u(32); private val gap get() = Ui.u(12)
    private fun w(i: Int) = text.measureText(S.t(names[i])) + Ui.u(56)
    private fun x(i: Int): Float { var v = 0f; for (k in 0 until i) v += w(k) + gap; return v }
    private fun mx(x: Float, w: Float) = Ui.mx(pad + x - scroll.value, w, width.toFloat())

    fun key(code: Int): Boolean {
        val n = sel + when (code) { KeyEvent.KEYCODE_DPAD_LEFT -> -1; KeyEvent.KEYCODE_DPAD_RIGHT -> 1; else -> return false }
        if (n !in names.indices) return false
        sel = n
        val vis = width - pad * 2
        scroll.to((x(n) + w(n) / 2 - vis / 2).coerceIn(0f, maxOf(0f, x(names.size) - gap - vis)))
        onChange(n); return true
    }

    override fun render(c: Canvas, dt: Float) {
        val t = Ui.u(14); val b = height - Ui.u(14)
        for (i in names.indices) {
            val cw = w(i); if (mx(x(i), cw) + cw < 0 || mx(x(i), cw) > width) continue
            r.set(mx(x(i), cw), t, mx(x(i), cw) + cw, b); c.drawRoundRect(r, r.height() / 2, r.height() / 2, face)
        }
        r.set(mx(x(sel), w(sel)), t, mx(x(sel), w(sel)) + w(sel), b)
        fill.shader = LinearGradient(r.left, 0f, r.right, 0f, Ui.ACCENT, if (lit) Ui.ACCENT2 else Ui.ACCENT, Shader.TileMode.CLAMP)
        c.drawRoundRect(r, r.height() / 2, r.height() / 2, fill)
        for (i in names.indices) {
            text.color = if (i == sel) 0xFF0A0B14.toInt() else Ui.TEXT
            Txt.draw(c, S.t(names[i]), mx(x(i), w(i)), height / 2f + Ui.u(10), w(i), text, true)
        }
    }

}
