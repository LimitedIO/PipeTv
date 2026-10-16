package dev.limitedio.pipetv.ui

import android.view.View
import dev.limitedio.pipetv.media.Session

/** Geometry of the whole screen for a given dock (0..1) and fullscreen (0..1) progress. Mirrors in RTL. */
object RootLayout {
    fun apply(root: Root, dk: Float, f: Float) {
        val w = root.width; val h = root.height; if (w == 0) return
        val panel = Ui.u(580) * dk; val rail = Ui.u(104); val rtl = Ui.rtl
        val l = if (rtl) panel else rail; val r = if (rtl) w - rail else w - panel
        (root.pages.views + root.screens.views).forEach { it.layout(l.toInt(), 0, r.toInt(), h) }
        val nw = Ui.u(340).toInt(); if (rtl) root.nav.layout(w - nw, 0, w, h) else root.nav.layout(0, 0, nw, h)
        val mw = Ui.u(580) - Ui.u(48); val mh = mw * 9 / 16
        val mx = if (rtl) Ui.u(24) else w - panel + Ui.u(24); val my = Ui.u(48)
        val x = mx * (1 - f); val y = my * (1 - f)
        if (Session.active) root.stage.layout(x.toInt(), y.toInt(), (x + mw + (w - mw) * f).toInt(), (y + mh + (h - mh) * f).toInt())
        else root.stage.layout(0, 0, 1, 1)
        root.dock.videoH = mh
        root.dock.layout(mx.toInt(), (my + mh + Ui.u(20)).toInt(), (mx + mw).toInt(), h - Ui.u(16).toInt())
        root.hud.layout(0, 0, w, h); root.toast.layout(0, 0, w, h)
        val mnw = Ui.u(820).toInt(); val mnx = if (rtl) Ui.u(40).toInt() else w - mnw - Ui.u(40).toInt()
        root.menu.layout(mnx, Ui.u(110).toInt(), mnx + mnw, h - Ui.u(110).toInt())
        root.stage.alpha = if (Session.active) 1f else 0f
        root.dock.visibility = if (dk > .02f && f < .02f) View.VISIBLE else View.GONE
        root.hud.visibility = if (f > .02f) View.VISIBLE else View.GONE
        root.nav.visibility = if (f > .98f) View.GONE else View.VISIBLE
    }
}
