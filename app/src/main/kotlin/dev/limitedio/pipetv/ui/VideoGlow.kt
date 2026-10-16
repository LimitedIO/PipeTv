package dev.limitedio.pipetv.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import kotlin.math.cos
import kotlin.math.sin

/**
 * The playing video as a living, watery backdrop: a tiny copy of the current frame (grabbed in real time,
 * as often as the device can afford) is stretched over the screen on a rippling mesh, twice, with
 * different motion. It also reports the left / middle / right colours of the picture for the colour clouds.
 */
class VideoGlow {
    private val bmp = Bitmap.createBitmap(48, 27, Bitmap.Config.ARGB_8888)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val shade = Paint()
    private val verts = FloatArray((MW + 1) * (MH + 1) * 2)
    private val px = IntArray(48 * 27)
    private var skip = 1; private var frame = 0; private var avg = 0f
    var ready = false; private set
    val colors = IntArray(3)

    /** Call every drawn frame: grabs a new picture when due and adapts the rate to how fast the device is. */
    fun tick(stage: Stage, playing: Boolean) {
        if (frame++ % (if (playing) skip else 30) != 0) return
        val t0 = System.nanoTime()
        if (!stage.grab(bmp)) return
        avg = avg * .85f + (System.nanoTime() - t0) / 1e6f * .15f
        skip = if (avg > 9f) 4 else if (avg > 5f) 2 else 1
        ready = true; analyse()
    }

    private fun analyse() {
        bmp.getPixels(px, 0, 48, 0, 0, 48, 27)
        for (third in 0..2) {
            var r = 0; var g = 0; var b = 0; var n = 0
            for (y in 0 until 27 step 2) for (x in third * 16 until third * 16 + 16 step 2) { val p = px[y * 48 + x]; r += Color.red(p); g += Color.green(p); b += Color.blue(p); n++ }
            val hsv = FloatArray(3); Color.RGBToHSV(r / n, g / n, b / n, hsv)
            hsv[1] = maxOf(hsv[1], .5f); hsv[2] = maxOf(hsv[2], .72f)
            colors[third] = Color.HSVToColor(hsv)
        }
    }

    private fun layer(c: Canvas, w: Float, h: Float, t: Float, level: Float, k: Int, alpha: Float) {
        val zoom = maxOf(w / 48f, h / 27f) * (1.18f + .1f * k + .04f * sin(t * .22f + k))
        val amp = w * (.010f + .008f * level) * (1 + k * .6f); val dir = if (k == 0) 1f else -1f
        for (j in 0..MH) for (i in 0..MW) {
            val o = (j * (MW + 1) + i) * 2
            val bx = ((i / MW.toFloat() - .5f) * 48f * zoom * dir) + w / 2 + w * .03f * sin(t * .09f + k)
            val by = ((j / MH.toFloat() - .5f) * 27f * zoom) + h / 2 + h * .03f * cos(t * .07f + k)
            verts[o] = bx + amp * sin(t * (1.6f + k * .5f) + j * .62f + i * .23f + k * 2f)
            verts[o + 1] = by + amp * .8f * cos(t * (1.3f + k * .4f) + i * .51f + j * .17f)
        }
        paint.alpha = (255 * alpha).toInt().coerceIn(0, 255)
        c.drawBitmapMesh(bmp, MW, MH, verts, 0, null, 0, paint)
    }

    fun draw(c: Canvas, w: Float, h: Float, t: Float, level: Float, fade: Float) {
        if (!ready) return
        layer(c, w, h, t, level, 0, fade * (.5f + .25f * level))
        layer(c, w, h, t, level, 1, fade * (.22f + .12f * level))
        shade.shader = LinearGradient(0f, 0f, 0f, h, 0x55050610, 0xBB050610.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, shade)
    }

    fun reset() { ready = false }

    private companion object { const val MW = 14; const val MH = 8 }
}
