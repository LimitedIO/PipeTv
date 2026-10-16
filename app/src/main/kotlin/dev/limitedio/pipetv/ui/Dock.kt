package dev.limitedio.pipetv.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import dev.limitedio.pipetv.media.Engine
import dev.limitedio.pipetv.media.Session

/** Info card docked under the mini player: title, progress and what plays next. */
class Dock(c: Context) : AnimView(c) {
    var lit = false
        set(v) { field = v; glow.to(if (v) 1f else 0f) }
    var videoH = 0f

    private val glow = spring(0f, 200f, 26f)
    private val title = Ui.paint(Ui.TEXT, 28f, true)
    private val sub = Ui.paint(Ui.DIM, 22f)
    private val next = Ui.paint(Ui.ACCENT2, 21f)
    private val track = Ui.paint(0x33FFFFFF)
    private val bar = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val r = RectF()
    override val animating get() = Session.loading
    override val ticking get() = visibility == VISIBLE

    override fun render(c: Canvas, dt: Float) {
        val it = Session.item ?: return
        val p = Engine.exo
        var y = Ui.u(44)
        for (l in Txt.wrap(title, it.title, width.toFloat(), 2)) { c.drawText(l, 0f, y, title); y += Ui.u(34) }
        val state = when {
            Session.failed -> "Can't play this video"
            Session.loading -> "Loading" + ".".repeat((System.nanoTime() / 400_000_000 % 4).toInt())
            !p.isPlaying && p.playWhenReady.not() -> "Paused · " + Ui.fmt(p.currentPosition)
            else -> Ui.fmt(p.currentPosition) + " / " + Ui.fmt(p.duration.coerceAtLeast(0))
        }
        c.drawText(Txt.fit(sub, it.sub, width.toFloat()), 0f, y - Ui.u(4), sub)
        val by = y + Ui.u(26)
        r.set(0f, by, width.toFloat(), by + Ui.u(7)); c.drawRoundRect(r, Ui.u(4), Ui.u(4), track)
        val frac = if (p.duration > 0) (p.currentPosition.toFloat() / p.duration).coerceIn(0f, 1f) else 0f
        r.right = width * frac
        bar.shader = LinearGradient(0f, 0f, width.toFloat(), 0f, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP)
        c.drawRoundRect(r, Ui.u(4), Ui.u(4), bar)
        c.drawText(state, 0f, by + Ui.u(40), sub)
        Session.related.firstOrNull()?.let { c.drawText("Next: " + Txt.fit(next, it.title, width - Ui.u(80)), 0f, by + Ui.u(78), next) }
        if (glow.value > .01f) {
            ring.shader = bar.shader; ring.strokeWidth = Ui.u(4); ring.alpha = (255 * glow.value).toInt()
            r.set(-Ui.u(10), -videoH - Ui.u(10), width + Ui.u(10), by + Ui.u(96))
            c.drawRoundRect(r, Ui.u(20), Ui.u(20), ring)
        }
    }
}
