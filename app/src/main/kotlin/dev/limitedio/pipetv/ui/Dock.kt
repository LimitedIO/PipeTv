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

/** Minimal docked player: progress in the frame, pause icon when paused, an opening box, the upcoming videos (focus centred). */
class Dock(c: Context) : AnimView(c) {
    var lit = false
        set(v) { field = v; glow.to(if (v) 1f else 0f); focusPop() }
    var videoH = 0f; var onExpand: () -> Unit = {}
    var compact = false
    var onPlay: (Item) -> Unit = {}; var onFocus: () -> Unit = {}
    private val pop = spring(0f, 300f, 20f); private val scroll = spring(0f, 160f, 24f)
    val popValue get() = pop.value
    val popMoving get() = pop.moving
    private var sel = 0; private val glow = spring(0f, 200f, 26f)
    private val rowT = Ui.paint(Ui.TEXT, 26f, true); private val dim = Ui.paint(Ui.DIM, 24f); private val boxT = Ui.paint(Ui.TEXT, 28f, true)
    private val face = Ui.paint(Ui.ACCENT); private val box = Ui.paint(0xFF171A2B.toInt()); private val track = Ui.paint(0x55FFFFFF)
    private val dark = Ui.paint(0x99000000.toInt()); private val white = Ui.paint(Ui.TEXT); private val bar = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val r = RectF(); private val list get() = if (compact) emptyList() else Session.related.take(12)
    private val rh get() = Ui.u(118)
    private val top get() = if (Session.pending != null && Session.item != null) Ui.u(124) else Ui.u(12)

    private fun focusPop() { pop.target = if (lit && sel == 0) 1f else 0f; invalidate(); onFocus() }

    fun key(code: Int): Boolean {
        when (code) {
            KeyEvent.KEYCODE_DPAD_UP -> if (sel > 0) sel-- else return true
            KeyEvent.KEYCODE_DPAD_DOWN -> if (sel < list.size) sel++
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> if (sel == 0) onExpand() else list.getOrNull(sel - 1)?.let(onPlay)
            else -> return false
        }
        val area = height - top; scroll.target = if (sel == 0) 0f else (((sel - 1) * rh + rh / 2 - area / 2).coerceIn(0f, maxOf(0f, list.size * rh - area)))
        focusPop(); return true
    }

    override fun render(c: Canvas, dt: Float) {
        if (Session.item == null && Session.pending == null) return
        val p = Engine.exo; val w = width.toFloat()
        if (Session.item != null) frame(c, w, p)
        Session.pending?.takeIf { Session.item != null }?.let { opening(c, w, it) }
        queue(c, w)
        if (glow.value > .01f && sel == 0) { val g = Ui.u(14) + w * .04f * pop.value; outline(c, -g, -videoH - g, w + g, g, true) }
        if (Session.pending != null || p.isPlaying) postInvalidateDelayed(if (Session.pending != null) 16 else 200)
    }

    private fun frame(c: Canvas, w: Float, p: androidx.media3.exoplayer.ExoPlayer) {
        val frac = if (p.duration > 0) (p.currentPosition.toFloat() / p.duration).coerceIn(0f, 1f) else 0f
        r.set(Ui.u(14), -Ui.u(18), w - Ui.u(14), -Ui.u(10)); c.drawRoundRect(r, Ui.u(4), Ui.u(4), track)
        bar.shader = LinearGradient(0f, 0f, w, 0f, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP)
        r.right = r.left + (w - Ui.u(28)) * frac; c.drawRoundRect(r, Ui.u(4), Ui.u(4), bar)
        if (!p.playWhenReady && !Session.failed) { c.drawCircle(w / 2, -videoH / 2, Ui.u(40), dark); Icons.pause(c, w / 2, -videoH / 2, Ui.u(18), white) }
    }

    /** The next video is opening: a plain box with its title and a moving progress bar. */
    private fun opening(c: Canvas, w: Float, it: Item) {
        r.set(0f, Ui.u(10), w, Ui.u(110)); c.drawRoundRect(r, Ui.u(22), Ui.u(22), box)
        Txt.draw(c, Txt.fit(boxT, S.f(K.OPENING, it.title), w - Ui.u(48)), Ui.u(24), Ui.u(54), w - Ui.u(48), boxT)
        Spinner.bar(c, Ui.u(24), Ui.u(78), w - Ui.u(24), Ui.u(90))
    }

    private fun queue(c: Canvas, w: Float) {
        val l = list; if (l.isEmpty()) return
        c.save(); c.clipRect(-Ui.u(24), top, w + Ui.u(24), height.toFloat()); c.translate(0f, -scroll.value)
        for (i in l.indices) {
            val y = top + i * rh + Ui.u(10); val tw = Ui.u(176); val th = tw * 9 / 16; val tx = if (Ui.rtl) w - tw else 0f
            if (y - scroll.value > height || y - scroll.value + rh < top) continue
            val on = glow.value > .01f && sel == i + 1
            c.save()
            if (on) { val k = 1f + .05f * glow.value; c.scale(k, k, w / 2, y + th / 2); face.alpha = (70 * glow.value).toInt(); r.set(-Ui.u(14), y - Ui.u(10), w + Ui.u(14), y + th + Ui.u(10)); c.drawRoundRect(r, Ui.u(18), Ui.u(18), face) }
            Cards.picture(c, l[i], tx, y, tw, th)
            val bx = if (Ui.rtl) 0f else tw + Ui.u(16); val bw = w - tw - Ui.u(16)
            for ((n, line) in Txt.wrap(rowT, l[i].title, bw, 2).withIndex()) Txt.draw(c, line, bx, y + Ui.u(30) + n * Ui.u(30), bw, rowT)
            Txt.draw(c, Txt.fit(dim, l[i].by, bw), bx, y + Ui.u(92), bw, dim)
            if (on) outline(c, -Ui.u(14), y - Ui.u(10), w + Ui.u(14), y + th + Ui.u(10))
            c.restore()
        }
        c.restore()
    }

    private fun outline(c: Canvas, l: Float, t: Float, rr: Float, b: Float, big: Boolean = false) {
        ring.shader = LinearGradient(l, 0f, rr, 0f, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP)
        r.set(l, t, rr, b)
        if (big) { ring.strokeWidth = Ui.u(20); ring.alpha = (60 * glow.value).toInt(); c.drawRoundRect(r, Ui.u(26), Ui.u(26), ring) }
        ring.strokeWidth = Ui.u(if (big) 7 else 4); ring.alpha = (255 * glow.value).toInt(); c.drawRoundRect(r, Ui.u(if (big) 24 else 16), Ui.u(if (big) 24 else 16), ring)
    }
}
