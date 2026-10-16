package dev.limitedio.pipetv.ui

import android.content.Context
import android.os.SystemClock
import android.view.View
import dev.limitedio.pipetv.Bg
import dev.limitedio.pipetv.data.Feed
import dev.limitedio.pipetv.data.Item
import dev.limitedio.pipetv.data.Store
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.ui.search.SearchPage
import dev.limitedio.pipetv.ui.timeline.TimelineView
import dev.limitedio.pipetv.ui.widgets.RowList

/** The five main pages and how each one loads its content. */
class Pages(c: Context, pick: (Item) -> Unit, menu: (Item) -> Unit, onLang: () -> Unit, toast: (String) -> Unit) {
    val home = Grid(c) { S.t(K.RECOMMENDED) }
    val search = SearchPage(c)
    val trending = Grid(c) { S.t(K.TRENDING) }
    val history = TimelineView(c)
    val settings = RowList(c, { S.t(K.SETTINGS) })
    val views: List<View> = listOf(home, search, trending, history, settings)
    private var homeAt = 0L
    private var trendingLoaded = false

    init {
        listOf(home, trending).forEach { it.onPick = pick; it.onMenu = menu }
        search.picks(pick, menu); history.onPick = pick; history.onMenu = menu
        settings.rows = Settings.rows(onLang, { search.keyboard.refresh() }, toast)
        home.items = mark(Store.load("feed"))
        if (home.items.isEmpty()) home.busy = true
    }

    private fun mark(l: List<Item>): List<Item> {
        val m = Store.entries().associate { it.item.id to it.fraction }
        l.forEach { it.progress = m[it.id] ?: 0f }
        return l
    }

    /** Called whenever a page becomes visible. */
    fun enter(i: Int) {
        when (i) {
            0 -> if (homeAt == 0L || SystemClock.uptimeMillis() - homeAt > 600_000) loadHome()
            2 -> if (!trendingLoaded) { trendingLoaded = true; load(trending) { Feed.trending() } }
            3 -> history.reload()
        }
    }

    /** The language changed: refresh everything that was fetched in the old one. */
    fun relocalize(current: Int) { homeAt = 0; trendingLoaded = false; search.keyboard.refresh(); enter(current) }

    private fun loadHome() {
        homeAt = SystemClock.uptimeMillis()
        Bg.io({ Feed.home() }) { r ->
            if (r.isNullOrEmpty()) { homeAt = 0; home.busy = false; if (home.items.isEmpty()) home.message = S.t(K.NO_CONNECTION) }
            else { home.items = mark(r); Store.save("feed", r) }
            home.invalidate()
        }
    }

    private fun load(g: Grid, work: () -> List<Item>) = Bg.io({ work() }) { r ->
        g.message = if (r == null) S.t(K.LOAD_FAILED) else null
        g.items = mark(r ?: emptyList()); g.busy = false
    }

    fun setCols(n: Int) { listOf(home, trending).forEach { it.cols = n } }

    fun lit(i: Int, on: Boolean) {
        when (val v = views[i]) {
            is Grid -> { v.lit = on; v.invalidate() }
            is SearchPage -> v.lit(on)
            is TimelineView -> { v.lit = on; v.invalidate() }
            is RowList -> { v.lit = on; v.invalidate() }
        }
    }

    fun key(i: Int, code: Int, repeat: Int): Boolean = when (val v = views[i]) {
        is Grid -> v.key(code); is SearchPage -> v.key(code); is TimelineView -> v.key(code)
        is RowList -> v.key(code, repeat); else -> false
    }
}
