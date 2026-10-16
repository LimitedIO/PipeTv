package dev.limitedio.pipetv.ui.input

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

/** Custom on-screen keyboard: real letter layouts, shift, symbols, language switch and a mic key. */
class Keyboard(c: Context) : AnimView(c) {
    var text = ""
    var lit = true
    var listening = false
    var onKey: (String) -> Unit = {}
    private var rows = listOf<List<Key>>()
    private var lang = 0; private var shift = false; private var sym = false
    private var cr = 1; private var cc = 0
    private val fx = spring(0f, 400f, 28f); private val fy = spring(0f, 400f, 28f); private val press = spring(0f, 500f, 18f)
    private val face = Ui.paint(Ui.SURFACE); private val glyph = Ui.paint(Ui.TEXT, 34f, true); private val ico = Ui.paint(Ui.TEXT)
    private val query = Ui.paint(Ui.TEXT, 40f, true); private val hint = Ui.paint(Ui.DIM, 34f); private val ring = Paint(Paint.ANTI_ALIAS_FLAG)
    private val r = RectF()
    private val unit get() = (width - Ui.u(48)) / 13f
    private val top get() = Ui.u(150)
    private val kh get() = unit * .86f

    init { build() }

    private fun build() {
        val layouts = Layouts.enabled(); lang = lang.coerceIn(0, layouts.size - 1)
        rows = KeyRows.build(layouts[lang], shift, sym)
        cr = cr.coerceIn(0, rows.size - 1); cc = cc.coerceIn(0, rows[cr].size - 1)
        snapFocus(); invalidate()
    }

    private fun kx(k: Key) = Ui.u(24) + k.x * unit
    private fun ky(k: Key) = top + k.row * (kh + Ui.u(4))
    private fun snapFocus() { val k = rows[cr][cc]; fx.snap(kx(k)); fy.snap(ky(k)) }
    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) = snapFocus()

    fun refresh() = build()

    /** Moves focus; returns false when an edge is hit so the parent can take over. */
    fun key(code: Int): Boolean {
        val row = rows[cr]; val k = row[cc]
        when (code) {
            KeyEvent.KEYCODE_DPAD_LEFT -> if (cc > 0) cc-- else return false
            KeyEvent.KEYCODE_DPAD_RIGHT -> if (cc < row.size - 1) cc++ else return false
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                val nr = cr + if (code == KeyEvent.KEYCODE_DPAD_UP) -1 else 1
                if (nr !in rows.indices) return true
                val mid = k.x + k.span / 2
                cc = rows[nr].indices.minByOrNull { Math.abs(rows[nr][it].x + rows[nr][it].span / 2 - mid) } ?: 0; cr = nr
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { tap(k); return true }
            else -> return false
        }
        val n = rows[cr][cc]; fx.to(kx(n)); fy.to(ky(n)); return true
    }

    private fun tap(k: Key) {
        press.snap(1f); press.to(0f)
        when (k.token) {
            "@shift" -> { shift = !shift; build() }
            "@sym" -> { sym = !sym; build() }
            "@lang" -> { lang = (lang + 1) % Layouts.enabled().size; shift = false; sym = false; build() }
            else -> onKey(k.token)
        }
    }

    private fun label(c: Canvas, k: Key, on: Boolean) {
        r.set(kx(k) + Ui.u(4), ky(k) + Ui.u(2), kx(k) + k.span * unit - Ui.u(4), ky(k) + kh - Ui.u(2))
        if (!on) c.drawRoundRect(r, Ui.u(14), Ui.u(14), face)
        val ink = if (on) 0xFF0A0B14.toInt() else if (k.token == "@go") Ui.ACCENT2 else if (k.token == "@mic" && listening) Ui.ACCENT2 else Ui.TEXT
        glyph.color = ink; ico.color = ink
        val icon = when (k.token) { "@mic" -> Icons.MIC; "@del" -> Icons.DEL; "@shift" -> Icons.SHIFT; else -> -1 }
        if (icon >= 0) Icons.draw(c, icon, r.centerX(), r.centerY(), kh * .26f, ico)
        else Txt.draw(c, Txt.fit(glyph, k.label, r.width()), r.left, r.centerY() + Ui.u(12), r.width(), glyph, true)
    }

    override fun render(c: Canvas, dt: Float) {
        r.set(Ui.u(24), Ui.u(24), width - Ui.u(24), Ui.u(120)); c.drawRoundRect(r, Ui.u(20), Ui.u(20), face)
        val q = text.ifEmpty { S.t(K.SEARCH_HINT) }; val p = if (text.isEmpty()) hint else query
        Txt.draw(c, Txt.fit(p, if (text.isEmpty()) q else text + "▏", r.width() - Ui.u(48)), r.left + Ui.u(24), r.centerY() + Ui.u(14), r.width() - Ui.u(48), p)
        for (row in rows) for (k in row) if (!(lit && rows[cr][cc] === k)) label(c, k, false)
        if (!lit) return
        val k = rows[cr][cc]; val s = 1f + .08f * press.value
        r.set(fx.value + Ui.u(4), fy.value + Ui.u(2), fx.value + k.span * unit - Ui.u(4), fy.value + kh - Ui.u(2))
        r.inset(-r.width() * (s - 1) / 2, -r.height() * (s - 1) / 2)
        ring.shader = LinearGradient(r.left, r.top, r.right, r.bottom, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP)
        c.drawRoundRect(r, Ui.u(16), Ui.u(16), ring); label(c, k, true)
    }
}
