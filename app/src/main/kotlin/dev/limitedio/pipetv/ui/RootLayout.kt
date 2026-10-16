package dev.limitedio.pipetv.ui

import android.view.View
import android.graphics.RectF
import dev.limitedio.pipetv.media.Session

/** Geometry of the whole screen for a given dock (0..1) and fullscreen (0..1) progress. Mirrors in RTL. */
object RootLayout {
    fun mini(root: Root): RectF {
        val width = Ui.u(260); val x = if (Ui.rtl) root.width - Ui.u(40) - width else Ui.u(40)
        val height = if (root.width > 0) root.height * width / root.width else width * 9 / 16
        return RectF(x, Ui.u(96), x + width, Ui.u(96) + height)
    }
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
        val mw = playerW - Ui.u(48); val mh = h * mw / w
        val mini = mini(root)
        val panelX = if (rtl) w - playerW + Ui.u(24) else Ui.u(24)
        val anchor = if (f > 0f || root.full) root.fullOrigin else RectF(
            mini.left + (panelX - mini.left) * dk,
            mini.top + (Ui.u(120) - mini.top) * dk,
            0f, 0f,
        ).apply { right = left + mini.width() + (mw - mini.width()) * dk }
        val x = anchor.left * (1 - f); val y = anchor.top * (1 - f)
        // Keep the decoder's TextureView at a stable size; transform the composed video instead.
        root.stage.layout(0, 0, w, h)
        val scale = anchor.width() / w + (1 - anchor.width() / w) * f
        root.stage.pivotX = 0f; root.stage.pivotY = 0f
        root.stage.scaleX = scale; root.stage.scaleY = scale
        root.stage.translationX = x; root.stage.translationY = y
        root.dock.videoH = mh
        root.dock.layout(playerX.toInt(), 0, (playerX + playerW).toInt(), h)
        root.nav.playerMenuFocused = root.dock.lit || dk > .001f
        root.hud.layout(0, 0, w, h); root.toast.layout(0, 0, w, h); root.update.layout(0, 0, w, h)
        val mnw = minOf(Ui.u(820).toInt(), w - Ui.u(80).toInt()); val mnx = (w - mnw) / 2
        root.playerSub.layout(0, 0, w, h)
        root.navigationShade.layout(0, 0, w, h)
        root.navigationShade.visibility = if (!root.full && (root.controls.onNav || root.dock.lit || dk > .001f)) View.VISIBLE else View.GONE
        root.menuShade.layout(0, 0, w, h)
        root.menu.layout(mnx, Ui.u(64).toInt(), mnx + mnw, h - Ui.u(64).toInt())
        root.stage.alpha = when {
            !Session.active -> 0f
            root.full || f > 0f || root.dock.lit || (root.controls.onNav && dk > .001f) -> 1f
            dk > .001f -> dk
            else -> root.nav.previewAlpha
        }
        val showPlayer = Session.active && dk > .001f && f == 0f && !root.full
        root.playerPanel.visibility = if (showPlayer) View.VISIBLE else View.GONE
        root.dock.visibility = if (showPlayer) View.VISIBLE else View.GONE
        root.hud.alpha = f
        root.hud.visibility = if (f > 0f && root.full) View.VISIBLE else View.GONE
        root.nav.visibility = if (root.full && f >= .999f) View.GONE else View.VISIBLE
    }
}
