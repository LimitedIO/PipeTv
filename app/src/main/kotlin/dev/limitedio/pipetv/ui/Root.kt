package dev.limitedio.pipetv.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.Choreographer
import android.view.View
import android.view.ViewGroup
import dev.limitedio.pipetv.media.Session

/** Screen layout + the two big motions: the player dock sliding in and the fullscreen expand. */
class Root(c: Context) : ViewGroup(c) {
    val nav = Nav(c); val stage = Stage(c); val dock = Dock(c); val hud = Hud(c); val toast = Toast(c)
    val controls = Controls(this)
    val pages = Pages(c) { controls.play(it) }
    val dockS = Spring(0f, 170f, 24f); val fullS = Spring(0f, 170f, 24f)
    var tab = 0; private set
    private val glow = Paint()
    private var running = false

    private val frame = object : Choreographer.FrameCallback {
        var last = 0L
        override fun doFrame(t: Long) {
            val dt = if (last == 0L) .016f else minOf((t - last) / 1e9f, .05f); last = t
            dockS.step(dt); fullS.step(dt); relayout()
            if (dockS.moving || fullS.moving) Choreographer.getInstance().postFrameCallback(this) else { last = 0; running = false }
        }
    }

    init {
        setWillNotDraw(false)
        pages.views.forEach { addView(it); it.visibility = GONE }
        listOf(nav, stage, dock, hud, toast).forEach { addView(it) }
        show(0, false)
        Session.onChange = { controls.sessionChanged() }
    }

    fun kick() { if (!running) { running = true; Choreographer.getInstance().postFrameCallback(frame) } }
    fun setDock(on: Boolean) { dockS.target = if (on) 1f else 0f; pages.setCols(if (on) 3 else 4); kick() }
    fun setFull(on: Boolean) { fullS.target = if (on) 1f else 0f; if (on) hud.nudge(); kick() }
    val full get() = fullS.target == 1f

    fun show(i: Int, animate: Boolean = true) {
        pages.views[tab].visibility = GONE
        tab = i; nav.sel = i
        val v = pages.views[i]; v.visibility = VISIBLE
        if (animate) { v.alpha = 0f; v.translationX = Ui.u(60); v.animate().alpha(1f).translationX(0f).setDuration(260).start() }
        pages.enter(i); pages.lit(i, !controls.onNav)
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        glow.shader = RadialGradient(w * .85f, 0f, w * .7f, 0x337C5CFF, 0, Shader.TileMode.CLAMP)
    }

    override fun onDraw(c: Canvas) { c.drawColor(Ui.BG); c.drawPaint(glow) }

    override fun onLayout(ch: Boolean, l: Int, t: Int, r: Int, b: Int) = relayout()

    private fun relayout() {
        val w = width; val h = height; if (w == 0) return
        val dk = dockS.value.coerceIn(0f, 1f); val f = fullS.value.coerceIn(0f, 1f)
        val panel = Ui.u(580) * dk
        pages.views.forEach { it.layout(Ui.u(104).toInt(), 0, (w - panel).toInt(), h) }
        nav.layout(0, 0, Ui.u(340).toInt(), h)
        val mw = Ui.u(580) - Ui.u(48); val mh = mw * 9 / 16
        val mx = w - panel + Ui.u(24); val my = Ui.u(48)
        val x = mx * (1 - f); val y = my * (1 - f)
        stage.layout(x.toInt(), y.toInt(), (x + mw + (w - mw) * f).toInt(), (y + mh + (h - mh) * f).toInt())
        dock.videoH = mh
        dock.layout(mx.toInt(), (my + mh + Ui.u(20)).toInt(), (mx + mw).toInt(), (my + mh + Ui.u(320)).toInt())
        hud.layout(0, 0, w, h); toast.layout(0, 0, w, h)
        stage.visibility = if (Session.active) VISIBLE else GONE
        dock.visibility = if (dk > .02f && f < .02f) VISIBLE else GONE
        hud.visibility = if (f > .02f) VISIBLE else GONE
        nav.visibility = if (f > .98f) GONE else VISIBLE
    }
}
