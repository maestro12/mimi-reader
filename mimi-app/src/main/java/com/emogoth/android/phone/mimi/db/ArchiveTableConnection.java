package com.emogoth.android.phone.mimi.db;

import com.emogoth.android.phone.mimi.db.model.Archive;
import com.mimireader.chanlib.models.ChanArchive;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;

public class ArchiveTableConnection {
    public static Single<List<Archive>> fetchArchives(String board) {
        return DatabaseUtils.fetchTable(Archive.class, Archive.TABLE_NAME, null, Archive.BOARD + "=?", board);
    }

    public static Observable<Boolean> putChanArchives(List<ChanArchive> archives) {
        return DatabaseUtils.insert(convertArchivesToDbModel(archives)).toObservable();
    }

    private static List<Archive> convertArchivesToDbModel(List<ChanArchive> archives) {
        List<Archive> models = new ArrayList<>(archives.size());

        for (ChanArchive archive : archives) {
            for (String board : archive.getBoards()) {

                Archive model = new Archive();
                model.board = board;

                model.uid = archive.getUid() == null ? -1 : archive.getUid();
                model.name = archive.getName();
                model.domain = archive.getDomain();
                model.https = archive.getHttps();
                model.software = archive.getSoftware();
                model.reports = archive.getReports() == null ? false : archive.getReports();

                models.add(model);
            }
        }

        return models;
    }

    public static Observable<Boolean> clear() {
        DatabaseUtils.WhereArg[] args = new DatabaseUtils.WhereArg[0];
        return DatabaseUtils.remove(new Archive(), true, args).toObservable();
    }
}
