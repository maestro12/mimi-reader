package com.emogoth.android.phone.mimi.db.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.emogoth.android.phone.mimi.db.MimiDatabase

@Entity(tableName = MimiDatabase.ARCHIVED_POSTS_TABLE, indices = [Index(value = [ArchivedPost.POST_ID]), Index(value = [ArchivedPost.THREAD_ID]), Index(value = [ArchivedPost.BOARD_NAME])])
data class ArchivedPost(
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = ID)
        val id: Int? = null,

        @ColumnInfo(name = POST_ID)
        val postId: Long = -1,

        @ColumnInfo(name = THREAD_ID)
        val threadId: Long = -1,

        @ColumnInfo(name = BOARD_NAME)
        val boardName: String = "",

        @ColumnInfo(name = MEDIA_LINK)
        val mediaLink: String? = null,

        @ColumnInfo(name = THUMB_LINK)
        val thumbLink: String? = null,

        @ColumnInfo(name = ARCHIVE_NAME)
        val archiveName: String? = null,

        @ColumnInfo(name = ARCHIVE_DOMAIN)
        val archiveDomain: String? = null
) {

    companion object {
        const val TABLE_NAME = "archived_posts"

        const val ID = "id"
        const val POST_ID = "post_id"
        const val THREAD_ID = "thread_id"
        const val BOARD_NAME = "board_name"
        const val MEDIA_LINK = "media_link"
        const val THUMB_LINK = "thumb_link"
        const val ARCHIVE_NAME = "archive_name"
        const val ARCHIVE_DOMAIN = "archive_domain"
    }

    fun isEmpty(): Boolean {
        return postId == -1L
    }
}