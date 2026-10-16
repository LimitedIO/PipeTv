package dev.limitedio.pipetv.ui

import android.content.Context
import android.view.KeyEvent
import android.view.ViewGroup
import dev.limitedio.pipetv.Bg
import dev.limitedio.pipetv.data.Feed
import dev.limitedio.pipetv.data.Item
import dev.limitedio.pipetv.data.Store
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S

/** Popular videos with topic filters: Trending, Music, Gaming, News, Sports, Movies, Learning, Comedy, Live. */
class PopularPage(c: Context) : ViewGroup(c) {
    private val topics = listOf(K.TRENDING, K.T_MUSIC, K.T_GAMING, K.T_NEWS, K.T_SPORTS, K.T_MOVIES, K.T_LEARN, K.T_COMEDY, K.T_LIVE)
    val grid = Grid(c) { "" }.apply { upExit = true }
    private val chips = TopicChips(c, topics)
    private val cache = HashMap<Int, List<Item>>()
    private var onChips = false; private var on = true; private var ask = 0

    init { addView(grid); addView(chips); chips.onChange = { show(it) }; grid.onRetry = { show(chips.sel) } }

    fun picks(pick: (Item) -> Unit, menu: (Item) -> Unit) { grid.onPick = pick; grid.onMenu = menu }

    /** Loads the selected topic once; later visits show the cached list. */
    fun enter() { onChips = true; refresh(); if (cache[chips.sel] == null) show(chips.sel) }
    fun reset() { cache.clear(); show(chips.sel) }
    fun back(): Boolean {
        if (onChips) return false
        onChips = true; refresh(); return true
    }

    private fun show(i: Int) {
        val t = ++ask
        cache[i]?.let { grid.message = null; grid.items = it; return }
        grid.items = emptyList(); grid.message = null; grid.busy = true
        Bg.io({ Feed.topic(S.t(topics[i]), i == 0) }) { r ->
            if (t != ask) return@io
            val seen = Store.entries().associate { e -> e.item.id to e.fraction }
            val l = (r ?: emptyList()).onEach { it.progress = seen[it.id] ?: 0f }
            if (r != null) cache[i] = l
            grid.message = if (r == null) S.t(K.LOAD_FAILED) else if (l.isEmpty()) S.t(K.NO_RESULTS) else null
            grid.items = l; grid.busy = false
        }
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
        chips.layout(0, Ui.u(20).toInt(), r - l, hh); grid.layout(0, hh, r - l, b - t)
    }
}
