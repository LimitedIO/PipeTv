package dev.limitedio.pipetv.data

import dev.limitedio.pipetv.App
import java.io.File

/** Plain-text item lists on disk: watch history and the cached home feed. */
object Store {
    private fun file(name: String) = File(App.ctx.filesDir, "$name.txt")

    fun load(name: String): List<Item> =
        runCatching { file(name).readLines().mapNotNull { Item.unpack(it) } }.getOrDefault(emptyList())

    fun save(name: String, items: List<Item>) {
        runCatching { file(name).writeText(items.joinToString("\n") { it.pack() }) }
    }

    fun history() = load("history")

    fun push(item: Item) {
        save("history", (listOf(item) + history().filter { it.id != item.id }).take(150))
    }

    fun seen(id: String) = history().any { it.id == id }
    fun clearHistory() { file("history").delete() }
}
