package dev.limitedio.pipetv.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader

/** Left rail: icons only while content has focus, glides open with labels when it gets focus. */
class Nav(c: Context) : AnimView(c) {
    val tabs = listOf("Home" to Icons.HOME, "Search" to Icons.SEARCH, "Trending" to Icons.TREND,
        "History" to Icons.HISTORY, "Settings" to Icons.SETTINGS)
    var sel = 0
        set(v) { field = v; pill.to(v.toFloat()) }
    var lit = false
        set(v) { field = v; open.to(if (v) 1f else 0f) }

    private val open = spring(0f, 200f, 26f)
    private val pill = spring(0f, 320f, 25f)
    private val bg = Ui.paint(0xF00B0D18.toInt())
    private val label = Ui.paint(Ui.TEXT, 30f, true)
    private val icon = Ui.paint(Ui.DIM)
    private val brand = Ui.paint(Ui.TEXT, 38f, true)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val r = RectF()

    private val itemH get() = Ui.u(96)
    private val y0 get() = Ui.u(250)

    override fun render(c: Canvas, dt: Float) {
        val pw = Ui.u(104) + open.value.coerceIn(0f, 1f) * Ui.u(226)
        c.drawRect(0f, 0f, pw, height.toFloat(), bg)
        brand.alpha = (255 * open.value).toInt().coerceIn(0, 255)
        c.drawText("PipeTv", Ui.u(40), Ui.u(130), brand)
        val py = y0 + pill.value * itemH
        r.set(Ui.u(20), py, pw - Ui.u(20), py + itemH - Ui.u(14))
        fill.shader = LinearGradient(r.left, 0f, r.right, 0f, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP)
        fill.alpha = if (lit) 255 else 90
        c.drawRoundRect(r, Ui.u(24), Ui.u(24), fill)
        for ((i, t) in tabs.withIndex()) {
            val cy = y0 + i * itemH + (itemH - Ui.u(14)) / 2
            val on = i == sel
            icon.color = if (on) 0xFF0A0B14.toInt() else Ui.DIM
            Icons.draw(c, t.second, Ui.u(62), cy, Ui.u(18), icon)
            label.color = if (on) 0xFF0A0B14.toInt() else Ui.TEXT
            label.alpha = (255 * open.value).toInt().coerceIn(0, 255)
            if (open.value > .05f) c.drawText(t.first, Ui.u(108), cy + Ui.u(10), label)
        }
    }
}
