package dev.limitedio.pipetv.media

import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.AudioTrackType
import org.schabi.newpipe.extractor.stream.StreamInfo

/** One selectable audio option: a language/dub track, or a bitrate when the video has a single track. */
class AudioOpt(val label: String, val stream: AudioStream)

object AudioOpts {
    private fun best(g: List<AudioStream>) = g.sortedWith(compareByDescending<AudioStream> { it.format == MediaFormat.M4A }
        .thenByDescending { it.averageBitrate }).first()

    private fun name(s: AudioStream): String {
        val n = s.audioTrackName?.takeIf { it.isNotBlank() } ?: s.audioLocale?.displayLanguage?.takeIf { it.isNotBlank() } ?: "?"
        return if (s.audioTrackType == AudioTrackType.ORIGINAL) n + " · " + S.t(K.AUDIO_ORIGINAL) else n
    }

    fun build(info: StreamInfo): List<AudioOpt> {
        val all = info.audioStreams.filter { it.isUrl }
        if (all.isEmpty()) return emptyList()
        val groups = all.groupBy { it.audioTrackId.orEmpty() }
        if (groups.size > 1) {
            return groups.values.map { AudioOpt(name(best(it)), best(it)) }.sortedBy { if (it.label.endsWith(S.t(K.AUDIO_ORIGINAL))) 0 else 1 }
        }
        val pool = all.filter { it.format == MediaFormat.M4A }.ifEmpty { all }
        return pool.sortedByDescending { it.averageBitrate }.distinctBy { it.averageBitrate }
            .map { AudioOpt(if (it.averageBitrate > 0) "${it.averageBitrate} kbps" else "Audio", it) }
    }
}
