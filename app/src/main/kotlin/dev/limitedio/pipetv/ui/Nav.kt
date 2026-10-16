package dev.limitedio.pipetv.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S

/** Left rail: icons only while content has focus, glides open with labels when it gets focus. */
class Nav(c: Context) : AnimView(c) {
    private val tabs get() = listOf(S.t(K.HOME) to Icons.HOME, S.t(K.SEARCH) to Icons.SEARCH, S.t(K.POPULAR) to Icons.TREND,
        S.t(K.FAVORITES) to Glyphs.FAV, S.t(K.HISTORY) to Icons.HISTORY, S.t(K.SETTINGS) to Icons.SETTINGS)
    var sel = 0
        set(v) { field = v; pill.to(v.toFloat()) }
    var lit = false
        set(v) { field = v; open.to(if (v) 1f else 0f) }

    private val open = spring(0f, 200f, 26f)
    private val pill = spring(0f, 320f, 25f)
    private val bg = Ui.paint(0xF00B0D18.toInt())
    private val label = Ui.paint(Ui.TEXT, 30f, true)
    private val icon = Ui.paint(Ui.DIM)
    private val brand = Ui.paint(Ui.TEXT, 38f, true)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val r = RectF()

    private val itemH get() = Ui.u(96)
    private val y0 get() = Ui.u(250)

    override fun render(c: Canvas, dt: Float) {
        val pw = Ui.u(104) + open.value.coerceIn(0f, 1f) * Ui.u(226)
        val ox = if (Ui.rtl) width - pw else 0f
        c.drawRect(ox, 0f, ox + pw, height.toFloat(), bg)
        brand.alpha = (255 * open.value).toInt().coerceIn(0, 255)
        Txt.draw(c, "PipeTv", ox + Ui.u(40), Ui.u(130), pw - Ui.u(80), brand)
        val py = y0 + pill.value * itemH
        r.set(ox + Ui.u(20), py, ox + pw - Ui.u(20), py + itemH - Ui.u(14))
        fill.shader = LinearGradient(r.left, 0f, r.right, 0f, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP)
        fill.alpha = if (lit) 255 else 90
        c.drawRoundRect(r, Ui.u(24), Ui.u(24), fill)
        for ((i, t) in tabs.withIndex()) {
            val cy = y0 + i * itemH + (itemH - Ui.u(14)) / 2
            val on = i == sel
            icon.color = if (on) 0xFF0A0B14.toInt() else Ui.DIM
            val ix = if (Ui.rtl) width - Ui.u(62) else Ui.u(62)
            if (t.second == Glyphs.FAV) Glyphs.star(c, ix, cy, Ui.u(20), icon, on) else Icons.draw(c, t.second, ix, cy, Ui.u(18), icon)
            label.color = if (on) 0xFF0A0B14.toInt() else Ui.TEXT
            label.alpha = (255 * open.value).toInt().coerceIn(0, 255)
            if (open.value > .05f) Txt.draw(c, t.first, ox + if (Ui.rtl) Ui.u(30) else Ui.u(108), cy + Ui.u(10), pw - Ui.u(140), label)
        }
    }
}
