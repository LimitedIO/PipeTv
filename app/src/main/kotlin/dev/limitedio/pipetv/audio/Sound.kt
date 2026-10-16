package dev.limitedio.pipetv.audio

import dev.limitedio.pipetv.data.Prefs
import dev.limitedio.pipetv.media.Engine
import dev.limitedio.pipetv.media.Session

/** Decides when the calm music plays: only in the foreground and while no video is actually playing. */
object Sound {
    var foreground = false

    /** True only while a video is really playing (not paused, buffering or still opening). */
    fun videoPlaying() = Session.item != null && Engine.exo.isPlaying

    /** Music and effects are allowed whenever no video is actually playing. */
    fun update() { Calm.want(foreground && Prefs.music && !videoPlaying()) }
}
