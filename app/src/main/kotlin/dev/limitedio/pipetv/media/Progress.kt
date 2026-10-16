package dev.limitedio.pipetv.media

import dev.limitedio.pipetv.Bg
import dev.limitedio.pipetv.data.Store

/** Resume points (saved every 10 s and on every change) and the "sound but no picture" watchdog. */
object Progress {
    private val saver = object : Runnable { override fun run() { save(); Bg.main.postDelayed(this, 10_000) } }

    private var dog: Runnable? = null

    fun schedule() { Bg.main.removeCallbacks(saver); Bg.main.postDelayed(saver, 10_000) }
    fun cancel() = Bg.main.removeCallbacks(saver)

    /** Persists the position (off the main thread: it rewrites a small file). */
    fun save() {
        val it = Session.item ?: return
        if (Session.info == null) return
        val pos = Engine.exo.currentPosition; val dur = Engine.exo.duration
        Bg.io { Store.progress(it.id, pos, dur) }
    }

    /** Sound but no picture after a few seconds means the decoder gave up: step the quality down. */
    fun watchFrames() {
        val t = Session.token; val si = Session.info ?: return
        if (Engine.frame || (si.videoOnlyStreams.isEmpty() && si.videoStreams.isEmpty())) return
        dog?.let(Bg.main::removeCallbacks)
        dog = Runnable { if (t == Session.token && !Engine.frame && Engine.exo.playWhenReady && Engine.exo.playbackState == androidx.media3.common.Player.STATE_READY) Recovery.noPicture() }
        Bg.main.postDelayed(dog!!, 3500)
    }
}
