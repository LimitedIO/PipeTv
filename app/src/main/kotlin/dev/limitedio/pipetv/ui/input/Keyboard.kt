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
import kotlin.math.abs

/** Custom on-screen keyboard: a grid of big equal blocks that uses the whole height. Shift, symbols, languages, mic. */
class Keyboard(c: Context) : AnimView(c) {
    var text = ""; var lit = true; var listening = false
    var onKey: (String) -> Unit = {}
    private var pad = Pad(emptyList(), 1, 1)
    private var cur: Key? = null
    private var lang = 0; private var shift = false; private var sym = false
    private val fx = spring(0f, 400f, 28f); private val fy = spring(0f, 400f, 28f); private val press = spring(0f, 500f, 18f)
    private val face = Ui.paint(Ui.SURFACE); private val glyph = Ui.paint(Ui.TEXT, 40f, true); private val ico = Ui.paint(Ui.TEXT)
    private val query = Ui.paint(Ui.TEXT, 48f, true); private val hint = Ui.paint(Ui.DIM, 42f); private val ring = Paint(Paint.ANTI_ALIAS_FLAG)
    private val r = RectF()
    private val top get() = Ui.u(170); private val side get() = Ui.u(24); private val cw get() = (width - side * 2) / pad.cols
    private val ch get() = (height - top - side) / pad.rows
    private val compact get() = height < Ui.u(420) // only the query bar shows (search results)

    init { build() }

    private fun build() {
        val layouts = Layouts.enabled(); lang = lang.coerceIn(0, layouts.size - 1)
        pad = KeyRows.build(layouts[lang], shift, sym)
        cur = pad.keys.firstOrNull { it.token == cur?.token } ?: pad.keys.firstOrNull { !it.token.startsWith("@") } ?: pad.keys[0]
        snapFocus(); invalidate()
    }

    private fun kx(k: Key) = Ui.mx(side + k.col * cw, k.cs * cw, width.toFloat())
    private fun ky(k: Key) = top + k.row * ch
    private fun snapFocus() { cur?.let { fx.snap(kx(it)); fy.snap(ky(it)) } }
    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) = snapFocus()
    fun refresh() = build()

    /** The neighbouring block in a direction (the nearest one, straight lines preferred), or null at an edge. */
    private fun neighbour(dx: Int, dy: Int): Key? {
        val k = cur ?: return null; val mx = k.col + k.cs / 2f; val my = k.row + k.rs / 2f
        return pad.keys.filter { it !== k && when {
            dx > 0 -> it.col >= k.col + k.cs; dx < 0 -> it.col + it.cs <= k.col
            dy > 0 -> it.row >= k.row + k.rs; else -> it.row + it.rs <= k.row } }
            .minByOrNull { val ox = abs(it.col + it.cs / 2f - mx); val oy = abs(it.row + it.rs / 2f - my); if (dx != 0) ox + 3 * oy else oy + 3 * ox }
    }

    /** Moves focus; returns false when a side edge is hit so the parent can take over. */
    fun key(code: Int): Boolean {
        val (dx, dy) = when (code) {
            KeyEvent.KEYCODE_DPAD_LEFT -> -1 to 0; KeyEvent.KEYCODE_DPAD_RIGHT -> 1 to 0
            KeyEvent.KEYCODE_DPAD_UP -> 0 to -1; KeyEvent.KEYCODE_DPAD_DOWN -> 0 to 1
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { cur?.let(::tap); return true }
            else -> return false
        }
        val n = neighbour(dx, dy) ?: return dx == 0
        cur = n; fx.to(kx(n)); fy.to(ky(n)); return true
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
        r.set(kx(k) + Ui.u(5), ky(k) + Ui.u(5), kx(k) + k.cs * cw - Ui.u(5), ky(k) + k.rs * ch - Ui.u(5))
        if (!on) { face.color = KeyRows.face(k.token); c.drawRoundRect(r, Ui.u(18), Ui.u(18), face) }
        val ink = if (on) 0xFF0A0B14.toInt() else if (k.token == "@mic" && listening) Ui.ACCENT2 else KeyRows.ink(k.token)
        glyph.color = ink; ico.color = ink; glyph.textSize = minOf(ch * .42f, Ui.u(54))
        val icon = when (k.token) { "@mic" -> Icons.MIC; "@del" -> Icons.DEL; "@shift" -> Icons.SHIFT; else -> -1 }
        if (icon >= 0) Icons.draw(c, icon, r.centerX(), r.centerY(), minOf(cw, ch) * .24f, ico)
        else Txt.draw(c, Txt.fit(glyph, k.label, r.width()), r.left, r.centerY() + glyph.textSize * .35f, r.width(), glyph, true)
    }

    override fun render(c: Canvas, dt: Float) {
        r.set(side, Ui.u(24), width - side, Ui.u(140)); c.drawRoundRect(r, Ui.u(24), Ui.u(24), face)
        val p = if (text.isEmpty()) hint else query; val w = r.width() - Ui.u(56)
        Txt.draw(c, Txt.fit(p, if (text.isEmpty()) S.t(K.SEARCH_HINT) else text + "▏", w), r.left + Ui.u(28), r.centerY() + Ui.u(16), w, p)
        if (compact) return else for (k in pad.keys) if (!(lit && cur === k)) label(c, k, false)
        val k = cur ?: return; if (!lit) return
        val s = 1f + .06f * press.value; r.set(fx.value + Ui.u(5), fy.value + Ui.u(5), fx.value + k.cs * cw - Ui.u(5), fy.value + k.rs * ch - Ui.u(5))
        r.inset(-r.width() * (s - 1) / 2, -r.height() * (s - 1) / 2)
        ring.shader = LinearGradient(r.left, r.top, r.right, r.bottom, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP)
        c.drawRoundRect(r, Ui.u(20), Ui.u(20), ring); label(c, k, true)
    }
}
