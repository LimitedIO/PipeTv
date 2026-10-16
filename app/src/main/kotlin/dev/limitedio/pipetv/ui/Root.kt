package dev.limitedio.pipetv.ui

import android.content.Context
import android.graphics.Canvas
import android.view.Choreographer
import android.view.ViewGroup
import dev.limitedio.pipetv.data.Favs
import dev.limitedio.pipetv.data.Prefs
import dev.limitedio.pipetv.data.Yt
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.media.Session
import dev.limitedio.pipetv.ui.input.Voice
import dev.limitedio.pipetv.ui.widgets.RowList
import dev.limitedio.pipetv.ui.widgets.SubMenu

/** The screen: owns the pages, the player and the two big motions (dock slide, fullscreen expand). */
class Root(c: Context) : ViewGroup(c) {
    val nav = Nav(c); val stage = Stage(c); val dock = Dock(c); val hud = Hud(c); val toast = Toast(c); val update = UpdateUi(c)
    val navigationShade = android.view.View(c).apply { setBackgroundColor(0x70050810); visibility = GONE }
    val playerPanel = android.view.View(c).apply { setBackgroundColor(0xFF101322.toInt()); visibility = GONE }
    val menuShade = android.view.View(c).apply { setBackgroundColor(0xB807080F.toInt()); visibility = GONE }
    val menu = RowList(c, { S.t(K.OPTIONS) }, true).apply { visibility = GONE }
    val playerSub = SubMenu(c).apply { visibility = GONE }
    val controls = Controls(this); val screens = ScreenStack(this); val opener = Opener(this)
    val pages = Pages(c, opener::pick, opener::details, { onLanguage() }, toast::show, opener::about)
    val dockS = Spring(0f, 1000f, 64f); val fullS = Spring(0f, 1200f, 70f)
    val fullOrigin = android.graphics.RectF()
    var tab = 0; private set
    var menuOpen = false; private set; var askMic: () -> Unit = {}
    var fromPlayer = false // a video-info page opened from fullscreen is up: Back returns to the video
    private val ambient = Ambient(); private var running = false; private var last = 0L

    private val frame = object : Choreographer.FrameCallback {
        var last = 0L
        override fun doFrame(t: Long) {
            val dt = if (last == 0L) .016f else minOf((t - last) / 1e9f, .05f); last = t
            dockS.step(dt); fullS.step(dt); relayout()
            if (dockS.moving || fullS.moving || nav.moving) Choreographer.getInstance().postFrameCallback(this) else { last = 0; running = false }
        }
    }

    init {
        setWillNotDraw(false); clipChildren = false // the dock draws its focus ring and progress over the video
        pages.views.forEach { addView(it); it.visibility = GONE }
        listOf(navigationShade, playerPanel, nav, stage, dock, hud, menuShade, menu, playerSub, toast, update).forEach { addView(it) }
        show(0, false)
        Session.onChange = { controls.sessionChanged() }
        Session.onReady = { setFull(true) }
        Session.onOpenFailed = { toast.show(S.t(K.CANT_PLAY).substringBefore(".")) }
        dock.onExpand = { setFull(true) }
        dock.onClose = { Session.stop(); toast.show(S.t(K.PLAYBACK_CLOSED)); controls.focusContent() }
        dock.onNav = { controls.focusNav() }
        pages.search.onVoice = { voice() }
        Voice.onFail = { quiet -> toast.show(S.t(if (quiet) K.NO_SPEECH else K.VOICE_NA)) }
    }

    fun kick() { if (!running) { running = true; Choreographer.getInstance().postFrameCallback(frame) } }
    fun setDock(on: Boolean) {
        val show = on && Session.active && !full
        val target = if (show) 1f else 0f
        if (dockS.target == target) return
        dockS.target = target; kick()
    }
    fun setFull(on: Boolean) {
        val changed = on != full
        if (changed) {
            if (on && stage.alpha > .001f && width > 0) {
                fullOrigin.set(stage.translationX, stage.translationY,
                    stage.translationX + width * stage.scaleX, stage.translationY + height * stage.scaleY)
            } else fullOrigin.set(RootLayout.mini(this))
        }
        fullS.target = if (on) 1f else 0f
        if (on) { setDock(false); hud.nudge() }
        else { if (changed) { if (Session.active) controls.focusNav() else controls.focusContent() }; closeMenu() }
        kick()
    }
    val full get() = fullS.target == 1f

    fun openMenu() { menu.rows = PlayerMenu.rows(playerSub); menuOpen = true; menuShade.visibility = VISIBLE; menu.visibility = VISIBLE; hud.nudge(); relayout() }
    fun closeMenu() { menuOpen = false; menuShade.visibility = GONE; menu.visibility = GONE; playerSub.close() }

    /** Adds or removes [item] from the favourites and says so. */
    fun toggleFav(item: dev.limitedio.pipetv.data.Item) {
        toast.show(S.t(if (Favs.toggle(item)) K.FAV_ADDED else K.FAV_REMOVED)); pages.refreshFavs()
        dock.invalidate()
        screens.views.forEach { it.invalidate() }
    }
    fun voice() { show(1); if (Voice.available(context)) askMic() else toast.show(S.t(K.VOICE_NA)) }
    fun onLanguage() { Yt.relocalize(); pages.relocalize(tab); nav.invalidate(); relayout() }

    fun syncDock() { kick() }

    fun show(i: Int, animate: Boolean = true) {
        fromPlayer = false
        screens.clear()
        pages.views[tab].visibility = GONE
        tab = i; nav.sel = i; syncDock()
        val v = pages.views[i]; v.visibility = VISIBLE
        if (animate) { v.alpha = 0f; v.translationX = Ui.u(if (Ui.rtl) -60 else 60); v.animate().alpha(1f).translationX(0f).setDuration(260).start() }
        pages.enter(i); pages.lit(i, !controls.onNav)
    }

    override fun onDraw(c: Canvas) {
        c.drawColor(Ui.BG)
        if (!Prefs.ambient || fullS.value > .98f) return
        val now = System.nanoTime(); val dt = if (last == 0L) .05f else minOf((now - last) / 1e9f, .1f); last = now
        ambient.draw(c, width.toFloat(), height.toFloat(), dt, now / 1e9f, tab)
        postInvalidateDelayed(50)
    }

    override fun onLayout(ch: Boolean, l: Int, t: Int, r: Int, b: Int) = relayout()
    private fun relayout() = RootLayout.apply(this, dockS.value.coerceIn(0f, 1f), fullS.value.coerceIn(0f, 1f))
}
