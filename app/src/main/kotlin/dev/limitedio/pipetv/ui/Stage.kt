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
    private val shade = Ui.paint(0x99000000.toInt())
    private val pic = Paint(Paint.ANTI_ALIAS_FLAG); private val m = android.graphics.Matrix()
    private val text = Ui.paint(Ui.TEXT, 30f, true); private val rect = android.graphics.RectF()
    override val animating get() = Session.active && !Engine.frame

    override fun render(c: Canvas, dt: Float) {
        val it = Session.shown ?: return
        if (Engine.frame) return
        val w = width.toFloat(); val h = height.toFloat()
        Img.get(it.thumb, 640)?.let { p ->
            val s = maxOf(w / p.bmp.width, h / p.bmp.height)
            m.setScale(s, s); m.postTranslate((w - p.bmp.width * s) / 2, (h - p.bmp.height * s) / 2)
            p.shader.setLocalMatrix(m); pic.shader = p.shader; c.drawRect(0f, 0f, w, h, pic)
        }
        c.drawRect(0f, 0f, w, h, shade)
        val s = minOf(Ui.u(44), h * .16f)
        Spinner.draw(c, w / 2, h * .42f, s, 1f)
        if (h > Ui.u(300)) Txt.draw(c, Txt.fit(text, S.f(K.OPENING, it.title), w - Ui.u(160)), 0f, h * .42f + s + Ui.u(60), w, text, true)
    }
}

/** Hosts the video (a TextureView: it survives resizes and visibility changes without black frames). */
class Stage(c: Context) : ViewGroup(c) {
    private val back = Backdrop(c)
    private val texture = TextureView(c)

    init { setBackgroundColor(android.graphics.Color.BLACK); addView(back); addView(texture); Engine.surface = texture }

    fun refresh() = back.invalidate()

    /** Copies the current video frame (scaled) into [bmp]; false while no frame is available. */
    fun grab(bmp: android.graphics.Bitmap): Boolean = Engine.frame && texture.isAvailable && runCatching { texture.getBitmap(bmp) }.isSuccess
    /** Re-fits the picture (e.g. when the video's aspect ratio became known) without the parent re-measuring. */
    fun refit() = onLayout(true, left, top, right, bottom)

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val w = r - l; val h = b - t; val ar = Engine.aspect
        var tw = w; var th = (w / ar).toInt()
        if (th > h) { th = h; tw = (h * ar).toInt() }
        back.layout(0, 0, w, h)
        texture.layout((w - tw) / 2, (h - th) / 2, (w + tw) / 2, (h + th) / 2) // fit inside: never stretched, tall videos get side bars
    }
}
