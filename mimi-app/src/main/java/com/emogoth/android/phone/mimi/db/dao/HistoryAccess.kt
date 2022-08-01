package com.emogoth.android.phone.mimi.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.emogoth.android.phone.mimi.db.MimiDatabase
import com.emogoth.android.phone.mimi.db.models.History
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
abstract class HistoryAccess : BaseDao<History>() {

    @Query("SELECT * FROM ${MimiDatabase.HISTORY_TABLE} WHERE ${History.THREAD_ID} = :threadId AND ${History.BOARD_NAME} = :boardName")
    abstract fun getHistoryByThread(boardName: String, threadId: Long): Flowable<History>

    @Query("SELECT * FROM ${MimiDatabase.HISTORY_TABLE}")
    abstract fun getAll(): Flowable<List<History>>

//    @Query("SELECT * FROM ${MimiDatabase.HISTORY_TABLE} WHERE ${History.THREAD_REMOVED} = 0 AND (${History.ACTIVE} = 1 OR ${History.WATCHED} = 1) ORDER BY ${History.LAST_ACCESS} ASC LIMIT 1")
//    abstract fun oldestActiveHistory(): Single<History>

    @Query("SELECT * FROM ${MimiDatabase.HISTORY_TABLE} WHERE ${History.THREAD_REMOVED} = 0 AND ${History.WATCHED} = 1 ORDER BY ${History.LAST_ACCESS} ASC LIMIT 1")
    abstract fun oldestActiveBookmark(): Single<History>

    @Query("SELECT * FROM ${MimiDatabase.HISTORY_TABLE} WHERE ${History.WATCHED} = 0 ORDER BY ${History.ORDER_ID} LIMIT :count")
    abstract fun getHistory(count: Int): Flowable<List<History>>

    @Query("SELECT * FROM ${MimiDatabase.HISTORY_TABLE} WHERE ${History.WATCHED} = 1 ORDER BY ${History.ORDER_ID} LIMIT :count")
    abstract fun getBookmarks(count: Int): Flowable<List<History>>

    @Query("SELECT * FROM ${MimiDatabase.HISTORY_TABLE} WHERE ${History.WATCHED} = 0 ORDER BY ${History.ORDER_ID}")
    abstract fun getHistory(): Flowable<List<History>>

    @Query("SELECT * FROM ${MimiDatabase.HISTORY_TABLE} WHERE ${History.WATCHED} = 1 ORDER BY ${History.ORDER_ID}")
    abstract fun getBookmarks(): Flowable<List<History>>

    @Query("SELECT * FROM ${MimiDatabase.HISTORY_TABLE} WHERE ${History.THREAD_REMOVED} = 0 LIMIT :count")
    abstract fun getAllActive(count: Int = 9999): Flowable<List<History>>

    @Query("SELECT * FROM ${MimiDatabase.HISTORY_TABLE} WHERE ${History.WATCHED} = 0 AND ${History.THREAD_REMOVED} = 0 ORDER BY ${History.ORDER_ID} LIMIT :count")
    abstract fun getActiveHistory(count: Int = 9999): Flowable<List<History>>

    @Query("SELECT * FROM ${MimiDatabase.HISTORY_TABLE} WHERE ${History.WATCHED} = 0 AND ${History.THREAD_REMOVED} = 0 ORDER BY ${History.ORDER_ID}")
    abstract fun getActiveHistory(): Flowable<List<History>>

    @Query("SELECT * FROM ${MimiDatabase.HISTORY_TABLE} WHERE ${History.WATCHED} = 1 AND ${History.THREAD_REMOVED} = 0 ORDER BY ${History.ORDER_ID} LIMIT :count")
    abstract fun getActiveBookmarks(count: Int): Flowable<List<History>>

    @Query("SELECT * FROM ${MimiDatabase.HISTORY_TABLE} WHERE ${History.WATCHED} = 1 AND ${History.THREAD_REMOVED} = 0 ORDER BY ${History.ORDER_ID}")
    abstract fun getActiveBookmarks(): Flowable<List<History>>

