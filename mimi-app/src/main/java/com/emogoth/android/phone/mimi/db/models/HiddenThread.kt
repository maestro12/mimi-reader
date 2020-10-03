package com.emogoth.android.phone.mimi.db.models

import androidx.room.*
import com.emogoth.android.phone.mimi.db.MimiDatabase

@Entity(tableName = MimiDatabase.HIDDEN_THREADS_TABLE,
        indices = [Index(HiddenThread.ID, unique = true), Index(HiddenThread.BOARD_NAME), Index(HiddenThread.THREAD_ID, unique = true)])
class HiddenThread {
    companion object {
        const val ID = "id"
        const val BOARD_NAME = "board_name"
        const val THREAD_ID = "thread_id"
        const val TIME = "time"
        const val STICKY = "sticky"
    }
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Int? = null

    @ColumnInfo(name = BOARD_NAME)
    var boardName: String = ""

    @ColumnInfo(name = THREAD_ID)
    var threadId: Long = 0

    @ColumnInfo(name = TIME)
    var time: Long = 0

    @ColumnInfo(name = STICKY)
    var sticky: Int = 0
}