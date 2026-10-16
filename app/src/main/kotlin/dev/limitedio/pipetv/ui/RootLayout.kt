package dev.limitedio.pipetv.ui

import android.view.View
import dev.limitedio.pipetv.media.Session

/** Geometry of the whole screen for a given dock (0..1) and fullscreen (0..1) progress. Mirrors in RTL. */
object RootLayout {
    fun apply(root: Root, dk: Float, f: Float) {
        val w = root.width; val h = root.height; if (w == 0) return
        val rail = Ui.u(104); val rtl = Ui.rtl
        val playerW = minOf(Ui.u(760), w * .58f); val panel = playerW * dk
        val l = if (rtl) 0f else rail; val r = if (rtl) w - rail else w.toFloat()
        (root.pages.views + root.screens.views).forEach { it.layout(l.toInt(), 0, r.toInt(), h) }
        val nw = Ui.u(340).toInt()
        if (rtl) root.nav.layout((w - panel - nw).toInt(), 0, (w - panel).toInt(), h)
        else root.nav.layout(panel.toInt(), 0, panel.toInt() + nw, h)
        val playerX = if (rtl) w - panel else panel - playerW
        root.playerPanel.layout(playerX.toInt(), 0, (playerX + playerW).toInt(), h)
        val mw = playerW - Ui.u(48); val mh = mw * 9 / 16
        val mx = playerX + Ui.u(24); val my = Ui.u(120)
        val x = mx * (1 - f); val y = my * (1 - f)
        if (Session.active) root.stage.layout(x.toInt(), y.toInt(), (x + mw + (w - mw) * f).toInt(), (y + mh + (h - mh) * f).toInt())
        else root.stage.layout(0, 0, 1, 1)
        root.dock.videoH = mh
        root.dock.layout(playerX.toInt(), 0, (playerX + playerW).toInt(), h)
        root.hud.layout(0, 0, w, h); root.toast.layout(0, 0, w, h); root.update.layout(0, 0, w, h)
        val mnw = minOf(Ui.u(820).toInt(), w - Ui.u(80).toInt()); val mnx = (w - mnw) / 2
        root.playerSub.layout(0, 0, w, h)
        root.navigationShade.layout(0, 0, w, h)
        root.navigationShade.visibility = if (f < .98f && (root.controls.onNav || root.dock.lit || dk > .001f)) View.VISIBLE else View.GONE
        root.menuShade.layout(0, 0, w, h)
        root.menu.layout(mnx, Ui.u(64).toInt(), mnx + mnw, h - Ui.u(64).toInt())
        root.stage.alpha = if (Session.active) maxOf(dk, f) else 0f
        val showPlayer = Session.item != null && dk > .001f && f < .98f
        root.playerPanel.visibility = if (showPlayer) View.VISIBLE else View.GONE
        root.dock.visibility = if (showPlayer) View.VISIBLE else View.GONE
        root.hud.visibility = if (f > .02f) View.VISIBLE else View.GONE
        root.nav.visibility = if (f > .98f) View.GONE else View.VISIBLE
    }
}
