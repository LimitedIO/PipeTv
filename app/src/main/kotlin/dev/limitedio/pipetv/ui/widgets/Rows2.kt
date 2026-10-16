package dev.limitedio.pipetv.ui.widgets

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import dev.limitedio.pipetv.ui.Ui

/** Pick one value from a list with ‹ › arrows (wraps around). */
class ChoiceRow(
    label: () -> String, help: () -> String, private val options: () -> List<String>,
    private val get: () -> Int, private val set: (Int) -> Unit,
) : Row(label, help) {
    override fun draw(c: Canvas, r: RectF, lit: Boolean) {
        frame(c, r)
        val o = options(); val s = o.getOrElse(get()) { "" }; val y = zone.centerY() + Ui.u(10)
        RowStyle.value.textAlign = Paint.Align.CENTER
        c.drawText(s, zone.centerX(), y, RowStyle.value)
        RowStyle.value.textAlign = Paint.Align.LEFT
        val half = RowStyle.value.measureText(s) / 2 + Ui.u(44)
        c.drawText("‹", zone.centerX() - half, y, RowStyle.value); c.drawText("›", zone.centerX() + half - Ui.u(14), y, RowStyle.value)
    }

    override fun adjust(dir: Int, fast: Boolean): Boolean {
        val n = options().size
        if (n > 0) set(((get() + dir) % n + n) % n); return true
    }
    override fun press() { adjust(1, false) }
}

/** A button-like row: shows a status/value and runs [act] on OK. */
class ActionRow(label: () -> String, help: () -> String, private val value: () -> String, private val act: () -> Unit) : Row(label, help) {
    override fun draw(c: Canvas, r: RectF, lit: Boolean) {
        frame(c, r)
        val s = dev.limitedio.pipetv.ui.Txt.fit(RowStyle.value, value(), zone.width() - Ui.u(50))
        valueText(c, s, zone.right - Ui.u(44), zone.centerY() + Ui.u(10))
        RowStyle.value.alpha = if (lit) 255 else 150
        c.drawText(if (Ui.rtl) "‹" else "›", zone.right - Ui.u(24), zone.centerY() + Ui.u(11), RowStyle.value); RowStyle.value.alpha = 255
    }
    override fun press() = act()
}

/** Non-focusable section title. */
class HeaderRow(title: () -> String) : Row(title) {
    override val focusable get() = false
    override val h get() = Ui.u(84)
    private val p = Ui.paint(Ui.ACCENT2, 26f, true)
    override fun draw(c: Canvas, r: RectF, lit: Boolean) {
        dev.limitedio.pipetv.ui.Txt.draw(c, label().uppercase(), r.left + Ui.u(32), r.bottom - Ui.u(16), r.width() - Ui.u(64), p)
    }
}

/** Chips you can switch on and off individually (keyboard layouts). */
class MultiRow(
    label: () -> String, help: () -> String, private val names: List<String>,
    private val on: (Int) -> Boolean, private val toggle: (Int) -> Unit,
) : Row(label, help) {
    private var cur = 0
    private val chip = Ui.paint(Ui.TEXT, 24f, true)
    override val h get() = Ui.u(70) + ((names.size + 3) / 4) * Ui.u(62)

    override fun draw(c: Canvas, r: RectF, lit: Boolean) {
        frame(c, r)
        val cw = zone.width() / 4f; val ch = Ui.u(52)
        for (i in names.indices) {
            val x = zone.left + (i % 4) * cw; val y = zone.top + Ui.u(24) + (i / 4) * (ch + Ui.u(10))
            RowStyle.box.set(x + Ui.u(4), y, x + cw - Ui.u(4), y + ch)
            if (on(i)) { RowStyle.gradient(zone.left, zone.right); c.drawRoundRect(RowStyle.box, ch / 2, ch / 2, RowStyle.fill); chip.color = 0xFF0A0B14.toInt() }
            else { c.drawRoundRect(RowStyle.box, ch / 2, ch / 2, RowStyle.track); chip.color = Ui.TEXT }
            dev.limitedio.pipetv.ui.Txt.draw(c, names[i], RowStyle.box.left, RowStyle.box.centerY() + Ui.u(8), RowStyle.box.width(), chip, true)
            if (lit && i == cur) { RowStyle.knob.style = Paint.Style.STROKE; RowStyle.knob.strokeWidth = Ui.u(3); c.drawRoundRect(RowStyle.box, ch / 2, ch / 2, RowStyle.knob); RowStyle.knob.style = Paint.Style.FILL }
        }
    }

    override fun adjust(dir: Int, fast: Boolean): Boolean {
        val n = cur + dir; if (n in names.indices) cur = n; return n in names.indices
    }
    override fun press() = toggle(cur)
}
