package com.emogoth.android.phone.mimi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.emogoth.android.phone.mimi.util.MimiPrefs
import com.mimireader.chanlib.models.ChanThread
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*

class ThreadViewModel(val boardName: String, val threadId: Long) : ViewModel() {
    private val TAG = "ThreadViewModel"
    private val dataSource = ChanDataSource()

    private val thread: ChanThread = ChanThread(boardName, threadId, Collections.emptyList())
    private var unread = 0
    private var lastReadPosition = -1
    private var bookmarked = false

    var firstFetchComplete = false
    var userPosts = dataSource.userPosts

    fun watchThread(): Flowable<ChanThread> {
        Log.d(TAG, "Getting thread")
        return dataSource.watchThread(boardName, threadId)
                .flatMap {
                    lastReadPosition = it.first.lastReadPosition
                    unread = if (it.first.threadSize > 0) it.first.threadSize - lastReadPosition else 0
                    bookmarked = it.first.watched == 1
                    Flowable.just(it.second)
                }
                .flatMap {
                    if (this.thread.posts.size < it.posts.size) {
                        unread = it.posts.size - this.thread.posts.size
                    }
                    this.thread.posts.clear()
                    this.thread.posts.addAll(it.posts)
                    Flowable.just(it)
                }
    }

    fun fetchThread(force: Boolean = true): Single<ChanThread> {
        val size = if (force) 0 else thread.posts.size
        return dataSource.fetchThread(boardName, threadId, size)
    }

    fun setLastReadPosition(pos: Int): Single<Boolean> {
        if (pos > this.lastReadPosition && pos > 0) {
            this.lastReadPosition = pos
            return dataSource.updateHistoryLastRead(boardName, threadId, pos)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
        }

        return Single.just(false)
    }

    fun setBookmarked(bookmarked: Boolean): Single<Boolean> {
        this.bookmarked = bookmarked
        if (!bookmarked) {
            MimiPrefs.removeWatch(threadId)
        }
        return dataSource.updateHistoryBookmark(boardName, threadId, bookmarked)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }

    fun unread(): Int {
        return this.unread
    }

    fun bookmarked(): Boolean {
        return this.bookmarked
    }

    fun lastReadPos(): Int {
        return this.lastReadPosition
    }

    override fun onCleared() {
        super.onCleared()
        thread.posts.clear()
        thread.threadId = 0
        thread.boardName = ""
    }
}