package dev.limitedio.pipetv.ui

import android.view.KeyEvent
import android.view.KeyEvent.*
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.media.Engine
import dev.limitedio.pipetv.media.Session

/** Keys that control playback: media keys everywhere, plus the fullscreen shortcuts. */
class PlayerKeys(private val root: Root) {
    fun media(k: Int): Boolean {
        if (!Session.active) return false
        val p = Engine.exo
        when (k) {
            KEYCODE_MEDIA_PLAY_PAUSE, KEYCODE_HEADSETHOOK -> p.playWhenReady = !p.playWhenReady
            KEYCODE_MEDIA_PLAY -> p.play(); KEYCODE_MEDIA_PAUSE -> p.pause()
            KEYCODE_MEDIA_STOP -> Session.stop()
            KEYCODE_MEDIA_NEXT, KEYCODE_CHANNEL_UP -> Session.next()
            KEYCODE_MEDIA_PREVIOUS, KEYCODE_CHANNEL_DOWN -> p.seekTo(0)
            KEYCODE_MEDIA_FAST_FORWARD -> seek(30_000)
            KEYCODE_MEDIA_REWIND -> seek(-30_000)
            else -> return false
        }
        root.hud.nudge(); root.dock.invalidate(); return true
    }

    private fun seek(ms: Long) {
        val p = Engine.exo; p.seekTo((p.currentPosition + ms).coerceIn(0, p.duration.coerceAtLeast(0)))
        root.hud.nudge(); root.toast.show(if (ms > 0) "+${ms / 1000}s" else "${ms / 1000}s")
    }

    fun full(e: KeyEvent): Boolean {
        val p = Engine.exo; val k = e.keyCode
        root.hud.nudge()
        when (k) {
            KEYCODE_DPAD_CENTER, KEYCODE_ENTER, KEYCODE_SPACE, KEYCODE_K -> p.playWhenReady = !p.playWhenReady
            KEYCODE_DPAD_RIGHT, KEYCODE_L -> seek(if (e.repeatCount > 20) 60_000 else if (e.repeatCount > 6) 30_000 else 10_000)
            KEYCODE_DPAD_LEFT, KEYCODE_J -> seek(-(if (e.repeatCount > 20) 60_000L else if (e.repeatCount > 6) 30_000L else 10_000L))
            KEYCODE_PROG_RED, KEYCODE_BOOKMARK, KEYCODE_B -> Session.item?.let(root::toggleFav)
            KEYCODE_DPAD_UP -> root.openMenu()
            KEYCODE_DPAD_DOWN -> { root.setFull(false); Session.item?.let { root.opener.details(it); root.fromPlayer = true } }
            KEYCODE_BACK, KEYCODE_MENU, KEYCODE_ESCAPE, KEYCODE_I -> { Session.noExpand(); root.setFull(false) }
            KEYCODE_M -> { p.volume = if (p.volume > 0f) 0f else 1f; root.toast.show(S.t(if (p.volume > 0f) K.SOUND_ON else K.MUTED)) }
            in KEYCODE_0..KEYCODE_9 -> p.seekTo(p.duration.coerceAtLeast(0) * (k - KEYCODE_0) / 10)
            else -> return false
        }
        return true
    }

    /** Options panel (Up key): everything goes to the panel, Back/Menu close it. */
    fun menu(e: KeyEvent): Boolean {
        if (e.keyCode == KEYCODE_BACK || e.keyCode == KEYCODE_MENU || e.keyCode == KEYCODE_ESCAPE) root.closeMenu()
        else root.menu.key(e.keyCode, e.repeatCount)
        root.hud.nudge(); return true
    }
}
