package dev.limitedio.pipetv.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.KeyEvent

/** Custom on-screen keyboard, D-pad friendly: 8 columns, spans for the wide keys. */
class Keyboard(c: Context) : AnimView(c) {
    private class Key(val label: String, val row: Int, val x: Int, val span: Int)

    var text = ""
    var lit = true
    var onKey: (String) -> Unit = {}
    private val keys = ArrayList<Key>()
    private var cur = 0
    private val fx = spring(0f, 380f, 28f); private val fy = spring(0f, 380f, 28f)
    private val press = spring(0f, 500f, 18f)
    private val face = Ui.paint(Ui.SURFACE); private val glyph = Ui.paint(Ui.TEXT, 34f, true)
    private val query = Ui.paint(Ui.TEXT, 44f, true); private val hint = Ui.paint(Ui.DIM, 28f)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG)
    private val r = RectF()

    init {
        val rows = listOf("abcdefgh", "ijklmnop", "qrstuvwx", "yz012345", "6789.-'&")
        rows.forEachIndexed { y, s -> s.forEachIndexed { x, ch -> keys.add(Key(ch.toString(), y, x, 1)) } }
        var x = 0
        for ((l, w) in listOf("Space" to 3, "Del" to 2, "Clear" to 1, "Go" to 2)) { keys.add(Key(l, 5, x, w)); x += w }
    }

    private val unit get() = (width - Ui.u(40)) / 8f
    private val top get() = Ui.u(130)
    private fun kx(k: Key) = Ui.u(20) + k.x * unit
    private fun ky(k: Key) = top + k.row * (unit * .82f)

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) { fx.snap(kx(keys[cur])); fy.snap(ky(keys[cur])) }

    /** Moves focus; returns false when the edge is hit so the parent can take over. */
    fun key(code: Int): Boolean {
        val k = keys[cur]
        val next = when (code) {
            KeyEvent.KEYCODE_DPAD_LEFT -> if (cur > 0 && keys[cur - 1].row == k.row) keys[cur - 1] else return false
            KeyEvent.KEYCODE_DPAD_RIGHT -> if (cur < keys.size - 1 && keys[cur + 1].row == k.row) keys[cur + 1] else return false
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                val row = k.row + if (code == KeyEvent.KEYCODE_DPAD_UP) -1 else 1
                val mid = k.x + k.span / 2f
                keys.filter { it.row == row }.minByOrNull { Math.abs(it.x + it.span / 2f - mid) } ?: return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { press.snap(1f); press.to(0f); onKey(k.label); return true }
            else -> return false
        }
        cur = keys.indexOf(next); fx.to(kx(next)); fy.to(ky(next))
        return true
    }

    override fun render(c: Canvas, dt: Float) {
        val q = if (text.isEmpty()) "Search videos" else text
        c.drawText(Txt.fit(if (text.isEmpty()) hint else query, q, width - Ui.u(40)), Ui.u(20), Ui.u(80), if (text.isEmpty()) hint else query)
        for (k in keys) {
            r.set(kx(k) + Ui.u(5), ky(k) + Ui.u(5), kx(k) + k.span * unit - Ui.u(5), ky(k) + unit * .82f - Ui.u(5))
            c.drawRoundRect(r, Ui.u(14), Ui.u(14), face)
            glyph.color = if (k.label == "Go") Ui.ACCENT2 else Ui.TEXT
            c.drawText(k.label, r.centerX() - glyph.measureText(k.label) / 2, r.centerY() + Ui.u(12), glyph)
        }
        if (!lit) return
        val k = keys[cur]; val p = 1f + .06f * press.value
        r.set(fx.value, fy.value, fx.value + k.span * unit, fy.value + unit * .82f)
        r.inset(Ui.u(2), Ui.u(2)); r.inset(-r.width() * (p - 1) / 2, -r.height() * (p - 1) / 2)
        ring.shader = LinearGradient(r.left, r.top, r.right, r.bottom, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP)
        ring.style = Paint.Style.FILL; ring.alpha = 255
        c.drawRoundRect(r, Ui.u(16), Ui.u(16), ring)
        glyph.color = 0xFF0A0B14.toInt()
        c.drawText(k.label, r.centerX() - glyph.measureText(k.label) / 2, r.centerY() + Ui.u(12), glyph)
    }
}
