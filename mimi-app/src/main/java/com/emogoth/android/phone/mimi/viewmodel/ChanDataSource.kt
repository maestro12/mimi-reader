package com.emogoth.android.phone.mimi.viewmodel

import android.util.Log
import com.emogoth.android.phone.mimi.async.ProcessThreadTask
import com.emogoth.android.phone.mimi.db.HistoryTableConnection
import com.emogoth.android.phone.mimi.db.PostTableConnection
import com.emogoth.android.phone.mimi.db.UserPostTableConnection
import com.emogoth.android.phone.mimi.db.model.History
import com.emogoth.android.phone.mimi.fourchan.FourChanConnector
import com.emogoth.android.phone.mimi.util.HttpClientFactory
import com.emogoth.android.phone.mimi.util.MimiUtil
import com.emogoth.android.phone.mimi.util.ThreadRegistry
import com.mimireader.chanlib.ChanConnector
import com.mimireader.chanlib.models.ChanThread
import com.mimireader.chanlib.models.ErrorChanThread
import io.reactivex.Flowable
import io.reactivex.Single
import java.util.*

open class ChanDataSource {
    val chanConnector: ChanConnector = FourChanConnector.Builder()
            .setCacheDirectory(MimiUtil.getInstance().cacheDir)
            .setEndpoint(FourChanConnector.getDefaultEndpoint())
            .setPostEndpoint(FourChanConnector.getDefaultPostEndpoint())
            .setClient(HttpClientFactory.getInstance().client)
            .build<ChanConnector>()

    fun watchThread(boardName: String, threadId: Long): Flowable<Pair<History, ChanThread>> {
        return PostTableConnection.watchThread(threadId)
                .flatMap { Flowable.just(PostTableConnection.convertDbPostsToChanThread(boardName, threadId, it)) }
                .flatMap { chanThread ->
                    UserPostTableConnection.fetchPosts()
                            .flatMap { userPosts ->
                                Log.d("ChanDataSource", "user posts size: ${userPosts.size}")
                                ThreadRegistry.getInstance().populateUserPosts(userPosts)
                                val postIds = UserPostTableConnection.postIdList(userPosts)
                                Flowable.just(ProcessThreadTask.processThread(chanThread.posts, postIds, boardName, threadId))
                            }
                }
                .flatMap { chanThread ->
                    HistoryTableConnection.fetchPost(boardName, threadId)
                            .flatMap { Flowable.just(Pair(it, chanThread)) }

                }
    }

    fun fetchThread(boardName: String, threadId: Long, size: Int = 0): Single<ChanThread> {
        return chanConnector.fetchThread(boardName, threadId, ChanConnector.CACHE_DEFAULT)
                .onErrorReturn { throwable ->
                    // ErrorChanThread(chanThread, throwable)
                    return@onErrorReturn ErrorChanThread(ChanThread(boardName, threadId, Collections.emptyList()), throwable)
                }
                .doOnNext {
                    if (it is ErrorChanThread) {
                        Log.d("ChanDataSource", "Error fetching thread: ${it.error.localizedMessage}", it.error)
                    } else if (it.posts.size > size){
                        PostTableConnection.putThread(it).subscribe()
                    }
                }
                .single(ChanThread.empty())
    }

    fun updateHistoryLastRead(boardName: String, threadId: Long, lastReadPosition: Int): Single<Boolean> {
        return HistoryTableConnection.fetchPost(boardName, threadId)
                .flatMap {
                    it.lastReadPosition = lastReadPosition
                    Flowable.just(it)
                }
                .flatMap {
                    HistoryTableConnection.updateHistory(it)
                }
                .single(false)
    }

    fun updateHistoryBookmark(boardName: String, threadId: Long, bookmarked: Boolean): Single<Boolean> {
        return HistoryTableConnection.fetchPost(boardName, threadId)
                .flatMap {
                    if (bookmarked) {
                        it.watched = 1
                    } else {
                        it.watched = 0
                    }

                    Flowable.just(it)
                }
                .flatMap {
                    HistoryTableConnection.updateHistory(it)
                }
                .single(false)
    }

}