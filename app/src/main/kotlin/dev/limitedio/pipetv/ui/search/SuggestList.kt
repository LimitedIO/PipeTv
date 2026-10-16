package dev.limitedio.pipetv.ui.search

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.KeyEvent
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.ui.AnimView
import dev.limitedio.pipetv.ui.Icons
import dev.limitedio.pipetv.ui.Txt
import dev.limitedio.pipetv.ui.Ui

/** Suggestions while typing, recent searches when the box is empty. */
class SuggestList(c: Context) : AnimView(c) {
    var items: List<String> = emptyList()
        set(v) { field = v; idx = 0; ry.snap(0f); invalidate() }
    var recent = true
    var lit = false
    var onPick: (String) -> Unit = {}
    var onClear: () -> Unit = {}
    private var idx = 0
    private val ry = spring(0f, 400f, 30f)
    private val row get() = Ui.u(88)
    private val head = Ui.paint(Ui.DIM, 26f, true)
    private val text = Ui.paint(Ui.TEXT, 32f)
    private val ico = Ui.paint(Ui.DIM)
    private val face = Ui.paint(Ui.SURFACE)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = Ui.u(4) }
    private val r = RectF()

    fun key(code: Int): Boolean {
        if (items.isEmpty()) return false
        when (code) {
            KeyEvent.KEYCODE_DPAD_UP -> if (idx >= 0) idx-- else return false
            KeyEvent.KEYCODE_DPAD_DOWN -> if (idx < items.size - 1) idx++
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { if (idx == -1) onClear() else items.getOrNull(idx)?.let(onPick); return true }
            else -> return false
        }
        ry.to(idx.toFloat()); return true
    }

    override fun render(c: Canvas, dt: Float) {
        val top = Ui.u(144); val pad = Ui.u(32)
        Txt.draw(c, S.t(if (recent) K.RECENT else K.SUGGESTED).uppercase(), pad, Ui.u(40), width - pad * 2, head)
        if (items.isNotEmpty()) {
            r.set(pad, Ui.u(60), width - pad, Ui.u(122)); c.drawRoundRect(r, Ui.u(16), Ui.u(16), face)
            Txt.draw(c, S.t(K.CLEAR), r.left, r.centerY() + Ui.u(11), r.width(), text, true)
            if (lit && idx == -1) outline(c)
        }
        val max = ((height - top) / row).toInt().coerceAtLeast(1)
        val first = (idx - max + 1).coerceAtLeast(0)
        for (i in first until minOf(items.size, first + max)) {
            val y = top + (i - first) * row
            r.set(pad, y, width - pad, y + row - Ui.u(10)); c.drawRoundRect(r, Ui.u(16), Ui.u(16), face)
            val ix = if (Ui.rtl) r.right - Ui.u(44) else r.left + Ui.u(44)
            Icons.draw(c, if (recent) Icons.HISTORY else Icons.SEARCH, ix, r.centerY(), Ui.u(14), ico)
            val tx = if (Ui.rtl) r.left + Ui.u(24) else r.left + Ui.u(88)
            Txt.draw(c, Txt.fit(text, items[i], r.width() - Ui.u(120)), tx, r.centerY() + Ui.u(11), r.width() - Ui.u(120), text)
            if (lit && i == idx) outline(c)
        }
    }

    private fun outline(c: Canvas) {
        ring.shader = LinearGradient(r.left, 0f, r.right, 0f, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP)
        c.drawRoundRect(r, Ui.u(16), Ui.u(16), ring)
    }
}
