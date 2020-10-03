package com.emogoth.android.phone.mimi.db.models

import androidx.room.*
import com.emogoth.android.phone.mimi.db.MimiDatabase

@Entity(tableName = MimiDatabase.REFRESH_QUEUE_TABLE,
        indices = [Index(RefreshQueue.ID, unique = true),
            Index(RefreshQueue.HISTORY_ID, unique = true)],
        foreignKeys = [ForeignKey(
                entity = History::class,
                parentColumns = [History.ID],
                childColumns = [RefreshQueue.HISTORY_ID],
                onDelete = ForeignKey.CASCADE)])
data class RefreshQueue(
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = RefreshQueue.ID)
        val id: Int? = null,

        @ColumnInfo(name = RefreshQueue.HISTORY_ID)
        val historyId: Int? = null,

        @ColumnInfo(name = RefreshQueue.THREAD_SIZE)
        val threadSize: Int = 0,

        @ColumnInfo(name = RefreshQueue.REPLY_COUNT)
        val unread: Int = 0,

        @ColumnInfo(name = RefreshQueue.LAST_REFRESH)
        val lastRefresh: Long = 0L
) {
    companion object {
        const val ID = "id"
        const val HISTORY_ID = "history_id"
        const val THREAD_SIZE = "thread_size"
        const val REPLY_COUNT = "reply_count"
        const val LAST_REFRESH = "last_refresh"
    }

}