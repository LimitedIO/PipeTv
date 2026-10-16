package dev.limitedio.pipetv.data

import dev.limitedio.pipetv.Bg

/** Builds the recommendation page from watch history, falling back to popular topics. */
object Feed {
    private val topics = listOf("trending today", "music", "gaming", "news", "science", "movie trailers")

    fun home(): List<Item> {
        val seeds = Store.entries().take(4).map { it.item }
        val tasks = seeds.map { s -> { Yt.related(Yt.stream(s.url)) } } +
            listOf({ Yt.trending() }) +
            topics.shuffled().take(if (seeds.isEmpty()) 4 else 2).map { t -> { Yt.search(t).items } }
        return mix(Bg.par(tasks), Store.entries().map { it.item.id }.toSet())
    }

    fun trending(): List<Item> =
        runCatching { Yt.trending() }.getOrNull()?.takeIf { it.isNotEmpty() } ?: Yt.search("trending now").items

    /** Round-robin merge so every source shows up near the top, minus duplicates and seen videos. */
    private fun mix(lists: List<List<Item>?>, seen: Set<String>): List<Item> {
        val out = LinkedHashMap<String, Item>()
        val src = lists.filterNotNull()
        for (i in 0 until (src.maxOfOrNull { it.size } ?: 0)) {
            for (l in src) l.getOrNull(i)?.let { if (it.id !in seen) out.putIfAbsent(it.id, it) }
        }
        return out.values.take(60)
    }
}