    @Query("SELECT * FROM ${MimiDatabase.HISTORY_TABLE} ORDER BY ${History.LAST_ACCESS} DESC")
    abstract fun getAllByLastAccess(): Flowable<List<History>>

    @Query("SELECT * FROM ${MimiDatabase.HISTORY_TABLE} WHERE ${History.LAST_ACCESS} < :timestamp AND ${History.WATCHED} = :watched ORDER BY ${History.LAST_ACCESS} DESC")
    abstract fun getOldHistory(timestamp: Long, watched: Boolean): Single<List<History>>

    @Query("UPDATE ${MimiDatabase.HISTORY_TABLE} SET ${History.WATCHED} = :watched WHERE ${History.THREAD_ID} = :threadId AND ${History.BOARD_NAME} = :boardName")
    abstract fun watched(boardName: String, threadId: Long, watched: Boolean): Single<Int>

    @Query("UPDATE ${MimiDatabase.HISTORY_TABLE} SET ${History.UNREAD_COUNT} = :count WHERE ${History.THREAD_ID} = :threadId AND ${History.BOARD_NAME} = :boardName")
    abstract fun unreadCount(boardName: String, threadId: Long, count: Int): Single<Int>

    @Query("UPDATE ${MimiDatabase.HISTORY_TABLE} SET ${History.LAST_ACCESS} = :timestamp WHERE ${History.THREAD_ID} = :threadId AND ${History.BOARD_NAME} = :boardName")
    abstract fun lastAccessed(boardName: String, threadId: Long, timestamp: Long): Single<Int>

    @Query("UPDATE ${MimiDatabase.HISTORY_TABLE} SET ${History.LAST_READ_POS} = :position WHERE ${History.THREAD_ID} = :threadId AND ${History.BOARD_NAME} = :boardName")
    abstract fun lastReadPos(boardName: String, threadId: Long, position: Int): Single<Int>

    @Query("UPDATE ${MimiDatabase.HISTORY_TABLE} SET ${History.THREAD_REMOVED} = :removed WHERE ${History.BOARD_NAME} = :boardName AND ${History.THREAD_ID} = :threadId")
    abstract fun threadRemoved(boardName: String, threadId: Long, removed: Boolean): Single<Int>

//    @Query("UPDATE ${MimiDatabase.HISTORY_TABLE} SET ${History.ACTIVE} = :active WHERE ${History.BOARD_NAME} = :boardName AND ${History.THREAD_ID} = :threadId")
//    abstract fun threadActive(boardName: String, threadId: Long, active: Boolean): Single<Int>

    @Query("UPDATE ${MimiDatabase.HISTORY_TABLE} SET ${History.SIZE} = :size WHERE ${History.BOARD_NAME} = :boardName AND ${History.THREAD_ID} = :threadId")
    abstract fun threadSize(boardName: String, threadId: Long, size: Int): Single<Int>

    @Query("UPDATE ${MimiDatabase.HISTORY_TABLE} SET ${History.ORDER_ID} = :order WHERE ${History.BOARD_NAME} = :boardName AND ${History.THREAD_ID} = :threadId")
    abstract fun order(boardName: String, threadId: Long, order: Int): Single<Int>

    @Query("DELETE FROM ${MimiDatabase.HISTORY_TABLE} WHERE ${History.LAST_ACCESS} < :timestamp AND ${History.WATCHED} = :watched")
    abstract fun prune(timestamp: Long, watched: Boolean)

    @Query("DELETE FROM ${MimiDatabase.HISTORY_TABLE} WHERE ${History.BOARD_NAME} = :boardName AND ${History.THREAD_ID} = :threadId")
    abstract fun removeThread(boardName: String, threadId: Long)

    @Query("DELETE FROM ${MimiDatabase.HISTORY_TABLE}")
    abstract fun clear()
}