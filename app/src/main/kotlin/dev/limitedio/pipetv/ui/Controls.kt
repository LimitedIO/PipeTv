package dev.limitedio.pipetv.ui

import android.view.KeyEvent
import android.view.KeyEvent.*
import dev.limitedio.pipetv.data.Item
import dev.limitedio.pipetv.media.Session

/** All keyboard / remote handling. Three focus zones while browsing: nav rail, content, player dock. */
class Controls(private val root: Root) {
    var onNav = false; private set
    private var zone = 1 // 0 nav, 1 content, 2 dock
    var onExit: () -> Unit = {}
    private val keys = PlayerKeys(root)

    fun key(e: KeyEvent): Boolean {
        if (keys.media(e.keyCode)) return true
        return if (root.full) keys.full(e) else browse(e)
    }

    fun play(item: Item) {
        if (Session.item?.id != item.id) Session.open(item)
        root.setFull(true)
    }

    fun sessionChanged() {
        if (Session.active && root.dockS.target == 0f) root.setDock(true)
        if (!Session.active) { root.setDock(false); root.setFull(false); if (zone == 2) setZone(1) }
        root.dock.invalidate(); root.hud.invalidate(); root.stage.requestLayout()
    }

    private fun setZone(z: Int) {
        zone = z; onNav = z == 0
        root.nav.lit = z == 0; root.dock.lit = z == 2; root.pages.lit(root.tab, z == 1)
    }

    private fun browse(e: KeyEvent): Boolean {
        val k = e.keyCode
        if (k == KEYCODE_MENU || k == KEYCODE_INFO) { if (Session.active) root.setFull(true); return true }
        if (k == KEYCODE_BACK || k == KEYCODE_ESCAPE) return back()
        return when (zone) {
            0 -> nav(k)
            2 -> when (k) {
                KEYCODE_DPAD_CENTER, KEYCODE_ENTER -> { root.setFull(true); true }
                KEYCODE_DPAD_LEFT -> { setZone(1); true }
                else -> k in KEYCODE_DPAD_UP..KEYCODE_DPAD_RIGHT
            }
            else -> content(e)
        }
    }

    private fun back(): Boolean {
        when {
            zone != 0 -> setZone(0)
            Session.active -> { Session.stop(); root.toast.show("Playback closed") }
            else -> onExit()
        }
        return true
    }

    private fun nav(k: Int): Boolean {
        when (k) {
            KEYCODE_DPAD_UP -> if (root.tab > 0) root.show(root.tab - 1)
            KEYCODE_DPAD_DOWN -> if (root.tab < 4) root.show(root.tab + 1)
            KEYCODE_DPAD_RIGHT, KEYCODE_DPAD_CENTER, KEYCODE_ENTER -> setZone(1)
            else -> return false
        }
        return true
    }

    private fun content(e: KeyEvent): Boolean {
        val k = e.keyCode
        if (root.tab == 1) {
            val ch = e.unicodeChar
            if (k == KEYCODE_DEL) { root.pages.search.backspace(); return true }
            if (ch in 32..126 && !e.isSystem && k !in KEYCODE_DPAD_UP..KEYCODE_DPAD_CENTER && k != KEYCODE_ENTER) {
                root.pages.search.type(ch.toChar().toString()); return true
            }
        }
        if (root.pages.key(root.tab, k)) return true
        if (k == KEYCODE_DPAD_LEFT) setZone(0)
        else if (k == KEYCODE_DPAD_RIGHT && Session.active) setZone(2)
        else return false
        return true
    }
}
