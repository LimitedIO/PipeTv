package dev.limitedio.pipetv.ui

import android.content.Context
import android.graphics.Canvas
import android.view.View
import kotlin.math.abs
import kotlin.math.min

/** Critically-damped-ish spring: gives every motion in the app the same physical feel. */
class Spring(var value: Float = 0f, private val k: Float = 220f, private val d: Float = 28f) {
    var target = value
    private var vel = 0f
    val moving get() = abs(target - value) > 0.002f || abs(vel) > 0.01f

    fun snap(v: Float) { value = v; target = v; vel = 0f }

    fun step(dt: Float) {
        var t = dt
        while (t > 0f) {
            val h = min(t, 0.008f)
            vel += (k * (target - value) - d * vel) * h
            value += vel * h
            t -= h
        }
        if (!moving) { value = target; vel = 0f }
    }
}

/** Base view: advances its springs once per frame and only keeps drawing while something moves. */
abstract class AnimView(c: Context) : View(c) {
    private val springs = ArrayList<Spring>()
    private var last = 0L
    protected open val animating get() = false
    protected open val ticking get() = false

    protected fun spring(v: Float = 0f, k: Float = 220f, d: Float = 28f) = Spring(v, k, d).also { springs.add(it) }

    protected fun Spring.to(t: Float) { target = t; invalidate() }

    final override fun onDraw(canvas: Canvas) {
        val now = System.nanoTime()
        val dt = if (last == 0L) 0.016f else min((now - last) / 1e9f, 0.05f)
        last = now
        var busy = animating
        for (s in springs) { s.step(dt); if (s.moving) busy = true }
        render(canvas, dt)
        if (busy) postInvalidateOnAnimation() else { last = 0L; if (ticking) postInvalidateDelayed(250) }
    }

    abstract fun render(c: Canvas, dt: Float)
}
