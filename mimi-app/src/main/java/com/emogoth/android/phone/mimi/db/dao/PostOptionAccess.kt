package com.emogoth.android.phone.mimi.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.emogoth.android.phone.mimi.db.MimiDatabase
import com.emogoth.android.phone.mimi.db.models.PostOption
import io.reactivex.Single

@Dao
abstract class PostOptionAccess : BaseDao<PostOption>() {
    @Query("SELECT * FROM ${MimiDatabase.POST_OPTIONS_TABLE}")
    abstract fun getAll(): Single<List<PostOption>>

    @Query("UPDATE ${MimiDatabase.POST_OPTIONS_TABLE} SET ${PostOption.USED_COUNT} = ${PostOption.USED_COUNT} + 1, ${PostOption.LAST_USED} = :timestamp WHERE ${PostOption.OPTION} = :option")
    abstract fun increment(option: String, timestamp: Long): Single<Int>

    @Query("DELETE FROM ${MimiDatabase.POST_OPTIONS_TABLE} WHERE ${PostOption.OPTION} = :option")
    abstract fun remove(option: String)
}