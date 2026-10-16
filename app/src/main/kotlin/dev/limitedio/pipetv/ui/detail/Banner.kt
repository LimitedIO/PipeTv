package dev.limitedio.pipetv.ui.detail

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import dev.limitedio.pipetv.data.Favs
import dev.limitedio.pipetv.data.Item
import dev.limitedio.pipetv.data.Paged
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.ui.AnimView
import dev.limitedio.pipetv.ui.Glyphs
import dev.limitedio.pipetv.ui.Img
import dev.limitedio.pipetv.ui.Txt
import dev.limitedio.pipetv.ui.Ui

/** What a channel or playlist page shows. */
class Listing(val name: String, val sub: String, val desc: String, val img: String, val videos: Paged<Item>, val item: Item)

/** Header of a channel/playlist page: round picture, names, and the favourite button. */
class Banner(c: Context) : AnimView(c) {
    var data: Listing? = null; var note = ""; var focus = false
    private val h1 = Ui.paint(Ui.TEXT, 44f, true); private val sub = Ui.paint(Ui.ACCENT2, 26f, true)
    private val dim = Ui.paint(Ui.DIM, 24f); private val btn = Ui.paint(Ui.TEXT, 24f, true)
    private val pic = Paint(Paint.ANTI_ALIAS_FLAG); private val m = Matrix(); private val face = Ui.paint(Ui.SURFACE)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val r = RectF()

    override fun render(c: Canvas, dt: Float) {
        val pad = Ui.u(56); val d = Ui.u(150); val ax = Ui.mx(pad, d, width.toFloat()); val ay = Ui.u(36)
        c.drawCircle(ax + d / 2, ay + d / 2, d / 2, face)
        val l = data ?: run { Txt.draw(c, note, pad, Ui.u(110), width - pad * 2, dim); return }
        Img.get(l.img, 300)?.let { p ->
            val s = d / minOf(p.bmp.width, p.bmp.height)
            m.setScale(s, s); m.postTranslate(ax - (p.bmp.width * s - d) / 2, ay - (p.bmp.height * s - d) / 2)
            p.shader.setLocalMatrix(m); pic.shader = p.shader; c.drawCircle(ax + d / 2, ay + d / 2, d / 2, pic)
        }
        val bw = Ui.u(230); val tx = if (Ui.rtl) pad + bw + Ui.u(24) else pad + d + Ui.u(36)
        val tw = width - pad * 2 - d - Ui.u(36) - bw - Ui.u(24)
        Txt.draw(c, Txt.fit(h1, l.name, tw), if (Ui.rtl) pad + bw + Ui.u(24) else tx, ay + Ui.u(52), tw, h1)
        Txt.draw(c, Txt.fit(sub, l.sub, tw), tx, ay + Ui.u(94), tw, sub)
        Txt.draw(c, Txt.fit(dim, l.desc.replace('\n', ' '), tw), tx, ay + Ui.u(134), tw, dim)
        favButton(c, l, if (Ui.rtl) pad else width - pad - bw, ay + Ui.u(36), bw)
    }

    private fun favButton(c: Canvas, l: Listing, x: Float, y: Float, w: Float) {
        val on = Favs.has(l.item.id); r.set(x, y, x + w, y + Ui.u(70))
        c.drawRoundRect(r, Ui.u(35), Ui.u(35), face)
        Glyphs.star(c, x + Ui.u(40), y + Ui.u(35), Ui.u(16), btn, on)
        Txt.draw(c, S.t(if (on) K.SAVED else K.FAVORITE), x + Ui.u(66), y + Ui.u(45), w - Ui.u(80), btn)
        if (focus) {
            ring.strokeWidth = Ui.u(4); ring.shader = LinearGradient(x, 0f, x + w, 0f, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP)
            c.drawRoundRect(r, Ui.u(35), Ui.u(35), ring)
        }
    }
}
