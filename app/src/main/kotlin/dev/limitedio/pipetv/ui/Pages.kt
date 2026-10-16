package dev.limitedio.pipetv.ui

import android.content.Context
import android.os.SystemClock
import android.view.View
import dev.limitedio.pipetv.Bg
import dev.limitedio.pipetv.data.Favs
import dev.limitedio.pipetv.data.Feed
import dev.limitedio.pipetv.data.Item
import dev.limitedio.pipetv.data.NetCheck
import dev.limitedio.pipetv.data.Store
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.ui.search.SearchPage
import dev.limitedio.pipetv.ui.timeline.TimelineView

/** The five main pages and how each one loads its content. */
class Pages(c: Context, pick: (Item) -> Unit, menu: (Item) -> Unit, onLang: () -> Unit, toast: (String) -> Unit, onAbout: () -> Unit) {
    val home = Grid(c) { S.t(K.RECOMMENDED) }
    val search = SearchPage(c)
    val popular = PopularPage(c)
    val favs = FavsPage(c)
    val history = TimelineView(c)
    val settings = SettingsPage(c, onLang, { search.keyboard.refresh() }, toast, onAbout)
    val views: List<View> = listOf(home, search, popular, favs, history, settings)
    private var homeAt = 0L; private var retries = 0

    init {
        home.onRetry = { homeAt = 0; retries = 0; loadHome() }; home.onPick = pick; home.onMenu = menu; popular.picks(pick, menu)
        favs.picks(pick, menu)
        search.picks(pick, menu); history.onPick = pick; history.onMenu = menu
        home.items = mark(Store.load("feed"))
        if (home.items.isEmpty()) home.busy = true
    }

    private fun mark(l: List<Item>): List<Item> {
        val m = Store.entries().associate { it.item.id to it.fraction }
        l.forEach { it.progress = m[it.id] ?: 0f }
        return l
    }

    fun enter(i: Int) {
        when (i) {
            0 -> if (homeAt == 0L || SystemClock.uptimeMillis() - homeAt > 600_000) loadHome()
            2 -> popular.enter()
            3 -> favs.enter()
            4 -> history.reload()
        }
    }

    /** The language changed: refresh everything that was fetched in the old one. */
    fun relocalize(current: Int) { homeAt = 0; popular.reset(); search.keyboard.refresh(); enter(current) }

    // The network often comes up a moment after a TV boots: keep trying quietly with a growing pause.
    private fun retryHome() {
        retries++
        Bg.main.postDelayed({ if (home.items.isEmpty()) { home.busy = true; home.message = null; home.invalidate(); loadHome() } }, minOf(30_000L, 3_000L * retries))
    }

    private fun loadHome() {
        homeAt = SystemClock.uptimeMillis()
        Bg.io({ Feed.home() }) { r ->
            if (r.isNullOrEmpty()) {
                homeAt = 0; home.busy = false
                if (home.items.isEmpty()) {
                    home.message = S.t(K.NO_CONNECTION)
                    Bg.io({ NetCheck.probe() + (Bg.lastError.takeIf { e -> e.isNotEmpty() }?.let { e -> " · $e" } ?: "") }) { d -> if (home.items.isEmpty()) home.message = S.t(K.NO_CONNECTION) + " · " + d; home.invalidate() }
                    retryHome()
                }
            } else { retries = 0; home.message = null; home.items = mark(r); Store.save("feed", r) }
            home.invalidate()
        }
    }
    fun closeSub(): Boolean = settings.closeSub()
    fun back(i: Int): Boolean = when (i) {
        1 -> search.back(); 2 -> popular.back(); 3 -> favs.back(); 5 -> closeSub(); else -> false
    }
    fun setCols(n: Int) { listOf(home, popular.grid, favs.grid).forEach { it.cols = n } }
    // Redraws every page and the grids inside them (cards show the loading state of the video being opened).
    fun repaint() = views.forEach { v -> v.invalidate(); (v as? android.view.ViewGroup)?.let { g -> for (i in 0 until g.childCount) g.getChildAt(i).invalidate() } }

    fun refreshFavs() { favs.reload(); views.forEach { it.invalidate() } }

    /** The focused card of page [i] (what the favourite key acts on). */
    fun focusedItem(i: Int): Item? = when (val v = views[i]) { is Grid -> v.focused(); is PopularPage -> v.grid.focused(); is FavsPage -> v.grid.focused(); is SearchPage -> v.grid.focused(); is TimelineView -> v.focused(); else -> null }

    fun lit(i: Int, on: Boolean) {
        when (val v = views[i]) {
            is Grid -> { v.lit = on; v.invalidate() }
            is SearchPage -> v.lit(on)
            is FavsPage -> v.lit(on)
            is PopularPage -> v.lit(on)
            is TimelineView -> { v.lit = on; v.invalidate() }
            is SettingsPage -> v.lit(on)
        }
    }

    fun key(i: Int, code: Int, repeat: Int): Boolean = when (val v = views[i]) {
        is Grid -> v.key(code); is PopularPage -> v.key(code); is FavsPage -> v.key(code); is SearchPage -> v.key(code); is TimelineView -> v.key(code)
        is SettingsPage -> v.key(code, repeat); else -> false
    }
}
