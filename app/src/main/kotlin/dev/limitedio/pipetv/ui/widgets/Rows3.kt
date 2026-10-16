package dev.limitedio.pipetv.ui.widgets

import android.graphics.Canvas
import android.graphics.RectF
import dev.limitedio.pipetv.ui.Ui

/** Stepped slider over a fixed list of options (quality, subtitle track...). */
class StepRow(
    label: () -> String, help: () -> String, private val options: () -> List<String>,
    private val get: () -> Int, private val set: (Int) -> Unit,
) : Row(label, help) {
    override fun draw(c: Canvas, r: RectF, lit: Boolean) {
        frame(c, r)
        val o = options(); val n = o.size; if (n == 0) return
        val x0 = zone.left + Ui.u(12); val x1 = zone.right - Ui.u(170); val y = zone.centerY()
        val cur = get().coerceIn(0, n - 1)
        RowStyle.box.set(x0, y - Ui.u(4), x1, y + Ui.u(4)); c.drawRoundRect(RowStyle.box, Ui.u(4), Ui.u(4), RowStyle.track)
        RowStyle.gradient(x0, x1)
        if (n > 1) RowStyle.box.right = x0 + (x1 - x0) * cur / (n - 1) else RowStyle.box.right = x0
        c.drawRoundRect(RowStyle.box, Ui.u(4), Ui.u(4), RowStyle.fill)
        for (i in 0 until n) c.drawCircle(if (n > 1) x0 + (x1 - x0) * i / (n - 1) else x0, y, Ui.u(if (i == cur) (if (lit) 17 else 13) else 6), if (i <= cur) RowStyle.knob else RowStyle.track)
        valueText(c, o[cur], zone.right, y + Ui.u(10))
    }

    override fun adjust(dir: Int, fast: Boolean): Boolean {
        val n = options().size
        if (n > 0) set((get() + dir).coerceIn(0, n - 1)); return true
    }
}
