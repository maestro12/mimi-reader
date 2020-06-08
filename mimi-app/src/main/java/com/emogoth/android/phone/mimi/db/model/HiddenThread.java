package com.emogoth.android.phone.mimi.db.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;

@Table(name = HiddenThread.TABLE_NAME)
public class HiddenThread extends BaseModel {
    public static final String TABLE_NAME = "hidden_threads";

    public static final String BOARD_NAME = "board_name";
    public static final String THREAD_ID = "thread_id";
    public static final String TIME = "time";
    public static final String STICKY = "sticky";

    @Column(name = BOARD_NAME)
    public String boardName;

    @Column(name = THREAD_ID)
    public long threadId;

    @Column(name = TIME)
    public long time;

    @Column(name = STICKY)
    public int sticky;

    @Override
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();

        if (boardName != null) {
            values.put(BOARD_NAME, boardName);
        }

        if (threadId > 0) {
            values.put(THREAD_ID, threadId);
        }

        if (time > 0) {
            values.put(TIME, time);
        }

        values.put(STICKY, sticky);

        return values;
    }

    public static Function<Cursor, Single<List<HiddenThread>>> mapper() {
        return cursor -> {
            cursor.moveToPosition(-1);
            List<HiddenThread> hiddenThreads = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                HiddenThread thread = new HiddenThread();
                thread.loadFromCursor(cursor);

                hiddenThreads.add(thread);
            }
            return Single.just(hiddenThreads);
        };
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public DatabaseUtils.WhereArg[] where() {
        DatabaseUtils.WhereArg[] arg = new DatabaseUtils.WhereArg[1];
        arg[0] = new DatabaseUtils.WhereArg(THREAD_ID + "=?", String.valueOf(threadId));
        return arg;
    }

    @Override
    public void copyValuesFrom(BaseModel model) {
        if (model instanceof HiddenThread) {
            HiddenThread hiddenThread = (HiddenThread) model;
            boardName = hiddenThread.boardName;
            threadId = hiddenThread.threadId;
            time = hiddenThread.time;
            sticky = hiddenThread.sticky;

        }
    }

    @Override
    public boolean isEmpty() {
        return TextUtils.isEmpty(boardName);
    }
}
