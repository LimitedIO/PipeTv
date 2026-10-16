package dev.limitedio.pipetv.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import dev.limitedio.pipetv.data.Item
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** The living background: three slow colour clouds whose colours follow the page, the video and the mood. */
class Ambient {
    private class Cloud { val r = Spring(0.3f, 14f, 8f); val g = Spring(0.3f, 14f, 8f); val b = Spring(0.3f, 14f, 8f) }
    private val clouds = List(3) { Cloud() }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var energy = Spring(1f, 20f, 9f)
    private val palettes = listOf(
        intArrayOf(0x7C5CFF, 0x00E0C6, 0x2E5BFF), intArrayOf(0x3D7BFF, 0xFF4FA3, 0x7C5CFF), intArrayOf(0xFF7A3D, 0xE0359B, 0xFFC23D),
        intArrayOf(0xFFB347, 0x00C2B8, 0x6B4DFF), intArrayOf(0x5C6B9A, 0x20B8D8, 0x7C5CFF))

    /** A vivid version of the picture's average colour (computed once per item). */
    fun tintOf(it: Item?): Int {
        if (it == null) return 0
        if (it.tint == 0) {
            val p = Img.peek(it.thumb) ?: return 0
            val px = Bitmap.createScaledBitmap(p.bmp, 1, 1, true).getPixel(0, 0)
            val hsv = FloatArray(3); Color.colorToHSV(px, hsv); hsv[1] = max(hsv[1], .55f); hsv[2] = max(hsv[2], .75f)
            it.tint = Color.HSVToColor(hsv)
        }
        return it.tint
    }

    /** Picks target colours: the page's palette, or the playing video's colour when something is on. */
    fun mood(tab: Int, tint: Int, playing: Boolean, paused: Boolean, loading: Boolean, voice: Float) {
        val base = palettes[tab.coerceIn(0, 4)]
        for ((i, cl) in clouds.withIndex()) {
            val col = if (tint != 0 && playing) when (i) { 0 -> tint; 1 -> Color.rgb(Color.red(tint) / 2, Color.green(tint) / 2, Color.blue(tint) / 2 + 60); else -> base[1] } else base[i]
            cl.r.target = Color.red(col) / 255f; cl.g.target = Color.green(col) / 255f; cl.b.target = Color.blue(col) / 255f
        }
        energy.target = (if (playing) (if (paused) .55f else 1.2f) else 1f) + voice * 1.4f + (if (loading) .3f else 0f)
    }

    fun draw(c: Canvas, w: Float, h: Float, dt: Float, t: Float) {
        energy.step(dt)
        val rad = max(w, h) * .62f
        for ((i, cl) in clouds.withIndex()) {
            cl.r.step(dt); cl.g.step(dt); cl.b.step(dt)
            val cx = w * (.5f + .42f * sin(t * .06f + i * 2.1f)); val cy = h * (.5f + .38f * cos(t * .045f + i * 1.7f))
            val a = (.2f * energy.value * (.85f + .15f * sin(t * .4f + i))).coerceIn(0f, .6f)
            val col = Color.argb((a * 255).toInt(), (cl.r.value * 255).toInt().coerceIn(0, 255), (cl.g.value * 255).toInt().coerceIn(0, 255), (cl.b.value * 255).toInt().coerceIn(0, 255))
            paint.shader = RadialGradient(cx, cy, rad, col, col and 0x00FFFFFF, Shader.TileMode.CLAMP)
            c.drawCircle(cx, cy, rad, paint)
        }
    }
}
