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

    fun count(n: Long) = when {
        n >= 1_000_000_000 -> "%.1fB".format(java.util.Locale.ROOT, n / 1e9)
        n >= 1_000_000 -> "%.1fM".format(java.util.Locale.ROOT, n / 1e6)
        n >= 1_000 -> "%.0fK".format(java.util.Locale.ROOT, n / 1e3)
        else -> "$n"
    }

    /** Picks a thumbnail around 170 px: sharp enough for a card, small enough to decode fast. */
    fun img(list: List<Image>): String =
        (list.sortedBy { it.height }.firstOrNull { it.height >= 160 } ?: list.lastOrNull())?.url.orEmpty()

    fun items(list: List<InfoItem>, by: String = ""): List<Item> = list.mapNotNull { map(it, by) }

    private fun map(i: InfoItem, by: String): Item? = when (i) {
        is StreamInfoItem -> idRegex.find(i.url)?.let {
            val views = if (i.viewCount >= 0) S.f(K.VIEWS, count(i.viewCount)) else ""
            val who = i.uploaderName.orEmpty().ifEmpty { by }
            val meta = listOf(views, i.textualUploadDate.orEmpty()).filter { s -> s.isNotEmpty() }.joinToString(" · ")
            val sub = listOf(who, meta).filter { s -> s.isNotEmpty() }.joinToString(" · ")
            Item(it.groupValues[1], i.url, i.name, sub, i.duration, Item.VIDEO, "", who, meta)
        }
        is ChannelInfoItem -> {
            val subs = if (i.subscriberCount >= 0) S.f(K.SUBS, count(i.subscriberCount)) else ""
            val vids = if (i.streamCount >= 0) S.f(K.VIDEOS_N, i.streamCount.toInt()) else ""
            Item("c" + i.url.hashCode(), i.url, i.name, subs, 0, Item.CHANNEL, img(i.thumbnails), subs, vids)
        }
        is PlaylistInfoItem -> {
            val n = S.f(K.VIDEOS_N, i.streamCount.toInt()); val who = i.uploaderName.orEmpty()
            Item("p" + i.url.hashCode(), i.url, i.name, listOf(who, n).filter { s -> s.isNotEmpty() }.joinToString(" · "), 0,
                Item.PLAYLIST, img(i.thumbnails), who, n)
        }
        else -> null
    }
}
