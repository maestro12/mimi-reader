package com.emogoth.android.phone.mimi.db.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.emogoth.android.phone.mimi.db.MimiDatabase

@Entity(tableName = MimiDatabase.USER_POSTS_TABLE, indices = [Index(UserPost.ID, unique = true), Index(UserPost.POST_ID, unique = true), Index(UserPost.THREAD_ID)])
data class UserPost(
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = ID)
        val id: Int? = null,

        @ColumnInfo(name = THREAD_ID)
        val threadId: Long = 0,

        @ColumnInfo(name = POST_ID)
        val postId: Long = 0,

        @ColumnInfo(name = BOARD_NAME)
        val boardName: String? = null,

        @ColumnInfo(name = POST_TIME)
        val postTime: Long = 0
) {

    companion object {
        const val ID = "id"
        const val THREAD_ID = "thread_id"
        const val POST_ID = "post_id"
        const val BOARD_NAME = "board_path"
        const val POST_TIME = "post_time"
    }
}