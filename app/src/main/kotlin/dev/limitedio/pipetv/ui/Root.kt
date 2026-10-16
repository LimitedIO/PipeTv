package dev.limitedio.pipetv.ui

import android.content.Context
import android.graphics.Canvas
import android.view.Choreographer
import android.view.ViewGroup
import dev.limitedio.pipetv.data.Prefs
import dev.limitedio.pipetv.data.Yt
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.media.Engine
import dev.limitedio.pipetv.media.Session
import dev.limitedio.pipetv.ui.input.Voice
import dev.limitedio.pipetv.ui.widgets.RowList

/** The screen: owns the pages, the player and the two big motions (dock slide, fullscreen expand). */
class Root(c: Context) : ViewGroup(c) {
    val nav = Nav(c); val stage = Stage(c); val dock = Dock(c); val hud = Hud(c); val toast = Toast(c)
    val menu = RowList(c, { S.t(K.OPTIONS) }, true).apply { visibility = GONE }
    val controls = Controls(this); val screens = ScreenStack(this); val opener = Opener(this)
    val pages = Pages(c, opener::pick, opener::details, { onLanguage() }, toast::show)
    val dockS = Spring(0f, 170f, 24f); val fullS = Spring(0f, 170f, 24f)
    var tab = 0; private set
    var menuOpen = false; private set
    var askMic: () -> Unit = {}
    private val ambient = Ambient(); private var running = false; private var last = 0L

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
        Session.onReady = { setFull(true) }
        Session.onOpenFailed = { toast.show(S.t(K.CANT_PLAY).substringBefore(".")) }
        dock.onExpand = { setFull(true) }; dock.onPlay = { Session.open(it) }
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

    override fun onDraw(c: Canvas) {
        c.drawColor(Ui.BG)
        if (!Prefs.ambient || fullS.value > .98f) return
        val now = System.nanoTime(); val dt = if (last == 0L) .04f else minOf((now - last) / 1e9f, .1f); last = now
        val it = Session.shown
        ambient.mood(tab, ambient.tintOf(it), Session.active, Session.item != null && !Engine.exo.playWhenReady, Session.loading, if (Voice.active) Voice.level else 0f)
        ambient.draw(c, width.toFloat(), height.toFloat(), dt, now / 1e9f)
        postInvalidateDelayed(40)
    }

    override fun onLayout(ch: Boolean, l: Int, t: Int, r: Int, b: Int) = relayout()
    private fun relayout() = RootLayout.apply(this, dockS.value.coerceIn(0f, 1f), fullS.value.coerceIn(0f, 1f))
}
