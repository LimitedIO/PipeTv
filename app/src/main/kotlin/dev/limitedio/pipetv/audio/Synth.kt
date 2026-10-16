package dev.limitedio.pipetv.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Tiny synthesis toolbox shared by the sound effects and the ambient music. */
object Synth {
    const val RATE = 22050
    private const val N = 2048
    private val table = FloatArray(N + 1) { sin(2.0 * PI * it / N).toFloat() }

    /** Fast sine for a phase in 0..1. */
    fun sin01(p: Float): Float {
        val x = p * N; val i = x.toInt(); val f = x - i
        return table[i] + (table[i + 1] - table[i]) * f
    }

    /** Soft bell: sine plus quiet harmonics with a quick attack and exponential decay. Returns the sample at [t] seconds. */
    fun bell(freq: Float, t: Float, decay: Float): Float {
        if (t < 0f) return 0f
        val env = (1f - exp(-t / 0.014f)) * exp(-t / decay)
        val p = freq * t
        return env * (sin01(p - p.toInt()) + 0.28f * exp(-t / (decay * .5f)) * sin01((2 * p) % 1f) + 0.08f * sin01((3 * p) % 1f))
    }

    fun clip(x: Float) = x / (1f + kotlin.math.abs(x))
}
