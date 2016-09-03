package com.emogoth.android.phone.mimi.db.model;

import android.content.ContentValues;
import android.database.Cursor;

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.functions.Func1;

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
    public int threadId;

    @Column(name = TIME)
    public long time;

    @Column(name = STICKY)
    public boolean sticky;

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

    public static Func1<Cursor, Observable<List<HiddenThread>>> mapper() {
        return new Func1<Cursor, Observable<List<HiddenThread>>>() {
            @Override
            public Observable<List<HiddenThread>> call(Cursor cursor) {
                cursor.moveToPosition(-1);
                List<HiddenThread> hiddenThreads = new ArrayList<>(cursor.getCount());
                while (cursor.moveToNext()) {
                    HiddenThread thread = new HiddenThread();
                    thread.loadFromCursor(cursor);

                    hiddenThreads.add(thread);
                }
                return Observable.just(hiddenThreads);
            }
        };
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public String whereClause() {
        return THREAD_ID + "=?";
    }

    @Override
    public String whereArg() {
        return String.valueOf(threadId);
    }
}
