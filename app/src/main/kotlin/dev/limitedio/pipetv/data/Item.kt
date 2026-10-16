package dev.limitedio.pipetv.data

/** A card in a grid: a video, a channel or a playlist. Text-layout caches live here. */
class Item(
    val id: String, val url: String, val title: String, val sub: String, val dur: Long,
    val kind: Int = VIDEO, val img: String = "",
) {
    val thumb get() = img.ifEmpty { "https://i.ytimg.com/vi/$id/mqdefault.jpg" }
    val isLive get() = dur < 0
    @JvmField var lines: List<String>? = null
    @JvmField var lineW = 0
    /** Watched fraction 0..1, filled in from the timeline when the card is shown. */
    @JvmField var progress = 0f

    fun pack() = listOf(id, url, title.replace('\n', ' '), sub, dur, kind, img).joinToString(SEP)

    companion object {
        const val VIDEO = 0; const val CHANNEL = 1; const val PLAYLIST = 2
        private const val SEP = "\u001f"

        fun unpack(s: String): Item? {
            val p = s.split(SEP)
            if (p.size < 5) return null
            return Item(p[0], p[1], p[2], p[3], p[4].toLongOrNull() ?: 0, p.getOrNull(5)?.toIntOrNull() ?: 0, p.getOrNull(6) ?: "")
        }
    }
}
