package dev.limitedio.pipetv.audio

import android.Manifest
import android.content.pm.PackageManager
import android.media.audiofx.Visualizer
import android.os.Build
import dev.limitedio.pipetv.App
import kotlin.math.sin
import kotlin.math.sqrt

/** How loud the sound is right now (0..1): drives the living background. Music is measured as it is generated. */
object Level {
    @Volatile var music = 0f
    @Volatile private var video = 0f
    private var viz: Visualizer? = null

    /** Level to show at time [t] seconds: the video's audio when one plays, otherwise the music. */
    fun now(t: Float): Float = when {
        !Sound.videoPlaying() -> music
        viz != null -> video
        else -> .35f + .25f * sin(t * 2.3f) * sin(t * .7f) // no audio capture permission: a gentle stand-in
    }

    /** Starts measuring the player's audio (needs the microphone permission that voice search asks for). */
    fun attach(session: Int) {
        release()
        val ok = Build.VERSION.SDK_INT < 23 || App.ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!ok || session <= 0) return
        viz = runCatching {
            Visualizer(session).apply {
                captureSize = Visualizer.getCaptureSizeRange()[0]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer, w: ByteArray, rate: Int) {
                        var s = 0f; for (b in w) { val x = ((b.toInt() and 0xFF) - 128) / 128f; s += x * x }
                        video += (minOf(1f, sqrt(s / w.size) * 4f) - video) * .4f
                    }
                    override fun onFftDataCapture(v: Visualizer, f: ByteArray, rate: Int) {}
                }, Visualizer.getMaxCaptureRate() / 2, true, false)
                enabled = true
            }
        }.getOrNull()
    }

    fun release() { runCatching { viz?.release() }; viz = null; video = 0f }
}
