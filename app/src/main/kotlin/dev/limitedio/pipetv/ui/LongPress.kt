package dev.limitedio.pipetv.ui

import android.view.KeyEvent
import android.view.View
import dev.limitedio.pipetv.audio.Sfx
import dev.limitedio.pipetv.data.Favs
import dev.limitedio.pipetv.data.Item
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.ui.input.Voice

/**
 * Hold OK on a card for quick actions (play, info, favourite, open the uploader's channel).
 * A short press still works as before; it just fires when the key is released.
 */
class LongPress(private val root: Root) {
    private var armed = false; private var fired = false

    private fun target(): Item? {
        val tabOk = root.tab != 1 || root.pages.search.grid.visibility == View.VISIBLE
        val free = !root.full && !root.menuOpen && !root.playerSub.open && !root.update.modal && !Voice.active && root.controls.inContent
        return if (free && tabOk) root.screens.top?.focused() ?: root.pages.focusedItem(root.tab) else null
    }

    /** 0 = not ours; 1 = handled; 2 = handled, and it was a short press (play the select sound). */
    fun handle(e: KeyEvent): Int {
        val k = e.keyCode
        if (k != KeyEvent.KEYCODE_DPAD_CENTER && k != KeyEvent.KEYCODE_ENTER) return 0
        if (e.action == KeyEvent.ACTION_DOWN) {
            if (e.repeatCount == 0) { armed = target() != null; fired = false; return if (armed) 1 else 0 }
            if (!armed) return 0
            if (!fired) { fired = true; target()?.let { Sfx.play(Sfx.Fx.OPEN); actions(it) } }
            return 1
        }
        if (!armed) return 0
        armed = false
        if (fired) return 1
        root.controls.key(KeyEvent(KeyEvent.ACTION_DOWN, k)); return 2
    }

    private fun actions(it: Item) {
        val names = ArrayList<String>(); val runs = ArrayList<() -> Unit>()
        fun add(n: String, r: () -> Unit) { names.add(n); runs.add(r) }
        when (it.kind) {
            Item.VIDEO -> { add(S.t(K.PLAY)) { root.controls.play(it) }; add(S.t(K.VIDEO_INFO)) { root.opener.details(it) } }
            else -> add(S.t(K.OPEN)) { root.opener.pick(it) }
        }
        add(S.t(if (Favs.has(it.id)) K.FAV_DEL else K.FAV_ADD)) { root.toggleFav(it) }
        if (it.kind == Item.VIDEO && it.chan.isNotEmpty()) add(S.t(K.OPEN_CHANNEL)) { root.opener.channel(it.chan) }
        root.playerSub.choose(it.title, it.by, names, 0) { i -> runs[i]() }
    }
}
