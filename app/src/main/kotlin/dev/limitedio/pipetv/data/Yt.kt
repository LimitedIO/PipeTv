package dev.limitedio.pipetv.data

import android.util.LruCache
import dev.limitedio.pipetv.i18n.S
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.kiosk.KioskInfo
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.util.Locale

/** Thin facade over NewPipeExtractor: no API key, no account, no Google services. */
object Yt {
    private val svc get() = ServiceList.YouTube
    private val streams = LruCache<String, StreamInfo>(8)
    private var ready = false

    private fun country() = Locale.getDefault().country.ifEmpty { "US" }

    @Synchronized fun init() {
        if (ready) return
        NewPipe.init(Http, Localization(S.cur.code, country()), ContentCountry(country()))
        ready = true
    }

    /** Call after the app language changed so results and recommendations switch language. */
    @Synchronized fun relocalize() {
        if (ready) NewPipe.setupLocalization(Localization(S.cur.code, country()), ContentCountry(country()))
        synchronized(streams) { streams.evictAll() }
    }

    /** [filter] is one of: all, videos, channels, playlists. */
    fun search(query: String, filter: String = "all"): Paged<Item> {
        init()
        val handler = svc.searchQHFactory.fromQuery(query, listOf(filter), "")
        val info = SearchInfo.getInfo(svc, handler)
        return Paged(YtMap.items(info.relatedItems), info.nextPage) { page ->
            val r = SearchInfo.getMoreItems(svc, handler, page)
            YtMap.items(r.items) to r.nextPage
        }
    }

    fun suggestions(query: String): List<String> { init(); return svc.suggestionExtractor.suggestionList(query) }

    fun trending(): List<Item> {
        init()
        val url = svc.kioskList.getListLinkHandlerFactoryByType("Trending").fromId("Trending").url
        return YtMap.items(KioskInfo.getInfo(svc, url).relatedItems)
    }

    fun stream(url: String): StreamInfo {
        init()
        synchronized(streams) { streams.get(url) }?.let { return it }
        return StreamInfo.getInfo(svc, url).also { synchronized(streams) { streams.put(url, it) } }
    }

    fun related(info: StreamInfo) = YtMap.items(info.relatedItems)
}
