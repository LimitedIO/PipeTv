package dev.limitedio.pipetv.ui.detail

import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import dev.limitedio.pipetv.Bg
import dev.limitedio.pipetv.data.Favs
import dev.limitedio.pipetv.data.Item
import dev.limitedio.pipetv.data.Yt
import dev.limitedio.pipetv.data.YtMap
import dev.limitedio.pipetv.data.YtPages
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.ui.Grid
import dev.limitedio.pipetv.ui.Screen
import dev.limitedio.pipetv.ui.Ui

/** Everything about one video: facts, description, comments, related videos and its channel. */
class DetailsView(c: Context, private val item: Item) : ViewGroup(c), Screen {
    var onPlay: (Item) -> Unit = {}
    var onChannel: (String) -> Unit = {}
    var onItem: (Item) -> Unit = {}; var onFav: (Item) -> Unit = {}
    private val head = DetailHead(c, item.title) { Favs.has(item.id) }
    private val hero = Hero(c, item)
    private val about = TextList(c); private val comments = TextList(c)
    private val related = Grid(c) { "" }.apply { cols = 3; upExit = true }
    private val bodies: List<View> = listOf(hero, about, comments, related, View(c), View(c))
    private var channelUrl = ""; private var zone = 0; private var commentsLoaded = false; private var on = true
    private var paged: dev.limitedio.pipetv.data.Comments? = null

    init {
        bodies.forEach { addView(it) }; addView(head); head.byline = item.sub
        head.onTab = { show(it) }; show(0); related.onPick = { onItem(it) }; related.onMenu = { onItem(it) }
        about.message = S.t(K.LOADING); Bg.io({ Yt.stream(item.url) }) { info ->
            if (info == null) { about.message = S.t(K.LOAD_FAILED); return@io }
            channelUrl = info.uploaderUrl.orEmpty()
            val subs = if (info.uploaderSubscriberCount >= 0) " · " + S.f(K.SUBS, YtMap.count(info.uploaderSubscriberCount)) else ""
            head.byline = info.uploaderName.orEmpty() + subs
            hero.info = info
            val desc = YtPages.plain(info.description)
            about.message = null; about.blocks = listOf(Block("", "", desc.ifEmpty { S.t(K.NO_DESC) }))
            related.items = Yt.related(info)
        }
    }

    private fun show(t: Int) {
        if (t == 5) return
        bodies.forEachIndexed { i, v -> v.visibility = if (i == t) VISIBLE else GONE }
        if (t == 2 && !commentsLoaded) loadComments()
    }

    private fun loadComments() {
        commentsLoaded = true; comments.message = S.t(K.LOADING)
        Bg.io({ YtPages.comments(item.url) }) { r ->
            if (r == null) { comments.message = S.t(K.LOAD_FAILED); return@io }
            paged = r
            comments.message = if (r.disabled) S.t(K.COMMENTS_OFF) else if (r.list.items.isEmpty()) S.t(K.NO_COMMENTS) else null
            comments.blocks = r.list.items.map(::block)
            comments.onEnd = { more() }
        }
    }

    private fun block(c: dev.limitedio.pipetv.data.Comment) = Block(
        c.author + if (c.pinned) " · " + S.t(K.PINNED) else "",
        listOf(c.date, c.likes, if (c.replies > 0) S.f(K.REPLIES, c.replies) else "").filter { it.isNotEmpty() }.joinToString(" · "), c.text, 8)

    private fun more() {
        val p = paged?.list ?: return
        if (!p.hasMore) return
        Bg.io({ p.loadMore() }) { r -> if (!r.isNullOrEmpty()) comments.blocks = comments.blocks + r.map(::block) }
    }

    private fun refreshLit() { head.lit = on && zone == 0; about.lit = on && zone == 1; comments.lit = on && zone == 1; related.lit = on && zone == 1 && head.sel == 3; listOf<View>(head, about, comments, related).forEach { it.invalidate() } }
    override fun lit(on: Boolean) { this.on = on; refreshLit() }

    override fun focused(): Item? = if (zone == 1 && head.sel == 3) related.focused() else item

    override fun key(code: Int, repeat: Int): Boolean {
        val tab = head.sel
        if (zone == 0) {
            if (head.key(code)) return true
            return when (code) {
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    when (tab) { 0 -> onPlay(item); 4 -> if (channelUrl.isNotEmpty()) onChannel(channelUrl) else zone = 0; 5 -> { onFav(item); head.invalidate() }; else -> enter() }; true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> { enter(); true }
                else -> false
            }
        }
        val used = when (tab) { 1 -> about.key(code); 2 -> comments.key(code); 3 -> related.key(code); else -> false }
        if (!used && code == KeyEvent.KEYCODE_DPAD_UP) { zone = 0; refreshLit(); return true }
        return used
    }

    private fun enter() { if (head.sel in 1..3) { zone = 1; refreshLit() } }

    override fun onLayout(ch: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val hh = Ui.u(280).toInt(); head.layout(0, 0, r - l, hh)
        bodies.forEach { it.layout(0, hh, r - l, b - t) }
    }
}
