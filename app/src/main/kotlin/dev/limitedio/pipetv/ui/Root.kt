package dev.limitedio.pipetv.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.Choreographer
import android.view.ViewGroup
import dev.limitedio.pipetv.data.Yt
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.media.Session
import dev.limitedio.pipetv.ui.input.Voice
import dev.limitedio.pipetv.ui.widgets.RowList

/** Screen layout plus the big motions: the player dock sliding in and the fullscreen expand. */
class Root(c: Context) : ViewGroup(c) {
    val nav = Nav(c); val stage = Stage(c); val dock = Dock(c); val hud = Hud(c); val toast = Toast(c)
    val menu = RowList(c, { S.t(K.OPTIONS) }, true).apply { visibility = GONE }
    val controls = Controls(this)
    val screens = ScreenStack(this)
    val opener = Opener(this)
    val pages = Pages(c, opener::pick, opener::details, { onLanguage() }, toast::show)
    val dockS = Spring(0f, 170f, 24f); val fullS = Spring(0f, 170f, 24f)
    var tab = 0; private set
    var menuOpen = false; private set
    var askMic: () -> Unit = {}
    private val glow = Paint(); private var running = false

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
        listOf(nav, stage, dock, hud, menu, toast).forEach { addView(it) }
        show(0, false)
        Session.onChange = { controls.sessionChanged() }
        pages.search.onVoice = { voice() }
        Voice.onFail = { quiet -> toast.show(S.t(if (quiet) K.NO_SPEECH else K.VOICE_NA)) }
    }

    fun kick() { if (!running) { running = true; Choreographer.getInstance().postFrameCallback(frame) } }
    fun setDock(on: Boolean) { dockS.target = if (on) 1f else 0f; pages.setCols(if (on) 3 else 4); kick() }
    fun setFull(on: Boolean) { fullS.target = if (on) 1f else 0f; if (on) hud.nudge() else closeMenu(); kick() }
    val full get() = fullS.target == 1f

    fun openMenu() { menu.rows = PlayerMenu.rows(); menuOpen = true; menu.visibility = VISIBLE; hud.nudge(); relayout() }
    fun closeMenu() { menuOpen = false; menu.visibility = GONE }

    fun voice() { show(1); if (Voice.available(context)) askMic() else toast.show(S.t(K.VOICE_NA)) }

    fun onLanguage() { Yt.relocalize(); pages.relocalize(tab); nav.invalidate(); relayout() }

    fun show(i: Int, animate: Boolean = true) {
        screens.clear()
        pages.views[tab].visibility = GONE
        tab = i; nav.sel = i
        val v = pages.views[i]; v.visibility = VISIBLE
        if (animate) { v.alpha = 0f; v.translationX = Ui.u(if (Ui.rtl) -60 else 60); v.animate().alpha(1f).translationX(0f).setDuration(260).start() }
        pages.enter(i); pages.lit(i, !controls.onNav)
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        glow.shader = RadialGradient(if (Ui.rtl) w * .15f else w * .85f, 0f, w * .7f, 0x337C5CFF, 0, Shader.TileMode.CLAMP)
    }

    override fun onDraw(c: Canvas) { c.drawColor(Ui.BG); c.drawPaint(glow) }

    override fun onLayout(ch: Boolean, l: Int, t: Int, r: Int, b: Int) = relayout()

    private fun relayout() {
        val w = width; val h = height; if (w == 0) return
        val dk = dockS.value.coerceIn(0f, 1f); val f = fullS.value.coerceIn(0f, 1f)
        val panel = Ui.u(580) * dk; val rail = Ui.u(104)
        val l = if (Ui.rtl) panel else rail; val r = if (Ui.rtl) w - rail else w - panel
        (pages.views + screens.views).forEach { it.layout(l.toInt(), 0, r.toInt(), h) }
        val nw = Ui.u(340).toInt(); if (Ui.rtl) nav.layout(w - nw, 0, w, h) else nav.layout(0, 0, nw, h)
        val mw = Ui.u(580) - Ui.u(48); val mh = mw * 9 / 16
        val mx = if (Ui.rtl) Ui.u(24) else w - panel + Ui.u(24); val my = Ui.u(48)
        val x = mx * (1 - f); val y = my * (1 - f)
        stage.layout(x.toInt(), y.toInt(), (x + mw + (w - mw) * f).toInt(), (y + mh + (h - mh) * f).toInt())
        dock.videoH = mh
        dock.layout(mx.toInt(), (my + mh + Ui.u(20)).toInt(), (mx + mw).toInt(), (my + mh + Ui.u(320)).toInt())
        hud.layout(0, 0, w, h); toast.layout(0, 0, w, h)
        val mnw = Ui.u(820).toInt(); val mnx = if (Ui.rtl) Ui.u(40).toInt() else w - mnw - Ui.u(40).toInt()
        menu.layout(mnx, Ui.u(110).toInt(), mnx + mnw, h - Ui.u(110).toInt())
        stage.visibility = if (Session.active) VISIBLE else GONE
        dock.visibility = if (dk > .02f && f < .02f) VISIBLE else GONE
        hud.visibility = if (f > .02f) VISIBLE else GONE
        nav.visibility = if (f > .98f) GONE else VISIBLE
    }
}
