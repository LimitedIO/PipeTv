package dev.limitedio.pipetv.ui

import android.content.Context
import android.graphics.Canvas
import android.view.KeyEvent
import dev.limitedio.pipetv.data.Item
import kotlin.math.max
import kotlin.math.min

/** A virtualised, spring-scrolled video grid with a gliding focus ring. */
class Grid(c: Context, private val title: String) : AnimView(c) {
    var onPick: (Item) -> Unit = {}
    var busy = true
    var message: String? = null
    var lit = true
    var items: List<Item> = emptyList()
        set(v) { field = v; busy = false; idx = idx.coerceIn(0, max(0, v.size - 1)); appear.snap(0f); appear.to(1f); refocus(true) }
    var cols = 4
        set(v) { field = v; refocus(false) }

    private var idx = 0
    private val scroll = spring(0f, 150f, 24f)
    private val rx = spring(0f, 300f, 27f); private val ry = spring(0f, 300f, 27f)
    private val rw = spring(0f, 300f, 27f); private val pop = spring(0f, 420f, 22f)
    private val appear = spring(0f, 70f, 14f)
    private val head = Ui.paint(Ui.TEXT, 44f, true)
    private val note = Ui.paint(Ui.DIM, 26f)
    private val fx = GridFx()
    init { Img.listen { invalidate() } }

    override val animating get() = busy && items.isEmpty()

    private val pad get() = Ui.u(56)
    private val gap get() = Ui.u(30)
    private val top get() = Ui.u(130)
    private val cw get() = (width - pad * 2 - gap * (cols - 1)) / cols
    private val rowH get() = Cards.height(cw) + gap * .5f

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) = refocus(ow == 0)

    private fun refocus(snap: Boolean) {
        if (width == 0) return
        val row = idx / cols
        val maxScroll = max(0f, top + ((items.size + cols - 1) / cols) * rowH - height + Ui.u(40))
        scroll.target = if (row == 0) 0f else min(maxScroll, top + row * rowH - Ui.u(50))
        rx.target = pad + (idx % cols) * (cw + gap); ry.target = top + row * rowH; rw.target = cw
        if (snap) { rx.snap(rx.target); ry.snap(ry.target); rw.snap(cw); scroll.snap(scroll.target) }
    }

    fun key(code: Int): Boolean {
        val n = items.size
        val to = when (code) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> if (idx % cols < cols - 1 && idx + 1 < n) idx + 1 else return false
            KeyEvent.KEYCODE_DPAD_LEFT -> if (idx % cols > 0) idx - 1 else return false
            KeyEvent.KEYCODE_DPAD_UP -> if (idx >= cols) idx - cols else idx
            KeyEvent.KEYCODE_DPAD_DOWN -> if (idx + cols < n) idx + cols else if (idx / cols < (n - 1) / cols) n - 1 else idx
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { items.getOrNull(idx)?.let(onPick); return true }
            else -> return false
        }
        if (to != idx) { idx = to; refocus(false); pop.snap(0f); pop.to(1f) } else return code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_DOWN
        return true
    }

    override fun render(c: Canvas, dt: Float) {
        val w = cw; val s = scroll.value
        c.drawText(title, pad, Ui.u(84) - s, head)
        if (items.isEmpty()) { fx.skeleton(c, pad, top, w, gap, rowH, cols, busy); message?.let { m -> c.drawText(m, pad, Ui.u(110), note) }; return }
        val h = Cards.height(w)
        for (i in 0 until items.size) {
            val y = top + (i / cols) * rowH - s
            if (y > height || y + h < 0) continue
            val x = pad + (i % cols) * (w + gap)
            val t = if (appear.moving) (appear.value * 1.8f - (i % cols + i / cols) * .15f).coerceIn(0f, 1f) else 1f
            c.save()
            val sc = (if (i == idx && lit) 1f + .05f * pop.value else .9f + .1f * t)
            c.translate(0f, (1 - t) * Ui.u(40)); c.scale(sc, sc, x + w / 2, y + h / 2)
            Cards.draw(c, items[i], x, y, w, i == idx && lit)
            c.restore()
        }
        if (lit) fx.ring(c, rx.value, ry.value - s, rw.value, pop.value)
    }
}
