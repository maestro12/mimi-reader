package com.emogoth.android.phone.mimi.util

import android.util.Log
import com.emogoth.android.phone.mimi.BuildConfig
import com.emogoth.android.phone.mimi.db.ArchiveTableConnection
import com.emogoth.android.phone.mimi.db.models.Archive
import com.emogoth.android.phone.mimi.viewmodel.ChanDataSource
import com.mimireader.chanlib.models.ChanThread
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.schedulers.Schedulers

class ArchivesManager constructor(private val connector: ChanDataSource) {
    val TAG = ArchivesManager::class.java.simpleName

    private fun archives(board: String): Single<List<Archive>> {
        return ArchiveTableConnection.fetchArchives(board)
    }

    fun thread(board: String, threadId: Long): Single<ChanThread> {
        return ArchiveTableConnection.fetchArchives(board)
                .observeOn(Schedulers.io())
                .flatMap { archiveItems: List<Archive> ->
                    if (BuildConfig.DEBUG) {
                        if (archiveItems.isNotEmpty()) {
                            for (item in archiveItems) {
                                Log.d(TAG, "Archive: name=${item.name}, domain=${item.domain}")
                            }
                        } else {
                            Log.w(TAG, "No archive servers found for /$board/")
                        }
                    }
                    Single.create { emitter: SingleEmitter<ChanThread> ->
                        var success = false
                        var done = archiveItems.isEmpty()
                        var i = 0
                        while (!done) {
                            done = try {
                                val item = archiveItems[i]
                                val archivedThread = connector.fetchArchivedThread(board, threadId, item).subscribeOn(Schedulers.io()).blockingGet()
                                if (archivedThread.name?.isNotEmpty() == true && archivedThread.posts.size > 0) {
                                    success = true
                                    if (!emitter.isDisposed) {
                                        emitter.onSuccess(archivedThread)
                                    }
                                    true
                                } else {
                                    i++
                                    archiveItems.size <= i
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Caught exception while fetching archives", e)
                                i++
                                archiveItems.size <= i
                            }
                        }

                        if (!success && !emitter.isDisposed) {
                            emitter.onError(Exception("No Archive Found For Thread"))
                        }
                    }
                }
    }
}