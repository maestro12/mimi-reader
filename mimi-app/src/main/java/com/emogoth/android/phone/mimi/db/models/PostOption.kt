package com.emogoth.android.phone.mimi.db.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.emogoth.android.phone.mimi.db.MimiDatabase

@Entity(tableName = MimiDatabase.POST_OPTIONS_TABLE, indices = [Index(PostOption.ID, unique = true), Index(PostOption.OPTION, unique = true)])
data class PostOption(
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = ID)
        var id: Int? = null,

        @ColumnInfo(name = OPTION)
        var option: String = "",

        @ColumnInfo(name = LAST_USED)
        var lastUsed: Long = System.currentTimeMillis(),

        @ColumnInfo(name = USED_COUNT)
        var usedCount: Int = 0
) {

    companion object {
        const val ID = "id"
        const val OPTION = "option"
        const val LAST_USED = "last_used"
        const val USED_COUNT = "used_count"
    }
}