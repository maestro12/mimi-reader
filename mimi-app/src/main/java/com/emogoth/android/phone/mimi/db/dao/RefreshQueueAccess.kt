package com.emogoth.android.phone.mimi.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.emogoth.android.phone.mimi.db.MimiDatabase
import com.emogoth.android.phone.mimi.db.models.History
import com.emogoth.android.phone.mimi.db.models.QueueItem
import com.emogoth.android.phone.mimi.db.models.RefreshQueue
import io.reactivex.Maybe
import io.reactivex.Single

@Dao
abstract class RefreshQueueAccess : BaseDao<RefreshQueue>() {
    @Query("SELECT ${MimiDatabase.REFRESH_QUEUE_TABLE}.${RefreshQueue.ID} as queue_id, ${MimiDatabase.REFRESH_QUEUE_TABLE}.${RefreshQueue.THREAD_SIZE} as old_thread_size, ${MimiDatabase.REFRESH_QUEUE_TABLE}.${RefreshQueue.REPLY_COUNT}, ${MimiDatabase.REFRESH_QUEUE_TABLE}.${RefreshQueue.LAST_REFRESH}, ${MimiDatabase.HISTORY_TABLE}.${History.ID} as history_id, ${MimiDatabase.HISTORY_TABLE}.${History.BOARD_NAME}, ${MimiDatabase.HISTORY_TABLE}.${History.THREAD_ID}, ${MimiDatabase.HISTORY_TABLE}.${History.THREAD_REMOVED}, ${MimiDatabase.HISTORY_TABLE}.${History.SIZE}, ${MimiDatabase.HISTORY_TABLE}.${History.UNREAD_COUNT} FROM ${MimiDatabase.REFRESH_QUEUE_TABLE} INNER JOIN ${MimiDatabase.HISTORY_TABLE} ON ${MimiDatabase.REFRESH_QUEUE_TABLE}.${RefreshQueue.HISTORY_ID} = ${MimiDatabase.HISTORY_TABLE}.${History.ID} WHERE ${MimiDatabase.HISTORY_TABLE}.${History.BOARD_NAME} = :boardName AND ${MimiDatabase.HISTORY_TABLE}.${History.THREAD_ID} = :threadId ORDER BY ${MimiDatabase.REFRESH_QUEUE_TABLE}.${RefreshQueue.LAST_REFRESH} ASC LIMIT 1")
    abstract fun getQueueItem(boardName: String, threadId: Long): Single<QueueItem>

    @Query("SELECT ${MimiDatabase.REFRESH_QUEUE_TABLE}.${RefreshQueue.ID} as queue_id, ${MimiDatabase.REFRESH_QUEUE_TABLE}.${RefreshQueue.THREAD_SIZE} as old_thread_size, ${MimiDatabase.REFRESH_QUEUE_TABLE}.${RefreshQueue.REPLY_COUNT}, ${MimiDatabase.REFRESH_QUEUE_TABLE}.${RefreshQueue.LAST_REFRESH}, ${MimiDatabase.HISTORY_TABLE}.${History.ID} as history_id, ${MimiDatabase.HISTORY_TABLE}.${History.BOARD_NAME}, ${MimiDatabase.HISTORY_TABLE}.${History.THREAD_ID}, ${MimiDatabase.HISTORY_TABLE}.${History.THREAD_REMOVED}, ${MimiDatabase.HISTORY_TABLE}.${History.SIZE}, ${MimiDatabase.HISTORY_TABLE}.${History.UNREAD_COUNT} FROM ${MimiDatabase.REFRESH_QUEUE_TABLE} INNER JOIN ${MimiDatabase.HISTORY_TABLE} ON ${MimiDatabase.REFRESH_QUEUE_TABLE}.${RefreshQueue.HISTORY_ID} = ${MimiDatabase.HISTORY_TABLE}.${History.ID} ORDER BY ${MimiDatabase.REFRESH_QUEUE_TABLE}.${RefreshQueue.ID} ASC")
    abstract fun getAllQueueItems(): Single<List<QueueItem>>

