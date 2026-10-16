package dev.limitedio.pipetv.ui

import android.content.Context
import android.view.KeyEvent
import android.view.ViewGroup
import dev.limitedio.pipetv.data.Favs
import dev.limitedio.pipetv.data.Item
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.ui.search.Chips

/** Favorites with a filter bar: All / Videos / Channels / Playlists. */
class FavsPage(c: Context) : ViewGroup(c) {
    val grid = Grid(c) { "" }.apply { upExit = true; busy = false }
    private val chips = Chips(c)
    private var onChips = true; private var on = true

    init {
        addView(grid); addView(chips)
        chips.onChange = { reload() }
    }

    fun picks(pick: (Item) -> Unit, menu: (Item) -> Unit) { grid.onPick = pick; grid.onMenu = menu }
    fun enter() { onChips = true; reload(); refresh() }
    fun back(): Boolean {
        if (onChips) return false
        onChips = true; refresh(); return true
    }

    /** Re-reads the favourites and applies the current filter. */
    fun reload() {
        val kind = when (chips.sel) { 1 -> Item.VIDEO; 2 -> Item.CHANNEL; 3 -> Item.PLAYLIST; else -> -1 }
        val all = Favs.items()
        grid.message = if (all.isEmpty()) S.t(K.NO_FAVS) else S.t(K.NO_RESULTS)
        grid.items = if (kind < 0) all else all.filter { it.kind == kind }
        grid.busy = false; invalidate()
    }

    fun lit(v: Boolean) { on = v; refresh() }
    private fun refresh() { grid.lit = on && !onChips; chips.lit = on && onChips; grid.invalidate(); chips.invalidate() }

    fun key(code: Int): Boolean {
        if (!onChips) {
            if (grid.key(code)) return true
            if (code == KeyEvent.KEYCODE_DPAD_UP) { onChips = true; refresh(); return true }
            return false
        }
        if (chips.key(code)) return true
        if (code == KeyEvent.KEYCODE_DPAD_DOWN || code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) { onChips = false; refresh(); return true }
        return false
    }

    override fun onLayout(ch: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val hh = Ui.u(130).toInt()
        chips.layout(0, Ui.u(20).toInt(), (r - l), hh); grid.layout(0, hh, r - l, b - t)
    }
}
