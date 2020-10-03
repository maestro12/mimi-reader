package com.emogoth.android.phone.mimi.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.emogoth.android.phone.mimi.db.MimiDatabase
import com.emogoth.android.phone.mimi.db.models.HiddenThread
import io.reactivex.Single

@Dao
abstract class HiddenThreadAccess : BaseDao<HiddenThread>() {
    @Query("SELECT * FROM ${MimiDatabase.HIDDEN_THREADS_TABLE}")
    abstract fun getAll(): Single<List<HiddenThread>>

    @Query("SELECT * FROM ${MimiDatabase.HIDDEN_THREADS_TABLE} WHERE ${HiddenThread.BOARD_NAME} = :boardName")
    abstract fun getHiddenThreadsForBoard(boardName: String): Single<List<HiddenThread>>

    @Query("UPDATE ${MimiDatabase.HIDDEN_THREADS_TABLE} SET ${HiddenThread.STICKY} = :sticky WHERE ${HiddenThread.BOARD_NAME} = :boardName AND ${HiddenThread.THREAD_ID} = :threadId")
    abstract fun hideThread(boardName: String, threadId: Long, sticky: Boolean)

    @Query("DELETE FROM ${MimiDatabase.HIDDEN_THREADS_TABLE} WHERE ${HiddenThread.TIME} < :timestamp AND ${HiddenThread.STICKY} = 0")
    abstract fun prune(timestamp: Long)

    @Query("DELETE FROM ${MimiDatabase.HIDDEN_THREADS_TABLE}")
    abstract fun clear()
}