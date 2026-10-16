package dev.limitedio.pipetv.media

import dev.limitedio.pipetv.Bg
import dev.limitedio.pipetv.data.Yt

/** Playback that keeps trying: lower the quality, refresh the stream links, then retry with a growing pause. */
object Recovery {
    var failed = false; private set
    private var tries = 0
    private val retry = Runnable { refresh() }

    fun onError() {
        tries++
        if (tries == 1 && lowerQuality()) return
        if (tries <= 2) return refresh()
        failed = true; Session.changed()
        Bg.main.removeCallbacks(retry); Bg.main.postDelayed(retry, minOf(30_000L, 3_000L * tries))
    }

    /** Sound but no picture: re-attach the surface and restart at 720p, then fall back to one combined stream. */
    fun noPicture() {
        if (Session.safe >= 2) return onError()
        Session.safe++
        Engine.surface?.let { Engine.exo.clearVideoTextureView(it); Engine.exo.setVideoTextureView(it) }
        Session.start(Engine.exo.currentPosition)
    }

    /** Playing again: clear the error state so the "can't play" notice disappears. */
    fun onSuccess() {
        if (tries == 0 && !failed) return
        tries = 0; failed = false; Bg.main.removeCallbacks(retry); Session.changed()
    }

    fun reset() { tries = 0; failed = false; Bg.main.removeCallbacks(retry) }

    private fun lowerQuality(): Boolean {
        if (Session.cap <= 360 || Session.info == null) return false
        Session.cap = if (Session.height > 720) 720 else 360; Session.start(Engine.exo.currentPosition); return true
    }

    /** Fetches fresh stream links (they expire) and resumes where we were. */
    private fun refresh() {
        val it = Session.item ?: return; val pos = Engine.exo.currentPosition; val t = Session.token
        Yt.invalidate(it.url)
        Bg.io({ Yt.stream(it.url) }) { si ->
            if (t != Session.token) return@io
            if (si == null) onError() else Session.resume(si, pos)
        }
    }
}
