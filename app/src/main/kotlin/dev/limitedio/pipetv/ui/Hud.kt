package dev.limitedio.pipetv.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import dev.limitedio.pipetv.data.Prefs
import dev.limitedio.pipetv.media.Engine
import dev.limitedio.pipetv.media.Session

/** Fullscreen playback overlay: fades in on any key, out after a few seconds of playback. */
class Hud(c: Context) : AnimView(c) {
    private var shownAt = 0L
    private val alpha = spring(0f, 180f, 24f)
    private val title = Ui.paint(Ui.TEXT, 42f, true)
    private val sub = Ui.paint(Ui.DIM, 26f)
    private val time = Ui.paint(Ui.TEXT, 28f, true)
    private val hint = Ui.paint(Ui.DIM, 23f)
    private val track = Ui.paint(0x40FFFFFF)
    private val buf = Ui.paint(0x55FFFFFF)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shade = Paint()
    private val dot = Ui.paint(Ui.TEXT)
    private val r = RectF()
    override val animating get() = Session.loading || Engine.exo.playbackState == 2
    override val ticking get() = visibility == VISIBLE

    fun nudge() { shownAt = SystemClock.uptimeMillis(); alpha.to(1f) }

    override fun render(c: Canvas, dt: Float) {
        val p = Engine.exo; val w = width.toFloat(); val h = height.toFloat()
        val idle = SystemClock.uptimeMillis() - shownAt > 3500
        val pinned = !p.isPlaying || Session.loading || Session.failed
        alpha.target = if (idle && !pinned) 0f else 1f
        val a = alpha.value.coerceIn(0f, 1f)
        if (a > .01f) chrome(c, w, h, a)
        if (animating) spinner(c, w, h)
        if (alpha.moving) postInvalidateOnAnimation()
    }

    private fun chrome(c: Canvas, w: Float, h: Float, a: Float) {
        val p = Engine.exo; val it = Session.item
        shade.shader = LinearGradient(0f, 0f, 0f, h * .3f, 0xDD000000.toInt(), 0, Shader.TileMode.CLAMP)
        shade.alpha = (255 * a).toInt(); c.drawRect(0f, 0f, w, h * .3f, shade)
        shade.shader = LinearGradient(0f, h * .6f, 0f, h, 0, 0xEE000000.toInt(), Shader.TileMode.CLAMP)
        shade.alpha = (255 * a).toInt(); c.drawRect(0f, h * .6f, w, h, shade)
        for (pt in listOf(title, sub, time, hint, dot)) pt.alpha = (255 * a).toInt()
        it?.let { c.drawText(Txt.fit(title, it.title, w - Ui.u(160)), Ui.u(80), Ui.u(100), title); c.drawText(it.sub, Ui.u(80), Ui.u(144), sub) }
        val x0 = Ui.u(80); val x1 = w - Ui.u(80); val y = h - Ui.u(150)
        val dur = p.duration.coerceAtLeast(1); val f = (p.currentPosition.toFloat() / dur).coerceIn(0f, 1f)
        r.set(x0, y, x1, y + Ui.u(10)); c.drawRoundRect(r, Ui.u(5), Ui.u(5), track)
        r.right = x0 + (x1 - x0) * (p.bufferedPosition.toFloat() / dur).coerceIn(0f, 1f); c.drawRoundRect(r, Ui.u(5), Ui.u(5), buf)
        r.right = x0 + (x1 - x0) * f
        fill.shader = LinearGradient(x0, 0f, x1, 0f, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP); fill.alpha = (255 * a).toInt()
        c.drawRoundRect(r, Ui.u(5), Ui.u(5), fill); c.drawCircle(r.right, y + Ui.u(5), Ui.u(14), dot)
        c.drawText(Ui.fmt(p.currentPosition), x0, y + Ui.u(52), time)
        val t = Ui.fmt(p.duration.coerceAtLeast(0)); c.drawText(t, x1 - time.measureText(t), y + Ui.u(52), time)
        val hints = "OK Play/Pause   ← → Seek   ↑ Quality ${Prefs.qualityLabel()}   ↓ Speed ${Prefs.speed}x   Next skip   Back Minimize"
        c.drawText(hints, (w - hint.measureText(hints)) / 2, h - Ui.u(48), hint)
        if (!p.isPlaying && !animating && !Session.failed) Icons.pause(c, w / 2, h / 2, Ui.u(46), dot)
        if (Session.failed) c.drawText("Can't play this video. Press Back.", w / 2 - Ui.u(260), h / 2, title)
    }

    private fun spinner(c: Canvas, w: Float, h: Float) {
        fill.shader = null; fill.style = Paint.Style.STROKE; fill.strokeWidth = Ui.u(8); fill.strokeCap = Paint.Cap.ROUND
        fill.color = Ui.ACCENT2; fill.alpha = 255
        val s = Ui.u(44); r.set(w / 2 - s, h / 2 - s, w / 2 + s, h / 2 + s)
        c.drawArc(r, System.nanoTime() / 2_500_000 % 360f, 100f, false, fill)
        fill.style = Paint.Style.FILL
    }
}
