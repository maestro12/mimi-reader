package com.emogoth.android.phone.mimi.db.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

@Table(name = PostOption.TABLE_NAME)
public class PostOption extends BaseModel {
    public static final String TABLE_NAME = "post_options";

    public static final String OPTION = "option";
    public static final String LAST_USED = "last_used";
    public static final String USED_COUNT = "used_count";

    @Column(name = OPTION)
    public String option;

    @Column(name = LAST_USED)
    public long lastUsed;

    @Column(name = USED_COUNT)
    public int usedCount;

    @Override
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(OPTION, option);
        values.put(LAST_USED, lastUsed);
        values.put(USED_COUNT, usedCount);
        return values;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public DatabaseUtils.WhereArg[] where() {
        DatabaseUtils.WhereArg[] arg = new DatabaseUtils.WhereArg[1];
        arg[0] = new DatabaseUtils.WhereArg(OPTION + "=?", option);
        return arg;
    }

    @Override
    public void copyValuesFrom(BaseModel model) {
        if (model instanceof PostOption) {
            PostOption postOption = (PostOption) model;

            option = postOption.option;
            lastUsed = postOption.lastUsed;
            usedCount = postOption.usedCount;
        }
    }

    @Override
    public boolean isEmpty() {
        return TextUtils.isEmpty(option);
    }

    public static Function<Cursor, Observable<List<PostOption>>> mapper() {
        return cursor -> {
            cursor.moveToPosition(-1);
            List<PostOption> postOptions = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                PostOption postOption = new PostOption();
                postOption.loadFromCursor(cursor);

                postOptions.add(postOption);
            }
            return Observable.just(postOptions);
        };
    }
}
