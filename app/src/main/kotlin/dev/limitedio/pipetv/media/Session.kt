package dev.limitedio.pipetv.media

import androidx.media3.common.PlaybackParameters
import dev.limitedio.pipetv.Bg
import dev.limitedio.pipetv.data.Item
import dev.limitedio.pipetv.data.Prefs
import dev.limitedio.pipetv.data.Store
import dev.limitedio.pipetv.data.Yt
import dev.limitedio.pipetv.i18n.S
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.SubtitlesStream

/** What is playing now, what is up next, subtitles, resume points and graceful quality fallback. */
object Session {
    var item: Item? = null; private set
    var info: StreamInfo? = null; private set
    var related: List<Item> = emptyList(); private set
    var subs: List<SubtitlesStream> = emptyList(); private set
    var sub = -1; private set
    var loading = false; private set
    var failed = false; private set
    var onChange: () -> Unit = {}
    private var token = 0
    private var cap = 2160
    private val saver = object : Runnable { override fun run() { saveNow(); Bg.main.postDelayed(this, 10_000) } }

    val active get() = item != null
    val height get() = if (Prefs.quality == 0) minOf(1080, cap) else minOf(Prefs.quality, cap)

    fun changed() = onChange()

    fun open(it: Item) {
        saveNow()
        val resume = Store.entries().firstOrNull { e -> e.item.id == it.id }?.resume ?: 0L
        item = it; info = null; loading = true; failed = false; cap = 2160; related = emptyList(); subs = emptyList(); sub = -1
        Store.push(it)
        val t = ++token
        Engine.exo.stop()
        changed()
        Bg.io({ Yt.stream(it.url) }) { si ->
            if (t != token) return@io
            if (si == null) { loading = false; failed = true } else {
                info = si; related = Yt.related(si); subs = si.subtitles.distinctBy { s -> s.languageTag }
                if (Prefs.subs) sub = subs.indexOfFirst { s -> s.languageTag.startsWith(S.cur.code) }.takeIf { i -> i >= 0 } ?: subs.indexOfFirst { s -> s.languageTag.startsWith("en") }
                start(resume)
                Bg.main.removeCallbacks(saver); Bg.main.postDelayed(saver, 10_000)
            }
            changed()
        }
    }

    private fun start(pos: Long) {
        val si = info ?: return
        loading = false
        Engine.exo.setMediaSource(Sources.build(si, height, subs.getOrNull(sub)), pos)
        Engine.exo.playbackParameters = PlaybackParameters(Prefs.speed)
        Engine.exo.volume = Prefs.volume
        Engine.exo.prepare(); Engine.exo.playWhenReady = true
    }

    /** Re-prepares at the same position, e.g. after the quality or subtitle track changed. */
    fun reload() { if (info != null) start(Engine.exo.currentPosition) }

    fun pickSubs(i: Int) { sub = i; reload(); changed() }

    fun error() {
        if (cap > 360 && info != null) { cap = if (height > 720) 720 else 360; start(Engine.exo.currentPosition) }
        else { failed = true; changed() }
    }

    fun ended() { item?.let { Store.progress(it.id, Engine.exo.duration, Engine.exo.duration) }; if (Prefs.autoplay) next() }

    fun next() { related.firstOrNull { !Store.seen(it.id) && it.id != item?.id }?.let { open(it) } }

    /** Persists the resume point (off the main thread: it rewrites a small file). */
    fun saveNow() {
        val it = item ?: return
        if (info == null) return
        val pos = Engine.exo.currentPosition; val dur = Engine.exo.duration
        Bg.io { Store.progress(it.id, pos, dur) }
    }

    fun stop() {
        saveNow(); token++; item = null; info = null; Bg.main.removeCallbacks(saver)
        Engine.exo.stop(); changed()
    }
}
