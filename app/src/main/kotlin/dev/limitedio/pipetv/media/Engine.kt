package dev.limitedio.pipetv.media

import android.view.TextureView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import dev.limitedio.pipetv.App

/** The single ExoPlayer instance, created lazily and tuned for weak TV hardware. */
object Engine {
    /** Set by the UI; attached as soon as the player exists (created off the launch path). */
    var surface: TextureView? = null

    /** True once the current video has drawn its first frame (until then the UI shows the thumbnail). */
    @Volatile var frame = false

    /** Width / height of the playing picture (16:9 until known): the stage letterboxes to it. */
    @Volatile var aspect = 16f / 9f; private set

    val exo: ExoPlayer by lazy {
        val renderers = DefaultRenderersFactory(App.ctx).setEnableDecoderFallback(true)
        val load = DefaultLoadControl.Builder().setBufferDurationsMs(20_000, 50_000, 2_000, 4_000).setPrioritizeTimeOverSizeThresholds(true).build()
        ExoPlayer.Builder(App.ctx, renderers).setLoadControl(load)
            .setAudioAttributes(AudioAttributes.Builder().setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA).build(), true)
            .setHandleAudioBecomingNoisy(true).build().also { it.addListener(Events); surface?.let(it::setVideoTextureView) }
    }

    private object Events : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_ENDED) Session.ended()
            if (state == Player.STATE_READY) { Progress.watchFrames(); Recovery.onSuccess() }
            Session.changed()
        }
        override fun onVideoSizeChanged(size: androidx.media3.common.VideoSize) {
            if (size.width > 0 && size.height > 0) {
                val ar = size.width * size.pixelWidthHeightRatio / size.height
                aspect = if (size.unappliedRotationDegrees % 180 == 90) 1f / ar else ar
                Session.changed()
            }
        }
        override fun onRenderedFirstFrame() { frame = true; Session.changed() }
        override fun onPlayerError(error: PlaybackException) = Recovery.onError()
        override fun onIsPlayingChanged(isPlaying: Boolean) { Progress.save(); if (isPlaying) Recovery.onSuccess(); Session.changed() }
    }
}
