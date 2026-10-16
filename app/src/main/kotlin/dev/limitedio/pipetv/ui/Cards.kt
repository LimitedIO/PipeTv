package dev.limitedio.pipetv.ui

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import dev.limitedio.pipetv.data.Favs
import dev.limitedio.pipetv.data.Item
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S

/** Draws a card. Videos, channels and playlists each have their own look. Position/animation come from the caller. */
object Cards {
    private val ph = Ui.paint(Ui.SURFACE)
    private val pic = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val badge = Ui.paint(0xCC000000.toInt())
    private val live = Ui.paint(0xFFE5254B.toInt())
    private val badgeText = Ui.paint(Ui.TEXT, 17f, true)
    private val title = Ui.paint(Ui.TEXT, 23f, true); private val by = Ui.paint(0xFFB4B9D2.toInt(), 20f); private val meta = Ui.paint(Ui.DIM, 18f)
    private val accent = Ui.paint(Ui.ACCENT2, 19f, true); private val bar = Ui.paint(Ui.ACCENT2)
    private val stack = Ui.paint(Ui.ACCENT); private val gold = Ui.paint(0xFFFFC83D.toInt())
    private val big = Ui.paint(Ui.TEXT, 30f, true); private val shade = Ui.paint(0xB3000000.toInt()); private val m = Matrix()
    private val r = RectF()

    fun height(w: Float) = w * 9 / 16 + Ui.u(150)

    private fun fade(p: Pic) = (255 * ((SystemClock.uptimeMillis() - p.born) / 250f).coerceIn(0f, 1f)).toInt()

    /** The rounded thumbnail (also used by the history timeline and the details hero). */
    fun picture(c: Canvas, it: Item, x: Float, y: Float, w: Float, h: Float) {
        r.set(x, y, x + w, y + h)
        val p = Img.get(it.thumb, w.toInt())
        if (p == null) { c.drawRoundRect(r, Ui.u(14), Ui.u(14), ph); return }
        m.setScale(w / p.bmp.width, h / p.bmp.height); m.postTranslate(x, y)
        p.shader.setLocalMatrix(m); pic.shader = p.shader; pic.alpha = fade(p)
        c.drawRoundRect(r, Ui.u(14), Ui.u(14), pic)
    }

    private fun tag(c: Canvas, text: String, x: Float, y: Float, w: Float, h: Float, paint: Paint) {
        val bw = badgeText.measureText(text) + Ui.u(16)
        r.set(x + w - bw - Ui.u(10), y + h - Ui.u(34), x + w - Ui.u(10), y + h - Ui.u(10))
        c.drawRoundRect(r, Ui.u(6), Ui.u(6), paint)
        c.drawText(text, r.left + Ui.u(8), r.bottom - Ui.u(6), badgeText)
    }

    /** Title (2 lines), then the channel line and the stats line (views · time ago). */
    private fun text(c: Canvas, it: Item, x: Float, y: Float, w: Float, lit: Boolean, center: Boolean = false) {
        title.color = if (lit) Ui.TEXT else 0xFFD5D8E8.toInt()
        var ty = y + Ui.u(32)
        for (l in Txt.lines(it, title, w - Ui.u(8), 2)) { Txt.draw(c, l, x + Ui.u(4), ty, w - Ui.u(8), title, center); ty += Ui.u(28) }
        Txt.draw(c, Txt.fit(by, it.by.ifEmpty { it.sub }, w - Ui.u(8)), x + Ui.u(4), y + Ui.u(98), w - Ui.u(8), by, center)
        Txt.draw(c, Txt.fit(if (it.kind == Item.VIDEO) meta else accent, it.meta, w - Ui.u(8)), x + Ui.u(4), y + Ui.u(126), w - Ui.u(8), if (it.kind == Item.VIDEO) meta else accent, center)
    }

