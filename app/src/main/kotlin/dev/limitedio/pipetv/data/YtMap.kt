package dev.limitedio.pipetv.data

import dev.limitedio.pipetv.i18n.K
import dev.limitedio.pipetv.i18n.S
import org.schabi.newpipe.extractor.Image
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/** Converts extractor items into our light [Item]s and formats counts. */
object YtMap {
    private val idRegex = Regex("(?:v=|youtu\\.be/|shorts/)([\\w-]{11})")

    private val suffixes = mapOf(
        "ar" to listOf(" ألف", " مليون", " مليار"), "es" to listOf(" mil", " M", " mil M"), "fr" to listOf(" k", " M", " Md"),
        "de" to listOf(" Tsd.", " Mio.", " Mrd."), "ru" to listOf(" тыс.", " млн", " млрд"), "tr" to listOf(" B", " Mn", " Mr"),
        "pt" to listOf(" mil", " mi", " bi"), "hi" to listOf(" हज़ार", " मिलियन", " अरब"))

    private fun num(v: Double) = (if (v >= 100) "%.0f" else "%.1f").format(java.util.Locale.ROOT, v).removeSuffix(".0")

    /** 250 000 -> "250K" in English, "250 ألف" in Arabic, "25万" in Chinese; always Latin digits. */
    fun count(n: Long): String {
        if (S.cur.code == "zh") return if (n >= 100_000_000) num(n / 1e8) + "亿" else if (n >= 10_000) num(n / 1e4) + "万" else "$n"
        val s = suffixes[S.cur.code] ?: listOf("K", "M", "B")
        return when { n >= 1_000_000_000 -> num(n / 1e9) + s[2]; n >= 1_000_000 -> num(n / 1e6) + s[1]; n >= 1_000 -> num(n / 1e3) + s[0]; else -> "$n" }
    }

    /** "24 videos", or just the infinity sign for endless mixes (the extractor reports a negative count). */
    fun countText(n: Long) = if (n < 0) "∞" else S.f(K.VIDEOS_N, n.toInt())

    /** Picks a thumbnail around 170 px: sharp enough for a card, small enough to decode fast. */
    fun img(list: List<Image>): String =
        (list.sortedBy { it.height }.firstOrNull { it.height >= 160 } ?: list.lastOrNull())?.url.orEmpty()

    fun items(list: List<InfoItem>, by: String = ""): List<Item> = list.mapNotNull { map(it, by) }

    private fun map(i: InfoItem, by: String): Item? = when (i) {
        is StreamInfoItem -> idRegex.find(i.url)?.let {
            val views = if (i.viewCount >= 0) S.f(K.VIEWS, count(i.viewCount)) else ""
            val who = i.uploaderName.orEmpty().ifEmpty { by }
            val meta = listOf(views, Ago.of(i.uploadDate, i.textualUploadDate)).filter { s -> s.isNotEmpty() }.joinToString(S.sep)
            val sub = listOf(who, meta).filter { s -> s.isNotEmpty() }.joinToString(S.sep)
            Item(it.groupValues[1], i.url, i.name, sub, i.duration, Item.VIDEO, "", who, meta, i.uploaderUrl.orEmpty())
        }
        is ChannelInfoItem -> {
            val subs = if (i.subscriberCount >= 0) S.f(K.SUBS, count(i.subscriberCount)) else ""
            val vids = if (i.streamCount >= 0) S.f(K.VIDEOS_N, i.streamCount.toInt()) else ""
            Item("c" + i.url.hashCode(), i.url, i.name, subs, 0, Item.CHANNEL, img(i.thumbnails), subs, vids)
        }
        is PlaylistInfoItem -> {
            val n = countText(i.streamCount); val who = i.uploaderName.orEmpty()
            Item("p" + i.url.hashCode(), i.url, i.name, listOf(who, n).filter { s -> s.isNotEmpty() }.joinToString(S.sep), 0,
                Item.PLAYLIST, img(i.thumbnails), who, n)
        }
        else -> null
    }
}
