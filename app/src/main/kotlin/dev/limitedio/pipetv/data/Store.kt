package dev.limitedio.pipetv.data

import dev.limitedio.pipetv.App
import java.io.File

/** One line of the watch timeline. */
class Entry(val item: Item, val at: Long, val pos: Long, val dur: Long) {
    /** Where playback should resume, or 0 when it was (almost) finished or barely started. */
    val resume get() = if (dur > 0 && pos > 10_000 && pos < dur * 0.95) pos else 0L
    val fraction get() = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
}

/** Plain-text storage: watch history with progress, recent searches and the cached home feed. */
object Store {
    private const val SEP = "\u001e"
    private fun file(name: String) = File(App.ctx.filesDir, "$name.txt")

    fun load(name: String): List<Item> =
        runCatching { file(name).readLines().mapNotNull { Item.unpack(it) } }.getOrDefault(emptyList())

    fun save(name: String, items: List<Item>) {
        runCatching { file(name).writeText(items.joinToString("\n") { it.pack() }) }
    }

    @Synchronized fun entries(): List<Entry> = runCatching {
        file("timeline").readLines().mapNotNull { l ->
            val p = l.split(SEP, limit = 4)
            if (p.size < 4) null else Item.unpack(p[3])?.let { Entry(it, p[0].toLong(), p[1].toLong(), p[2].toLong()) }
        }
    }.getOrDefault(emptyList())

    @Synchronized private fun write(list: List<Entry>) {
        runCatching { file("timeline").writeText(list.take(400).joinToString("\n") { "${it.at}$SEP${it.pos}$SEP${it.dur}$SEP${it.item.pack()}" }) }
    }

    /** Marks [item] as watched now, keeping any saved position. */
    @Synchronized fun push(item: Item) {
        val old = entries(); val prev = old.firstOrNull { it.item.id == item.id }
        write(listOf(Entry(item, System.currentTimeMillis(), prev?.pos ?: 0, prev?.dur ?: item.dur)) + old.filter { it.item.id != item.id })
    }

    @Synchronized fun progress(id: String, pos: Long, dur: Long) {
        write(entries().map { if (it.item.id == id) Entry(it.item, it.at, pos, if (dur > 0) dur else it.dur) else it })
    }

    @Synchronized fun remove(id: String) = write(entries().filter { it.item.id != id })
    fun seen(id: String) = entries().any { it.item.id == id }
    fun clearHistory() { file("timeline").delete() }

    fun recents(): List<String> = runCatching { file("searches").readLines().filter { it.isNotBlank() } }.getOrDefault(emptyList())

    fun addSearch(q: String) {
        runCatching { file("searches").writeText((listOf(q) + recents().filter { !it.equals(q, true) }).take(12).joinToString("\n")) }
    }
}
