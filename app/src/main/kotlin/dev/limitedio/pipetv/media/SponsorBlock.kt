package dev.limitedio.pipetv.media

import android.net.Uri
import android.os.SystemClock
import dev.limitedio.pipetv.Bg
import dev.limitedio.pipetv.Config
import dev.limitedio.pipetv.data.Prefs
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

/** Quiet, read-only SponsorBlock integration. Failures never interrupt playback. */
object SponsorBlock {
    data class Segment(val start: Long, val end: Long, val color: Int, val duration: Long)
    var segments: List<Segment> = emptyList(); private set
    private var generation = 0
    private var video = ""
    private var lastEnd = -1L; private var lastSkip = 0L
    private val cache = linkedMapOf<String, List<Segment>>()
    private val colors = linkedMapOf(
        "sponsor" to 0xFF00D400.toInt(), "selfpromo" to 0xFFFFFF00.toInt(),
        "interaction" to 0xFFCC00FF.toInt(), "intro" to 0xFF00FFFF.toInt(),
        "outro" to 0xFF3366FF.toInt(), "preview" to 0xFF008FD6.toInt(), "hook" to 0xFFF8A80D.toInt(),
    )
    private val tick = object : Runnable {
        override fun run() {
            if (!Prefs.sponsorBlock || Session.item?.id != video) return
            val p = Engine.exo
            if (p.isPlaying && Playback.seekable && !Playback.live) {
                val pos = p.currentPosition; var end = pos
                for (s in segments) if (matches(s) && s.start <= end && s.end > end) end = s.end
                val now = SystemClock.uptimeMillis()
                if (end > pos && (end != lastEnd || now - lastSkip > 1500)) {
                    lastEnd = end; lastSkip = now; p.seekTo(end.coerceAtMost(Playback.duration))
                }
            }
            Bg.main.postDelayed(this, 200)
        }
    }

    fun matches(s: Segment) = Playback.duration > 0 && (s.duration <= 0 || abs(s.duration - Playback.duration) <= 3000)

    fun reset() {
        generation++; video = ""; segments = emptyList(); lastEnd = -1L; Bg.main.removeCallbacks(tick)
    }

    fun configure() { if (Prefs.sponsorBlock) Session.item?.let { load(it.id) } else { reset(); Session.changed() } }

    fun load(id: String) {
        reset()
        if (!Prefs.sponsorBlock || Playback.live || !id.matches(Regex("[A-Za-z0-9_-]{11}"))) return
        video = id; val request = generation
        fun apply(found: List<Segment>) {
            if (request != generation || Session.item?.id != id || !Prefs.sponsorBlock) return
            segments = found; if (found.isNotEmpty()) Bg.main.post(tick); Session.changed()
        }
        cache[id]?.let { apply(it); return }
        Bg.io({ runCatching { fetch(id) }.getOrDefault(emptyList()) }) { found ->
            val list = found.orEmpty(); cache[id] = list
            if (cache.size > 20) cache.remove(cache.keys.first())
            apply(list)
        }
    }

    private fun fetch(id: String): List<Segment> {
        val uri = Uri.parse("https://sponsor.ajay.app/api/skipSegments").buildUpon()
            .appendQueryParameter("videoID", id).appendQueryParameter("categories", JSONArray(colors.keys.toList()).toString())
            .appendQueryParameter("actionTypes", "[\"skip\"]").build()
        val connection = URL(uri.toString()).openConnection() as HttpURLConnection
        connection.connectTimeout = 8000; connection.readTimeout = 8000
        connection.setRequestProperty("User-Agent", Config.UA)
        return try {
            if (connection.responseCode != 200) emptyList() else {
                val data = JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
                (0 until data.length()).mapNotNull { i ->
                    val entry = data.getJSONObject(i); val range = entry.getJSONArray("segment")
                    val start = range.getDouble(0); val end = range.getDouble(1)
                    val color = colors[entry.optString("category")]
                    if (color == null || entry.optString("actionType", "skip") != "skip" || !start.isFinite() || !end.isFinite() || start < 0 || end <= start) null
                    else Segment((start * 1000).toLong(), (end * 1000).toLong(), color, (entry.optDouble("videoDuration", 0.0) * 1000).toLong())
                }.sortedBy { it.start }
            }
        } finally { connection.disconnect() }
    }
}
