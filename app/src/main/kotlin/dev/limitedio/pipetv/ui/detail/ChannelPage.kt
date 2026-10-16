package dev.limitedio.pipetv.ui.detail

import android.content.Context
import android.graphics.Canvas
import android.view.KeyEvent
import android.view.ViewGroup
import dev.limitedio.pipetv.Bg
import dev.limitedio.pipetv.data.Item
import dev.limitedio.pipetv.data.Paged
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.ui.*

/** Channel tabs cache their pages; stale requests never replace the selected tab. */
class ChannelPage(c: Context, load: () -> Listing) : ViewGroup(c), Screen {
    val grid = Grid(c) { "" }.apply { cols = 3; upExit = true }
    var onFav: (Item) -> Unit = {}
    private val banner = Banner(c)
    private val about = TextList(c).apply { visibility = GONE }
    private var selected = 0; private var zone = 2; private var on = true
    private val channel get() = banner.data?.item?.kind == Item.CHANNEL
    private fun names(): List<String> = if (!channel) emptyList() else banner.data!!.tabs.map {
        when (it.name) { "videos" -> S.t(K.VIDEOS); "playlists" -> S.t(K.F_PLAYLISTS); else -> it.name.replaceFirstChar { ch -> ch.titlecase() } }
    } + S.t(K.ABOUT)
    private val tabs = object : AnimView(c) {
        private val ink = Ui.paint(Ui.TEXT, 26f, true)
        private val face = Ui.paint(Ui.ACCENT)
        override fun render(c: Canvas, dt: Float) {
            val names = names(); if (names.isEmpty()) return
            val gap = Ui.u(12); val pad = Ui.u(40)
            val w = (width - pad * 2 - gap * (names.size - 1)) / names.size
            names.forEachIndexed { i, name ->
                val x = Ui.mx(pad + i * (w + gap), w, width.toFloat())
                face.color = if (i == selected) Ui.ACCENT else Ui.SURFACE
                c.drawRoundRect(x, Ui.u(6), x + w, Ui.u(58), Ui.u(16), Ui.u(16), face)
                Txt.draw(c, Txt.fit(ink, name, w - Ui.u(16)), x, Ui.u(41), w, ink, true)
                if (on && zone == 1 && selected == i) c.drawRect(x + Ui.u(14), Ui.u(61), x + w - Ui.u(14), Ui.u(65), ink)
            }
        }
    }
    private val cache = mutableMapOf<Int, Paged<Item>>()
    private val loadingTabs = mutableSetOf<Int>()
    private val loadingPages = mutableSetOf<Paged<Item>>()
    private var paged: Paged<Item>? = null

    init {
        addView(banner); addView(tabs); addView(grid); addView(about)
        grid.onScroll = { refresh() }
        banner.note = S.t(K.LOADING)
        grid.onMore = {
            paged?.takeIf { it.hasMore && loadingPages.add(it) }?.let { p ->
                grid.loading = true
                Bg.io({ p.loadMore() }) { result ->
                    loadingPages.remove(p)
                    if (paged === p) { grid.loading = false; if (!result.isNullOrEmpty()) grid.append(p.items.drop(grid.items.size)) }
                }
            }
        }
        Bg.io(load) { l ->
            if (l == null) { banner.note = S.t(K.LOAD_FAILED); grid.busy = false; banner.invalidate(); return@io }
            banner.data = l; cache[0] = l.videos
            about.blocks = listOf(Block("", "", l.desc.ifBlank { S.t(K.NO_DESC) }))
            select(0)
            // Root positions its children directly, so unchanged outer bounds do not trigger layout.
            if (width > 0) onLayout(true, left, top, right, bottom)
            requestLayout(); refresh()
        }
    }

    private fun select(i: Int) {
        selected = i; paged = null; grid.loading = false; grid.message = null; grid.onRetry = null
        val l = banner.data ?: return
        val isAbout = channel && i == l.tabs.size
        about.visibility = if (isAbout) VISIBLE else GONE
        grid.visibility = if (isAbout) GONE else VISIBLE
        if (isAbout) { about.reset(); refresh(); return }
        cache[i]?.let { paged = it; grid.items = it.items; grid.loading = it in loadingPages; refresh(); return }
        grid.items = emptyList(); grid.busy = true; grid.invalidate()
        l.tabs.getOrNull(i)?.let { tab ->
            if (!loadingTabs.add(i)) { refresh(); return }
            Bg.io(tab.load) { p ->
                loadingTabs.remove(i)
                if (p != null) cache[i] = p
                if (selected == i) {
                    paged = p; grid.items = p?.items.orEmpty()
                    if (p == null) { grid.message = S.t(K.LOAD_FAILED); grid.onRetry = { select(i) } }
                    grid.invalidate()
                }
            }
        }
        refresh()
    }

    override fun focused(): Item? = if (zone == 0) banner.data?.item else if (zone == 2 && grid.visibility == VISIBLE) grid.focused() else null
    override fun back(): Boolean {
        if (zone != 2 || !channel) return false
        zone = 1; refresh(); return true
    }
    override fun key(code: Int, repeat: Int): Boolean {
        if (zone == 2) {
            if (if (about.visibility == VISIBLE) about.key(code) else grid.key(code)) return true
            if (code == KeyEvent.KEYCODE_DPAD_UP) { zone = if (channel) 1 else 0; refresh(); return true }
            return false
        }
        when (code) {
            KeyEvent.KEYCODE_DPAD_UP -> zone = 0
            KeyEvent.KEYCODE_DPAD_DOWN -> zone = if (zone == 0 && channel) 1 else 2
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (zone != 1) return false
                val i = selected + if (code == KeyEvent.KEYCODE_DPAD_RIGHT) 1 else -1
                if (i !in names().indices) return false
                select(i)
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> if (zone == 0) banner.data?.item?.let(onFav) else zone = 2
            else -> return false
        }
        refresh(); return true
    }

    private fun refresh() {
        val compact = channel && zone == 2 && grid.visibility == VISIBLE && grid.hasScrolled
        if (banner.compact != compact) {
            banner.compact = compact
            if (width > 0) onLayout(true, left, top, right, bottom)
        }
        grid.lit = on && zone == 2; about.lit = grid.lit; banner.focus = on && zone == 0
        grid.invalidate(); about.invalidate(); banner.invalidate(); tabs.invalidate()
    }
    override fun lit(on: Boolean) { this.on = on; refresh() }
    override fun onLayout(ch: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val hh = Ui.u(if (banner.compact) 104 else if (banner.data?.banner?.isNotEmpty() == true) 300 else 200).toInt()
        val th = if (channel) Ui.u(76).toInt() else 0
        banner.layout(0, 0, r - l, hh); tabs.layout(0, hh, r - l, hh + th)
        grid.layout(0, hh + th, r - l, b - t); about.layout(0, hh + th, r - l, b - t)
    }
}
