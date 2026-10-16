package dev.limitedio.pipetv.ui

import android.view.View
import dev.limitedio.pipetv.media.Session

/** Geometry of the whole screen for a given dock (0..1) and fullscreen (0..1) progress. Mirrors in RTL. */
object RootLayout {
    fun apply(root: Root, dk: Float, f: Float) {
        val w = root.width; val h = root.height; if (w == 0) return
        val pw = Ui.u(root.panelW.value); val panel = pw * dk; val rail = Ui.u(104); val rtl = Ui.rtl
        val l = if (rtl) panel else rail; val r = if (rtl) w - rail else w - panel
        (root.pages.views + root.screens.views).forEach { it.layout(l.toInt(), 0, r.toInt(), h) }
        val nw = Ui.u(340).toInt(); if (rtl) root.nav.layout(w - nw, 0, w, h) else root.nav.layout(0, 0, nw, h)
        val mw = pw - Ui.u(48); val mh = mw * 9 / 16
        val mx = if (rtl) Ui.u(24) else w - panel + Ui.u(24); val my = Ui.u(48)
        val g = 1f + .06f * root.dock.popValue * (1 - f); val gx = (mw * g - mw) / 2; val gy = (mh * g - mh) / 2
        val x = (mx - gx) * (1 - f); val y = (my - gy) * (1 - f)
        if (Session.active) root.stage.layout(x.toInt(), y.toInt(), (x + mw * g + (w - mw * g) * f).toInt(), (y + mh * g + (h - mh * g) * f).toInt())
        else root.stage.layout(0, 0, 1, 1)
        root.dock.videoH = mh
        root.dock.layout(mx.toInt(), (my + mh + Ui.u(20)).toInt(), (mx + mw).toInt(), h - Ui.u(16).toInt())
        root.hud.layout(0, 0, w, h); root.toast.layout(0, 0, w, h); root.update.layout(0, 0, w, h)
        val mnw = Ui.u(820).toInt(); val mnx = if (rtl) Ui.u(40).toInt() else w - mnw - Ui.u(40).toInt()
        root.playerSub.layout(0, 0, w, h)
        root.menu.layout(mnx, Ui.u(110).toInt(), mnx + mnw, h - Ui.u(110).toInt())
        root.stage.alpha = if (Session.active) 1f else 0f
        root.dock.visibility = if (dk > .02f && f < .02f) View.VISIBLE else View.GONE
        root.hud.visibility = if (f > .02f) View.VISIBLE else View.GONE
        root.nav.visibility = if (f > .98f) View.GONE else View.VISIBLE
    }
}
