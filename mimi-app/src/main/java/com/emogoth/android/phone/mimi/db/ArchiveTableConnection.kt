package com.emogoth.android.phone.mimi.db

import com.emogoth.android.phone.mimi.db.MimiDatabase.Companion.getInstance
import com.emogoth.android.phone.mimi.db.models.Archive
import com.mimireader.chanlib.models.ChanArchive
import io.reactivex.Single
import java.util.*

object ArchiveTableConnection {
    fun fetchArchives(board: String): Single<List<Archive>> {
        return getInstance()?.archives()?.getAllForBoard(board)?.firstOrError() ?: Single.just(emptyList())
    }

    @JvmStatic
    fun putChanArchives(archives: List<ChanArchive>): Single<Boolean> {
        return Single.defer {
            getInstance()?.archives()?.upsert(convertArchivesToDbModel(archives)) ?: return@defer Single.just(false)
            Single.just(true)
        }
    }

    private fun convertArchivesToDbModel(archives: List<ChanArchive>): List<Archive> {
        val models: MutableList<Archive> = ArrayList(archives.size)
        for (archive in archives) {
            for (board in archive.boards) {
                val model = Archive(null,
                        if (archive.uid == null) -1 else archive.uid,
                        archive.name,
                        archive.domain,
                        archive.https,
                        archive.software,
                        board, if (archive.reports == null) false else archive.reports)
                models.add(model)
            }
        }
        return models
    }

    @JvmStatic
    fun clear(): Single<Boolean> {
        return Single.defer {
            getInstance()?.archives()?.clear() ?: return@defer Single.just(false)
            Single.just(true)
        }
    }
}