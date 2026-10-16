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
        v.onPlay = { i -> root.controls.play(i) }; v.onChannel = ::channel; v.onItem = ::pick
        show(v)
    }

    fun channel(url: String) = show(ChannelPage(root.context) {
        val c = YtPages.channel(url)
        Listing(c.name, if (c.subs >= 0) S.f(K.SUBS, YtMap.count(c.subs)) else "", c.desc, c.avatar, c.videos)
    }.also(::wire))

    private fun playlist(url: String) = show(ChannelPage(root.context) {
        val p = YtPages.playlist(url)
        Listing(p.name, p.by + " · " + S.f(K.VIDEOS_N, p.count.toInt()), "", p.thumb, p.videos)
    }.also(::wire))

    private fun wire(p: ChannelPage) { p.grid.onPick = ::pick; p.grid.onMenu = ::details }

    private fun show(v: android.view.View) { root.screens.push(v); root.controls.focusContent() }
}
