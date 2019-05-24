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

@Table(name = "post_filter")
public class Filter extends BaseModel {
    public static final String TABLE_NAME = "post_filter";

    public static final String NAME = "name";
    public static final String FILTER = "filter";
    public static final String BOARD = "board";
    public static final String HIGHLIGHT = "highlight";

    @Column(name = NAME)
    public String name;

    @Column(name = FILTER)
    public String filter;

    @Column(name = BOARD)
    public String board;

    @Column(name = HIGHLIGHT)
    public int highlight;

    @Override
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(NAME, name);
        values.put(FILTER, filter);
        values.put(BOARD, board);
        values.put(HIGHLIGHT, highlight);
        return values;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public DatabaseUtils.WhereArg[] where() {
        DatabaseUtils.WhereArg[] args = new DatabaseUtils.WhereArg[1];
        args[0] = new DatabaseUtils.WhereArg(NAME + "=?", name);
        return args;
    }

    @Override
    public void copyValuesFrom(BaseModel model) {
        if (model instanceof Filter) {
            Filter filterModel = (Filter) model;
            name = filterModel.name;
            filter = filterModel.filter;
            board = filterModel.board;
            highlight = filterModel.highlight;
        }
    }

    @Override
    public boolean isEmpty() {
        return TextUtils.isEmpty(name) && TextUtils.isEmpty(filter) && TextUtils.isEmpty(board);
    }

    public static Function<Cursor, Observable<List<Filter>>> mapper() {
        return cursor -> {
            cursor.moveToPosition(-1);
            List<Filter> filters = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                Filter filter = new Filter();
                filter.loadFromCursor(cursor);

                filters.add(filter);
            }
            return Observable.just(filters);
        };
    }
}
