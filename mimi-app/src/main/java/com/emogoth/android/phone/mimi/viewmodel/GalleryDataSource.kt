package com.emogoth.android.phone.mimi.viewmodel

import com.emogoth.android.phone.mimi.db.PostTableConnection
import com.mimireader.chanlib.models.ChanPost
import io.reactivex.Single
import java.util.*

class GalleryDataSource: ChanDataSource() {
    fun postsWithImages(boardName: String, threadId: Long, ids: LongArray): Single<List<ChanPost>> {

        return PostTableConnection.watchThread(threadId)
                .first(Collections.emptyList())
                .map { PostTableConnection.convertDbPostsToChanThread(boardName, threadId, it) }
                .flatMap { thread ->
                    val posts = thread.posts.filter {
                        ids.contains(it.no)
                    }
                    Single.just(posts)
                }
    }
}