    @Query("SELECT ${MimiDatabase.REFRESH_QUEUE_TABLE}.${RefreshQueue.ID} as queue_id, ${MimiDatabase.REFRESH_QUEUE_TABLE}.${RefreshQueue.THREAD_SIZE} as old_thread_size, ${MimiDatabase.REFRESH_QUEUE_TABLE}.${RefreshQueue.REPLY_COUNT}, ${MimiDatabase.REFRESH_QUEUE_TABLE}.${RefreshQueue.LAST_REFRESH}, ${MimiDatabase.HISTORY_TABLE}.${History.ID} as history_id, ${MimiDatabase.HISTORY_TABLE}.${History.BOARD_NAME}, ${MimiDatabase.HISTORY_TABLE}.${History.THREAD_ID}, ${MimiDatabase.HISTORY_TABLE}.${History.THREAD_REMOVED}, ${MimiDatabase.HISTORY_TABLE}.${History.SIZE}, ${MimiDatabase.HISTORY_TABLE}.${History.UNREAD_COUNT} FROM ${MimiDatabase.REFRESH_QUEUE_TABLE} INNER JOIN ${MimiDatabase.HISTORY_TABLE} ON ${MimiDatabase.REFRESH_QUEUE_TABLE}.${RefreshQueue.HISTORY_ID} = ${MimiDatabase.HISTORY_TABLE}.${History.ID} WHERE ${MimiDatabase.HISTORY_TABLE}.${History.THREAD_REMOVED} = 0 ORDER BY ${MimiDatabase.REFRESH_QUEUE_TABLE}.${RefreshQueue.LAST_REFRESH} ASC LIMIT 1")
    abstract fun nextItem(): Maybe<QueueItem>

    @Query("DELETE FROM ${MimiDatabase.REFRESH_QUEUE_TABLE} WHERE ${MimiDatabase.REFRESH_QUEUE_TABLE}.${RefreshQueue.ID} IN (SELECT ${MimiDatabase.REFRESH_QUEUE_TABLE}.${RefreshQueue.ID} FROM ${MimiDatabase.REFRESH_QUEUE_TABLE} INNER JOIN ${MimiDatabase.HISTORY_TABLE} ON ${MimiDatabase.REFRESH_QUEUE_TABLE}.${RefreshQueue.HISTORY_ID} = ${MimiDatabase.HISTORY_TABLE}.${History.ID} WHERE ${History.BOARD_NAME} = :boardName AND ${History.THREAD_ID} = :threadId)")
    abstract fun removeItem(boardName: String, threadId: Long): Single<Int>

    @Query("DELETE FROM ${MimiDatabase.REFRESH_QUEUE_TABLE} WHERE ${MimiDatabase.REFRESH_QUEUE_TABLE}.${RefreshQueue.ID} IN (SELECT ${MimiDatabase.REFRESH_QUEUE_TABLE}.${RefreshQueue.ID} FROM ${MimiDatabase.REFRESH_QUEUE_TABLE} INNER JOIN ${MimiDatabase.HISTORY_TABLE} ON ${MimiDatabase.REFRESH_QUEUE_TABLE}.${RefreshQueue.HISTORY_ID} = ${MimiDatabase.HISTORY_TABLE}.${History.ID} WHERE ${History.WATCHED} = 0)")
    abstract fun removeUnwatched(): Single<Int>

    @Query("DELETE FROM ${MimiDatabase.REFRESH_QUEUE_TABLE} WHERE ${MimiDatabase.REFRESH_QUEUE_TABLE}.${RefreshQueue.ID} IN (SELECT ${MimiDatabase.REFRESH_QUEUE_TABLE}.${RefreshQueue.ID} FROM ${MimiDatabase.REFRESH_QUEUE_TABLE} INNER JOIN ${MimiDatabase.HISTORY_TABLE} ON ${MimiDatabase.REFRESH_QUEUE_TABLE}.${RefreshQueue.HISTORY_ID} = ${MimiDatabase.HISTORY_TABLE}.${History.ID} WHERE ${History.THREAD_REMOVED} = 1)")
    abstract fun removeComplete(): Single<Int>
}
