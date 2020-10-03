package com.emogoth.android.phone.mimi.db.models

import androidx.annotation.NonNull
import androidx.room.*
import com.emogoth.android.phone.mimi.db.MimiDatabase

@Entity(tableName = MimiDatabase.HISTORY_TABLE,
        indices = [Index(History.ID, unique = true), Index(History.BOARD_NAME), Index(History.THREAD_ID, unique = true)])

/*
@Entity(tableName = MimiDatabase.HISTORY_TABLE,
        indices = [Index(History.KEY_THREAD_ID, unique = true)],
        foreignKeys = [ForeignKey(entity = Board::class,
                parentColumns = [Board.KEY_ID],
                childColumns = [History.KEY_BOARD_ID],
                onUpdate = ForeignKey.NO_ACTION,
                onDelete = ForeignKey.NO_ACTION),
            ForeignKey(entity = Post::class,
                    parentColumns = [History.KEY_ID],
                    childColumns = [Post.KEY_HISTORY_ID],
                    onDelete = ForeignKey.CASCADE)])
 */
class History(
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = ID)
        var id: Int? = null,

        @ColumnInfo(name = ORDER_ID)
        var orderId: Int = 0,

        @ColumnInfo(name = THREAD_ID)
        var threadId: Long = 0,

        @ColumnInfo(name = BOARD_NAME)
        var boardName: String = "",

        @ColumnInfo(name = USER_NAME)
        var userName: String = "",

        @ColumnInfo(name = LAST_ACCESS)
        var lastAccess: Long = 0,

        @ColumnInfo(name = POST_TIM)
        var tim: String = "",

        @NonNull
        @ColumnInfo(name = POST_TEXT)
        var text: String = "",

        @ColumnInfo(name = WATCHED)
        var watched: Boolean = false, // TODO: Change to Bool

        @ColumnInfo(name = SIZE)
        var threadSize: Int = 0,

        @ColumnInfo(name = REPLIES)
        var replies: String = "",

        @ColumnInfo(name = THREAD_REMOVED)
        var removed: Boolean = false,

        @ColumnInfo(name = LAST_READ_POS)
        var lastReadPosition: Int = 0,

        @ColumnInfo(name = UNREAD_COUNT)
        var unreadCount: Int = 0
) {

    companion object {
        const val ID = "id"
        const val ORDER_ID = "order_id"
        const val THREAD_ID = "thread_id"
        const val BOARD_NAME = "board_path"
        const val USER_NAME = "user_name"
        const val POST_TIM = "post_tim" // thumbnail
        const val POST_TEXT = "post_text" // preview text
        const val LAST_ACCESS = "last_access"
        const val WATCHED = "watched"
        const val SIZE = "thread_size"
        const val REPLIES = "post_replies"
        const val THREAD_REMOVED = "thread_removed"
        const val LAST_READ_POS = "last_read_position"
        const val UNREAD_COUNT = "unread_count"
    }

    @Ignore
    var comment: CharSequence? = null
}