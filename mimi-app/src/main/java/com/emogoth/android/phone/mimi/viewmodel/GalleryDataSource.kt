package com.emogoth.android.phone.mimi.viewmodel

import com.mimireader.chanlib.models.ChanPost
import io.reactivex.Flowable
import io.reactivex.Single
import java.util.*

class GalleryDataSource : ChanDataSource() {
    fun postsWithImages(boardName: String, threadId: Long, ids: LongArray): Single<List<ChanPost>> {

        return watchThread(boardName, threadId)
                .flatMap { (_, thread) ->
                    val posts = thread.posts.filter {
                        ids.contains(it.no)
                    }
                    Flowable.just(posts)
                }
                .first(Collections.emptyList())
    }
}