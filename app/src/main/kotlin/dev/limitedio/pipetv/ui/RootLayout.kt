package dev.limitedio.pipetv.ui

import android.view.View
import dev.limitedio.pipetv.media.Session

/** Geometry of the whole screen for a given dock (0..1) and fullscreen (0..1) progress. Mirrors in RTL. */
object RootLayout {
    fun apply(root: Root, dk: Float, f: Float) {
        val w = root.width; val h = root.height; if (w == 0) return
        val rail = Ui.u(104); val rtl = Ui.rtl
        val sidebar = rail + Ui.u(236) * dk
        val l = if (rtl) 0f else sidebar; val r = if (rtl) w - sidebar else w.toFloat()
        (root.pages.views + root.screens.views).forEach { it.layout(l.toInt(), 0, r.toInt(), h) }
        val nw = Ui.u(340).toInt(); if (rtl) root.nav.layout(w - nw, 0, w, h) else root.nav.layout(0, 0, nw, h)
        val mw = Ui.u(292); val mh = mw * 9 / 16
        val mx = if (rtl) w - sidebar + Ui.u(24) else sidebar - Ui.u(316)
        val my = h - Ui.u(380)
        val x = mx * (1 - f); val y = my * (1 - f)
        if (Session.active) root.stage.layout(x.toInt(), y.toInt(), (x + mw + (w - mw) * f).toInt(), (y + mh + (h - mh) * f).toInt())
        else root.stage.layout(0, 0, 1, 1)
        root.dock.videoH = mh
        root.dock.layout(mx.toInt(), (my + mh + Ui.u(10)).toInt(), (mx + mw).toInt(), h - Ui.u(12).toInt())
        root.hud.layout(0, 0, w, h); root.toast.layout(0, 0, w, h); root.update.layout(0, 0, w, h)
        val mnw = minOf(Ui.u(820).toInt(), w - Ui.u(80).toInt()); val mnx = (w - mnw) / 2
        root.playerSub.layout(0, 0, w, h)
        root.menuShade.layout(0, 0, w, h)
        root.menu.layout(mnx, Ui.u(64).toInt(), mnx + mnw, h - Ui.u(64).toInt())
        root.stage.alpha = if (Session.active) 1f else 0f
        root.dock.visibility = if (dk > .02f && f < .02f) View.VISIBLE else View.GONE
        root.hud.visibility = if (f > .02f) View.VISIBLE else View.GONE
        root.nav.visibility = if (f > .98f) View.GONE else View.VISIBLE
    }
}
