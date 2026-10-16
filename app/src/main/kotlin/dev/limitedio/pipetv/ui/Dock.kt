package dev.limitedio.pipetv.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.KeyEvent
import dev.limitedio.pipetv.data.Item
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.media.Engine
import dev.limitedio.pipetv.media.Session

/**
 * Everything under the docked player: an equaliser and "now playing" card with progress,
 * then the next recommended videos you can jump to with Up/Down.
 */
class Dock(c: Context) : AnimView(c) {
    var lit = false
        set(v) { field = v; glow.to(if (v) 1f else 0f); focusPop() }
    var videoH = 0f; var onExpand: () -> Unit = {}
    var onPlay: (Item) -> Unit = {}; var onFocus: () -> Unit = {}
    private val pop = spring(0f, 300f, 20f)
    val popValue get() = pop.value
    val popMoving get() = pop.moving
    private var sel = 0; private val glow = spring(0f, 200f, 26f)
    private val title = Ui.paint(Ui.TEXT, 28f, true); private val sub = Ui.paint(0xFFB4B9D2.toInt(), 22f)
    private val dim = Ui.paint(Ui.DIM, 21f); private val tag = Ui.paint(Ui.ACCENT2, 20f, true); private val rowT = Ui.paint(Ui.TEXT, 22f, true)
    private val face = Ui.paint(Ui.ACCENT); private val track = Ui.paint(0x33FFFFFF); private val bar = Paint(Paint.ANTI_ALIAS_FLAG);
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val r = RectF(); private val list get() = Session.related.take(6)

    private fun focusPop() { pop.target = if (lit && sel == 0) 1f else 0f; invalidate(); onFocus() }

    fun key(code: Int): Boolean {
        when (code) {
            KeyEvent.KEYCODE_DPAD_UP -> if (sel > 0) sel-- else return true
            KeyEvent.KEYCODE_DPAD_DOWN -> if (sel < list.size) sel++
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> if (sel == 0) onExpand() else list.getOrNull(sel - 1)?.let(onPlay)
            else -> return false
        }
        focusPop(); return true
    }

    override fun render(c: Canvas, dt: Float) {
        val it = Session.item ?: Session.pending ?: return
        val p = Engine.exo; val w = width.toFloat(); val playing = Session.item != null && p.isPlaying
        Equaliser.draw(c, if (Ui.rtl) w - Ui.u(52) else 0f, Ui.u(34), playing)
        Txt.draw(c, S.t(K.NOW_PLAYING).uppercase(), if (Ui.rtl) 0f else Ui.u(70), Ui.u(32), w - Ui.u(70), tag)
        var y = Ui.u(78)
        for (l in Txt.wrap(title, it.title, w, 2)) { Txt.draw(c, l, 0f, y, w, title); y += Ui.u(34) }
        Txt.draw(c, Txt.fit(sub, it.by.ifEmpty { it.sub }, w), 0f, Ui.u(180), w, sub)
        Txt.draw(c, Txt.fit(dim, it.meta, w), 0f, Ui.u(208), w, dim)
        r.set(0f, Ui.u(232), w, Ui.u(239)); c.drawRoundRect(r, Ui.u(4), Ui.u(4), track)
        val frac = if (p.duration > 0 && Session.item != null) (p.currentPosition.toFloat() / p.duration).coerceIn(0f, 1f) else 0f
        bar.shader = LinearGradient(0f, 0f, w, 0f, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP)
        r.right = w * frac; c.drawRoundRect(r, Ui.u(4), Ui.u(4), bar)
        val state = when {
            Session.pending != null && Session.item != null -> S.f(K.OPENING, Session.pending!!.title)
            Session.loading -> S.f(K.OPENING, it.title)
            Session.failed -> S.t(K.RETRYING)
            !p.playWhenReady -> S.t(K.PAUSED) + " · " + Ui.fmt(p.currentPosition)
            else -> Ui.fmt(p.currentPosition) + " / " + Ui.fmt(p.duration.coerceAtLeast(0)) + (if (p.videoSize.height > 0) " · ${p.videoSize.height}p" else "")
        }
        Txt.draw(c, Txt.fit(if (Session.loading) tag else dim, state, w), 0f, Ui.u(274), w, if (Session.loading) tag else dim)
        queue(c, w)
        if (glow.value > .01f && sel == 0) { val g = Ui.u(16) + w * .04f * pop.value; outline(c, -g, -videoH - g, w + g, Ui.u(292), true) }
        if (playing || Session.loading) postInvalidateDelayed(45)
    }

    private fun queue(c: Canvas, w: Float) {
        val l = list; if (l.isEmpty()) return
        Txt.draw(c, S.t(K.UP_NEXT).uppercase(), 0f, Ui.u(338), w, tag)
        val first = (sel - 3).coerceAtLeast(0); val rh = Ui.u(100)
        for (i in first until minOf(l.size, first + 3)) {
            val y = Ui.u(354) + (i - first) * rh
            val tw = Ui.u(142); val tx = if (Ui.rtl) w - tw else 0f; val on = glow.value > .01f && sel == i + 1
            c.save()
            if (on) {
                val k = 1f + .05f * glow.value; c.scale(k, k, w / 2, y + rh / 2 - Ui.u(8))
                ring.alpha = 0; face.alpha = (70 * glow.value).toInt(); r.set(-Ui.u(14), y - Ui.u(10), w + Ui.u(14), y + tw * 9 / 16 + Ui.u(10)); c.drawRoundRect(r, Ui.u(18), Ui.u(18), face)
            }
            Cards.picture(c, l[i], tx, y, tw, tw * 9 / 16)
            val bx = if (Ui.rtl) 0f else tw + Ui.u(16); val bw = w - tw - Ui.u(16)
            for ((n, line) in Txt.wrap(rowT, l[i].title, bw, 2).withIndex()) Txt.draw(c, line, bx, y + Ui.u(26) + n * Ui.u(26), bw, rowT)
            Txt.draw(c, Txt.fit(dim, l[i].by, bw), bx, y + Ui.u(82), bw, dim)
            if (on) outline(c, -Ui.u(14), y - Ui.u(10), w + Ui.u(14), y + tw * 9 / 16 + Ui.u(10))
            c.restore()
        }
    }

    private fun outline(c: Canvas, l: Float, t: Float, rr: Float, b: Float, big: Boolean = false) {
        ring.shader = LinearGradient(l, 0f, rr, 0f, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP)
        r.set(l, t, rr, b)
        if (big) { ring.strokeWidth = Ui.u(20); ring.alpha = (60 * glow.value).toInt(); c.drawRoundRect(r, Ui.u(26), Ui.u(26), ring) }
        ring.strokeWidth = Ui.u(if (big) 7 else 4); ring.alpha = (255 * glow.value).toInt(); c.drawRoundRect(r, Ui.u(if (big) 24 else 16), Ui.u(if (big) 24 else 16), ring)
    }
}
