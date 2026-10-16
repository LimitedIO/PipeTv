package dev.limitedio.pipetv.audio

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.random.Random

/**
 * Endless, very quiet generative ambient music: slow breathing pads over a gentle A-minor
 * progression, sparse bell notes and a soft echo. Rendered live, so it never repeats.
 */
object Calm {
    private val chords = listOf(
        floatArrayOf(110f, 164.81f, 220f, 261.63f, 329.63f), floatArrayOf(87.31f, 130.81f, 220f, 261.63f, 329.63f),
        floatArrayOf(130.81f, 196f, 246.94f, 329.63f, 392f), floatArrayOf(98f, 146.83f, 246.94f, 293.66f, 329.63f))
    private val scale = floatArrayOf(440f, 523.25f, 587.33f, 659.25f, 783.99f, 880f)
    private const val HOP = 16f
    private val lock = Object()
    @Volatile private var want = false
    private var thread: Thread? = null

    /** Fades the music in or out; the first call starts the generator thread. */
    fun want(on: Boolean) {
        synchronized(lock) { want = on; lock.notifyAll() }
        if (on && thread == null) thread = Thread({ runCatching { run() } }, "calm").apply { isDaemon = true; priority = Thread.MIN_PRIORITY; start() }
    }

    private class Bell(val f: Float, val t0: Float)

    @Suppress("DEPRECATION")
    private fun run() {
        val rate = Synth.RATE
        val track = AudioTrack(AudioManager.STREAM_MUSIC, rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
            rate, AudioTrack.MODE_STREAM)
        val d1 = FloatArray((rate * .43f).toInt()); val d2 = FloatArray((rate * .29f).toInt()); var i1 = 0; var i2 = 0; var lp = 0f
        val phase = Array(2) { FloatArray(10) }; val bells = ArrayList<Bell>(); var nextBell = 2f
        var gain = 0f; var t = 0f; var lpo = 0f; var acc = 0f; val buf = ShortArray(512)
        track.play()
        while (true) {
            if (!want && gain < .0005f) {
                track.pause()
                synchronized(lock) { while (!want) lock.wait() }
                track.play()
            }
            for (k in buf.indices) {
                val dt = 1f / rate; t += dt
                val c = (t / HOP).toInt(); var s = 0f
                for (slot in 0..1) {
                    val idx = c - slot; if (idx < 0) continue
                    val u = (t - idx * HOP) / (HOP * 1.5f)
                    val env = if (u < .2f) u / .2f else if (u > .7f) (1f - u) / .3f else 1f
                    val ch = chords[idx % chords.size]
                    for ((n, f) in ch.withIndex()) {
                        val p = phase[slot]; p[n * 2] = (p[n * 2] + f * dt) % 1f; p[n * 2 + 1] = (p[n * 2 + 1] + f * 1.004f * dt) % 1f
                        s += env * .042f * (Synth.sin01(p[n * 2]) + Synth.sin01(p[n * 2 + 1])) * (1f + .2f * Synth.sin01((t * .09f + n * .17f) % 1f))
                    }
                }
                if (t > nextBell) { bells.add(Bell(scale[Random.nextInt(scale.size)] * (if (Random.nextInt(4) == 0) .5f else 1f), t)); nextBell = t + 4f + Random.nextFloat() * 6f }
                bells.removeAll { t - it.t0 > 7f }
                for (b in bells) s += .085f * Synth.bell(b.f, t - b.t0, 2.4f)
                val e = d1[i1] * .5f + d2[i2] * .3f; lp += (e - lp) * .35f
                d1[i1] = s + lp * .62f; d2[i2] = s + lp * .5f
                i1 = (i1 + 1) % d1.size; i2 = (i2 + 1) % d2.size
                gain += ((if (want) .55f else 0f) - gain) * (dt / 2.5f)
                lpo += ((s + e) * gain - lpo) * .22f
                buf[k] = (Synth.clip(lpo) * 3200).toInt().toShort(); acc += lpo * lpo
            }
            track.write(buf, 0, buf.size)
        }
    }
}
