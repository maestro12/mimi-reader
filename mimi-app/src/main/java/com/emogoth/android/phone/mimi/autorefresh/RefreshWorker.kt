package com.emogoth.android.phone.mimi.autorefresh

import android.content.Context
import android.util.Log
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import com.emogoth.android.phone.mimi.async.ProcessThreadTask
import com.emogoth.android.phone.mimi.db.HistoryTableConnection
import com.emogoth.android.phone.mimi.db.PostTableConnection
import com.emogoth.android.phone.mimi.db.RefreshQueueTableConnection
import com.emogoth.android.phone.mimi.db.UserPostTableConnection
import com.emogoth.android.phone.mimi.db.models.QueueItem
import com.emogoth.android.phone.mimi.db.models.UserPost
import com.emogoth.android.phone.mimi.viewmodel.ChanDataSource
import com.mimireader.chanlib.models.ArchivedChanThread
import com.mimireader.chanlib.models.ChanThread
import com.mimireader.chanlib.models.ErrorChanThread
import io.reactivex.Single
import io.reactivex.functions.Function3


class RefreshWorker(private val appContext: Context, private val workerParams: WorkerParameters) : RxWorker(appContext, workerParams) {

    override fun createWork(): Single<Result> {
        val dataSource = ChanDataSource()
        val boardName = workerParams.inputData.getString(RefreshScheduler2.BOARD_NAME) ?: ""
        val threadId = workerParams.inputData.getLong(RefreshScheduler2.THREAD_ID, 0)
        val originalThreadSize = workerParams.inputData.getInt(RefreshScheduler2.THREAD_SIZE, 0)
        val background = workerParams.inputData.getBoolean(RefreshScheduler2.BACKGROUND, false)

        if (boardName.isEmpty() || threadId <= 0) {
            return Single.just(Result.failure())
        }

        return Single.zip(dataSource.fetchThread(boardName, threadId, originalThreadSize),
                RefreshQueueTableConnection.fetchItem(boardName, threadId).onErrorReturn {
                    Log.e(LOG_TAG, "Error getting refresh queue for /$boardName/$threadId", it)
                    QueueItem()
                },
                UserPostTableConnection.fetchPosts(boardName, threadId),
                { chanThread, queueItem, userPosts ->

                    if (chanThread is ArchivedChanThread || chanThread is ErrorChanThread || (chanThread.posts.size > 0 && chanThread.posts[0].isClosed)) {
                        Log.d(LOG_TAG, chanThread.toString())
                        HistoryTableConnection.setHistoryRemovedStatus(queueItem.boardName, queueItem.threadId, true).subscribe()
                    }

                    if (queueItem.historyId >= 0) {
                        val unread = queueItem.unread
                        val oldThreadSize = queueItem.oldThreadSize
                        val updatedThreadSize = chanThread.posts.size

                        val newPostCount = updatedThreadSize - oldThreadSize
                        val unreadCount = newPostCount + unread

                        if (updatedThreadSize > oldThreadSize) {
                            HistoryTableConnection.setThreadSize(queueItem.boardName, queueItem.threadId, updatedThreadSize)
                                    .flatMap {
                                        HistoryTableConnection.setUnreadCount(queueItem.boardName, queueItem.threadId, unreadCount)
                                    }.subscribe({
                                        if (newPostCount > queueItem.unread) {
                                            PostTableConnection.putThread(chanThread)
                                        }

                                        val replyCount = processThread(chanThread, queueItem, userPosts)
                                        if (LOG_DEBUG) Log.d(LOG_TAG, "Found $replyCount replies to your post")
                                        val currentTime = System.currentTimeMillis()
                                        try {
                                            RefreshQueueTableConnection.addItem(queueItem.historyId, updatedThreadSize, replyCount, currentTime, queueItem.queueId)
                                        } catch (e: Exception) {
                                            Log.e(LOG_TAG, "Error putting refresh queue data into the queue\nhistory id = ${queueItem.historyId}, size = $updatedThreadSize, reply count = $replyCount, time = $currentTime, queue id = ${queueItem.queueId}", e)
                                        }
                                        RefreshNotification.show()

                                        if (LOG_DEBUG) Log.d(LOG_TAG, "Showing notification for /${boardName}/${threadId} with ${updatedThreadSize - oldThreadSize} new post(s) since the last refresh")
                                    }, {
                                        if (LOG_DEBUG) Log.e(LOG_TAG, "Error putting refresh queue data", it)
                                    })
                        } else {
                            RefreshQueueTableConnection.addItem(queueItem.historyId, queueItem.threadSize, queueItem.replyCount, System.currentTimeMillis(), queueItem.queueId)
                            Log.d(LOG_TAG, "Not showing notification for /${boardName}/${threadId} because no new posts found (previous size=${oldThreadSize}, new size=${updatedThreadSize})")
                        }
                    } else {
                        Log.e(LOG_TAG, "Refresh queue item not found for /${boardName}/${threadId}")
                    }


                    Pair(chanThread, queueItem)
                })
                .map {
                    runNext(background)
                    Result.success()
                }
                .onErrorReturn {
                    runNext(background)
                    Result.failure()
                }
    }

    private fun processThread(chanThread: ChanThread, queueItem: QueueItem, userPosts: List<UserPost>): Int {
        val postIds = ArrayList<Long>(userPosts.size)
        for (userPost in userPosts) {
            postIds.add(userPost.postId)
        }

        val currentThread = ProcessThreadTask.processThread(
                chanThread.posts,
                postIds,
                chanThread.boardName,
                chanThread.threadId
        )

        val pos = chanThread.posts.size - queueItem.unread

        var repliesToYou = 0
        var userPostCount = 0
        if (currentThread != null) {
            for (i in pos until currentThread.posts.size) {
                val post = currentThread.posts[i]
                if (LOG_DEBUG) {
                    Log.d(LOG_TAG, "post id=" + post.no)
                }
                if (post.repliesTo.size > 0) {
                    for (l in postIds) {
                        val s = l.toString()
                        if (LOG_DEBUG) {
                            Log.d(LOG_TAG, "Checking post id $s")
                        }
                        if (post.repliesTo.contains(s)) {
                            if (LOG_DEBUG) {
                                Log.d(LOG_TAG, "Found reply to $s")
                            }
                            repliesToYou++
                            userPostCount++
                        }
                    }
                }
            }
        }

        return repliesToYou
    }

    private fun runNext(background: Boolean) {
        val scheduler = RefreshScheduler2()
        scheduler.startRequest(background)
    }

    companion object {
        private val LOG_DEBUG = true

        //        private val LOG_DEBUG = BuildConfig.DEBUG
        private const val LOG_TAG = "RefreshWorker"
    }
}