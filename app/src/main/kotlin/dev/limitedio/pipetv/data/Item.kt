package dev.limitedio.pipetv.data

/** A video card. Text-layout caches live here so drawing stays allocation free. */
class Item(val id: String, val url: String, val title: String, val sub: String, val dur: Long) {
    val thumb get() = "https://i.ytimg.com/vi/$id/mqdefault.jpg"
    @JvmField var lines: List<String>? = null
    @JvmField var lineW = 0

    fun pack() = listOf(id, url, title.replace('\n', ' '), sub, dur).joinToString(SEP)

    companion object {
        private const val SEP = "\u001f"
        fun unpack(s: String): Item? {
            val p = s.split(SEP)
            return if (p.size == 5) Item(p[0], p[1], p[2], p[3], p[4].toLongOrNull() ?: 0) else null
        }
    }
}
