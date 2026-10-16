package dev.limitedio.pipetv.ui.timeline

import android.content.Context
import android.graphics.Canvas
import android.view.KeyEvent
import dev.limitedio.pipetv.data.Item
import dev.limitedio.pipetv.data.Store
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.ui.AnimView
import dev.limitedio.pipetv.ui.Scrollbar
import dev.limitedio.pipetv.ui.Txt
import dev.limitedio.pipetv.ui.Ui

/** Watch history as a vertical timeline: days, times, resume points and progress bars. */
class TimelineView(c: Context) : AnimView(c) {
    var onPick: (Item) -> Unit = {}
    var onMenu: (Item) -> Unit = {}
    var lit = true
    private var lines: List<Line> = emptyList()
    private var idx = 0
    private val scroll = spring(0f, 160f, 26f)
    private val head = Ui.paint(Ui.TEXT, 44f, true)
    private val dim = Ui.paint(Ui.DIM, 22f); private val axis = Ui.paint(0x33FFFFFF)
    private val paint = TimelinePaint({ width }, Ui.u(158))
    private val pad get() = Ui.u(56); private val rowH get() = Ui.u(158); private val dayH get() = Ui.u(112)
    private val top get() = Ui.u(130)

    private var total = ""

    init { reload() }

    fun focused(): Item? = (lines.getOrNull(idx) as? Line.Watch)?.e?.item

    fun reload() {
        val all = Store.entries(); val mins = all.sumOf { it.pos } / 60_000
        total = S.f(K.TOTAL_WATCHED, "${mins / 60}h ${"%02d".format(java.util.Locale.ROOT, mins % 60)}m")
        lines = Days.group(all)
        idx = lines.indexOfFirst { it is Line.Watch }.coerceAtLeast(0); scroll.snap(0f); invalidate()
    }

    private fun yOf(i: Int): Float { var y = top; for (k in 0 until i) y += if (lines[k] is Line.Day) dayH else rowH; return y }
    private fun x(x: Float, w: Float) = Ui.mx(x, w, width.toFloat())

    fun key(code: Int): Boolean {
        val cur = (lines.getOrNull(idx) as? Line.Watch)?.e
        when (code) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                val d = if (code == KeyEvent.KEYCODE_DPAD_UP) -1 else 1
                var n = idx + d; while (n in lines.indices && lines[n] !is Line.Watch) n += d
                if (n in lines.indices) { idx = n; val y = yOf(n) - (if (lines[n - 1] is Line.Day) dayH else 0f); scroll.to2(y - Ui.u(160)) }
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> if (cur != null) onPick(cur.item)
            KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_INFO -> cur?.let { onMenu(it.item) }
            else -> return false
        }
        invalidate(); return true
    }

    private fun dev.limitedio.pipetv.ui.Spring.to2(v: Float) { target = v.coerceAtLeast(0f); invalidate() }

    override fun render(c: Canvas, dt: Float) {
        Txt.draw(c, S.t(K.HISTORY), pad, Ui.u(84) - scroll.value.coerceAtMost(Ui.u(60)), width - pad * 2, head)
        if (lines.isNotEmpty()) { val tw = dim.measureText(total); Txt.draw(c, total, Ui.mx(width - pad - tw, tw, width.toFloat()), Ui.u(84) - scroll.value.coerceAtMost(Ui.u(60)), tw, dim) }
        if (lines.isEmpty()) { Txt.draw(c, S.t(K.NOTHING_WATCHED), pad, Ui.u(150), width - pad * 2, dim.apply { textSize = Ui.u(28) }); return }
        val ax = pad + Ui.u(150)
        c.drawRect(x(ax - Ui.u(2), Ui.u(4)), 0f, x(ax - Ui.u(2), Ui.u(4)) + Ui.u(4), height.toFloat(), axis)
        var y = top - scroll.value
        for ((i, l) in lines.withIndex()) {
            val h = if (l is Line.Day) dayH else rowH
            if (y + h > 0 && y < height) if (l is Line.Day) paint.day(c, l, y, ax) else paint.entry(c, (l as Line.Watch).e, y, ax, lit && i == idx)
            y += h
        }
        Scrollbar.draw(c, width.toFloat(), top, height - Ui.u(20), yOf(lines.size), height.toFloat(), scroll.value, lit)
    }
}
