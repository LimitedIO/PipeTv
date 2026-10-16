package dev.limitedio.pipetv.ui.detail

import android.content.Context
import android.graphics.Canvas
import android.view.KeyEvent
import dev.limitedio.pipetv.ui.AnimView
import dev.limitedio.pipetv.ui.Txt
import dev.limitedio.pipetv.ui.Ui

/** A block of text with a small heading line (a comment, or the video description). */
class Block(val head: String, val tag: String, val body: String, val max: Int = 400) {
    var lines: List<String> = emptyList(); var w = 0
}

/** Smooth-scrolling text reader. Wraps lazily for the current width and asks for more at the end. */
class TextList(c: Context) : AnimView(c) {
    var blocks: List<Block> = emptyList()
        set(v) { field = v; invalidate() }
    var message: String? = null
    var onEnd: () -> Unit = {}
    private val scroll = spring(0f, 170f, 26f)
    private val head = Ui.paint(Ui.ACCENT2, 24f, true)
    private val tag = Ui.paint(Ui.DIM, 22f)
    private val body = Ui.paint(Ui.TEXT, 28f)
    private var total = 0f
    private var asked = -1
    private val pad get() = Ui.u(56)

    fun reset() { scroll.snap(0f); asked = -1 }

    fun key(code: Int): Boolean {
        val max = (total - height + Ui.u(40)).coerceAtLeast(0f)
        when (code) {
            KeyEvent.KEYCODE_DPAD_UP -> if (scroll.target > 0f) scroll.to2((scroll.target - Ui.u(180)).coerceAtLeast(0f)) else return false
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                scroll.to2((scroll.target + Ui.u(180)).coerceAtMost(max))
                if (scroll.target >= max - Ui.u(700) && asked != blocks.size) { asked = blocks.size; onEnd() }
            }
            else -> return false
        }
        return true
    }

    private fun dev.limitedio.pipetv.ui.Spring.to2(v: Float) { target = v; invalidate() }

    override fun render(c: Canvas, dt: Float) {
        val w = width - pad * 2
        message?.let { Txt.draw(c, it, pad, Ui.u(60), w, tag.apply { textSize = Ui.u(28) }) }
        var y = Ui.u(40) - scroll.value
        for (b in blocks) {
            if (b.w != w.toInt()) { b.lines = b.body.split('\n').flatMap { Txt.wrap(body, it, w, b.max).ifEmpty { listOf("") } }.take(b.max); b.w = w.toInt() }
            val h = (if (b.head.isEmpty()) 0f else Ui.u(44)) + b.lines.size * Ui.u(38) + Ui.u(30)
            if (y + h > 0 && y < height) {
                var ly = y
                if (b.head.isNotEmpty()) {
                    ly += Ui.u(28); Txt.draw(c, b.head, pad, ly, w, head)
                    if (b.tag.isNotEmpty()) Txt.draw(c, b.tag, pad, ly + Ui.u(32), w, tag).also { ly += Ui.u(30) }
                    ly += Ui.u(14)
                }
                for (l in b.lines) { ly += Ui.u(38); Txt.draw(c, l, pad, ly, w, body) }
            }
            y += h
        }
        total = y + scroll.value - Ui.u(40)
    }
}
