package dev.limitedio.pipetv.data

import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.kiosk.KioskInfo
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.Locale

/** Thin facade over NewPipeExtractor: no API key, no account, no Google services. */
object Yt {
    private val svc get() = ServiceList.YouTube
    private val idRegex = Regex("(?:v=|youtu\\.be/|shorts/)([\\w-]{11})")
    private var ready = false

    @Synchronized private fun init() {
        if (ready) return
        val loc = Locale.getDefault()
        NewPipe.init(Http, Localization.fromLocale(loc), ContentCountry(loc.country.ifEmpty { "US" }))
        ready = true
    }

    fun search(query: String): List<Item> {
        init()
        val handler = svc.searchQHFactory.fromQuery(query, listOf("videos"), "")
        return items(SearchInfo.getInfo(svc, handler).relatedItems)
    }

    fun trending(): List<Item> {
        init()
        val url = svc.kioskList.getListLinkHandlerFactoryByType("Trending").fromId("Trending").url
        return items(KioskInfo.getInfo(svc, url).relatedItems)
    }

    fun stream(url: String): StreamInfo { init(); return StreamInfo.getInfo(svc, url) }

    fun related(info: StreamInfo) = items(info.relatedItems)

    fun items(list: List<InfoItem>): List<Item> = list.filterIsInstance<StreamInfoItem>().mapNotNull {
        val id = idRegex.find(it.url)?.groupValues?.get(1) ?: return@mapNotNull null
        val views = if (it.viewCount >= 0) " · ${count(it.viewCount)} views" else ""
        Item(id, it.url, it.name, it.uploaderName.orEmpty() + views, it.duration)
    }

    private fun count(n: Long) = when {
        n >= 1_000_000_000 -> "%.1fB".format(n / 1e9)
        n >= 1_000_000 -> "%.1fM".format(n / 1e6)
        n >= 1_000 -> "%.0fK".format(n / 1e3)
        else -> "$n"
    }
}
