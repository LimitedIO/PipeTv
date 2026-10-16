package dev.limitedio.pipetv.data

/** Favourite videos, channels and playlists (newest first). Kept in memory, saved on every change. */
object Favs {
    private var list: List<Item>? = null
    private fun all(): List<Item> = list ?: Store.load("favs").also { list = it }

    fun items(): List<Item> = all()
    fun has(id: String) = all().any { it.id == id }

    /** Toggles [item]; returns true if it is now a favourite. */
    fun toggle(item: Item): Boolean {
        val now = !has(item.id)
        list = if (now) listOf(item) + all() else all().filter { it.id != item.id }
        Store.save("favs", list!!)
        return now
    }
}
