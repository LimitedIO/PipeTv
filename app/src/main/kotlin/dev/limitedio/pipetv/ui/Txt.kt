package dev.limitedio.pipetv.ui

import android.graphics.Canvas
import android.graphics.Paint
import dev.limitedio.pipetv.data.Item

/** Text measuring helpers (cached on [Item]) so cards never allocate while drawing. */
object Txt {
    /** True if [s] reads right-to-left (first strong character); neutral text follows the UI direction. */
    fun isRtl(s: String): Boolean {
        for (ch in s) when (Character.getDirectionality(ch)) {
            Character.DIRECTIONALITY_LEFT_TO_RIGHT -> return false
            Character.DIRECTIONALITY_RIGHT_TO_LEFT, Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC -> return true
        }
        return Ui.rtl
    }

    /** Draws [s] inside the box [x, x+w]: right-aligned for RTL text, centred if [center]. */
    fun draw(c: Canvas, s: String, x: Float, y: Float, w: Float, p: Paint, center: Boolean = false) {
        val rtl = !center && isRtl(s)
        p.textAlign = if (center) Paint.Align.CENTER else if (rtl) Paint.Align.RIGHT else Paint.Align.LEFT
        c.drawText(s, if (center) x + w / 2 else if (rtl) x + w else x, y, p)
        p.textAlign = Paint.Align.LEFT
    }

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
