package dev.limitedio.pipetv.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.KeyEvent
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.media.Engine
import dev.limitedio.pipetv.media.Session
import dev.limitedio.pipetv.media.Playback

/** Separate player menu, revealed only while it has focus. */
class Dock(c: Context) : AnimView(c) {
    var lit = false
        set(v) { field = v; invalidate() }
    var videoH = 0f
    var onAction: (Int) -> Unit = {}
    var onNav: () -> Unit = {}
    var drawIcon: (Canvas, Int, Float, Float, Float) -> Unit = { _, _, _, _, _ -> }
    private var sel = 0
    private val title = Ui.paint(Ui.TEXT, 26f, true)
    private val heading = Ui.paint(Ui.TEXT, 34f, true)
    private val dim = Ui.paint(Ui.DIM, 26f)
    private val track = Ui.paint(0x33FFFFFF)
    private val accent = Ui.paint(Ui.ACCENT2)
    private val face = Ui.paint(Ui.ACCENT)
    private val ring = Ui.paint(Ui.ACCENT2).apply { style = Paint.Style.STROKE; strokeWidth = Ui.u(3) }
    private val r = RectF()
    private val names = listOf(K.A_OPEN, K.A_PLAYPAUSE, K.NEXT, K.FAVORITE, K.OPTIONS, K.A_CLOSE)

    fun key(code: Int): Boolean {
        when (code) {
            KeyEvent.KEYCODE_DPAD_UP -> sel = 0
            KeyEvent.KEYCODE_DPAD_DOWN -> if (sel == 0) sel = 1
            KeyEvent.KEYCODE_DPAD_LEFT -> if (sel > 1) sel-- else return false
            KeyEvent.KEYCODE_DPAD_RIGHT -> if (sel in 1..4) sel++ else onNav()
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> onAction(sel)
            else -> return false
        }
        invalidate(); return true
    }

    override fun render(c: Canvas, dt: Float) {
        val item = Session.item ?: return
        val pad = Ui.u(24); val w = width - pad * 2; val p = Engine.exo
        Txt.draw(c, S.t(K.NOW_PLAYING), pad, Ui.u(72), w, heading)
        c.save(); c.translate(pad, Ui.u(120) + videoH + Ui.u(24))
        val lines = Txt.wrap(title, item.title, w, 2)
        lines.forEachIndexed { i, line -> Txt.draw(c, line, 0f, Ui.u(26 + i * 34), w, title) }
        if (lit && sel == 0) { r.set(0f, -videoH - Ui.u(24), w, -Ui.u(24)); c.drawRoundRect(r, Ui.u(10), Ui.u(10), ring) }
        c.translate(0f, (lines.size - 1).coerceAtLeast(0) * Ui.u(34))
        if (Playback.showProgress) {
            r.set(0f, Ui.u(40), w, Ui.u(46)); c.drawRoundRect(r, Ui.u(3), Ui.u(3), track)
            r.right = w * Playback.fraction; c.drawRoundRect(r, Ui.u(3), Ui.u(3), accent)
        }
        val time = if (Playback.live) S.t(K.LIVE) else if (Playback.duration > 0)
            Ui.fmt(p.currentPosition.coerceAtLeast(0)) + " / " + Ui.fmt(Playback.duration) else ""
        Txt.draw(c, Txt.fit(dim, time, w), 0f, Ui.u(78), w, dim)
        val cell = w / 5
        for (i in 1..5) {
            val x = Ui.mx((i - 1) * cell, cell, w)
            if (lit && sel == i) c.drawRoundRect(x + Ui.u(2), Ui.u(92), x + cell - Ui.u(2), Ui.u(142), Ui.u(12), Ui.u(12), face)
            drawIcon(c, i, x + cell / 2, Ui.u(117), Ui.u(14))
        }
        if (lit) {
            Txt.draw(c, Txt.fit(dim, S.t(names[sel]), w), 0f, Ui.u(174), w, dim, true)
        }
        c.restore()
        if (p.isPlaying) postInvalidateDelayed(500)
    }
}
