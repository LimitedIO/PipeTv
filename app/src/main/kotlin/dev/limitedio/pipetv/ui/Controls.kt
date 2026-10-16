package dev.limitedio.pipetv.ui

import android.view.KeyEvent
import android.view.KeyEvent.*
import dev.limitedio.pipetv.audio.Sound
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.media.Session
import dev.limitedio.pipetv.ui.input.Voice

/** All keyboard / remote handling. Focus zones: 0 nav rail, 1 content, 2 player dock, 3 shortcut rail. */
class Controls(private val root: Root) {
    var onNav = false; private set
    val inContent get() = zone == 1; val hold = LongPress(root)
    private var zone = 1
    var onExit: () -> Unit = {}; private val keys = PlayerKeys(root)

    fun key(e: KeyEvent): Boolean {
        val k = e.keyCode
        if (root.update.modal) return root.update.key(k)
        if (Voice.active) { if (k == KEYCODE_BACK || k == KEYCODE_DPAD_CENTER || k == KEYCODE_ESCAPE) Voice.stop(); return true }
        if (k == KEYCODE_VOICE_ASSIST || k == KEYCODE_SEARCH) { root.voice(); return true }
        if (root.playerSub.open) { root.playerSub.key(k, e.repeatCount); root.hud.nudge(); return true }
        if (root.menuOpen) return keys.menu(e)
        if (keys.media(k)) return true
        return if (root.full) keys.full(e) else browse(e)
    }

    fun play(item: dev.limitedio.pipetv.data.Item) {
        if (Session.item?.id == item.id) { root.setFull(true); return }
        if (Session.pending?.id != item.id) Session.open(item, true)
        if (Session.item == null) root.setFull(true)
    }

    fun focusContent() = setZone(1)

    fun sessionChanged() {
        if (Session.active && root.dockS.target == 0f) root.setDock(true)
        if (!Session.active) { root.setDock(false); root.setFull(false); if (zone == 2 || zone == 3) setZone(1) }
        Sound.update(); root.actions.focus(zone, Session.active); root.kick()
        root.pages.repaint(); root.dock.invalidate(); root.hud.invalidate(); root.stage.refresh(); root.stage.refit()
    }

    private fun setZone(z: Int) {
        zone = z; onNav = z == 0; if (z != 0) root.nav.exit = false
        root.nav.lit = z == 0; root.dock.lit = z == 2; root.actions.focus(z, Session.active); root.kick()
        root.pages.lit(root.tab, z == 1); root.screens.top?.lit(z == 1)
    }

    private fun browse(e: KeyEvent): Boolean {
        val k = mirror(e.keyCode)
        if (k == KEYCODE_BACK || k == KEYCODE_ESCAPE) return back()
        if ((k == KEYCODE_MENU || k == KEYCODE_INFO) && zone != 1) { if (Session.active) root.setFull(true); return true }
        return when (zone) {
            0 -> nav(k)
            3 -> if (k == KEYCODE_DPAD_RIGHT) { setZone(0); true } else root.actions.key(k) || k in KEYCODE_DPAD_UP..KEYCODE_DPAD_RIGHT
            2 -> if (k == KEYCODE_DPAD_LEFT) { setZone(1); true } else if (k == KEYCODE_PROG_RED || k == KEYCODE_BOOKMARK) { Session.item?.let(root::toggleFav); true } else root.dock.key(k) || k in KEYCODE_DPAD_UP..KEYCODE_DPAD_RIGHT
            else -> content(e, k)
        }
    }

    private fun mirror(k: Int) = if (!Ui.rtl) k else when (k) { KEYCODE_DPAD_LEFT -> KEYCODE_DPAD_RIGHT; KEYCODE_DPAD_RIGHT -> KEYCODE_DPAD_LEFT; else -> k }

    private fun back(): Boolean {
        when {
            zone == 1 && root.tab == 5 && !root.screens.any && root.pages.closeSub() -> {}
            zone == 1 && root.screens.any -> if (root.screens.top?.back() != true) root.screens.pop()
            zone == 1 && root.tab == 1 && !root.screens.any && root.pages.search.back() -> {}
            zone != 0 -> setZone(0)
            Session.active -> root.toast.show(S.t(K.BACK_HINT))
            else -> onExit()
        }
        return true
    }

    private fun nav(k: Int): Boolean {
        when (k) {
            KEYCODE_DPAD_UP -> if (root.nav.exit) root.nav.exit = false else if (root.tab > 0) root.show(root.tab - 1)
            KEYCODE_DPAD_DOWN -> if (root.tab < 5) root.show(root.tab + 1) else root.nav.exit = true
            KEYCODE_DPAD_LEFT -> if (Session.active) setZone(3) else return false
            KEYCODE_DPAD_RIGHT, KEYCODE_DPAD_CENTER, KEYCODE_ENTER -> if (!root.nav.exit) setZone(1) else if (k != KEYCODE_DPAD_RIGHT) onExit()
            else -> return false
        }
        return true
    }

    private fun content(e: KeyEvent, k: Int): Boolean {
        if (root.tab == 1 && !root.screens.any && root.pages.search.typed(e)) return true
        if (k == KEYCODE_PROG_RED || k == KEYCODE_BOOKMARK) { (root.screens.top?.focused() ?: root.pages.focusedItem(root.tab))?.let(root::toggleFav); return true }
        val used = root.screens.top?.key(k, e.repeatCount) ?: root.pages.key(root.tab, k, e.repeatCount)
        if (used) return true
        when {
            k == KEYCODE_DPAD_LEFT -> setZone(0)
            k == KEYCODE_DPAD_RIGHT && Session.active -> setZone(2)
            (k == KEYCODE_MENU || k == KEYCODE_INFO) && Session.active -> root.setFull(true)
            else -> return false
        }
        return true
    }
}
