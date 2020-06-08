package com.emogoth.android.phone.mimi.fourchan.models.archives

import com.mimireader.chanlib.interfaces.ArchiveConverter
import com.mimireader.chanlib.models.ArchivedChanPost
import com.mimireader.chanlib.models.ArchivedChanThread
import java.util.*

class FoolFuukaThreadConverter(val thread: Map<String, FoolFuukaPosts>) : ArchiveConverter {
    override fun toArchivedThread(board: String, threadId: Long, name: String, domain: String): ArchivedChanThread {
        if (thread.size != 1) {
            throw Exception("Thread map should only contain one item")
        }

        val posts = ArrayList<ArchivedChanPost>()
        for (threadEntry in thread) {
            val threadPosts = threadEntry.value
            posts.add(toArchivedPost(threadPosts.op))

            if (threadPosts.posts != null) {
                for (foolfuukaPost in threadPosts.posts) {
                    posts.add(toArchivedPost(foolfuukaPost.value))
                }
            }
        }

        return ArchivedChanThread(board, threadId, posts, name, domain)
    }

    fun toArchivedPost(foolFuukaPost: Post): ArchivedChanPost {
        val post = ArchivedChanPost()
        post.setClosed(foolFuukaPost.locked.toBoolean())
        post.setSticky(foolFuukaPost.sticky.toBoolean())

        post.com = normalizeComment(foolFuukaPost.comment_processed)
        post.time = foolFuukaPost.timestamp
        post.sub = foolFuukaPost.title
        post.no = foolFuukaPost.num.toLong()
        post.name = foolFuukaPost.name
        post.trip = foolFuukaPost.trip
        post.capcode = foolFuukaPost.capcode
        post.trollCountry = foolFuukaPost.poster_country
        post.country = foolFuukaPost.poster_country
        post.countryName = foolFuukaPost.poster_country_name
        post.resto = foolFuukaPost.thread_num.toInt()

        if (foolFuukaPost.media != null) {
            post.mediaLink = foolFuukaPost.media.remote_media_link ?: foolFuukaPost.media.media_link
            post.thumbLink = foolFuukaPost.media.thumb_link
            post.thumbnailHeight = foolFuukaPost.media.preview_h.toInt()
            post.thumbnailWidth = foolFuukaPost.media.preview_w.toInt()
            post.width = foolFuukaPost.media.media_w.toInt()
            post.height = foolFuukaPost.media.media_h.toInt()
            post.fsize = foolFuukaPost.media.media_size.toInt()
            post.filename = foolFuukaPost.media.media_filename

            val lastDotPos = foolFuukaPost.media.media_filename.lastIndexOf('.')

            val tim: String
            val ext: String
            if (lastDotPos >= 0) {
                tim = foolFuukaPost.media.media_filename.substring(0, lastDotPos - 1)
                ext = foolFuukaPost.media.media_filename.substring(lastDotPos)
            } else {
                tim = foolFuukaPost.media.media_filename
                ext = ".???"
            }

            post.tim = tim
            post.ext = ext
        }

        return post
    }

    private fun normalizeComment(com: String): String {
        val quoteLink = "class=\"quotelink\""
        var updatedComment = com.replace("greentext", "quote")
        updatedComment = updatedComment.replace("class=\"backlink\"", quoteLink)
        updatedComment = updatedComment.replace("class=\"backlink op\"", quoteLink)
        updatedComment = updatedComment.replace("\n", "")
        updatedComment = updatedComment.replace("<br />", "<br>")

        var cursor = 0
        var quoteLinkStart = updatedComment.indexOf(quoteLink)
        while (quoteLinkStart >= 0) {
            val quoteLinkEnd = updatedComment.indexOf('>', quoteLinkStart)
            updatedComment = updatedComment.removeRange(quoteLinkStart + quoteLink.length, quoteLinkEnd)
            cursor = quoteLinkEnd
            quoteLinkStart = updatedComment.indexOf(quoteLink, cursor)
        }

        return updatedComment
    }
}

data class FoolFuukaPosts(
        val op: Post,
        val posts: Map<String, Post> = HashMap()
)

data class Board(
        val name: String,
        val shortname: String
)

data class Media(
        val banned: String,
        val exif: Any,
        val media: String,
        val media_filename: String,
        val media_filename_processed: String,
        val media_h: String,
        val media_hash: String,
        val media_id: String,
        val media_link: String?,
        val media_orig: String,
        val media_size: String,
        val media_status: String,
        val media_w: String,
        val preview_h: String,
        val preview_op: String,
        val preview_orig: String,
        val preview_reply: Any,
        val preview_w: String,
        val remote_media_link: String?,
        val safe_media_hash: String,
        val spoiler: String,
        val thumb_link: String?,
        val total: String
)

data class Post(
        val board: Board,
        val capcode: String,
        val comment: String,
        val comment_processed: String,
        val comment_sanitized: String,
        val deleted: String,
        val doc_id: String,
        val email: String,
        val email_processed: String,
        val formatted: Boolean,
        val fourchan_date: String,
        val locked: String,
        val media: Media?,
        val name: String,
        val name_processed: String,
        val nimages: Any,
        val nreplies: Any,
        val num: String,
        val op: String,
        val poster_country: String,
        val poster_country_name: String,
        val poster_country_name_processed: String,
        val poster_hash: String,
        val poster_hash_processed: String,
        val sticky: String,
        val subnum: String,
        val thread_num: String,
        val timestamp: Long,
        val timestamp_expired: String,
        val title: String,
        val title_processed: String,
        val trip: String,
        val trip_processed: String
)