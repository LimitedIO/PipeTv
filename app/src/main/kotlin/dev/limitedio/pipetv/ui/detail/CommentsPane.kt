package dev.limitedio.pipetv.ui.detail

import dev.limitedio.pipetv.Bg
import dev.limitedio.pipetv.data.Comment
import dev.limitedio.pipetv.data.Paged
import dev.limitedio.pipetv.data.YtPages
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S

/** The comments tab: loads comments page by page and opens the replies of a comment (Back returns to the list). */
class CommentsPane(private val url: String, private val list: TextList) {
    private var loaded = false
    private var shown: List<Comment> = emptyList(); private var paged: Paged<Comment>? = null
    private var saved: Triple<List<Comment>, Paged<Comment>?, Int>? = null

    init { list.onEnd = { more() }; list.onSelect = ::select }

    fun load() {
        if (loaded) return
        loaded = true; list.message = S.t(K.LOADING); list.hint = S.t(K.REPLIES_HINT)
        Bg.io({ YtPages.comments(url) }) { r ->
            if (r == null) { list.message = S.t(K.LOAD_FAILED); return@io }
            paged = r.list; set(r.list.items)
            list.message = if (r.disabled) S.t(K.COMMENTS_OFF) else if (r.list.items.isEmpty()) S.t(K.NO_COMMENTS) else null
        }
    }

    private fun set(l: List<Comment>) { shown = l; list.blocks = l.map(::block) }

    private fun block(c: Comment) = Block(
        c.author + if (c.pinned) S.sep + S.t(K.PINNED) else "",
        listOf(c.date, c.likes, if (c.replies > 0) S.f(K.REPLIES, c.replies) else "").filter { it.isNotEmpty() }.joinToString(S.sep), c.text, 8, c.replies > 0 && c.page != null)

    private fun more() {
        val p = paged?.takeIf { it.hasMore } ?: return
        Bg.io({ p.loadMore() }) { r -> if (!r.isNullOrEmpty() && paged === p) { shown = shown + r; list.blocks = list.blocks + r.map(::block) } }
    }

    private fun select(i: Int) {
        val c = shown.getOrNull(i) ?: return
        val page = c.page ?: return
        if (saved != null || c.replies <= 0) return
        saved = Triple(shown, paged, i); paged = null; set(listOf(c)); list.reset(); list.message = S.t(K.LOADING)
        Bg.io({ YtPages.replies(url, page) }) { r ->
            if (saved == null) return@io
            list.message = if (r == null) S.t(K.LOAD_FAILED) else null
            if (r != null) { paged = r; set(listOf(c) + r.items) }
        }
    }

    /** Leaves a reply thread; false when the list was not showing one. */
    fun back(): Boolean {
        val s = saved ?: return false
        saved = null; paged = s.second; list.message = null; set(s.first); list.focusAt(s.third); return true
    }
}
