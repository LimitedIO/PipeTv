package dev.limitedio.pipetv.ui

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import dev.limitedio.pipetv.data.Item

/** Draws one video card. Pure drawing: positions and animation come from the caller. */
object Cards {
    private val ph = Ui.paint(Ui.SURFACE)
    private val pic = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val badge = Ui.paint(0xCC000000.toInt())
    private val badgeText = Ui.paint(Ui.TEXT, 17f, true)
    private val title = Ui.paint(Ui.TEXT, 23f, true)
    private val sub = Ui.paint(Ui.DIM, 19f)
    private val m = Matrix()
    private val r = RectF()

    fun height(w: Float) = w * 9 / 16 + Ui.u(104)

    fun draw(c: Canvas, it: Item, x: Float, y: Float, w: Float, lit: Boolean) {
        val h = w * 9 / 16
        val rad = Ui.u(14)
        r.set(x, y, x + w, y + h)
        val p = Img.get(it.thumb, w.toInt())
        if (p == null) c.drawRoundRect(r, rad, rad, ph) else {
            m.setScale(w / p.bmp.width, h / p.bmp.height); m.postTranslate(x, y)
            p.shader.setLocalMatrix(m)
            pic.shader = p.shader
            pic.alpha = (255 * ((SystemClock.uptimeMillis() - p.born) / 250f).coerceIn(0f, 1f)).toInt()
            c.drawRoundRect(r, rad, rad, pic)
        }
        if (it.dur > 0) {
            val t = Ui.fmt(it.dur * 1000)
            val bw = badgeText.measureText(t) + Ui.u(16)
            val b = RectF(x + w - bw - Ui.u(10), y + h - Ui.u(34), x + w - Ui.u(10), y + h - Ui.u(10))
            c.drawRoundRect(b, Ui.u(6), Ui.u(6), badge)
            c.drawText(t, b.left + Ui.u(8), b.bottom - Ui.u(6), badgeText)
        }
        title.color = if (lit) Ui.TEXT else 0xFFD5D8E8.toInt()
        var ty = y + h + Ui.u(32)
        for (l in Txt.lines(it, title, w - Ui.u(8), 2)) { c.drawText(l, x + Ui.u(4), ty, title); ty += Ui.u(28) }
        c.drawText(Txt.fit(sub, it.sub, w - Ui.u(8)), x + Ui.u(4), y + h + Ui.u(32) + Ui.u(28) * 2 + Ui.u(2), sub)
    }
}
