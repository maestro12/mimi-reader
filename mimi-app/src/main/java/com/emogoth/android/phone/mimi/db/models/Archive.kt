package com.emogoth.android.phone.mimi.db.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.emogoth.android.phone.mimi.db.MimiDatabase

@Entity(tableName = MimiDatabase.ARCHIVES_TABLE, indices = [Index(value = [Archive.BOARD_NAME]), Index(value = [Archive.DOMAIN]), Index(value = [Archive.UID])])
data class Archive(
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = ID)
        val id: Int? = null,

        @ColumnInfo(name = UID)
        val uid: Int = 0,

        @ColumnInfo(name = NAME)
        val name: String? = null,

        @ColumnInfo(name = DOMAIN)
        val domain: String? = null,

        @ColumnInfo(name = HTTPS)
        val https: Boolean = false,

        @ColumnInfo(name = SOFTWARE)
        val software: String? = null,

        @ColumnInfo(name = BOARD_NAME)
        val boardName: String = "",

        @ColumnInfo(name = REPORTS)
        val reports: Boolean = false
) {
    companion object {
        const val TABLE_NAME = "archives"

        const val ID = "id"
        const val UID = "uid"
        const val NAME = "name"
        const val DOMAIN = "domain"
        const val HTTPS = "https"
        const val SOFTWARE = "software"
        const val BOARD_NAME = "board"
        const val REPORTS = "reports"
    }

}