    private fun video(c: Canvas, it: Item, x: Float, y: Float, w: Float, h: Float) {
        picture(c, it, x, y, w, h)
        if (it.isLive) tag(c, S.t(K.LIVE), x, y, w, h, live) else if (it.dur > 0) tag(c, Ui.fmt(it.dur * 1000), x, y, w, h, badge)
        if (it.progress > .02f) { r.set(x + Ui.u(14), y + h - Ui.u(7), x + Ui.u(14) + (w - Ui.u(28)) * it.progress, y + h - Ui.u(3)); c.drawRoundRect(r, Ui.u(2), Ui.u(2), bar) }
    }

    /** Playlist: a stack of layers behind the thumbnail and a count strip on the right. */
    private fun playlist(c: Canvas, it: Item, x: Float, y: Float, w: Float, h: Float) {
        stack.alpha = 70; r.set(x + Ui.u(26), y, x + w - Ui.u(26), y + Ui.u(14)); c.drawRoundRect(r, Ui.u(8), Ui.u(8), stack)
        stack.alpha = 130; r.set(x + Ui.u(13), y + Ui.u(8), x + w - Ui.u(13), y + Ui.u(22)); c.drawRoundRect(r, Ui.u(8), Ui.u(8), stack)
        picture(c, it, x, y + Ui.u(16), w, h - Ui.u(16))
        val sw = w * .3f; val sx = if (Ui.rtl) x else x + w - sw
        r.set(sx, y + Ui.u(16), sx + sw, y + h); c.drawRect(r, badge)
        if (it.meta == "∞") { accent.strokeCap = Paint.Cap.ROUND; Glyphs.infinity(c, r.centerX(), r.centerY(), Ui.u(24), accent); return }
        for (i in 0..2) c.drawRect(r.centerX() - Ui.u(22), r.centerY() - Ui.u(30) + i * Ui.u(14), r.centerX() + Ui.u(22) - (if (i == 2) Ui.u(14) else 0f), r.centerY() - Ui.u(26) + i * Ui.u(14), accent)
        Txt.draw(c, Txt.fit(badgeText, it.meta, sw - Ui.u(8)), r.left, r.centerY() + Ui.u(44), sw, badgeText, true)
    }

    /** Channel, like YouTube: just the round picture with the name and subscriber count below, no tile. */
    private fun channel(c: Canvas, it: Item, x: Float, y: Float, w: Float, h: Float) {
        val d = h * .86f; val cx = x + w / 2; val cy = y + h / 2
        val p = Img.get(it.thumb, 240)
        if (p == null) { c.drawCircle(cx, cy, d / 2, ph); return }
        val s = d / minOf(p.bmp.width, p.bmp.height)
        m.setScale(s, s); m.postTranslate(cx - d / 2 - (p.bmp.width * s - d) / 2, cy - d / 2 - (p.bmp.height * s - d) / 2)
        p.shader.setLocalMatrix(m); pic.shader = p.shader; pic.alpha = fade(p); c.drawCircle(cx, cy, d / 2, pic)
    }

    /** This card is the video being opened right now: dimmed, with a moving progress bar. */
    private fun loading(c: Canvas, x: Float, y: Float, w: Float, h: Float) {
        r.set(x, y, x + w, y + h); c.drawRoundRect(r, Ui.u(14), Ui.u(14), shade)
        Txt.draw(c, S.t(K.LOADING), x, y + h / 2 + Ui.u(8), w, big, true)
        Spinner.bar(c, x + Ui.u(18), y + h - Ui.u(26), x + w - Ui.u(18), y + h - Ui.u(16))
    }

    fun draw(c: Canvas, it: Item, x: Float, y: Float, w: Float, lit: Boolean) {
        val h = w * 9 / 16
        when (it.kind) { Item.CHANNEL -> channel(c, it, x, y, w, h); Item.PLAYLIST -> playlist(c, it, x, y, w, h); else -> video(c, it, x, y, w, h) }
        text(c, it, x, y + h, w, lit, it.kind == Item.CHANNEL)
        if (dev.limitedio.pipetv.media.Session.pending?.id == it.id) loading(c, x, y, w, h)
        if (Favs.has(it.id)) { c.drawCircle(x + Ui.u(30), y + Ui.u(30), Ui.u(19), badge); Glyphs.star(c, x + Ui.u(30), y + Ui.u(30), Ui.u(12), gold, true) }
    }
}
