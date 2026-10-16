package dev.limitedio.pipetv.ui.detail

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

/** A block of text with a small heading line (a comment, or the video description). */
class Block(val head: String, val tag: String, val body: String, val max: Int = 400) {
    var lines: List<String> = emptyList(); var w = 0
}

/** Reader with a clear focus: the current block is framed, a scrollbar and an "n / total" counter show where you are. */
class TextList(c: Context) : AnimView(c) {
    var blocks: List<Block> = emptyList()
        set(v) { field = v; if (idx >= v.size) idx = 0; invalidate() }
    var message: String? = null
    var lit = false
    var onEnd: () -> Unit = {}
    private var idx = 0
    private val scroll = spring(0f, 170f, 26f)
    private val head = Ui.paint(Ui.ACCENT2, 24f, true); private val tag = Ui.paint(Ui.DIM, 22f)
    private val body = Ui.paint(Ui.TEXT, 28f); private val count = Ui.paint(Ui.DIM, 22f, true)
    private val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = Ui.u(3) }
    private val bar = Paint(Paint.ANTI_ALIAS_FLAG); private val r = RectF()
    private var asked = -1
    private val pad get() = Ui.u(56); private val top get() = Ui.u(24)

    private fun wrap(b: Block, w: Float) {
        if (b.w != w.toInt()) { b.lines = b.body.split('\n').flatMap { Txt.wrap(body, it, w, b.max).ifEmpty { listOf("") } }.take(b.max); b.w = w.toInt() }
    }
    private fun heightOf(b: Block) = (if (b.head.isEmpty()) 0f else Ui.u(58)) + b.lines.size * Ui.u(38) + Ui.u(34)
    private fun topOf(i: Int): Float { var y = top; for (k in 0 until i) y += heightOf(blocks[k]); return y }
    private fun total() = topOf(blocks.size)

    fun reset() { idx = 0; scroll.snap(0f); asked = -1 }

    private fun reveal() {
        val t = topOf(idx); val h = heightOf(blocks[idx]); val view = height - Ui.u(24)
        if (t < scroll.target) scroll.to2((t - Ui.u(12)).coerceAtLeast(0f))
        else if (t + h > scroll.target + view) scroll.to2(if (h > view) t - Ui.u(12) else t + h - view + Ui.u(12))
    }

    fun key(code: Int): Boolean {
        if (blocks.isEmpty()) return false
        val w = width - pad * 2; blocks.forEach { wrap(it, w) }
        val b = blocks[idx]; val view = height - Ui.u(24)
        when (code) {
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (topOf(idx) + heightOf(b) > scroll.target + view + 2f) scroll.to2(scroll.target + Ui.u(180)) else if (idx < blocks.size - 1) { idx++; reveal() }
                if (idx >= blocks.size - 3 && asked != blocks.size) { asked = blocks.size; onEnd() }
            }
            KeyEvent.KEYCODE_DPAD_UP -> if (scroll.target > topOf(idx) + 2f) scroll.to2((scroll.target - Ui.u(180)).coerceAtLeast(topOf(idx) - Ui.u(12))) else if (idx > 0) { idx--; reveal() } else return false
            else -> return false
        }
        invalidate(); return true
    }

    private fun dev.limitedio.pipetv.ui.Spring.to2(v: Float) { target = v; invalidate() }

    override fun render(c: Canvas, dt: Float) {
        val w = width - pad * 2
        message?.let { Txt.draw(c, it, pad, Ui.u(60), w, tag.apply { textSize = Ui.u(28) }) }
        var y = top - scroll.value
        for ((i, b) in blocks.withIndex()) {
            wrap(b, w); val h = heightOf(b); val on = lit && i == idx
            if (y + h > 0 && y < height) {
                if (on) {
                    r.set(pad - Ui.u(20), y + Ui.u(4), pad + w + Ui.u(20), y + h - Ui.u(8))
                    frame.shader = LinearGradient(r.left, 0f, r.right, 0f, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP); c.drawRoundRect(r, Ui.u(18), Ui.u(18), frame)
                }
                var ly = y
                if (b.head.isNotEmpty()) {
                    ly += Ui.u(36); Txt.draw(c, b.head, pad, ly, w, head)
                    if (b.tag.isNotEmpty()) { ly += Ui.u(32); Txt.draw(c, b.tag, pad, ly, w, tag) }
                    ly += Ui.u(10)
                }
                body.alpha = if (lit && !on) 170 else 255
                for (l in b.lines) { ly += Ui.u(38); Txt.draw(c, l, pad, ly, w, body) }
            }
            y += h
        }
        Scrollbar.draw(c, width.toFloat(), Ui.u(12), height - Ui.u(12), total(), height.toFloat(), scroll.value, lit)
        if (lit && blocks.size > 1) Txt.draw(c, "${idx + 1} / ${blocks.size}", pad, height - Ui.u(20), w, count.apply { textAlign = Paint.Align.LEFT }.also { it.alpha = 200 })
    }
}
