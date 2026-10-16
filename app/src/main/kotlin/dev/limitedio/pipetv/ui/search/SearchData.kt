package dev.limitedio.pipetv.ui.search

import dev.limitedio.pipetv.Bg
import dev.limitedio.pipetv.data.Item
import dev.limitedio.pipetv.data.Paged
import dev.limitedio.pipetv.data.Store
import dev.limitedio.pipetv.data.Yt
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.ui.Grid

/** The network side of search: suggestions as you type, results, and endless paging. */
class SearchData(private val grid: Grid, private val suggest: SuggestList) {
    var results = false; private set
    private var token = 0
    private var paged: Paged<Item>? = null

    fun typing() { results = false; token++ }

    fun showRecents() { suggest.recent = true; suggest.items = Store.recents() }

    fun suggestions(q: String) {
        val t = ++token
        Bg.io({ Yt.suggestions(q) }) { r -> if (t == token && r != null && !results) { suggest.recent = false; suggest.items = r.take(8) } }
    }

    fun search(q: String, filter: String) {
        Store.addSearch(q)
        results = true
        grid.loading = true; grid.message = null
        if (grid.items.isEmpty()) { grid.busy = true; grid.message = S.t(K.SEARCHING) }
        val t = ++token
        Bg.io({ Yt.search(q, filter) }) { r ->
            if (t != token) return@io
            grid.loading = false; paged = r
            grid.message = if (r == null) S.t(K.SEARCH_FAILED) else if (r.items.isEmpty()) S.t(K.NO_RESULTS) else null
            grid.items = r?.items ?: emptyList(); grid.busy = false
        }
    }

    fun more() {
        val p = paged?.takeIf { it.hasMore } ?: return
        val t = token; grid.loading = true
        Bg.io({ p.loadMore() }) { r -> if (t == token) { grid.loading = false; if (!r.isNullOrEmpty()) grid.append(r) } }
    }
}
