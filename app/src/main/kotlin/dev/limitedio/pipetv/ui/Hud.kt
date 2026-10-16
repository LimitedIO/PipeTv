package dev.limitedio.pipetv.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import androidx.media3.common.Player
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.media.Engine
import dev.limitedio.pipetv.media.Session
import dev.limitedio.pipetv.media.Subs
import dev.limitedio.pipetv.media.Playback

/** Fullscreen playback overlay: fades in on any key, out after a few seconds of playback. */
class Hud(c: Context) : AnimView(c) {
    private var shownAt = 0L; private var waitSince = 0L
    private val alpha = spring(0f, 180f, 24f); private val spin = spring(0f, 160f, 22f)
    private val title = Ui.paint(Ui.TEXT, 42f, true); private val sub = Ui.paint(Ui.DIM, 26f)
    private val time = Ui.paint(Ui.TEXT, 28f, true); private val hint = Ui.paint(Ui.DIM, 23f)
    private val track = Ui.paint(0x40FFFFFF); private val buf = Ui.paint(0x55FFFFFF); private val dot = Ui.paint(Ui.TEXT)
    private val badge = Ui.paint(Ui.ACCENT2, 26f, true); private val dark = Ui.paint(0x66000000)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG); private val shade = Paint()
    private val r = RectF(); private val extras = HudExtras()
    /** Buffering mid-video. The very first load is the stage backdrop's job, so the two spinners never show together. */
    private val waiting get() = Session.item != null && Engine.frame && Session.pending == null && Engine.exo.playWhenReady &&
        !Engine.exo.isPlaying && Engine.exo.playbackState != Player.STATE_ENDED && !Session.failed
    override val animating get() = waiting || spin.value > .01f
    override val ticking get() = visibility == VISIBLE
    override val tickMs get() = 120L // chrome refresh; the spinner itself runs per frame via animating

    fun nudge() { shownAt = SystemClock.uptimeMillis(); alpha.to(1f) }

    override fun render(c: Canvas, dt: Float) {
        val p = Engine.exo; val w = width.toFloat(); val h = height.toFloat(); val now = SystemClock.uptimeMillis()
        waitSince = if (waiting) (if (waitSince == 0L) now else waitSince) else 0L
        spin.target = if (waitSince != 0L && now - waitSince > 400) 1f else 0f
        alpha.target = if (now - shownAt > 3500 && p.isPlaying && !Session.loading && !Session.failed) 0f else 1f
        val a = alpha.value.coerceIn(0f, 1f)
        if (a > .01f) chrome(c, w, h, a)
        if (spin.value > .01f) { dark.alpha = (90 * spin.value).toInt(); c.drawRect(0f, 0f, w, h, dark); Spinner.draw(c, w / 2, h / 2, Ui.u(46), spin.value) }
        extras.draw(c, w, h, a)
        if (alpha.moving) postInvalidateOnAnimation()
    }

    private fun chrome(c: Canvas, w: Float, h: Float, a: Float) {
        val p = Engine.exo; val it = Session.item
        shade.shader = LinearGradient(0f, 0f, 0f, h * .3f, 0xDD000000.toInt(), 0, Shader.TileMode.CLAMP)
        shade.alpha = (255 * a).toInt(); c.drawRect(0f, 0f, w, h * .3f, shade)
        shade.shader = LinearGradient(0f, h * .6f, 0f, h, 0, 0xEE000000.toInt(), Shader.TileMode.CLAMP)
        shade.alpha = (255 * a).toInt(); c.drawRect(0f, h * .6f, w, h, shade)
        for (pt in listOf(title, sub, time, hint, dot, badge)) pt.alpha = (255 * a).toInt()
        val q = if (Playback.live) S.t(K.LIVE) else if (p.videoSize.height > 0) "${p.videoSize.height}p" else ""
        val bw = if (q.isEmpty()) 0f else badge.measureText(q) + Ui.u(40)
        if (q.isNotEmpty()) { r.set(w - Ui.u(80) - bw, Ui.u(60), w - Ui.u(80), Ui.u(112)); fill.shader = null; fill.color = 0x33FFFFFF; c.drawRoundRect(r, Ui.u(26), Ui.u(26), fill); Txt.draw(c, q, r.left, r.centerY() + Ui.u(9), bw, badge, true) }
        it?.let { Txt.draw(c, Txt.fit(title, it.title, w - Ui.u(200) - bw), Ui.u(80), Ui.u(100), w - Ui.u(200) - bw, title); Txt.draw(c, it.sub, Ui.u(80), Ui.u(144), w - Ui.u(160), sub) }
        val x0 = Ui.u(80); val x1 = w - Ui.u(80); val y = h - Ui.u(150)
        if (Playback.showProgress) {
            track.alpha = (64 * a).toInt(); buf.alpha = (85 * a).toInt()
            r.set(x0, y, x1, y + Ui.u(10)); c.drawRoundRect(r, Ui.u(5), Ui.u(5), track)
            r.right = x0 + (x1 - x0) * Playback.buffered; c.drawRoundRect(r, Ui.u(5), Ui.u(5), buf)
            r.right = x0 + (x1 - x0) * Playback.fraction
            fill.shader = LinearGradient(x0, 0f, x1, 0f, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP); fill.alpha = (255 * a).toInt()
            c.drawRoundRect(r, Ui.u(5), Ui.u(5), fill)
            SponsorMarks.draw(c, x0, y, x1, y + Ui.u(10), a)
            c.drawCircle(r.right, y + Ui.u(5), Ui.u(14), dot)
        }
        if (!Playback.live && Playback.duration > 0) {
            c.drawText(Ui.fmt(p.currentPosition.coerceAtLeast(0)), x0, y + Ui.u(52), time)
            val t = Ui.fmt(Playback.duration); c.drawText(t, x1 - time.measureText(t), y + Ui.u(52), time)
        }
        Txt.draw(c, S.t(K.HINT_PLAYER), 0f, h - Ui.u(48), w, hint, true)
        if (!p.isPlaying && !waiting && Engine.frame && !Session.failed && Session.item != null) Icons.pause(c, w / 2, h / 2, Ui.u(46), dot)
        if (Session.failed) Txt.draw(c, S.t(K.RETRYING), 0f, h / 2, w, title, true)
        if (Subs.state == 1) Txt.draw(c, S.t(K.SUBS_LOADING), 0f, h - Ui.u(300), w, hint, true)
    }
}
