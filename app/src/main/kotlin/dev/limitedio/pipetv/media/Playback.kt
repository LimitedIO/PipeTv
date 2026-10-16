package dev.limitedio.pipetv.media

import org.schabi.newpipe.extractor.stream.StreamType

/** Progress and seeking share the player's current window, including a live DVR window. */
object Playback {
    val live get() = Session.item?.isLive == true || Engine.exo.isCurrentMediaItemLive ||
        Session.info?.streamType == StreamType.LIVE_STREAM || Session.info?.streamType == StreamType.AUDIO_LIVE_STREAM
    val duration get() = Engine.exo.duration.coerceAtLeast(0L)
    val seekable get() = Engine.exo.isCurrentMediaItemSeekable
    val showProgress get() = duration > 0 && (!live || seekable)
    val fraction get() = fraction(Engine.exo.currentPosition)
    val buffered get() = fraction(Engine.exo.bufferedPosition)

    private fun fraction(position: Long) = if (duration > 0) (position.toDouble() / duration).coerceIn(0.0, 1.0).toFloat() else 0f

    fun seekBy(ms: Long): Boolean {
        if (!seekable) return false
        val p = Engine.exo
        val target = (p.currentPosition + ms).coerceAtLeast(0L)
        if (live && duration > 0 && target >= duration) p.seekToDefaultPosition()
        else p.seekTo(if (duration > 0) target.coerceAtMost(duration) else target)
        return true
    }

    fun seekFraction(fraction: Float) {
        if (!seekable || duration <= 0) return
        if (live && fraction >= 1f) Engine.exo.seekToDefaultPosition()
        else Engine.exo.seekTo((duration * fraction.coerceIn(0f, 1f)).toLong())
    }
}
