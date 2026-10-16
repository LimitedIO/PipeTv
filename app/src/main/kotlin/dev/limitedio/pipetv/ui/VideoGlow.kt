package dev.limitedio.pipetv.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import kotlin.math.cos
import kotlin.math.sin

/**
 * The playing video as a soft, slowly drifting backdrop: a tiny copy of the current frame (a few times a
 * second) stretched over the screen, so the upscaling itself blurs it. It pulses with the audio level.
 */
class VideoGlow {
    private val bmp = Bitmap.createBitmap(64, 36, Bitmap.Config.ARGB_8888)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val shade = Paint()
    private val m = Matrix()
    private var ready = false; private var frame = 0

    /** Call every drawn frame; copies a new picture from [stage] every few frames. */
    fun tick(stage: Stage, playing: Boolean) {
        if (frame++ % (if (playing) 4 else 40) == 0 && stage.grab(bmp)) ready = true
    }

    fun draw(c: Canvas, w: Float, h: Float, t: Float, level: Float, alpha: Float) {
        if (!ready) return
        val zoom = maxOf(w / bmp.width, h / bmp.height) * (1.25f + .08f * sin(t * .09f) + .05f * level)
        m.setScale(zoom, zoom)
        m.postTranslate(-(bmp.width * zoom - w) / 2 + w * .04f * sin(t * .07f), -(bmp.height * zoom - h) / 2 + h * .04f * cos(t * .05f))
        paint.alpha = (255 * (alpha * (.42f + .22f * level))).toInt().coerceIn(0, 255)
        c.save(); c.concat(m); c.drawBitmap(bmp, 0f, 0f, paint); c.restore()
        shade.shader = LinearGradient(0f, 0f, 0f, h, 0x66050610, 0xCC050610.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w, h, shade)
    }

    fun reset() { ready = false }
}
