package com.emogoth.android.phone.mimi.db

import com.emogoth.android.phone.mimi.db.MimiDatabase.Companion.getInstance
import com.emogoth.android.phone.mimi.db.models.CatalogPost
import com.mimireader.chanlib.models.ChanCatalog
import com.mimireader.chanlib.models.ChanPost
import io.reactivex.Single
import io.reactivex.functions.Function

object CatalogTableConnection {
    val LOG_TAG = CatalogTableConnection::class.java.simpleName
    @JvmStatic
    fun fetchPosts(): Single<List<CatalogPost>> {
        return getInstance()!!.catalog().getAll().firstOrError()
    }

    @JvmStatic
    fun convertDbPostsToChanPosts(): Function<List<CatalogPost>, List<ChanPost>> {
        return Function { CatalogPostModels: List<CatalogPost> ->
            val posts: ArrayList<ChanPost> = ArrayList(CatalogPostModels.size)
            for (dbPost in CatalogPostModels) {
                posts.add(dbPost.toPost())
            }
            posts
        }
    }

    @JvmStatic
    fun putPosts(catalog: ChanCatalog): Single<Boolean> {
        return Single.defer {
            val posts = catalog.posts ?: return@defer Single.just(false)
            val catalogPosts: MutableList<CatalogPost> = ArrayList(posts.size)
            for (i in posts.indices) {
                catalogPosts.add(CatalogPost(posts[i]))
            }
            val resultsList = getInstance()!!.catalog().insert(catalogPosts)
            var success = true
            for (value in resultsList) {
                if (success) {
                    success = value > 0
                }
            }
            Single.just(success)
        }
    }

    @JvmStatic
    fun removeThread(threadId: Long): Single<Boolean> {
        return Single.defer {
            getInstance()?.catalog()?.removeThread(threadId) ?: return@defer Single.just(false)
            Single.just(true)
        }
    }

    @JvmStatic
    fun clear(): Single<Boolean> {
        return Single.defer {
            getInstance()?.catalog()?.clear() ?: return@defer Single.just(false)
            Single.just(true)
        }
    }
}