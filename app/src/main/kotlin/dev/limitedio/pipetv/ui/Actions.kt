package dev.limitedio.pipetv.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.view.KeyEvent
import dev.limitedio.pipetv.data.Favs
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.media.Engine
import dev.limitedio.pipetv.media.Session

/** Shortcut rail for the playing video, left of the main menu: open, play/pause, next, favourite, options, close. */
class Actions(c: Context, private val run: (Int) -> Unit) : AnimView(c) {
    var lit = false
        set(v) { field = v; invalidate() }
    private var sel = 0
    private val pill = spring(0f, 320f, 26f)
    private val names = listOf(K.A_OPEN, K.A_PLAYPAUSE, K.NEXT, K.FAVORITE, K.OPTIONS, K.A_CLOSE)
    private val bg = Ui.paint(0xF2101322.toInt()); private val ink = Paint(Paint.ANTI_ALIAS_FLAG); private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tip = Ui.paint(0xFF222640.toInt()); private val label = Ui.paint(Ui.TEXT, 30f, true)
    private val r = RectF(); private val path = Path()
    private val rh get() = Ui.u(104)
    private val y0 get() = (height - names.size * rh) / 2

    /** Up/Down choose, OK runs, Right (towards the content) hands focus back; returns false for keys it does not use. */
    fun key(k: Int): Boolean {
        when (k) {
            KeyEvent.KEYCODE_DPAD_UP -> if (sel > 0) sel--
            KeyEvent.KEYCODE_DPAD_DOWN -> if (sel < names.size - 1) sel++
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> run(sel)
            else -> return false
        }
        pill.to(sel.toFloat()); return true
    }

    override fun render(c: Canvas, dt: Float) {
        val w = width.toFloat(); val x0 = Ui.mx(0f, w, w)
        c.drawRect(0f, 0f, w, height.toFloat(), bg)
        val py = y0 + pill.value * rh
        r.set(Ui.u(12), py + Ui.u(6), w - Ui.u(12), py + rh - Ui.u(6))
        fill.shader = LinearGradient(r.left, 0f, r.right, 0f, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP); fill.alpha = if (lit) 255 else 70
        c.drawRoundRect(r, Ui.u(26), Ui.u(26), fill)
        for (i in names.indices) {
            ink.color = if (lit && i == sel) 0xFF0A0B14.toInt() else Ui.TEXT
            icon(c, i, w / 2, y0 + i * rh + rh / 2, Ui.u(20))
        }
        if (lit) { // the name of the focused action floats out over the main menu
            val s = S.t(names[sel]); val tw = label.measureText(s) + Ui.u(48); val ty = y0 + sel * rh + rh / 2
            val tx = if (Ui.rtl) x0 - tw - Ui.u(10) else w + Ui.u(10)
            r.set(tx, ty - Ui.u(36), tx + tw, ty + Ui.u(36)); c.drawRoundRect(r, Ui.u(36), Ui.u(36), tip)
            Txt.draw(c, s, tx, ty + Ui.u(11), tw, label, true)
        }
    }

    private fun icon(c: Canvas, i: Int, x: Float, y: Float, s: Float) {
        ink.style = Paint.Style.STROKE; ink.strokeWidth = s * .16f; ink.strokeCap = Paint.Cap.ROUND; ink.strokeJoin = Paint.Join.ROUND
        path.reset()
        when (i) {
            0 -> { val a = s * .5f; for ((sx, sy) in listOf(-1 to -1, 1 to -1, -1 to 1, 1 to 1)) { path.moveTo(x + sx * s, y + sy * (s - a)); path.lineTo(x + sx * s, y + sy * s); path.lineTo(x + sx * (s - a), y + sy * s) } }
            1 -> if (Engine.exo.playWhenReady) { Icons.pause(c, x, y, s * .8f, ink.apply { style = Paint.Style.FILL }) } else { ink.style = Paint.Style.FILL; path.moveTo(x - s * .6f, y - s); path.lineTo(x + s, y); path.lineTo(x - s * .6f, y + s); path.close() }
            2 -> { ink.style = Paint.Style.FILL; path.moveTo(x - s, y - s * .9f); path.lineTo(x + s * .5f, y); path.lineTo(x - s, y + s * .9f); path.close(); path.addRect(x + s * .65f, y - s * .9f, x + s, y + s * .9f, Path.Direction.CW) }
            3 -> { Glyphs.star(c, x, y, s * 1.1f, ink, Session.item?.let { Favs.has(it.id) } == true); return }
            4 -> for (n in -1..1) { path.moveTo(x - s, y + n * s * .65f); path.lineTo(x + s, y + n * s * .65f) }
            else -> { path.moveTo(x - s * .8f, y - s * .8f); path.lineTo(x + s * .8f, y + s * .8f); path.moveTo(x + s * .8f, y - s * .8f); path.lineTo(x - s * .8f, y + s * .8f) }
        }
        c.drawPath(path, ink); ink.style = Paint.Style.FILL
    }
}
