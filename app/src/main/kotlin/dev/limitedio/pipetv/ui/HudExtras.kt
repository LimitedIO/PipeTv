package dev.limitedio.pipetv.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import dev.limitedio.pipetv.data.Prefs
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.media.Engine
import dev.limitedio.pipetv.media.Session

/** Overlays that sit on top of the fullscreen video: the "opening next…" pill and the autoplay countdown card. */
class HudExtras {
    private val pill = Ui.paint(0xE6141829.toInt()); private val text = Ui.paint(Ui.TEXT, 28f, true)
    private val small = Ui.paint(Ui.DIM, 22f); private val acc = Ui.paint(Ui.ACCENT2, 24f, true)
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; color = Ui.ACCENT2 }
    private val r = RectF()

    fun draw(c: Canvas, w: Float, h: Float) { opening(c, w); countdown(c, w, h) }

    private fun opening(c: Canvas, w: Float) {
        val p = Session.pending ?: return
        if (Session.item == null) return
        val s = Txt.fit(text, S.f(K.OPENING, p.title), w * .6f); val tw = text.measureText(s) + Ui.u(110)
        r.set((w - tw) / 2, Ui.u(40), (w + tw) / 2, Ui.u(112)); c.drawRoundRect(r, Ui.u(36), Ui.u(36), pill)
        arc.strokeWidth = Ui.u(5); val a = Ui.u(15); val cx = r.left + Ui.u(44)
        c.drawArc(cx - a, r.centerY() - a, cx + a, r.centerY() + a, System.nanoTime() / 2_500_000 % 360f, 100f, false, arc)
        c.drawText(s, r.left + Ui.u(76), r.centerY() + Ui.u(10), text)
    }

    private fun countdown(c: Canvas, w: Float, h: Float) {
        val it = Session.item; val next = Session.related.firstOrNull()
        val p = Engine.exo; val left = p.duration - p.currentPosition
        if (it == null || next == null || !Prefs.autoplay || it.isLive || p.duration <= 0 || left > 8000 || left < 0 || Session.loading) return
        val cw = Ui.u(560); val x = if (Ui.rtl) Ui.u(70) else w - cw - Ui.u(70); val y = h - Ui.u(400)
        r.set(x, y, x + cw, y + Ui.u(130)); c.drawRoundRect(r, Ui.u(22), Ui.u(22), pill)
        Cards.picture(c, next, x + Ui.u(14), y + Ui.u(14), Ui.u(170), Ui.u(102))
        val tx = x + Ui.u(204); val tw = cw - Ui.u(224)
        Txt.draw(c, S.f(K.UP_NEXT_IN, (left / 1000 + 1).toInt()), tx, y + Ui.u(40), tw, acc)
        Txt.draw(c, Txt.fit(small, next.title, tw), tx, y + Ui.u(78), tw, small)
        Txt.draw(c, Txt.fit(small, next.by, tw), tx, y + Ui.u(108), tw, small)
    }
}
