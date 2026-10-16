package dev.limitedio.pipetv.ui.detail

import android.content.Context
import android.graphics.Canvas
import dev.limitedio.pipetv.data.Item
import dev.limitedio.pipetv.data.YtMap
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.ui.AnimView
import dev.limitedio.pipetv.ui.Cards
import dev.limitedio.pipetv.ui.Icons
import dev.limitedio.pipetv.ui.Txt
import dev.limitedio.pipetv.ui.Ui
import org.schabi.newpipe.extractor.stream.StreamInfo

/** The "Play" tab: a big thumbnail plus the key facts about the video. */
class Hero(c: Context, private val item: Item) : AnimView(c) {
    var info: StreamInfo? = null
        set(v) { field = v; invalidate() }
    private val key = Ui.paint(Ui.DIM, 22f)
    private val value = Ui.paint(Ui.TEXT, 29f, true)
    private val icon = Ui.paint(Ui.TEXT)
    private val dark = Ui.paint(0x99000000.toInt())
    private val hint = Ui.paint(Ui.DIM, 24f)

    private fun facts(i: StreamInfo?): List<Pair<String, String>> {
        if (i == null) return emptyList()
        val list = ArrayList<Pair<String, String>>()
        val date = dev.limitedio.pipetv.data.Ago.of(i.uploadDate, i.textualUploadDate)
        if (date.isNotEmpty()) list.add(S.t(K.PUBLISHED) to date)
        list.add(S.t(K.DURATION) to if (i.duration > 0) Ui.fmt(i.duration * 1000) else S.t(K.LIVE))
        if (i.viewCount >= 0) list.add(S.t(K.VIEWS).replace("%s", "") .trim().ifEmpty { "Views" } to YtMap.count(i.viewCount))
        if (i.likeCount >= 0) list.add(S.t(K.LIKES).replace("%s", "").trim().ifEmpty { "Likes" } to YtMap.count(i.likeCount))
        if (!i.category.isNullOrEmpty()) list.add(S.t(K.CATEGORY) to i.category)
        if (i.tags.isNotEmpty()) list.add(S.t(K.TAGS) to i.tags.take(6).joinToString(" · "))
        return list
    }

    override fun render(c: Canvas, dt: Float) {
        val pad = Ui.u(56); val tw = minOf(width * .42f, Ui.u(760)); val th = tw * 9 / 16
        val tx = Ui.mx(pad, tw, width.toFloat()); val ty = Ui.u(30)
        Cards.picture(c, item, tx, ty, tw, th)
        c.drawCircle(tx + tw / 2, ty + th / 2, Ui.u(46), dark); Icons.play(c, tx + tw / 2 + Ui.u(4), ty + th / 2, Ui.u(24), icon)
        Txt.draw(c, "OK ▶  " + S.t(K.PLAY), tx, ty + th + Ui.u(44), tw, hint, true)
        val fx = if (Ui.rtl) pad else pad + tw + Ui.u(48); val fw = width - tw - pad * 2 - Ui.u(48)
        var y = ty + Ui.u(30)
        for ((k, v) in facts(info)) {
            Txt.draw(c, k, fx, y, fw, key); Txt.draw(c, Txt.fit(value, v, fw), fx, y + Ui.u(36), fw, value); y += Ui.u(88)
        }
    }
}
