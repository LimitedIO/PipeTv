package dev.limitedio.pipetv.media

import androidx.media3.common.PlaybackParameters
import dev.limitedio.pipetv.Bg
import dev.limitedio.pipetv.data.Item
import dev.limitedio.pipetv.data.Prefs
import dev.limitedio.pipetv.data.Store
import dev.limitedio.pipetv.data.Yt
import org.schabi.newpipe.extractor.stream.StreamInfo

/** What is playing now, what is up next, and how quality degrades gracefully on errors. */
object Session {
    var item: Item? = null; private set
    var info: StreamInfo? = null; private set
    var related: List<Item> = emptyList(); private set
    var loading = false; private set
    var failed = false; private set
    var onChange: () -> Unit = {}
    private var token = 0
    private var cap = 2160

    val active get() = item != null
    val height get() = if (Prefs.quality == 0) minOf(1080, cap) else minOf(Prefs.quality, cap)

    fun changed() = onChange()

    fun open(it: Item) {
        item = it; info = null; loading = true; failed = false; cap = 2160; related = emptyList()
        Store.push(it)
        val t = ++token
        Engine.exo.stop()
        changed()
        Bg.io({ Yt.stream(it.url) }) { si ->
            if (t != token) return@io
            if (si == null) { loading = false; failed = true } else { info = si; related = Yt.related(si); start(0) }
            changed()
        }
    }

    private fun start(pos: Long) {
        val si = info ?: return
        loading = false
        Engine.exo.setMediaSource(Sources.build(si, height), pos)
        Engine.exo.playbackParameters = PlaybackParameters(Prefs.speed)
        Engine.exo.prepare(); Engine.exo.playWhenReady = true
    }

    /** Re-prepares at the same position, e.g. after the quality changed. */
    fun reload() { if (info != null) start(Engine.exo.currentPosition) }

    fun error() {
        if (cap > 360 && info != null) { cap = if (height > 720) 720 else 360; start(Engine.exo.currentPosition) }
        else { failed = true; changed() }
    }

    fun ended() { if (Prefs.autoplay) next() }

    fun next() { related.firstOrNull { !Store.seen(it.id) && it.id != item?.id }?.let { open(it) } }

    fun stop() { token++; item = null; info = null; Engine.exo.stop(); changed() }
}
