package com.emogoth.android.phone.mimi.db.models

import androidx.room.*
import com.emogoth.android.phone.mimi.db.MimiDatabase

@Entity(tableName = MimiDatabase.FILTERS_TABLE,
        indices = [Index(Filter.ID, unique = true), Index(Filter.BOARD_NAME), Index(Filter.NAME, unique = true)])
class Filter {
    companion object {
        const val ID = "id"
        const val NAME = "name"
        const val FILTER = "filter"
        const val BOARD_NAME = "board"
        const val HIGHLIGHT = "highlight"
    }

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ID)
    var id: Int? = null

    @ColumnInfo(name = NAME)
    var name: String = ""

    @ColumnInfo(name = FILTER)
    var filter: String = ""

    @ColumnInfo(name = BOARD_NAME)
    var boardName: String = ""

    @ColumnInfo(name = HIGHLIGHT)
    var highlight: Int = 0
}