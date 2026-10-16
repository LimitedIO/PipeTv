package dev.limitedio.pipetv.ui.timeline

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import dev.limitedio.pipetv.data.Entry
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.ui.Cards
import dev.limitedio.pipetv.ui.Txt
import dev.limitedio.pipetv.ui.Ui

/** Draws the two kinds of timeline lines: day headings and watched-video cards. */
class TimelinePaint(private val widthOf: () -> Int, private val rowH: Float) {
    private val day = Ui.paint(Ui.TEXT, 34f, true); private val dim = Ui.paint(Ui.DIM, 22f)
    private val time = Ui.paint(Ui.DIM, 24f, true); private val title = Ui.paint(Ui.TEXT, 27f, true)
    private val face = Ui.paint(Ui.SURFACE)
    private val dot = Ui.paint(Ui.ACCENT); private val track = Ui.paint(0x33FFFFFF); private val bar = Ui.paint(Ui.ACCENT2)
    private val red = Ui.paint(0xFFE5254B.toInt()); private val white = Ui.paint(Ui.TEXT, 24f, true)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = Ui.u(4) }
    private val r = RectF()
    private val pad get() = Ui.u(56)
    private val width get() = widthOf()
    private fun x(x: Float, w: Float) = Ui.mx(x, w, width.toFloat())

    fun day(c: Canvas, l: Line.Day, y: Float, ax: Float) {
        c.drawCircle(x(ax, 0f), y + Ui.u(40), Ui.u(11), dot)
        Txt.draw(c, l.label, x(ax + Ui.u(40), width - ax - pad - Ui.u(40)), y + Ui.u(50), width - ax - pad - Ui.u(40), day)
        Txt.draw(c, l.summary, x(ax + Ui.u(40), width - ax - pad - Ui.u(40)), y + Ui.u(84), width - ax - pad - Ui.u(40), dim)
    }

    fun entry(c: Canvas, e: Entry, y: Float, ax: Float, on: Boolean, removing: Boolean) {
        val cx = ax + Ui.u(36); val cw = width - cx - pad
        dot.alpha = if (on) 255 else 120; c.drawCircle(x(ax, 0f), y + rowH / 2, Ui.u(if (on) 9 else 6), dot)
        Txt.draw(c, Days.time(e.at), x(pad, Ui.u(110)), y + rowH / 2 + Ui.u(8), Ui.u(110), time)
        r.set(x(cx, cw), y + Ui.u(6), x(cx, cw) + cw, y + rowH - Ui.u(10)); c.drawRoundRect(r, Ui.u(18), Ui.u(18), face)
        val tw = Ui.u(208); val th = tw * 9 / 16; val tx = if (Ui.rtl) r.right - Ui.u(12) - tw else r.left + Ui.u(12)
        Cards.picture(c, e.item, tx, r.top + (r.height() - th) / 2, tw, th)
        val bx = if (Ui.rtl) r.left + Ui.u(24) else tx + tw + Ui.u(24); val bw = r.width() - tw - Ui.u(60)
        Txt.draw(c, Txt.fit(title, e.item.title, bw), bx, r.top + Ui.u(46), bw, title)
        Txt.draw(c, Txt.fit(dim, e.item.sub, bw), bx, r.top + Ui.u(78), bw, dim)
        r.set(bx, r.top + Ui.u(100), bx + minOf(bw, Ui.u(420)), r.top + Ui.u(108)); c.drawRoundRect(r, Ui.u(4), Ui.u(4), track)
        val bar0 = r.right; r.right = r.left + (bar0 - r.left) * e.fraction; c.drawRoundRect(r, Ui.u(4), Ui.u(4), bar)
        val note = if (e.fraction >= .95f) S.t(K.WATCHED) else if (e.resume > 0) S.f(K.RESUME_AT, Ui.fmt(e.resume)) else ""
        Txt.draw(c, note, bx, r.top + Ui.u(32), bw, dim)
        if (on) outline(c, x(cx, cw), y, cw)
        if (on && removing) removeChip(c, x(cx, cw) + cw, y)
    }

    private fun outline(c: Canvas, cx: Float, y: Float, cw: Float) {
        r.set(cx, y + Ui.u(6), cx + cw, y + rowH - Ui.u(10))
        ring.shader = LinearGradient(r.left, 0f, r.right, 0f, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP)
        c.drawRoundRect(r, Ui.u(18), Ui.u(18), ring)
    }

    private fun removeChip(c: Canvas, edge: Float, y: Float) {
        val w = white.measureText(S.t(K.REMOVE)) + Ui.u(48)
        val l = if (Ui.rtl) edge - width.toFloat() + pad else edge - w - Ui.u(20)
        r.set(l, y + rowH / 2 - Ui.u(28), l + w, y + rowH / 2 + Ui.u(28)); c.drawRoundRect(r, Ui.u(28), Ui.u(28), red)
        Txt.draw(c, S.t(K.REMOVE), r.left, r.centerY() + Ui.u(9), r.width(), white, true)
    }

}
