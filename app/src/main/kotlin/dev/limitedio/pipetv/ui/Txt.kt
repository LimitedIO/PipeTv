package dev.limitedio.pipetv.ui

import android.graphics.Paint
import dev.limitedio.pipetv.data.Item

/** Text measuring helpers (cached on [Item]) so cards never allocate while drawing. */
object Txt {
    fun fit(p: Paint, s: String, w: Float): String {
        if (p.measureText(s) <= w) return s
        val n = p.breakText(s, true, w - p.measureText("…"), null)
        return s.take(n).trimEnd() + "…"
    }

    fun wrap(p: Paint, s: String, w: Float, max: Int): List<String> {
        val out = ArrayList<String>(max)
        var rest = s
        while (rest.isNotEmpty() && out.size < max) {
            val n = p.breakText(rest, true, w, null)
            if (n >= rest.length) { out.add(rest); break }
            val cut = rest.lastIndexOf(' ', n).takeIf { it > n / 2 } ?: n
            if (out.size == max - 1) { out.add(fit(p, rest, w)); break }
            out.add(rest.take(cut).trimEnd()); rest = rest.drop(cut).trimStart()
        }
        return out
    }

    fun lines(it: Item, p: Paint, w: Float, max: Int): List<String> {
        if (it.lines == null || it.lineW != w.toInt()) { it.lines = wrap(p, it.title, w, max); it.lineW = w.toInt() }
        return it.lines!!
    }
}
