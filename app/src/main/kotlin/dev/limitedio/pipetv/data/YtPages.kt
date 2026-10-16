package dev.limitedio.pipetv.data

import android.text.Html
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo
import org.schabi.newpipe.extractor.comments.CommentsInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.stream.Description

/** Channel, playlist and comment loading. All calls block: run them off the main thread. */
object YtPages {
    private val svc get() = ServiceList.YouTube

    fun plain(d: Description?): String = when {
        d == null -> ""
        d.type == Description.HTML -> Html.fromHtml(d.content, Html.FROM_HTML_MODE_LEGACY).toString().trim()
        else -> d.content.trim()
    }

    fun channel(url: String): Channel {
        Yt.init()
        val info = ChannelInfo.getInfo(svc, url)
        val tab = info.tabs.firstOrNull { it.contentFilters.contains("videos") } ?: info.tabs.first()
        val first = ChannelTabInfo.getInfo(svc, tab)
        val videos = Paged(YtMap.items(first.relatedItems, info.name), first.nextPage) { page ->
            val r = ChannelTabInfo.getMoreItems(svc, tab, page)
            YtMap.items(r.items, info.name) to r.nextPage
        }
        return Channel(info.name, info.subscriberCount, info.description.orEmpty(), YtMap.img(info.avatars), videos)
    }

    fun playlist(url: String): Playlist {
        Yt.init()
        val info = PlaylistInfo.getInfo(svc, url)
        val videos = Paged(YtMap.items(info.relatedItems, info.uploaderName.orEmpty()), info.nextPage) { page ->
            val r = PlaylistInfo.getMoreItems(svc, url, page)
            YtMap.items(r.items, info.uploaderName.orEmpty()) to r.nextPage
        }
        return Playlist(info.name, info.uploaderName.orEmpty(), info.streamCount, YtMap.img(info.thumbnails), videos)
    }

    fun comments(url: String): Comments {
        Yt.init()
        val info = CommentsInfo.getInfo(svc, url)
        val list = Paged(info.relatedItems.map(::comment), info.nextPage) { page ->
            val r = CommentsInfo.getMoreItems(svc, info, page)
            r.items.map(::comment) to r.nextPage
        }
        return Comments(info.commentsCount, info.isCommentsDisabled, list)
    }

    private fun comment(c: org.schabi.newpipe.extractor.comments.CommentsInfoItem) = Comment(
        c.uploaderName.orEmpty(), plain(c.commentText), Ago.of(c.uploadDate, c.textualUploadDate),
        c.textualLikeCount.orEmpty().ifEmpty { if (c.likeCount > 0) c.likeCount.toString() else "" },
        c.isPinned, c.isHeartedByUploader, c.replyCount.coerceAtLeast(0), c.replies,
    )

    /** The replies to one comment ([page] comes from the comment itself). */
    fun replies(url: String, page: org.schabi.newpipe.extractor.Page): Paged<Comment> {
        Yt.init()
        val first = CommentsInfo.getMoreItems(svc, url, page)
        return Paged(first.items.map(::comment), first.nextPage) { p ->
            val r = CommentsInfo.getMoreItems(svc, url, p)
            r.items.map(::comment) to r.nextPage
        }
    }
}
