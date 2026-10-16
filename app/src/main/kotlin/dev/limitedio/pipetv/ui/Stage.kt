package dev.limitedio.pipetv.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.TextureView
import android.view.ViewGroup
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.media.Engine
import dev.limitedio.pipetv.media.Session

/** Placeholder under the video: the thumbnail with a spinner until the first frame is drawn. */
private class Backdrop(c: Context) : AnimView(c) {
    private val shade = Ui.paint(0x99000000.toInt()); private val arc = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pic = Paint(Paint.ANTI_ALIAS_FLAG); private val m = android.graphics.Matrix()
    private val text = Ui.paint(Ui.TEXT, 30f, true); private val rect = android.graphics.RectF()
    override val animating get() = Session.active && (!Engine.frame || Session.loading)

    override fun render(c: Canvas, dt: Float) {
        val it = Session.shown ?: return
        if (Engine.frame && !Session.loading) return
        val w = width.toFloat(); val h = height.toFloat()
        Img.get(it.thumb, 640)?.let { p ->
            val s = maxOf(w / p.bmp.width, h / p.bmp.height)
            m.setScale(s, s); m.postTranslate((w - p.bmp.width * s) / 2, (h - p.bmp.height * s) / 2)
            p.shader.setLocalMatrix(m); pic.shader = p.shader; c.drawRect(0f, 0f, w, h, pic)
        }
        c.drawRect(0f, 0f, w, h, shade)
        arc.style = Paint.Style.STROKE; arc.strokeWidth = Ui.u(8) * minOf(1f, w / 900f).coerceAtLeast(.5f); arc.strokeCap = Paint.Cap.ROUND; arc.color = Ui.ACCENT2
        val s = minOf(Ui.u(44), h * .16f); rect.set(w / 2 - s, h * .42f - s, w / 2 + s, h * .42f + s)
        c.drawArc(rect, System.nanoTime() / 2_500_000 % 360f, 100f, false, arc)
        if (h > Ui.u(300)) Txt.draw(c, Txt.fit(text, S.f(K.OPENING, it.title), w - Ui.u(160)), 0f, h * .42f + s + Ui.u(60), w, text, true)
    }
}

/** Hosts the video (a TextureView: it survives resizes and visibility changes without black frames). */
class Stage(c: Context) : ViewGroup(c) {
    private val back = Backdrop(c)
    private val texture = TextureView(c)

    init { addView(back); addView(texture); Engine.surface = texture }

    fun refresh() = back.invalidate()

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        back.layout(0, 0, r - l, b - t); texture.layout(0, 0, r - l, b - t)
    }
}
