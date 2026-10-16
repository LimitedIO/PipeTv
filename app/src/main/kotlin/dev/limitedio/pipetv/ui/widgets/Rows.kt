package dev.limitedio.pipetv.ui.widgets

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import dev.limitedio.pipetv.ui.Txt
import dev.limitedio.pipetv.ui.Ui

/** Shared paints for every settings-style row. */
object RowStyle {
    val label = Ui.paint(Ui.TEXT, 32f, true)
    val help = Ui.paint(Ui.DIM, 22f)
    val value = Ui.paint(Ui.TEXT, 30f, true)
    val track = Ui.paint(0x33FFFFFF)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    val knob = Ui.paint(Ui.TEXT)
    val box = RectF()

    fun gradient(x0: Float, x1: Float) { fill.shader = LinearGradient(x0, 0f, x1, 0f, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP) }
}

/** One line of a settings list: a label + help text on one side, an interactive widget on the other. */
abstract class Row(val label: () -> String, val help: () -> String = { "" }) {
    open val focusable get() = true
    open val h get() = Ui.u(if (help().isEmpty()) 100 else 126)
    protected val zone = RectF()

    abstract fun draw(c: Canvas, r: RectF, lit: Boolean)
    open fun adjust(dir: Int, fast: Boolean): Boolean = false
    open fun press() {}

    /** Draws label + help on the reading side and fills [zone] with the widget area. */
    protected fun frame(c: Canvas, r: RectF) {
        val tw = r.width() * .5f - Ui.u(40)
        val tx = if (Ui.rtl) r.right - Ui.u(32) - tw else r.left + Ui.u(32)
        val help = help()
        Txt.draw(c, Txt.fit(RowStyle.label, label(), tw), tx, r.centerY() + if (help.isEmpty()) Ui.u(11) else -Ui.u(8), tw, RowStyle.label)
        if (help.isNotEmpty()) Txt.draw(c, Txt.fit(RowStyle.help, help, tw), tx, r.centerY() + Ui.u(32), tw, RowStyle.help)
        val ww = r.width() * .5f - Ui.u(48)
        if (Ui.rtl) zone.set(r.left + Ui.u(32), r.top, r.left + Ui.u(32) + ww, r.bottom)
        else zone.set(r.right - Ui.u(32) - ww, r.top, r.right - Ui.u(32), r.bottom)
    }

    protected fun valueText(c: Canvas, s: String, right: Float, y: Float) {
        RowStyle.value.textAlign = Paint.Align.RIGHT; c.drawText(s, right, y, RowStyle.value); RowStyle.value.textAlign = Paint.Align.LEFT
    }
}

/** Continuous slider: Left/Right nudge it, hold to move faster. */
class SliderRow(
    label: () -> String, help: () -> String, private val min: Float, private val max: Float, private val step: Float,
    private val get: () -> Float, private val set: (Float) -> Unit, private val fmt: (Float) -> String,
) : Row(label, help) {
    override fun draw(c: Canvas, r: RectF, lit: Boolean) {
        frame(c, r)
        val x0 = zone.left; val x1 = zone.right - Ui.u(150); val y = zone.centerY()
        val f = ((get() - min) / (max - min)).coerceIn(0f, 1f)
        RowStyle.box.set(x0, y - Ui.u(5), x1, y + Ui.u(5)); c.drawRoundRect(RowStyle.box, Ui.u(5), Ui.u(5), RowStyle.track)
        RowStyle.gradient(x0, x1); RowStyle.box.right = x0 + (x1 - x0) * f
        c.drawRoundRect(RowStyle.box, Ui.u(5), Ui.u(5), RowStyle.fill)
        c.drawCircle(RowStyle.box.right, y, Ui.u(if (lit) 17 else 13), RowStyle.knob)
        valueText(c, fmt(get()), zone.right, y + Ui.u(10))
    }

    override fun adjust(dir: Int, fast: Boolean): Boolean {
        set((Math.round((get() + dir * step * (if (fast) 4 else 1)) / step) * step).coerceIn(min, max)); return true
    }
}

/** On/off switch. */
class ToggleRow(label: () -> String, help: () -> String, private val get: () -> Boolean, private val set: (Boolean) -> Unit) : Row(label, help) {
    override fun draw(c: Canvas, r: RectF, lit: Boolean) {
        frame(c, r)
        val on = get(); val w = Ui.u(104); val hh = Ui.u(54); val x1 = zone.right; val y = zone.centerY()
        RowStyle.box.set(x1 - w, y - hh / 2, x1, y + hh / 2)
        if (on) { RowStyle.gradient(RowStyle.box.left, x1); c.drawRoundRect(RowStyle.box, hh / 2, hh / 2, RowStyle.fill) }
        else c.drawRoundRect(RowStyle.box, hh / 2, hh / 2, RowStyle.track)
        c.drawCircle(if (on) x1 - hh / 2 else x1 - w + hh / 2, y, hh / 2 - Ui.u(6), RowStyle.knob)
        valueText(c, S.t(if (on) K.ON else K.OFF), x1 - w - Ui.u(24), y + Ui.u(10))
    }

    override fun adjust(dir: Int, fast: Boolean): Boolean { set(dir > 0); return true }
    override fun press() = set(!get())
}
