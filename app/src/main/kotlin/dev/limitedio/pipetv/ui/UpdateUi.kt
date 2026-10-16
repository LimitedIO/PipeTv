package dev.limitedio.pipetv.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.KeyEvent
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.update.Updater
import dev.limitedio.pipetv.update.Updater.St

/** Update dialog (found / ready to install) and the download progress bar along the bottom of the screen. */
class UpdateUi(c: Context) : AnimView(c) {
    /** True while the dialog is up: it takes every key. */
    val modal get() = (Updater.st == St.FOUND || Updater.st == St.READY) && !Updater.snoozed
    private var sel = 0
    private val show = spring(0f, 220f, 26f); private val bar = spring(0f, 120f, 20f); private val dock = spring(0f, 200f, 26f)
    private val dim = Ui.paint(0xB0000000.toInt()); private val card = Ui.paint(0xFF171A2B.toInt()); private val face = Ui.paint(Ui.SURFACE)
    private val title = Ui.paint(Ui.TEXT, 46f, true); private val body = Ui.paint(Ui.DIM, 32f); private val btn = Ui.paint(Ui.TEXT, 34f, true)
    private val small = Ui.paint(Ui.TEXT, 28f, true); private val track = Ui.paint(0x33FFFFFF)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG); private val r = RectF()
    override val animating get() = Updater.st == St.DOWNLOADING

    fun refresh() { sel = 0; show.to(if (modal) 1f else 0f); dock.to(if (Updater.st == St.DOWNLOADING) 1f else 0f); invalidate() }

    fun key(k: Int): Boolean {
        when (k) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> sel = 1 - sel
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> accept()
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> later()
        }
        invalidate(); return true
    }

    private fun accept() { if (sel == 1) return later(); if (Updater.st == St.READY) Updater.install() else Updater.download() }
    private fun later() { Updater.snoozed = true; refresh() }

    override fun render(c: Canvas, dt: Float) {
        val w = width.toFloat(); val h = height.toFloat()
        bar.target = Updater.progress
        if (show.value > .01f) dialog(c, w, h, show.value)
        if (dock.value > .01f) progress(c, w, h, dock.value)
    }

    private fun dialog(c: Canvas, w: Float, h: Float, a: Float) {
        dim.alpha = (176 * a).toInt(); c.drawRect(0f, 0f, w, h, dim)
        val cw = Ui.u(960); val ch = Ui.u(470); val x = (w - cw) / 2; val y = (h - ch) / 2 + (1 - a) * Ui.u(60)
        val ready = Updater.st == St.READY
        r.set(x, y, x + cw, y + ch); card.alpha = (255 * a).toInt(); c.drawRoundRect(r, Ui.u(36), Ui.u(36), card)
        for (p in listOf(title, body, btn)) p.alpha = (255 * a).toInt()
        Txt.draw(c, S.t(K.UPD_TITLE), x + Ui.u(56), y + Ui.u(100), cw - Ui.u(112), title)
        val mb = if (Updater.size > 0) " · %.1f MB".format(java.util.Locale.ROOT, Updater.size / 1048576f) else ""
        val text = if (ready) S.f(K.UPD_READY_BODY, Updater.tag) else S.f(K.UPD_BODY, Updater.tag) + mb
        Txt.draw(c, text, x + Ui.u(56), y + Ui.u(170), cw - Ui.u(112), body)
        val bw = (cw - Ui.u(168)) / 2; val by = y + ch - Ui.u(150)
        for (i in 0..1) {
            val bx = x + Ui.u(56) + i * (bw + Ui.u(56)); r.set(bx, by, bx + bw, by + Ui.u(100))
            if (i == sel) { fill.shader = LinearGradient(r.left, 0f, r.right, 0f, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP); fill.alpha = (255 * a).toInt(); c.drawRoundRect(r, Ui.u(50), Ui.u(50), fill) }
            else { face.alpha = (255 * a).toInt(); c.drawRoundRect(r, Ui.u(50), Ui.u(50), face) }
            btn.color = if (i == sel) 0xFF0A0B14.toInt() else Ui.TEXT; btn.alpha = (255 * a).toInt()
            Txt.draw(c, S.t(if (i == 1) K.UPD_LATER else if (ready) K.UPD_INSTALL else K.UPD_NOW), r.left, r.centerY() + Ui.u(12), bw, btn, true)
        }
    }

    private fun progress(c: Canvas, w: Float, h: Float, a: Float) {
        val bw = minOf(w - Ui.u(160), Ui.u(1100)); val x = (w - bw) / 2; val y = h - Ui.u(130) + (1 - a) * Ui.u(120)
        r.set(x, y, x + bw, y + Ui.u(96)); card.alpha = (240 * a).toInt(); c.drawRoundRect(r, Ui.u(30), Ui.u(30), card)
        small.alpha = (255 * a).toInt()
        Txt.draw(c, S.f(K.UPD_DL, Updater.tag, (Updater.progress * 100).toInt()), x + Ui.u(32), y + Ui.u(40), bw - Ui.u(64), small)
        r.set(x + Ui.u(32), y + Ui.u(58), x + bw - Ui.u(32), y + Ui.u(72)); track.alpha = (80 * a).toInt(); c.drawRoundRect(r, Ui.u(7), Ui.u(7), track)
        r.right = r.left + (bw - Ui.u(64)) * bar.value
        fill.shader = LinearGradient(r.left, 0f, x + bw, 0f, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP); fill.alpha = (255 * a).toInt()
        c.drawRoundRect(r, Ui.u(7), Ui.u(7), fill)
    }
}
