package dev.limitedio.pipetv.audio

import dev.limitedio.pipetv.data.Prefs
import dev.limitedio.pipetv.media.Session

/** Decides when the calm music plays: only in the foreground, only while no video is playing or opening. */
object Sound {
    var foreground = false

    fun update() { Calm.want(foreground && Prefs.music && !Session.active); if (!Session.active) Level.release() }
}
