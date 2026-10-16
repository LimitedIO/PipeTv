package dev.limitedio.pipetv.media

import android.text.Html
import dev.limitedio.pipetv.Bg
import dev.limitedio.pipetv.Config
import org.schabi.newpipe.extractor.stream.SubtitlesStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Subtitles are fetched and parsed on their own and drawn by the HUD from the player position,
 * so loading (or failing to load) a track never touches playback.
 */
object Subs {
    class Cue(val start: Long, val end: Long, val text: String)

    /** 0 off, 1 loading, 2 ready, 3 failed. */
    var state = 0; private set
    private var cues: List<Cue> = emptyList()
    private var token = 0

    fun select(s: SubtitlesStream?) {
        val t = ++token; cues = emptyList()
        if (s == null) { state = 0; Session.changed(); return }
        state = 1; Session.changed()
        Bg.io({ parse(download(s.content)) }) { r ->
            if (t != token) return@io
            cues = r ?: emptyList(); state = if (r.isNullOrEmpty()) 3 else 2; Session.changed()
        }
    }

    /** The text to show at [pos] ms (empty if none). */
    fun at(pos: Long): String {
        val c = cues; if (c.isEmpty()) return ""
        var lo = 0; var hi = c.size - 1; var best = -1
        while (lo <= hi) { val m = (lo + hi) / 2; if (c[m].start <= pos) { best = m; lo = m + 1 } else hi = m - 1 }
        for (i in best downTo maxOf(0, best - 3)) if (pos < c[i].end) return c[i].text
        return ""
    }

    private fun download(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000; c.readTimeout = 20_000; c.setRequestProperty("User-Agent", Config.UA)
        return c.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
    }

    private fun time(s: String): Long {
        val v = s.trim().replace(',', '.')
        return when {
            v.endsWith("ms") -> v.dropLast(2).toDouble().toLong()
            v.endsWith("s") -> (v.dropLast(1).toDouble() * 1000).toLong()
            v.contains(':') -> v.split(':').fold(0.0) { a, p -> a * 60 + p.toDouble() }.let { (it * 1000).toLong() }
            else -> v.toDouble().toLong()
        }
    }

    private fun plain(s: String) = Html.fromHtml(s.replace(Regex("<br\\s*/?>"), "\n").replace(Regex("<[^>]+>"), ""), Html.FROM_HTML_MODE_LEGACY).toString().trim()

    private fun parse(src: String): List<Cue> {
        val out = ArrayList<Cue>()
        if (src.contains("<tt") || src.trimStart().startsWith("<?xml")) {
            for (m in Regex("<p\\b([^>]*)>(.*?)</p>", RegexOption.DOT_MATCHES_ALL).findAll(src)) {
                val a = m.groupValues[1]
                fun at(n: String) = Regex("\\b$n=\"([^\"]+)\"").find(a)?.groupValues?.get(1)
                val st = at("begin")?.let(::time) ?: at("t")?.let(::time) ?: continue
                val en = at("end")?.let(::time) ?: at("d")?.let { st + time(it) } ?: continue
                plain(m.groupValues[2]).takeIf { it.isNotEmpty() }?.let { out.add(Cue(st, en, it)) }
            }
        } else {
            for (blk in src.replace("\r", "").split(Regex("\n\\s*\n"))) {
                val lines = blk.lines(); val i = lines.indexOfFirst { it.contains("-->") }; if (i < 0) continue
                val (a, b) = lines[i].split("-->").map { it.trim().substringBefore(' ') }
                plain(lines.drop(i + 1).joinToString("\n")).takeIf { it.isNotEmpty() }?.let { out.add(Cue(time(a), time(b), it)) }
            }
        }
        return out.sortedBy { it.start }
    }
}
