package dev.limitedio.pipetv.ui

import android.content.Context
import android.os.SystemClock
import android.view.View
import dev.limitedio.pipetv.Bg
import dev.limitedio.pipetv.data.Feed
import dev.limitedio.pipetv.data.Item
import dev.limitedio.pipetv.data.Store

/** The five pages of the app and how each one loads its content. */
class Pages(c: Context, play: (Item) -> Unit) {
    val home = Grid(c, "Recommended for you")
    val search = SearchPage(c)
    val trending = Grid(c, "Trending")
    val history = Grid(c, "Watch history")
    val settings = SettingsView(c)
    val views: List<View> = listOf(home, search, trending, history, settings)
    private var homeAt = 0L
    private var trendingLoaded = false

    init {
        listOf(home, trending, history).forEach { it.onPick = play }
        search.onPick = play
        home.items = Store.load("feed")
        if (home.items.isEmpty()) home.busy = true
    }

    /** Called whenever a page becomes visible. */
    fun enter(i: Int) {
        when (i) {
            0 -> if (SystemClock.uptimeMillis() - homeAt > 600_000 || homeAt == 0L) loadHome()
            2 -> if (!trendingLoaded) { trendingLoaded = true; load(trending) { Feed.trending() } }
            3 -> { history.message = "Nothing watched yet. Pick a video!"; history.items = Store.history() }
        }
    }

    private fun loadHome() {
        homeAt = SystemClock.uptimeMillis()
        Bg.io({ Feed.home() }) { r ->
            if (r.isNullOrEmpty()) { homeAt = 0; home.busy = false; if (home.items.isEmpty()) home.message = "No connection. Press OK to retry." }
            else { home.items = r; Store.save("feed", r) }
            home.invalidate()
        }
    }

    private fun load(g: Grid, work: () -> List<Item>) = Bg.io({ work() }) { r ->
        g.message = if (r == null) "Couldn't load. Check your connection." else null
        g.items = r ?: emptyList(); g.busy = false
    }

    fun setCols(n: Int) { listOf(home, trending, history).forEach { it.cols = n } }

    fun lit(i: Int, on: Boolean) {
        when (val v = views[i]) { is Grid -> { v.lit = on; v.invalidate() }; is SearchPage -> v.lit(on) }
    }

    fun key(i: Int, code: Int): Boolean = when (val v = views[i]) {
        is Grid -> v.key(code); is SearchPage -> v.key(code); is SettingsView -> v.key(code); else -> false
    }
}
