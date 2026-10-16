package dev.limitedio.pipetv.ui.search

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.KeyEvent
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.ui.AnimView
import dev.limitedio.pipetv.ui.Txt
import dev.limitedio.pipetv.ui.Ui

/** Result filter chips: All / Videos / Channels / Playlists. */
class Chips(c: Context) : AnimView(c) {
    val values = listOf("all", "videos", "channels", "playlists")
    private val names = listOf(K.F_ALL, K.F_VIDEOS, K.F_CHANNELS, K.F_PLAYLISTS)
    var sel = 0; private set
    var lit = false
    var onChange: () -> Unit = {}
    private val pill = spring(0f, 350f, 26f)
    private val text = Ui.paint(Ui.TEXT, 28f, true)
    private val face = Ui.paint(Ui.SURFACE)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val r = RectF()

    fun key(code: Int): Boolean {
        val d = when (code) { KeyEvent.KEYCODE_DPAD_LEFT -> -1; KeyEvent.KEYCODE_DPAD_RIGHT -> 1; else -> return false }
        val n = sel + d
        if (n !in 0..3) return false
        sel = n; pill.to(n.toFloat()); onChange()
        return true
    }

    override fun render(c: Canvas, dt: Float) {
        val pad = Ui.u(32); val w = (width - pad * 2) / 4f
        for (i in 0 until 4) {
            val x = pad + Ui.mx(i * w, w, width - pad * 2)
            r.set(x + Ui.u(5), Ui.u(14), x + w - Ui.u(5), height - Ui.u(14)); c.drawRoundRect(r, r.height() / 2, r.height() / 2, face)
        }
        val px = pad + Ui.mx(pill.value * w, w, width - pad * 2)
        r.set(px + Ui.u(5), Ui.u(14), px + w - Ui.u(5), height - Ui.u(14))
        fill.shader = LinearGradient(r.left, 0f, r.right, 0f, Ui.ACCENT, if (lit) Ui.ACCENT2 else Ui.ACCENT, Shader.TileMode.CLAMP)
        c.drawRoundRect(r, r.height() / 2, r.height() / 2, fill)
        for (i in 0 until 4) {
            val x = pad + Ui.mx(i * w, w, width - pad * 2)
            text.color = if (i == sel) 0xFF0A0B14.toInt() else Ui.TEXT
            Txt.draw(c, S.t(names[i]), x, height / 2f + Ui.u(10), w, text, true)
        }
    }
}
