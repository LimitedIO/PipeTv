package dev.limitedio.pipetv.ui.detail

import android.content.Context
import android.graphics.Canvas
import android.view.ViewGroup
import dev.limitedio.pipetv.Bg
import dev.limitedio.pipetv.data.Item
import dev.limitedio.pipetv.data.Paged
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.ui.AnimView
import dev.limitedio.pipetv.ui.Grid
import dev.limitedio.pipetv.ui.Img
import dev.limitedio.pipetv.ui.Screen
import dev.limitedio.pipetv.ui.Txt
import dev.limitedio.pipetv.ui.Ui

/** What a channel or a playlist page shows. */
class Listing(val name: String, val sub: String, val desc: String, val img: String, val videos: Paged<Item>)

private class Banner(c: Context) : AnimView(c) {
    var data: Listing? = null; var note = ""
    private val h1 = Ui.paint(Ui.TEXT, 44f, true); private val sub = Ui.paint(Ui.ACCENT2, 26f, true)
    private val dim = Ui.paint(Ui.DIM, 24f); private val pic = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    private val m = android.graphics.Matrix(); private val face = Ui.paint(Ui.SURFACE)

    override fun render(c: Canvas, dt: Float) {
        val pad = Ui.u(56); val d = Ui.u(150); val ax = Ui.mx(pad, d, width.toFloat()); val ay = Ui.u(36)
        c.drawCircle(ax + d / 2, ay + d / 2, d / 2, face)
        val l = data ?: run { Txt.draw(c, note, pad, Ui.u(110), width - pad * 2, dim); return }
        Img.get(l.img, 300)?.let { p ->
            val s = d / minOf(p.bmp.width, p.bmp.height)
            m.setScale(s, s); m.postTranslate(ax - (p.bmp.width * s - d) / 2, ay - (p.bmp.height * s - d) / 2)
            p.shader.setLocalMatrix(m); pic.shader = p.shader; c.drawCircle(ax + d / 2, ay + d / 2, d / 2, pic)
        }
        val tx = if (Ui.rtl) pad else pad + d + Ui.u(36); val tw = width - pad * 2 - d - Ui.u(36)
        Txt.draw(c, Txt.fit(h1, l.name, tw), tx, ay + Ui.u(52), tw, h1)
        Txt.draw(c, Txt.fit(sub, l.sub, tw), tx, ay + Ui.u(94), tw, sub)
        Txt.draw(c, Txt.fit(dim, l.desc.replace('\n', ' '), tw), tx, ay + Ui.u(134), tw, dim)
    }
}

/** A channel or playlist: header plus an endlessly scrolling grid of videos. */
class ChannelPage(c: Context, load: () -> Listing) : ViewGroup(c), Screen {
    val grid = Grid(c) { "" }.apply { cols = 3 }
    private val banner = Banner(c)
    private var paged: Paged<Item>? = null

    init {
        addView(banner); addView(grid)
        banner.note = S.t(K.LOADING)
        grid.onMore = {
            paged?.takeIf { it.hasMore }?.let { p -> Bg.io({ p.loadMore() }) { r -> if (!r.isNullOrEmpty()) grid.append(r) } }
        }
        Bg.io(load) { l ->
            if (l == null) { banner.note = S.t(K.LOAD_FAILED); grid.busy = false; banner.invalidate(); return@io }
            banner.data = l; paged = l.videos; grid.items = l.videos.items; banner.invalidate()
        }
    }

    override fun key(code: Int, repeat: Int) = grid.key(code)
    override fun lit(on: Boolean) { grid.lit = on; grid.invalidate() }

    override fun onLayout(ch: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val hh = Ui.u(230).toInt()
        banner.layout(0, 0, r - l, hh); grid.layout(0, hh, r - l, b - t)
    }
}
