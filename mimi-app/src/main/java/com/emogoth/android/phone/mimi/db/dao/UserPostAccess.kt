package com.emogoth.android.phone.mimi.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.emogoth.android.phone.mimi.db.MimiDatabase
import com.emogoth.android.phone.mimi.db.models.UserPost
import io.reactivex.Flowable
import io.reactivex.Single

@Dao
abstract class UserPostAccess : BaseDao<UserPost>() {
    @Query("SELECT * FROM ${MimiDatabase.USER_POSTS_TABLE}")
    abstract fun getAll(): Single<List<UserPost>>

    @Query("SELECT * FROM ${MimiDatabase.USER_POSTS_TABLE} WHERE ${UserPost.BOARD_NAME} = :boardName AND ${UserPost.THREAD_ID} = :threadId")
    abstract fun getPosts(boardName: String, threadId: Long): Flowable<List<UserPost>>

    @Query("DELETE FROM ${MimiDatabase.USER_POSTS_TABLE} WHERE ${UserPost.BOARD_NAME} = :boardName AND ${UserPost.THREAD_ID} = :threadId AND ${UserPost.POST_ID} = :postId")
    abstract fun removePost(boardName: String, threadId: Long, postId: Long): Single<Int>

    @Query("DELETE FROM ${MimiDatabase.USER_POSTS_TABLE} WHERE ${UserPost.POST_TIME} < :timestamp")
    abstract fun prune(timestamp: Long)
}