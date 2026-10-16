package dev.limitedio.pipetv.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.KeyEvent
import dev.limitedio.pipetv.data.Prefs
import dev.limitedio.pipetv.data.Store
import dev.limitedio.pipetv.update.Updater

/** Settings list: Left/Right changes a value, OK triggers an action. */
class SettingsView(c: Context) : AnimView(c) {
    private class Row(val name: String, val value: () -> String, val act: (Int) -> Unit)

    private val rows = listOf(
        Row("Max quality", { Prefs.qualityLabel() }) { d -> Prefs.quality = Prefs.qualities[cycle(Prefs.qualities.indexOf(Prefs.quality), Prefs.qualities.size, d)] },
        Row("Autoplay next", { if (Prefs.autoplay) "On" else "Off" }) { Prefs.autoplay = !Prefs.autoplay },
        Row("Playback speed", { "${Prefs.speed}x" }) { d -> Prefs.speed = Prefs.speeds[cycle(Prefs.speeds.indexOf(Prefs.speed), Prefs.speeds.size, d)] },
        Row("Clear watch history", { "OK" }) { Store.clearHistory() },
        Row("Updates", { Updater.status }) { if (Updater.ready != null) Updater.install() else Updater.check() },
        Row("About", { "PipeTv v${Updater.current} · by LimitedIO · GPL-3.0" }) {},
    )
    private var idx = 0
    private val ry = spring(0f, 380f, 28f)
    private val head = Ui.paint(Ui.TEXT, 44f, true)
    private val name = Ui.paint(Ui.TEXT, 32f, true)
    private val value = Ui.paint(Ui.DIM, 30f)
    private val face = Ui.paint(Ui.SURFACE)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = Ui.u(4) }
    private val r = RectF()

    init { Updater.onChange = { invalidate() } }

    private fun cycle(i: Int, n: Int, d: Int) = ((i + (if (d == 0) 1 else d)) % n + n) % n

    fun key(code: Int): Boolean {
        when (code) {
            KeyEvent.KEYCODE_DPAD_UP -> if (idx > 0) { idx--; ry.to(idx.toFloat()) }
            KeyEvent.KEYCODE_DPAD_DOWN -> if (idx < rows.size - 1) { idx++; ry.to(idx.toFloat()) }
            KeyEvent.KEYCODE_DPAD_LEFT -> if (rows[idx].name in listOf("Max quality", "Playback speed")) rows[idx].act(-1) else return false
            KeyEvent.KEYCODE_DPAD_RIGHT -> rows[idx].act(1)
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> rows[idx].act(0)
            else -> return false
        }
        invalidate(); return true
    }

    override fun render(c: Canvas, dt: Float) {
        c.drawText("Settings", Ui.u(56), Ui.u(84), head)
        val w = width - Ui.u(112); val h = Ui.u(92)
        for ((i, row) in rows.withIndex()) {
            val y = Ui.u(130) + i * (h + Ui.u(18))
            r.set(Ui.u(56), y, Ui.u(56) + w, y + h)
            c.drawRoundRect(r, Ui.u(18), Ui.u(18), face)
            c.drawText(row.name, r.left + Ui.u(32), r.centerY() + Ui.u(11), name)
            val v = Txt.fit(value, row.value(), w * .55f)
            c.drawText(v, r.right - Ui.u(32) - value.measureText(v), r.centerY() + Ui.u(10), value)
        }
        val y = Ui.u(130) + ry.value * (h + Ui.u(18))
        r.set(Ui.u(56), y, Ui.u(56) + w, y + h)
        ring.shader = LinearGradient(r.left, 0f, r.right, 0f, Ui.ACCENT, Ui.ACCENT2, Shader.TileMode.CLAMP)
        c.drawRoundRect(r, Ui.u(18), Ui.u(18), ring)
    }
}
