package com.emogoth.android.phone.mimi.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.emogoth.android.phone.mimi.db.MimiDatabase
import com.emogoth.android.phone.mimi.db.models.ArchivedPost
import io.reactivex.Flowable

@Dao
abstract class ArchivedPostAccess: BaseDao<ArchivedPost>() {
    @Query("SELECT * FROM ${MimiDatabase.ARCHIVED_POSTS_TABLE}")
    abstract fun getAll(): Flowable<List<ArchivedPost>>

    @Query("SELECT * FROM ${MimiDatabase.ARCHIVED_POSTS_TABLE} WHERE ${ArchivedPost.BOARD_NAME} = :boardName AND ${ArchivedPost.THREAD_ID} = :threadId")
    abstract fun getThread(boardName: String, threadId: Long): Flowable<List<ArchivedPost>>

    @Query("DELETE FROM ${MimiDatabase.ARCHIVED_POSTS_TABLE} WHERE ${ArchivedPost.BOARD_NAME} = :boardName AND ${ArchivedPost.THREAD_ID} = :threadId")
    abstract fun removeThread(boardName: String, threadId: Long)

    @Query("DELETE FROM ${MimiDatabase.ARCHIVED_POSTS_TABLE} WHERE ${ArchivedPost.THREAD_ID} in (:ids)")
    abstract fun removeThreads(ids: List<Long>)

    @Query("DELETE FROM ${MimiDatabase.ARCHIVED_POSTS_TABLE}")
    abstract fun clear()
}