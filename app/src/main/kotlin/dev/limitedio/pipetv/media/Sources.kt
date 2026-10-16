package dev.limitedio.pipetv.media

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.SingleSampleMediaSource
import dev.limitedio.pipetv.Config
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.extractor.stream.SubtitlesStream
import org.schabi.newpipe.extractor.stream.VideoStream

/** Turns extractor streams into an ExoPlayer source, preferring widely supported H.264 + AAC. */
object Sources {
    private val http = DefaultHttpDataSource.Factory()
        .setUserAgent(Config.UA).setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15_000).setReadTimeoutMs(20_000)

    private fun prog(url: String): MediaSource =
        ProgressiveMediaSource.Factory(http).createMediaSource(MediaItem.fromUri(url))

    /** Video heights that can actually be played for [info], best first. */
    fun heights(info: StreamInfo): List<Int> =
        (info.videoOnlyStreams + info.videoStreams).map { it.height }.filter { it > 0 }.distinct().sortedDescending()

    private fun text(s: SubtitlesStream): MediaSource {
        val cfg = MediaItem.SubtitleConfiguration.Builder(Uri.parse(s.content))
            .setMimeType(s.format?.mimeType ?: "text/vtt").setLanguage(s.languageTag)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build()
        return SingleSampleMediaSource.Factory(http).createMediaSource(cfg, C.TIME_UNSET)
    }

    fun build(info: StreamInfo, maxH: Int, sub: SubtitlesStream?, pick: AudioStream?): MediaSource {
        val live = info.streamType == StreamType.LIVE_STREAM || info.streamType == StreamType.AUDIO_LIVE_STREAM
        if (live && info.hlsUrl.isNotEmpty())
            return HlsMediaSource.Factory(http).createMediaSource(MediaItem.fromUri(info.hlsUrl))
        val video = info.videoOnlyStreams.filter { it.height <= maxH && it.isUrl }
            .sortedWith(compareByDescending<VideoStream> { it.height }.thenByDescending { it.format == MediaFormat.MPEG_4 })
        val audio = info.audioStreams.filter { it.isUrl }
            .sortedWith(compareByDescending<AudioStream> { it.format == MediaFormat.M4A }.thenByDescending { it.averageBitrate })
        val best = video.firstOrNull { it.format == MediaFormat.MPEG_4 } ?: video.firstOrNull()
        val main: MediaSource = if (best != null && audio.isNotEmpty()) {
            MergingMediaSource(prog(best.content), prog((pick ?: audio[0]).content))
        } else {
            val combined = info.videoStreams.filter { it.isUrl }.sortedByDescending { it.height }
            prog((combined.firstOrNull { it.height <= maxH } ?: combined.last()).content)
        }
        return if (sub == null) main else MergingMediaSource(main, text(sub))
    }
}
