package com.emogoth.android.phone.mimi.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.emogoth.android.phone.mimi.db.MimiDatabase
import com.emogoth.android.phone.mimi.db.models.CatalogPost
import io.reactivex.Flowable

@Dao
abstract class CatalogAccess : BaseDao<CatalogPost>() {

    @Query("SELECT * FROM ${MimiDatabase.CATALOG_TABLE}")
    abstract fun getAll(): Flowable<List<CatalogPost>>

    @Query("DELETE FROM ${MimiDatabase.CATALOG_TABLE} WHERE ${CatalogPost.POST_ID} = :threadId")
    abstract fun removeThread(threadId: Long): Int

    @Query("DELETE FROM ${MimiDatabase.CATALOG_TABLE}")
    abstract fun clear()
}