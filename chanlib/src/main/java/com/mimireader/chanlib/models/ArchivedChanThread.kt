package com.mimireader.chanlib.models

import com.google.gson.annotations.Expose
import java.util.*

class ArchivedChanThread(boardName: String, threadId: Long, posts: List<ArchivedChanPost>, @field:Expose val name: String?, @field:Expose val domain: String?) : ChanThread(boardName, threadId, ArrayList(posts.size)) {

    override fun toString(): String {
        return """ArchivedChanThread{board='$boardName', title='$boardTitle', thread id='$threadId', post count='${posts.size}', archive name='$name', archive domain='$domain'}"""
    }

    init {
        this.posts.addAll(posts)
    }
}