package dev.limitedio.pipetv.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** The calm background: two very slow soft colour clouds, tinted per page. Nothing reacts to audio or video. */
class Ambient {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val palettes = listOf(
        intArrayOf(0x7C5CFF, 0x00E0C6), intArrayOf(0x3D7BFF, 0x7C5CFF), intArrayOf(0xE0359B, 0xFF7A3D),
        intArrayOf(0xFF4F6D, 0x7C5CFF), intArrayOf(0x00C2B8, 0x6B4DFF), intArrayOf(0x5C6B9A, 0x20B8D8))
    private val cur = FloatArray(6)

    fun draw(c: Canvas, w: Float, h: Float, dt: Float, t: Float, tab: Int) {
        val pal = palettes[tab.coerceIn(0, 5)]; val rad = max(w, h) * .6f
        for (i in 0 until 2) {
            val col = pal[i]
            for (k in 0..2) { val to = (col shr (16 - 8 * k) and 255) / 255f; cur[i * 3 + k] += (to - cur[i * 3 + k]) * minOf(1f, dt * 2f) }
            val cx = w * (.5f + .4f * sin(t * .05f + i * 3f)); val cy = h * (.5f + .35f * cos(t * .04f + i * 2f))
            val a = Color.argb(46, (cur[i * 3] * 255).toInt(), (cur[i * 3 + 1] * 255).toInt(), (cur[i * 3 + 2] * 255).toInt())
            paint.shader = RadialGradient(cx, cy, rad, a, a and 0x00FFFFFF, Shader.TileMode.CLAMP)
            c.drawCircle(cx, cy, rad, paint)
        }
    }
}
