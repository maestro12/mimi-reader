package com.emogoth.android.phone.mimi.db;

import com.emogoth.android.phone.mimi.db.model.Filter;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;

public class FilterTableConnection {
    public static Single<List<Filter>> fetchFilters() {
        return DatabaseUtils.fetchTable(Filter.class, Filter.TABLE_NAME);
    }

    public static Single<List<Filter>> fetchFilters(String boardName, String filterName) {
        return DatabaseUtils.fetchTable(Filter.class, Filter.TABLE_NAME, null, Filter.BOARD + "=? AND " + Filter.NAME + "=?", boardName, filterName);
    }

    public static Single<List<Filter>> fetchFiltersByBoard(String boardName) {
        return DatabaseUtils.fetchTable(Filter.class, Filter.TABLE_NAME, null, Filter.BOARD + "=?", boardName);
    }

    public static Single<List<Filter>> fetchFiltersByName(String filterName) {
        return DatabaseUtils.fetchTable(Filter.class, Filter.TABLE_NAME, null, Filter.NAME + "=?", filterName);
    }

    public static Flowable<Boolean> addFilter(String name, String filter, String boardName, boolean highlight) {
        Filter filterModel = new Filter();
        filterModel.name = name;
        filterModel.filter = filter;
        filterModel.board = boardName;
        filterModel.highlight = (byte) (highlight ? 1 : 0);

        return DatabaseUtils.insertOrUpdateRow(filterModel);
    }

    public static Flowable<Boolean> removeFilter(String name) {
        Filter filterModel = new Filter();
        filterModel.name = name;
        return DatabaseUtils.removeRow(filterModel);
    }
}
