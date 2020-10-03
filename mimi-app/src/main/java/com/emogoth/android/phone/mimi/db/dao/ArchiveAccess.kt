package com.emogoth.android.phone.mimi.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.emogoth.android.phone.mimi.db.MimiDatabase
import com.emogoth.android.phone.mimi.db.models.Archive
import io.reactivex.Flowable

@Dao
abstract class ArchiveAccess : BaseDao<Archive>() {
    @Query("SELECT * FROM ${MimiDatabase.ARCHIVES_TABLE}")
    abstract fun getAll(): Flowable<List<Archive>>

    @Query("SELECT * FROM ${MimiDatabase.ARCHIVES_TABLE} WHERE ${Archive.BOARD_NAME} = :boardName")
    abstract fun getAllForBoard(boardName: String): Flowable<List<Archive>>

    @Query("DELETE FROM ${MimiDatabase.ARCHIVES_TABLE}")
    abstract fun clear()
}