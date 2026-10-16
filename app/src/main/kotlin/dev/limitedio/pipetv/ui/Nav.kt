package dev.limitedio.pipetv.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.media.Session
import dev.limitedio.pipetv.media.Engine

/** Left rail: icons only while content has focus, glides open with labels when it gets focus. */
class Nav(c: Context) : AnimView(c) {
    private val tabs get() = listOf(S.t(K.HOME) to Icons.HOME, S.t(K.SEARCH) to Icons.SEARCH, S.t(K.POPULAR) to Icons.TREND,
        S.t(K.FAVORITES) to Glyphs.FAV, S.t(K.HISTORY) to Icons.HISTORY, S.t(K.SETTINGS) to Icons.SETTINGS, S.t(K.EXIT) to Glyphs.EXIT)
    var sel = 0
        set(v) { field = v; pill.to(v.toFloat()) }
    /** The Exit entry under Settings has focus (it is not a page). */
    var exit = false
        set(v) { field = v; pill.to(if (v) 6f else sel.toFloat()) }
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
    private val status = Ui.paint(Ui.ACCENT2, 26f, true)
    private val playingTitle = Ui.paint(Ui.TEXT, 26f, true)
    private val playerHint = Ui.paint(Ui.DIM, 26f)
    private val playerCard = Ui.paint(Ui.SURFACE)

    private val itemH get() = Ui.u(90)
    private val y0 get() = Ui.u(if (Session.item != null) 230 else 190)

    override fun render(c: Canvas, dt: Float) {
        val expanded = open.value.coerceIn(0f, 1f)
        val pw = Ui.u(104) + expanded * Ui.u(236)
        val ox = if (Ui.rtl) width - pw else 0f
        c.drawRect(ox, 0f, ox + pw, height.toFloat(), bg)
        brand.alpha = (255 * expanded).toInt().coerceIn(0, 255)
        Txt.draw(c, "PipeTv", ox + Ui.u(40), Ui.u(if (Session.item != null) 64 else 130), pw - Ui.u(80), brand)
        Session.item?.let { item ->
            if (expanded > .15f) {
                val x = ox + Ui.u(20); val cw = pw - Ui.u(40)
                r.set(x, Ui.u(88), x + cw, Ui.u(208)); c.drawRoundRect(r, Ui.u(18), Ui.u(18), playerCard)
                val tx = x + Ui.u(16); val tw = cw - Ui.u(32)
                Txt.draw(c, Txt.fit(status, S.t(if (Engine.exo.playWhenReady) K.NOW_PLAYING else K.PAUSED), tw), tx, Ui.u(118), tw, status)
                Txt.draw(c, Txt.fit(playingTitle, item.title, tw), tx, Ui.u(154), tw, playingTitle)
                val cue = S.f(K.PLAYER_MENU_HINT, if (Ui.rtl) "→" else "←")
                Txt.draw(c, Txt.fit(playerHint, cue, tw), tx, Ui.u(188), tw, playerHint)
            } else {
                val cx = if (Ui.rtl) width - Ui.u(52) else Ui.u(52)
                c.drawCircle(cx, Ui.u(134), Ui.u(8), status)
            }
        }
        val py = y0 + pill.value * itemH
        r.set(ox + Ui.u(20), py, ox + pw - Ui.u(20), py + itemH - Ui.u(14))
        fill.shader = LinearGradient(r.left, 0f, r.right, 0f, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP)
        fill.alpha = if (lit) 255 else 90
        c.drawRoundRect(r, Ui.u(24), Ui.u(24), fill)
        for ((i, t) in tabs.withIndex()) {
            val cy = y0 + i * itemH + (itemH - Ui.u(14)) / 2
            val on = if (exit) i == 6 else i == sel
            icon.color = if (on && lit) 0xFF0A0B14.toInt() else Ui.DIM
            val ix = if (Ui.rtl) width - Ui.u(52) else Ui.u(52) // centre of the 104-wide collapsed rail (same as the highlight)
            if (t.second == Glyphs.FAV) Glyphs.star(c, ix, cy, Ui.u(20), icon, on) else if (t.second == Glyphs.EXIT) Glyphs.power(c, ix, cy, Ui.u(18), icon) else Icons.draw(c, t.second, ix, cy, Ui.u(18), icon)
            label.color = if (on && lit) 0xFF0A0B14.toInt() else Ui.TEXT
            label.alpha = (255 * expanded).toInt().coerceIn(0, 255)
            if (expanded > .05f) Txt.draw(c, t.first, ox + if (Ui.rtl) Ui.u(30) else Ui.u(108), cy + Ui.u(10), pw - Ui.u(140), label)
        }
    }
}
