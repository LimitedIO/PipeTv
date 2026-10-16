package dev.limitedio.pipetv.media

import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.AudioTrackType
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.util.Locale

/** One selectable audio option: a language/dub track, or a bitrate when the video has a single track. */
class AudioOpt(val label: String, val stream: AudioStream)

object AudioOpts {
    private fun best(g: List<AudioStream>) = g.sortedWith(compareByDescending<AudioStream> { it.format == MediaFormat.M4A }
        .thenByDescending { it.averageBitrate }).first()

    /** "English · Original", "Español · Dubbed": the language in the app language, plus the track type. */
    private fun name(s: AudioStream): String {
        val lang = s.audioLocale?.getDisplayLanguage(Locale(S.cur.code))?.takeIf { it.isNotBlank() }?.replaceFirstChar { it.uppercase() }
            ?: s.audioTrackName?.takeIf { it.isNotBlank() } ?: "?"
        return when (s.audioTrackType) {
            AudioTrackType.ORIGINAL -> lang + " · " + S.t(K.AUDIO_ORIGINAL)
            AudioTrackType.DESCRIPTIVE -> "$lang · AD"
            else -> lang
        }
    }

    fun build(info: StreamInfo): List<AudioOpt> {
        val all = info.audioStreams.filter { it.isUrl }
        if (all.isEmpty()) return emptyList()
        val groups = all.groupBy { it.audioTrackId.orEmpty() }
        if (groups.size > 1) {
            return groups.values.map { AudioOpt(name(best(it)), best(it)) }.sortedBy { if (it.label.endsWith(S.t(K.AUDIO_ORIGINAL))) 0 else 1 }
        }
        return listOf(best(all).let { AudioOpt(name(it), it) })
    }
}
