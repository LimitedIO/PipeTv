package dev.limitedio.pipetv.ui.search

import android.view.View
import dev.limitedio.pipetv.ui.Ui

/**
 * Two layouts. Typing: a big keyboard (62%) beside the suggestions. After Enter: the keyboard shrinks to a
 * slim query bar and the results, with their filter chips, get the whole width.
 */
object SearchLayout {
    fun apply(w: Int, h: Int, results: Boolean, kb: View, suggest: View, chips: View, grid: View, orb: View) {
        val rtl = Ui.rtl
        if (results) {
            val bar = Ui.u(130).toInt(); val ch = Ui.u(96).toInt()
            kb.layout(0, 0, w, bar); chips.layout(0, bar, w, bar + ch); grid.layout(0, bar + ch, w, h)
            suggest.layout(0, 0, 0, 0); orb.layout(0, 0, w, h)
        } else {
            val kw = (w * .62f).toInt(); val kx = if (rtl) w - kw else 0; val rx = if (rtl) 0 else kw
            kb.layout(kx, 0, kx + kw, h); suggest.layout(rx, 0, rx + w - kw, h); orb.layout(rx, 0, rx + w - kw, h)
            chips.layout(0, 0, 0, 0); grid.layout(0, 0, 0, 0)
        }
    }
}
