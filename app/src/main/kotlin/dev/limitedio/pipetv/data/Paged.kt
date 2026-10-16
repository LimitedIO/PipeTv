package dev.limitedio.pipetv.data

import org.schabi.newpipe.extractor.Page

/** A list that can grow: [loadMore] fetches the next page from the extractor. */
class Paged<T>(var items: List<T>, var next: Page?, private val more: (Page) -> Pair<List<T>, Page?>) {
    val hasMore get() = next != null

    fun loadMore(): List<T> {
        val n = next ?: return emptyList()
        val (got, page) = more(n)
        next = page; items = items + got
        return got
    }
}

class Channel(val name: String, val subs: Long, val desc: String, val avatar: String, val videos: Paged<Item>)

class Playlist(val name: String, val by: String, val count: Long, val thumb: String, val videos: Paged<Item>)

class Comment(
    val author: String, val text: String, val date: String, val likes: String,
    val pinned: Boolean, val hearted: Boolean, val replies: Int, val page: Page? = null,
)

class Comments(val total: Int, val disabled: Boolean, val list: Paged<Comment>)
