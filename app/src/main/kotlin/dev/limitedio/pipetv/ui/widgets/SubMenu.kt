package dev.limitedio.pipetv.ui.widgets

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
import dev.limitedio.pipetv.ui.Txt
import dev.limitedio.pipetv.ui.Ui

/**
 * A focused pop-up for one setting: a list to choose from, a multi-select list, or a big slider.
 * While it is open it takes every key, so nothing behind it moves. Back closes it.
 */
class SubMenu(c: Context) : AnimView(c) {
    var open = false; private set
    private var title = ""; private var help = ""
    private var options = listOf<String>()
    private var current: () -> Int = { 0 }
    private var checked: ((Int) -> Boolean)? = null
    private var onPick: (Int) -> Unit = {}
    private var slider: SubSlider? = null
    private var idx = 0
    private val show = spring(0f, 260f, 24f); private val ry = spring(0f, 400f, 30f)
    private val shade = Ui.paint(0xB0050610.toInt()); private val panel = Ui.paint(0xFF141829.toInt())
    private val h1 = Ui.paint(Ui.TEXT, 40f, true); private val dim = Ui.paint(Ui.DIM, 24f)
    private val item = Ui.paint(Ui.TEXT, 30f, true)
    private val mark = Paint(Paint.ANTI_ALIAS_FLAG); private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = Ui.u(4) }
    private val r = RectF()

    private fun begin(t: String, h: String) { title = t; help = h; open = true; show.snap(0f); show.to(1f); visibility = VISIBLE; slider = null; checked = null }

    fun choose(t: String, h: String, opts: List<String>, cur: Int, pick: (Int) -> Unit) {
        if (opts.isEmpty()) return
        begin(t, h); options = opts; idx = cur.coerceIn(0, opts.size - 1); onPick = pick; current = { cur }; ry.snap(idx.toFloat())
    }

    fun multi(t: String, h: String, opts: List<String>, on: (Int) -> Boolean, toggle: (Int) -> Unit) {
        begin(t, h); options = opts; idx = 0; checked = on; onPick = toggle; ry.snap(0f)
    }

    fun slider(t: String, h: String, min: Float, max: Float, step: Float, get: () -> Float, set: (Float) -> Unit, fmt: (Float) -> String) {
        begin(t, h); options = emptyList(); slider = SubSlider(min, max, step, get, set, fmt)
    }

    fun close() { open = false; visibility = GONE }

    fun key(code: Int, repeat: Int): Boolean {
        val s = slider
        when {
            s != null && (code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT) -> {
                s.nudge((if (code == KeyEvent.KEYCODE_DPAD_RIGHT) 1 else -1) * (if (Ui.rtl) -1 else 1), repeat > 8)
            }
            code == KeyEvent.KEYCODE_DPAD_UP && s == null -> { if (idx > 0) idx--; ry.to(idx.toFloat()) }
            code == KeyEvent.KEYCODE_DPAD_DOWN && s == null -> { if (idx < options.size - 1) idx++; ry.to(idx.toFloat()) }
            code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER -> {
                if (s != null) close() else { onPick(idx); if (checked == null) close() }
            }
            code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_ESCAPE -> close()
        }
        invalidate(); return true
    }

    override fun render(c: Canvas, dt: Float) {
        val a = show.value.coerceIn(0f, 1f); if (!open && a < .01f) return
        shade.alpha = (176 * a).toInt(); c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shade)
        val pw = minOf(Ui.u(880), width - Ui.u(80))
        val helpLines = Txt.wrap(dim, help, pw - Ui.u(96), 3)
        val titleLines = Txt.wrap(h1, title, pw - Ui.u(96), 2)
        val headerH = Ui.u(52) + titleLines.size * Ui.u(48) + helpLines.size * Ui.u(34) + Ui.u(28)
        val footerH = Ui.u(82); val rowH = Ui.u(78)
        val available = height - Ui.u(80) - headerH - footerH
        val vis = minOf(options.size, 7, (available / rowH).toInt().coerceAtLeast(1))
        val ph = headerH + (if (slider != null) Ui.u(220) else vis * rowH) + footerH
        val cx = width / 2f; val top = (height - ph) / 2f + (1 - a) * Ui.u(40)
        val left = cx - pw / 2; val bottom = top + ph
        c.save(); c.scale(.97f + .03f * a, .97f + .03f * a, cx, top + ph / 2)
        r.set(left, top, left + pw, bottom); c.drawRoundRect(r, Ui.u(30), Ui.u(30), panel)
        ring.shader = LinearGradient(left, 0f, left + pw, 0f, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP)
        ring.alpha = 90; ring.strokeWidth = Ui.u(2); c.drawRoundRect(r, Ui.u(30), Ui.u(30), ring)
        ring.alpha = 255; ring.strokeWidth = Ui.u(4)
        var textY = top + Ui.u(66)
        for (line in titleLines) { Txt.draw(c, line, left + Ui.u(48), textY, pw - Ui.u(96), h1); textY += Ui.u(48) }
        textY += Ui.u(2)
        for (line in helpLines) { Txt.draw(c, line, left + Ui.u(48), textY, pw - Ui.u(96), dim); textY += Ui.u(34) }
        slider?.draw(c, left + Ui.u(60), top + headerH, pw - Ui.u(120)) ?: drawList(c, left + Ui.u(32), top + headerH, pw - Ui.u(64), rowH, vis)
        c.drawRect(left + Ui.u(32), bottom - footerH + Ui.u(12), left + pw - Ui.u(32), bottom - footerH + Ui.u(14), RowStyle.track)
        Txt.draw(c, Txt.fit(dim, S.t(K.HINT_SUB), pw - Ui.u(80)), left + Ui.u(40), bottom - Ui.u(28), pw - Ui.u(80), dim, true)
        c.restore()
    }

    private fun drawList(c: Canvas, x: Float, y: Float, w: Float, rowH: Float, vis: Int) {
        val first = (idx - vis + 1).coerceAtLeast(0)
        for (i in first until minOf(options.size, first + vis)) {
            val ry0 = y + (i - first) * rowH
            r.set(x, ry0, x + w, ry0 + rowH - Ui.u(8))
            val on = checked?.invoke(i) ?: (i == (if (checked == null) current() else -1))
            val mx = if (Ui.rtl) r.right - Ui.u(40) else r.left + Ui.u(40)
            c.drawCircle(mx, r.centerY(), Ui.u(13), RowStyle.track)
            if (on) { mark.shader = LinearGradient(mx - 13, 0f, mx + 13, 0f, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP); c.drawCircle(mx, r.centerY(), Ui.u(13), mark) }
            Txt.draw(c, Txt.fit(item, options[i], w - Ui.u(130)), if (Ui.rtl) r.left + Ui.u(30) else r.left + Ui.u(84), r.centerY() + Ui.u(11), w - Ui.u(130), item)
            if (i == idx) { ring.shader = LinearGradient(r.left, 0f, r.right, 0f, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP); c.drawRoundRect(r, Ui.u(16), Ui.u(16), ring) }
        }
    }
}
