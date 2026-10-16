package dev.limitedio.pipetv.ui

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import dev.limitedio.pipetv.data.Item
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S

/** Draws one card (video, channel or playlist). Pure drawing: position and animation come from the caller. */
object Cards {
    private val ph = Ui.paint(Ui.SURFACE)
    private val pic = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val badge = Ui.paint(0xCC000000.toInt())
    private val live = Ui.paint(0xFFE5254B.toInt())
    private val badgeText = Ui.paint(Ui.TEXT, 17f, true)
    private val title = Ui.paint(Ui.TEXT, 23f, true)
    private val sub = Ui.paint(Ui.DIM, 19f)
    private val bar = Ui.paint(Ui.ACCENT2)
    private val m = Matrix()
    private val r = RectF()

    fun height(w: Float) = w * 9 / 16 + Ui.u(104)

    private fun fade(p: Pic) = (255 * ((SystemClock.uptimeMillis() - p.born) / 250f).coerceIn(0f, 1f)).toInt()

    fun picture(c: Canvas, it: Item, x: Float, y: Float, w: Float, h: Float) {
        val rad = Ui.u(14)
        r.set(x, y, x + w, y + h)
        val p = Img.get(it.thumb, if (it.kind == Item.CHANNEL) 200 else w.toInt())
        if (p == null || it.kind == Item.CHANNEL) c.drawRoundRect(r, rad, rad, ph)
        if (p == null) return
        pic.alpha = fade(p); pic.shader = p.shader
        if (it.kind == Item.CHANNEL) {
            val d = h * .72f; val s = d / minOf(p.bmp.width, p.bmp.height)
            m.setScale(s, s); m.postTranslate(x + (w - d) / 2 - (p.bmp.width * s - d) / 2, y + (h - d) / 2 - (p.bmp.height * s - d) / 2)
            p.shader.setLocalMatrix(m); c.drawCircle(x + w / 2, y + h / 2, d / 2, pic)
        } else {
            m.setScale(w / p.bmp.width, h / p.bmp.height); m.postTranslate(x, y)
            p.shader.setLocalMatrix(m); c.drawRoundRect(r, rad, rad, pic)
        }
    }

    private fun tag(c: Canvas, text: String, x: Float, y: Float, w: Float, h: Float, paint: Paint) {
        val bw = badgeText.measureText(text) + Ui.u(16)
        r.set(x + w - bw - Ui.u(10), y + h - Ui.u(34), x + w - Ui.u(10), y + h - Ui.u(10))
        c.drawRoundRect(r, Ui.u(6), Ui.u(6), paint)
        c.drawText(text, r.left + Ui.u(8), r.bottom - Ui.u(6), badgeText)
    }

    fun draw(c: Canvas, it: Item, x: Float, y: Float, w: Float, lit: Boolean) {
        val h = w * 9 / 16
        picture(c, it, x, y, w, h)
        when {
            it.isLive -> tag(c, S.t(K.LIVE), x, y, w, h, live)
            it.kind == Item.PLAYLIST -> tag(c, S.t(K.PLAYLIST), x, y, w, h, badge)
            it.kind == Item.VIDEO && it.dur > 0 -> tag(c, Ui.fmt(it.dur * 1000), x, y, w, h, badge)
        }
        if (it.progress > .02f) { r.set(x + Ui.u(14), y + h - Ui.u(7), x + Ui.u(14) + (w - Ui.u(28)) * it.progress, y + h - Ui.u(3)); c.drawRoundRect(r, Ui.u(2), Ui.u(2), bar) }
        title.color = if (lit) Ui.TEXT else 0xFFD5D8E8.toInt()
        var ty = y + h + Ui.u(32)
        for (l in Txt.lines(it, title, w - Ui.u(8), 2)) { Txt.draw(c, l, x + Ui.u(4), ty, w - Ui.u(8), title); ty += Ui.u(28) }
        Txt.draw(c, Txt.fit(sub, it.sub, w - Ui.u(8)), x + Ui.u(4), y + h + Ui.u(32) + Ui.u(28) * 2 + Ui.u(2), w - Ui.u(8), sub)
    }
}
