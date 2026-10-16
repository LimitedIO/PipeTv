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

/**
 * What is playing, what is being opened, up next, audio/subtitle tracks and quality fallback.
 * Opening a video never interrupts the current one: it keeps playing until the new one is ready.
 */
object Session {
    var item: Item? = null; private set
    var pending: Item? = null; private set
    var info: StreamInfo? = null; private set
    var related: List<Item> = emptyList(); private set
    var subs: List<SubtitlesStream> = emptyList(); private set
    var audios: List<AudioOpt> = emptyList(); private set
    var sub = -1; private set
    var audioIdx = 0; private set
    var failed = false; private set
    var onChange: () -> Unit = {}
    var onOpenFailed: () -> Unit = {}
    var onReady: () -> Unit = {}
    private var expand = false
    var token = 0; private set
    private var cap = 2160

    val active get() = item != null || pending != null
    val shown get() = pending ?: item
    val loading get() = pending != null
    val height get() = if (Prefs.quality == 0) minOf(1080, cap) else minOf(Prefs.quality, cap)

    fun changed() = onChange()
    fun noExpand() { expand = false }

    private class Opened(val info: StreamInfo, val related: List<Item>, val resume: Long)

    /** Starts opening [it]. With [expandWhenReady] the player goes fullscreen as soon as the video is ready. */
    fun open(it: Item, expandWhenReady: Boolean = false) {
        pending = it; expand = expandWhenReady; failed = false
        val t = ++token
        changed()
        Bg.io({
            val si = Yt.stream(it.url)
            Opened(si, Yt.related(si), Store.entries().firstOrNull { e -> e.item.id == it.id }?.resume ?: 0L)
        }) { r ->
            if (t != token) return@io
            pending = null
            if (r == null) onOpenFailed() else swap(it, r)
            changed()
        }
    }

    private fun swap(it: Item, o: Opened) {
        Progress.save()
        item = it; info = o.info; related = o.related; cap = 2160; failed = false
        subs = o.info.subtitles.distinctBy { s -> s.languageTag }; audios = AudioOpts.build(o.info); audioIdx = 0
        sub = if (!Prefs.subs) -1 else subs.indexOfFirst { s -> s.languageTag.startsWith(S.cur.code) }.takeIf { i -> i >= 0 }
            ?: subs.indexOfFirst { s -> s.languageTag.startsWith("en") }
        Store.push(it)
        start(o.resume)
        Progress.schedule()
        if (expand) onReady()
    }

    private fun start(pos: Long) {
        val si = info ?: return
        Engine.frame = false
        Engine.exo.setMediaSource(Sources.build(si, height, subs.getOrNull(sub), audios.getOrNull(audioIdx)?.stream), pos)
        Engine.exo.playbackParameters = PlaybackParameters(Prefs.speed)
        Engine.exo.volume = Prefs.volume
        Engine.exo.prepare(); Engine.exo.playWhenReady = true
    }

    /** Re-prepares at the same position, e.g. after the quality, audio or subtitle track changed. */
    fun reload() { if (info != null) start(Engine.exo.currentPosition) }
    fun pickSubs(i: Int) { sub = i; reload(); changed() }
    fun pickAudio(i: Int) { audioIdx = i; reload(); changed() }

    fun error() {
        if (cap > 360 && info != null) { cap = if (height > 720) 720 else 360; start(Engine.exo.currentPosition) }
        else { failed = true; changed() }
    }

    fun ended() { item?.let { Store.progress(it.id, Engine.exo.duration, Engine.exo.duration) }; if (Prefs.autoplay) next() }

    fun next() { related.firstOrNull { !Store.seen(it.id) && it.id != item?.id }?.let { open(it) } }

    fun stop() {
        Progress.save(); token++; item = null; pending = null; info = null; Progress.cancel()
        Engine.exo.stop(); changed()
    }
}
