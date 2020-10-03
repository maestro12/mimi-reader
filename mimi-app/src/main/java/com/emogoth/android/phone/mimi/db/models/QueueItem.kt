package com.emogoth.android.phone.mimi.db.models

import androidx.room.ColumnInfo

class QueueItem {
    companion object {
        const val QUEUE_ID = "queue_id"
        const val HISTORY_ID = "history_id"
        const val OLD_THREAD_SIZE = "old_thread_size"
    }

    @ColumnInfo(name = QUEUE_ID)
    var queueId: Int = -1

    @ColumnInfo(name = HISTORY_ID)
    var historyId: Int = -1

    @ColumnInfo(name = OLD_THREAD_SIZE)
    var oldThreadSize: Int = 0

    @ColumnInfo(name = History.SIZE)
    var threadSize: Int = 0

    @ColumnInfo(name = RefreshQueue.REPLY_COUNT)
    var replyCount: Int = 0

    @ColumnInfo(name = RefreshQueue.LAST_REFRESH)
    var lastRefresh: Long = 0

    @ColumnInfo(name = History.UNREAD_COUNT)
    var unread: Int = 0

    @ColumnInfo(name = History.BOARD_NAME)
    var boardName: String = ""

    @ColumnInfo(name = History.THREAD_ID)
    var threadId: Long = 0

    @ColumnInfo(name = History.THREAD_REMOVED)
    var threadRemoved: Boolean = false
}