package dev.limitedio.pipetv.ui.detail

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.KeyEvent
import dev.limitedio.pipetv.ui.AnimView
import dev.limitedio.pipetv.ui.Scrollbar
import dev.limitedio.pipetv.ui.Spring
import dev.limitedio.pipetv.ui.Txt
import dev.limitedio.pipetv.ui.Ui

/** A block of text with a heading (a comment with its author, or the video description). */
class Block(val head: String, val tag: String, val body: String, val max: Int = 400) {
    var lines: List<String> = emptyList(); var w = 0
}

/** Card-style reader: every block is a card with an avatar, the focused one is centred, framed and bright. */
class TextList(c: Context) : AnimView(c) {
    var blocks: List<Block> = emptyList()
        set(v) { field = v; if (idx >= v.size) idx = 0; invalidate() }
    var message: String? = null
    var lit = false
    var onEnd: () -> Unit = {}
    private var idx = 0; private var asked = -1
    private val scroll = spring(0f, 150f, 24f)
    private val head = Ui.paint(Ui.TEXT, 27f, true); private val tag = Ui.paint(Ui.DIM, 21f)
    private val body = Ui.paint(0xFFE4E6F2.toInt(), 28f); private val count = Ui.paint(Ui.DIM, 22f, true)
    private val initial = Ui.paint(0xFF0A0B14.toInt(), 28f, true)
    private val card = Ui.paint(Ui.SURFACE); private val dot = Paint(Paint.ANTI_ALIAS_FLAG)
    private val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = Ui.u(4) }
    private val r = RectF()
    private val pad get() = Ui.u(56); private val gap get() = Ui.u(18)

    private fun wrap(b: Block, w: Float) {
        if (b.w != w.toInt()) { b.lines = b.body.split('\n').flatMap { Txt.wrap(body, it, w, b.max).ifEmpty { listOf("") } }.take(b.max); b.w = w.toInt() }
    }
    private fun heightOf(b: Block) = Ui.u(30) + (if (b.head.isEmpty()) 0f else Ui.u(76)) + b.lines.size * Ui.u(38)
    private fun topOf(i: Int): Float { var y = Ui.u(20); for (k in 0 until i) y += heightOf(blocks[k]) + gap; return y }
    private fun Spring.go(v: Float) { target = v.coerceAtLeast(0f); invalidate() }

    fun reset() { idx = 0; scroll.snap(0f); asked = -1 }

    /** Centre the focused block (or align its top when it is taller than the view). */
    private fun centre() {
        val h = heightOf(blocks[idx]); val t = topOf(idx); val view = height.toFloat()
        scroll.go(if (h > view - Ui.u(60)) t - Ui.u(30) else t + h / 2 - view / 2)
    }

    fun key(code: Int): Boolean {
        if (blocks.isEmpty()) return false
        val w = width - pad * 2 - Ui.u(40); blocks.forEach { wrap(it, w - Ui.u(56)) }
        val t = topOf(idx); val h = heightOf(blocks[idx]); val view = height.toFloat()
        when (code) {
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (t + h > scroll.target + view - Ui.u(20)) scroll.go(scroll.target + Ui.u(200)) else if (idx < blocks.size - 1) { idx++; centre() }
                if (idx >= blocks.size - 3 && asked != blocks.size) { asked = blocks.size; onEnd() }
            }
            KeyEvent.KEYCODE_DPAD_UP -> if (scroll.target > t + Ui.u(10) - Ui.u(30)) scroll.go(maxOf(scroll.target - Ui.u(200), t - Ui.u(30))) else if (idx > 0) { idx--; centre() } else return false
            else -> return false
        }
        return true
    }

    override fun render(c: Canvas, dt: Float) {
        val cw = width - pad * 2; val tw = cw - Ui.u(56)
        message?.let { Txt.draw(c, it, pad, Ui.u(60), cw, tag.apply { textSize = Ui.u(28) }) }
        var y = Ui.u(20) - scroll.value
        for ((i, b) in blocks.withIndex()) {
            wrap(b, tw); val h = heightOf(b)
            if (y + h > 0 && y < height) item(c, b, pad, y, cw, h, lit && i == idx, lit && i != idx)
            y += h + gap
        }
        Scrollbar.draw(c, width.toFloat(), Ui.u(12), height - Ui.u(12), y + scroll.value, height.toFloat(), scroll.value, lit)
        if (lit && blocks.size > 1) Txt.draw(c, "${idx + 1} / ${blocks.size}", pad, height - Ui.u(14), cw, count)
    }

    private fun item(c: Canvas, b: Block, x: Float, y: Float, w: Float, h: Float, on: Boolean, dim: Boolean) {
        card.alpha = if (dim) 120 else 255; body.alpha = if (dim) 150 else 255; head.alpha = body.alpha; tag.alpha = body.alpha
        r.set(x, y, x + w, y + h); c.drawRoundRect(r, Ui.u(22), Ui.u(22), card)
        if (on) { frame.shader = LinearGradient(x, 0f, x + w, 0f, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP); c.drawRoundRect(r, Ui.u(22), Ui.u(22), frame) }
        val inner = w - Ui.u(56); val ix = x + Ui.u(28); var ly = y + Ui.u(26)
        if (b.head.isNotEmpty()) {
            val ax = if (Ui.rtl) x + w - Ui.u(28) - Ui.u(52) else ix; val tx = if (Ui.rtl) ix else ix + Ui.u(68)
            dot.color = Color.HSVToColor(floatArrayOf(Math.abs(b.head.hashCode()) % 360f, .5f, .95f)); dot.alpha = body.alpha
            c.drawCircle(ax + Ui.u(26), ly + Ui.u(26), Ui.u(26), dot)
            Txt.draw(c, b.head.take(1).uppercase(), ax, ly + Ui.u(37), Ui.u(52), initial, true)
            Txt.draw(c, Txt.fit(head, b.head, inner - Ui.u(68)), tx, ly + Ui.u(22), inner - Ui.u(68), head)
            Txt.draw(c, Txt.fit(tag, b.tag, inner - Ui.u(68)), tx, ly + Ui.u(52), inner - Ui.u(68), tag)
            ly += Ui.u(76)
        }
        for (l in b.lines) { ly += Ui.u(38); Txt.draw(c, l, ix, ly - Ui.u(8), inner, body) }
    }
}
