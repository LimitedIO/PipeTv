package dev.limitedio.pipetv.ui

import dev.limitedio.pipetv.data.Item
import dev.limitedio.pipetv.data.YtMap
import dev.limitedio.pipetv.data.YtPages
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.ui.detail.ChannelPage
import dev.limitedio.pipetv.ui.detail.DetailsView
import dev.limitedio.pipetv.ui.detail.Listing

/** Decides what a pick means: play a video, open a channel or a playlist, show details. */
class Opener(private val root: Root) {
    fun pick(it: Item) {
        when (it.kind) {
            Item.CHANNEL -> channel(it.url)
            Item.PLAYLIST -> playlist(it.url)
            else -> root.controls.play(it)
        }
    }

    fun details(it: Item) {
        if (it.kind != Item.VIDEO) return pick(it)
        val v = DetailsView(root.context, it)
        v.onPlay = { i -> root.controls.play(i) }; v.onChannel = ::channel; v.onItem = ::pick; v.onFav = root::toggleFav
        show(v)
    }

    fun channel(url: String) = show(ChannelPage(root.context) {
        val c = YtPages.channel(url)
        val subs = if (c.subs >= 0) S.f(K.SUBS, YtMap.count(c.subs)) else ""
        Listing(c.name, subs, c.desc, c.avatar, c.videos, Item("c" + url.hashCode(), url, c.name, subs, 0, Item.CHANNEL, c.avatar, subs, ""))
    }.also(::wire))

    private fun playlist(url: String) = show(ChannelPage(root.context) {
        val p = YtPages.playlist(url)
        val n = YtMap.countText(p.count)
        Listing(p.name, p.by + " · " + n, "", p.thumb, p.videos, Item("p" + url.hashCode(), url, p.name, p.by, 0, Item.PLAYLIST, p.thumb, p.by, n))
    }.also(::wire))

    private fun wire(p: ChannelPage) { p.grid.onPick = ::pick; p.grid.onMenu = ::details; p.onFav = root::toggleFav }

    private fun show(v: android.view.View) { root.screens.push(v); root.controls.focusContent() }
}
