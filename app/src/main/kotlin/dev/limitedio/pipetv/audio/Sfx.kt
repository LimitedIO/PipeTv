package dev.limitedio.pipetv.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import dev.limitedio.pipetv.App
import dev.limitedio.pipetv.Bg
import dev.limitedio.pipetv.data.Prefs
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Calm UI sounds, synthesised on first run (no audio assets) and played through a SoundPool. */
object Sfx {
    enum class Fx { TICK, SELECT, BACK, OPEN, ON, OFF }

    private var pool: SoundPool? = null
    private val ids = HashMap<Fx, Int>()
    private var last = 0L

    private fun render(fx: Fx): FloatArray {
        val n = Synth.RATE * 4 / 10
        return FloatArray(n) { i ->
            val t = i / Synth.RATE.toFloat()
            when (fx) {
                Fx.TICK -> .16f * Synth.bell(1175f, t, .05f)
                Fx.SELECT -> .17f * (Synth.bell(523.25f, t, .2f) + .7f * Synth.bell(783.99f, t - .09f, .26f))
                Fx.BACK -> .2f * Synth.bell(261.63f, t, .18f)
                Fx.OPEN -> .15f * (Synth.bell(392f, t, .16f) + Synth.bell(523.25f, t - .08f, .18f) + Synth.bell(783.99f, t - .16f, .26f))
                Fx.ON -> .17f * (Synth.bell(440f, t, .16f) + Synth.bell(659.25f, t - .1f, .22f))
                Fx.OFF -> .17f * (Synth.bell(659.25f, t, .16f) + Synth.bell(440f, t - .1f, .22f))
            }
        }
    }

    private fun wav(samples: FloatArray): ByteArray {
        val b = ByteBuffer.allocate(44 + samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt(36 + samples.size * 2).put("WAVEfmt ".toByteArray()).putInt(16)
        b.putShort(1).putShort(1).putInt(Synth.RATE).putInt(Synth.RATE * 2).putShort(2).putShort(16)
        b.put("data".toByteArray()).putInt(samples.size * 2)
        samples.forEach { b.putShort((Synth.clip(it) * 30000).toInt().toShort()) }
        return b.array()
    }

    /** Builds the sounds in the background so launch is not delayed. */
    fun init(c: Context = App.ctx) = Bg.io {
        val sp = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
        for (fx in Fx.values()) {
            val f = File(c.cacheDir, "sfx2_${fx.name}.wav")
            if (!f.exists()) f.writeBytes(wav(render(fx)))
            ids[fx] = sp.load(f.path, 1)
        }
        pool = sp
    }

    fun play(fx: Fx) {
        if (!Prefs.sfx || Sound.videoPlaying()) return
        val now = System.currentTimeMillis()
        if (fx == Fx.TICK && now - last < 55) return
        last = now
        ids[fx]?.let { pool?.play(it, .35f, .35f, 1, 0, 1f) }
    }
}
