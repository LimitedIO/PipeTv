package dev.limitedio.pipetv.ui.detail

import android.content.Context
import android.view.KeyEvent
import android.view.ViewGroup
import dev.limitedio.pipetv.Bg
import dev.limitedio.pipetv.data.Item
import dev.limitedio.pipetv.data.Paged
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.ui.Grid
import dev.limitedio.pipetv.ui.Screen
import dev.limitedio.pipetv.ui.Ui

/** A channel or playlist: header with a favourite button, then an endlessly scrolling grid of videos. */
class ChannelPage(c: Context, load: () -> Listing) : ViewGroup(c), Screen {
    val grid = Grid(c) { "" }.apply { cols = 3; upExit = true }
    var onFav: (Item) -> Unit = {}
    private val banner = Banner(c)
    private var paged: Paged<Item>? = null
    private var onButton = false; private var on = true

    init {
        addView(banner); addView(grid)
        banner.note = S.t(K.LOADING)
        grid.onMore = { paged?.takeIf { it.hasMore }?.let { p -> grid.loading = true; Bg.io({ p.loadMore() }) { r -> grid.loading = false; if (!r.isNullOrEmpty()) grid.append(r) } } }
        Bg.io(load) { l ->
            if (l == null) { banner.note = S.t(K.LOAD_FAILED); grid.busy = false; banner.invalidate(); return@io }
            banner.data = l; paged = l.videos; grid.items = l.videos.items; banner.invalidate()
        }
    }

    override fun focused(): Item? = if (onButton) banner.data?.item else grid.focused()

    override fun key(code: Int, repeat: Int): Boolean {
        if (!onButton) {
            if (grid.key(code)) return true
            if (code == KeyEvent.KEYCODE_DPAD_UP && banner.data != null) { onButton = true; refresh(); return true }
            return false
        }
        when (code) {
            KeyEvent.KEYCODE_DPAD_DOWN -> { onButton = false; refresh() }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { banner.data?.item?.let(onFav); banner.invalidate() }
            else -> return false
        }
        return true
    }

    private fun refresh() { grid.lit = on && !onButton; banner.focus = on && onButton; grid.invalidate(); banner.invalidate() }
    override fun lit(on: Boolean) { this.on = on; refresh() }

    override fun onLayout(ch: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val hh = Ui.u(230).toInt()
        banner.layout(0, 0, r - l, hh); grid.layout(0, hh, r - l, b - t)
    }
}
