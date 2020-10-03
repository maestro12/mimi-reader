package com.emogoth.android.phone.mimi.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.emogoth.android.phone.mimi.db.MimiDatabase
import com.emogoth.android.phone.mimi.db.models.Filter
import io.reactivex.Single

@Dao
abstract class FilterAccess : BaseDao<Filter>() {
    @Query("SELECT * FROM ${MimiDatabase.FILTERS_TABLE}")
    abstract fun getAll(): Single<List<Filter>>

    @Query("SELECT * FROM ${MimiDatabase.FILTERS_TABLE} WHERE ${Filter.BOARD_NAME} = :boardName")
    abstract fun getFiltersByBoard(boardName: String): Single<List<Filter>>

    @Query("SELECT * FROM ${MimiDatabase.FILTERS_TABLE} WHERE ${Filter.NAME} = :name")
    abstract fun getFiltersByName(name: String): Single<List<Filter>>

    @Query("SELECT * FROM ${MimiDatabase.FILTERS_TABLE} WHERE ${Filter.BOARD_NAME} = :boardName AND ${Filter.NAME} = :filterName")
    abstract fun getFilter(boardName: String, filterName: String): Single<Filter>

    @Query("DELETE FROM ${MimiDatabase.FILTERS_TABLE} WHERE ${Filter.BOARD_NAME} = :boardName AND ${Filter.NAME} = :filterName")
    abstract fun removeFilter(boardName: String, filterName: String): Single<Int>
}