package dev.limitedio.pipetv.ui

import android.content.Context
import android.graphics.Canvas
import android.view.KeyEvent
import dev.limitedio.pipetv.data.Item
import kotlin.math.max
import kotlin.math.min

/** A virtualised, spring-scrolled card grid with a gliding focus ring. Mirrors itself in RTL languages. */
class Grid(c: Context, private val title: () -> String) : AnimView(c) {
    var onPick: (Item) -> Unit = {}; var onMenu: (Item) -> Unit = {}
    var loading = false; set(v) { field = v; invalidate() } // results (or more of them) are being fetched
    var onMore: () -> Unit = {}; var onRetry: (() -> Unit)? = null // onRetry: shows a Retry button on a failed, empty grid
    var busy = true; var message: String? = null; var lit = true
    /** When true, Up on the first row is not consumed so a parent can take focus. */
    var upExit = false
    var cols = 4
        set(v) { field = v; refocus(false) }
    var items: List<Item>
        get() = list
        set(v) = put(v, true)

    private var list: List<Item> = emptyList()
    private var idx = 0; private var asked = -1
    private val scroll = spring(0f, 150f, 24f); private val pop = spring(0f, 420f, 22f)
    private val rx = spring(0f, 300f, 27f); private val ry = spring(0f, 300f, 27f); private val rw = spring(0f, 300f, 27f)
    private val appear = spring(0f, 70f, 14f); private val head = Ui.paint(Ui.TEXT, 44f, true); private val fx = GridFx()
    override val animating get() = (busy && list.isEmpty()) || loading || dev.limitedio.pipetv.media.Session.pending != null

    private val pad get() = Ui.u(56)
    private val gap get() = Ui.u(30); private val top get() = if (title().isEmpty()) Ui.u(28) else Ui.u(130)
    private val cw get() = (width - pad * 2 - gap * (cols - 1)) / cols
    private val rowH get() = Cards.height(cw) + gap * .5f
    private fun xOf(i: Int) = Ui.mx(pad + (i % cols) * (cw + gap), cw, width.toFloat())

    init { Img.listen { invalidate() } }

    private fun put(v: List<Item>, animate: Boolean) {
        list = v; busy = false; idx = idx.coerceIn(0, max(0, v.size - 1))
        if (animate) { asked = -1; idx = 0; appear.snap(0f); appear.to(1f) }
        refocus(animate)
    }

    fun append(more: List<Item>) = put(list + more, false)
    fun focused(): Item? = list.getOrNull(idx)
    private fun retry() { if (!busy) onRetry?.let { busy = true; message = null; invalidate(); it() } }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) = refocus(ow == 0)

    private fun refocus(snap: Boolean) {
        if (width == 0) return
        val row = idx / cols
        val maxScroll = max(0f, top + ((list.size + cols - 1) / cols) * rowH - height + Ui.u(40))
        scroll.target = if (row == 0) 0f else min(maxScroll, top + row * rowH - Ui.u(50))
        rx.target = xOf(idx); ry.target = top + row * rowH; rw.target = cw
        if (snap) { rx.snap(rx.target); ry.snap(ry.target); rw.snap(cw); scroll.snap(scroll.target) }
        invalidate()
    }

    fun key(code: Int): Boolean {
        val n = list.size
        val to = when (code) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> if (idx % cols < cols - 1 && idx + 1 < n) idx + 1 else return false
            KeyEvent.KEYCODE_DPAD_LEFT -> if (idx % cols > 0) idx - 1 else return false
            KeyEvent.KEYCODE_DPAD_UP -> if (idx >= cols) idx - cols else if (upExit) return false else idx
            KeyEvent.KEYCODE_DPAD_DOWN -> if (idx + cols < n) idx + cols else if (idx / cols < (n - 1) / cols) n - 1 else idx
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { if (list.isEmpty()) retry() else onPick(list[idx]); return true }
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_INFO -> { list.getOrNull(idx)?.let(onMenu); return true }
            else -> return false
        }
        if (to != idx) {
            idx = to; refocus(false); pop.snap(0f); pop.to(1f)
            if (idx >= n - cols * 2 && asked != n) { asked = n; onMore() }
        }
        return code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_DOWN || to != -1
    }

    override fun render(c: Canvas, dt: Float) {
        val w = cw; val s = scroll.value
        if (title().isNotEmpty()) Txt.draw(c, title(), pad, Ui.u(84) - s, width - pad * 2, head)
        if (list.isEmpty()) { fx.empty(c, pad, top, w, gap, rowH, cols, busy, width.toFloat(), message, onRetry != null, lit); if (loading) fx.pill(c, width.toFloat(), height.toFloat()); return }
        val h = Cards.height(w)
        for (i in list.indices) {
            val y = top + (i / cols) * rowH - s
            if (y > height || y + h < 0) continue
            val x = xOf(i)
            val t = if (appear.moving) (appear.value * 1.8f - (i % cols + i / cols) * .15f).coerceIn(0f, 1f) else 1f
            c.save()
            val sc = if (i == idx && lit) 1f + .05f * pop.value else .9f + .1f * t
            c.translate(0f, (1 - t) * Ui.u(40)); c.scale(sc, sc, x + w / 2, y + h / 2)
            Cards.draw(c, list[i], x, y, w, i == idx && lit)
            c.restore()
        }
        if (lit) fx.ring(c, rx.value, ry.value - s, rw.value, pop.value)
        if (loading) fx.pill(c, width.toFloat(), height.toFloat())
        Scrollbar.draw(c, width.toFloat(), top, height - Ui.u(20), top + ((list.size + cols - 1) / cols) * rowH, height.toFloat(), s, lit)
    }
}
