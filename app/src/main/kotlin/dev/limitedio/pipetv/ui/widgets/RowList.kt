package dev.limitedio.pipetv.ui.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.KeyEvent
import dev.limitedio.pipetv.ui.AnimView
import dev.limitedio.pipetv.ui.Scrollbar
import dev.limitedio.pipetv.ui.Txt
import dev.limitedio.pipetv.ui.Ui

/** Scrollable list of [Row]s with one gliding focus ring. Used by Settings and the player options. */
class RowList(c: Context, private val title: () -> String, private val panel: Boolean = false) : AnimView(c) {
    var rows: List<Row> = emptyList()
        set(v) { field = v; idx = v.indexOfFirst { it.focusable }.coerceAtLeast(0); if (width > 0) { refocus(); ry.snap(ry.target); rh.snap(rh.target); scroll.snap(0f) }; invalidate() }
    var lit = true
    private var idx = 0
    private val scroll = spring(0f, 180f, 26f)
    private val ry = spring(0f, 400f, 30f); private val rh = spring(100f, 400f, 30f)
    private val head = Ui.paint(Ui.TEXT, 44f, true)
    private val face = Ui.paint(Ui.SURFACE)
    private val back = Ui.paint(0xE6101322.toInt())
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = Ui.u(4) }
    private val r = RectF()
    private val gap get() = Ui.u(14)
    private val top get() = if (panel) Ui.u(110) else Ui.u(130)
    private val pad get() = if (panel) Ui.u(24) else Ui.u(56)

    private fun yOf(i: Int): Float { var y = top; for (k in 0 until i) y += rows[k].h + gap; return y }

    private fun refocus() {
        if (rows.isEmpty()) return
        ry.target = yOf(idx); rh.target = rows[idx].h
        val vis = height - top - Ui.u(30); val y = yOf(idx) - top
        if (y + rows[idx].h > scroll.target + vis) scroll.target = y + rows[idx].h - vis
        if (y < scroll.target) scroll.target = y
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) { refocus(); ry.snap(ry.target); rh.snap(rh.target) }

    fun key(code: Int, repeat: Int = 0): Boolean {
        if (rows.isEmpty()) return false
        when (code) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                val d = if (code == KeyEvent.KEYCODE_DPAD_UP) -1 else 1
                var n = idx + d; while (n in rows.indices && !rows[n].focusable) n += d
                if (n in rows.indices) { idx = n; refocus() }
            }
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                val d = (if (code == KeyEvent.KEYCODE_DPAD_RIGHT) 1 else -1)
                if (!rows[idx].adjust(d, repeat > 8)) return false
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> rows[idx].press()
            else -> return false
        }
        invalidate(); return true
    }

    override fun render(c: Canvas, dt: Float) {
        if (panel) { r.set(0f, 0f, width.toFloat(), height.toFloat()); c.drawRoundRect(r, Ui.u(28), Ui.u(28), back) }
        Txt.draw(c, title(), pad + Ui.u(8), Ui.u(if (panel) 72 else 84), width - pad * 2 - Ui.u(16), head)
        c.save(); c.clipRect(0f, top - Ui.u(10), width.toFloat(), height.toFloat())
        c.translate(0f, -scroll.value)
        var y = top
        for (row in rows) {
            r.set(pad, y, width - pad, y + row.h)
            if (row.focusable) c.drawRoundRect(r, Ui.u(18), Ui.u(18), face)
            row.draw(c, r, lit && rows.getOrNull(idx) === row)
            y += row.h + gap
        }
        if (lit && rows.isNotEmpty()) {
            r.set(pad, ry.value, width - pad, ry.value + rh.value)
            ring.shader = LinearGradient(r.left, 0f, r.right, 0f, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP)
            c.drawRoundRect(r, Ui.u(18), Ui.u(18), ring)
        }
        c.restore()
        Scrollbar.draw(c, width.toFloat(), top, height - Ui.u(24), yOf(rows.size), height.toFloat(), scroll.value, lit)
    }
}
