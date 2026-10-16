package dev.limitedio.pipetv.ui.detail

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
import dev.limitedio.pipetv.ui.Glyphs
import dev.limitedio.pipetv.ui.Txt
import dev.limitedio.pipetv.ui.Ui

/** Title, channel line and the tab bar (Play / About / Comments / Up next / Channel). */
class DetailHead(c: Context, private val title: String, private val fav: () -> Boolean) : AnimView(c) {
    var byline = ""
        set(v) { field = v; invalidate() }
    var sel = 0; private set
    var lit = true
    var onTab: (Int) -> Unit = {}
    private val keys = listOf(K.PLAY, K.ABOUT, K.COMMENTS, K.UP_NEXT, K.CHANNEL, K.FAVORITE)
    private val pill = spring(0f, 350f, 26f)
    private val h1 = Ui.paint(Ui.TEXT, 40f, true); private val sub = Ui.paint(Ui.DIM, 26f)
    private val text = Ui.paint(Ui.TEXT, 26f, true); private val face = Ui.paint(Ui.SURFACE)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG); private val r = RectF()
    private val pad get() = Ui.u(56)

    fun key(code: Int): Boolean {
        val n = sel + when (code) { KeyEvent.KEYCODE_DPAD_LEFT -> -1; KeyEvent.KEYCODE_DPAD_RIGHT -> 1; else -> return false }
        if (n !in keys.indices) return false
        sel = n; pill.to(n.toFloat()); onTab(n); return true
    }

    override fun render(c: Canvas, dt: Float) {
        val w = width - pad * 2
        val lines = Txt.wrap(h1, title, w, 2)
        lines.forEachIndexed { i, l -> Txt.draw(c, l, pad, Ui.u(62) + i * Ui.u(48), w, h1) }
        Txt.draw(c, Txt.fit(sub, byline, w), pad, Ui.u(62) + lines.size * Ui.u(48) + Ui.u(10), w, sub)
        val tw = minOf(Ui.u(230), w / 6f); val ty = height - Ui.u(78)
        for (i in keys.indices) {
            val x = pad + Ui.mx(i * tw, tw, w)
            r.set(x + Ui.u(4), ty, x + tw - Ui.u(4), ty + Ui.u(62)); c.drawRoundRect(r, Ui.u(31), Ui.u(31), face)
        }
        val px = pad + Ui.mx(pill.value * tw, tw, w)
        r.set(px + Ui.u(4), ty, px + tw - Ui.u(4), ty + Ui.u(62))
        fill.shader = LinearGradient(r.left, 0f, r.right, 0f, Ui.ACCENT, if (lit) Ui.ACCENT2 else Ui.ACCENT, Shader.TileMode.CLAMP)
        c.drawRoundRect(r, Ui.u(31), Ui.u(31), fill)
        for (i in keys.indices) {
            text.color = if (i == sel) 0xFF0A0B14.toInt() else Ui.TEXT
            val x = pad + Ui.mx(i * tw, tw, w)
            if (i == 5) {
                val on = fav(); text.alpha = 255
                Glyphs.star(c, x + Ui.u(34), ty + Ui.u(31), Ui.u(14), text, on)
                Txt.draw(c, S.t(if (on) K.SAVED else K.FAVORITE), x + Ui.u(26), ty + Ui.u(40), tw - Ui.u(26), text, true)
            } else Txt.draw(c, S.t(keys[i]), x, ty + Ui.u(40), tw, text, true)
        }
    }
}
