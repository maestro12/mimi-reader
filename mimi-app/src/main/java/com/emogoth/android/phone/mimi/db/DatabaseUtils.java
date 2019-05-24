/*
 * Copyright (c) 2016. Eli Connelly
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.emogoth.android.phone.mimi.db;


import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.activeandroid.query.Delete;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.emogoth.android.phone.mimi.app.MimiApplication;
import com.emogoth.android.phone.mimi.db.model.BaseModel;
import com.squareup.sqlbrite3.BriteDatabase;
import com.squareup.sqlbrite3.QueryObservable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import static com.emogoth.android.phone.mimi.db.ActiveAndroidSqlBriteBridge.runQuery;


public class DatabaseUtils {
    public static final String LOG_TAG = DatabaseUtils.class.getSimpleName();

    public static int update(BriteDatabase db, BaseModel baseModel) {
        return db.update(baseModel.getTableName(), SQLiteDatabase.CONFLICT_REPLACE, baseModel.toContentValues(), baseModel.clause(), baseModel.vals());
    }

    public static long insert(BriteDatabase db, BaseModel baseModel) {
        return db.insert(baseModel.getTableName(), SQLiteDatabase.CONFLICT_REPLACE, baseModel.toContentValues());
    }

    public static <T> FlowableTransformer<T, T> applySchedulers() {
        return f -> f.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public static <K extends BaseModel> Flowable<List<K>> fetchTable(final Class<K> c, String tableName, String order, String whereClause, Object... whereArgs) {
        return observeTable(c, tableName, order, whereClause, whereArgs).take(1);
    }

    public static <K extends BaseModel> Flowable<List<K>> observeTable(final Class<K> c, String tableName, String order, String whereClause, Object... whereArgs) {
        From query = new Select().from(c);
        if (whereClause != null && whereArgs != null && whereArgs.length > 0) {
            boolean hasNull = false;
            for (Object whereArg : whereArgs) {
                hasNull = whereArg == null;
            }

            if (!hasNull) {
                query.where(whereClause, whereArgs);
            }
        }

        if (!TextUtils.isEmpty(order)) {
            query.orderBy(order);
        }

        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
        return db.createQuery(tableName, query.toSql(), query.getArguments())
                .toFlowable(BackpressureStrategy.BUFFER)
                .map(runQuery())
                .map(BaseModel.mapper(c))
                .onErrorReturn(throwable -> {
                    Log.e(LOG_TAG, "Error fetching rows from table: " + c.getSimpleName(), throwable);
                    return new ArrayList<>();
                })
                .compose(DatabaseUtils.applySchedulers());

    }

    public static <K extends BaseModel> Flowable<List<K>> fetchTable(final Class<K> c, String tableName, String whereClause, Object... whereArgs) {
        return fetchTable(c, tableName, null, whereClause, whereArgs);
    }

    public static <K extends BaseModel> Flowable<List<K>> fetchTable(final Class<K> c, String tableName) {
        return fetchTable(c, tableName, null);
    }

    public static <K extends BaseModel> Flowable<Boolean> updateTable(final K model) {
        return Flowable.defer(() -> {
            BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
            BriteDatabase.Transaction transaction = db.newTransaction();
            int val = 0;
            try {
                val = db.update(model.getTableName(), SQLiteDatabase.CONFLICT_REPLACE, model.toContentValues(), model.clause(), model.vals());
                transaction.markSuccessful();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error putting model " + model.getClass().getSimpleName() + " into the database", e);
            } finally {
                transaction.end();
            }

            return Flowable.just(val > 0);
        });
    }

    public static <K extends BaseModel> Flowable<Boolean> insertOrUpdateRow(final K model, WhereArg... wheres) {
        From query = new Select()
                .from(model.getClass());

        final WhereArg[] args;
        if (wheres != null && wheres.length > 0) {
            args = wheres;

        } else {
            args = model.where();
        }

        query = query.where(args[0].where, args[0].args);
        for (int i = 1; i < args.length; i++) {
            WhereArg where = args[i];
            query = query.and(where.where, where.args);
        }

        Log.d(LOG_TAG, "SQL=" + query.toSql());

        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
        return db.createQuery(model.getTableName(), query.toSql(), query.getArguments())
                .toFlowable(BackpressureStrategy.BUFFER)
                .take(1)
                .map(runQuery())
                .map(BaseModel.mapper(model.getClass()))
                .map((Function<List<? extends BaseModel>, K>) data -> {
                    if (data.size() > 0) {
                        return model;
                    }

                    try {
                        return (K) model.getClass().newInstance();
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Error creating instance of " + model.getClass().getSimpleName() + " in insertOrUpdateRow()");
                    }
                    return model;
                })
                .flatMap((Function<K, Flowable<Boolean>>) data -> {
                    if (data.isEmpty()) {
                        return insertItemObservable(model);
                    }

                    data.copyValuesFrom(model);
                    return updateItemObservable(data);
                })
                .onErrorReturn(throwable -> {
                    Log.e(LOG_TAG, "Error updating table: name=" + model.getClass().getSimpleName(), throwable);
                    return false;
                })
                .compose(DatabaseUtils.<Boolean>applySchedulers());
    }

    public static <K extends BaseModel> Flowable<Boolean> insert(final List<K> models) {

        return Flowable.defer((Callable<Flowable<Boolean>>) () -> {
            BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
            BriteDatabase.Transaction transaction = db.newTransaction();

            long val = 0;

            for (K model : models) {
                try {
                    val = db.insert(model.getTableName(), SQLiteDatabase.CONFLICT_REPLACE, model.toContentValues());
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error putting model " + model.getClass().getSimpleName() + " into the database", e);
                }
            }

            transaction.markSuccessful();
            transaction.end();

            return Flowable.just(val > 0);
        });


    }

    public static <K extends BaseModel> Flowable<Boolean> remove(final K model, boolean removeAll, WhereArg... wheres) {
        From query = new Delete().from(model.getClass());

        final WhereArg[] args;
        if (wheres != null && wheres.length > 0) {
            args = wheres;

        } else {
            args = model.where();
        }

        if (!removeAll) {
            query = query.where(args[0].where, args[0].args);
            for (int i = 1; i < args.length; i++) {
                WhereArg where = args[i];
                query = query.and(where.where, where.args);
            }
        }

        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
        Flowable<Boolean> executor = db.createQuery(model.getTableName(), query.toSql(), query.getArguments())
                .toFlowable(BackpressureStrategy.BUFFER)
                .take(1)
                .map(runQuery())
                .map(BaseModel.mapper(model.getClass()))
                .flatMap((Function<List<? extends BaseModel>, Flowable<Boolean>>) baseModels -> Flowable.just(true))
                .onErrorReturn(throwable -> {
                    Log.e(LOG_TAG, "Error clearing database rows", throwable);
                    return false;
                });
        if (!removeAll) {
            return executor.take(1)
                    .compose(DatabaseUtils.<Boolean>applySchedulers());
        }

        return executor.compose(DatabaseUtils.<Boolean>applySchedulers());
    }

    public static <K extends BaseModel> Flowable<Boolean> remove(final K model, WhereArg wheres) {

        return Flowable.defer((Callable<Flowable<Boolean>>) () -> {
            BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
            long val = db.delete(model.getTableName(), wheres.where, wheres.where);

            return Flowable.just(val >= 0);
        });
    }

    public static <K extends BaseModel> Flowable<Boolean> removeRow(final K model, WhereArg... wheres) {
        return remove(model, false, wheres);
    }

    public static QueryObservable rawQuery(String tableName, String query) {
        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
        return db.createQuery(tableName, query);
    }

    private static Flowable<Boolean> insertItemObservable(final BaseModel model) {
        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
        BriteDatabase.Transaction transaction = db.newTransaction();
        long val = 0;
        try {
            val = db.insert(model.getTableName(), SQLiteDatabase.CONFLICT_REPLACE, model.toContentValues());
            transaction.markSuccessful();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error putting post options into the database", e);
        } finally {
            transaction.end();
        }

        return Flowable.just(val > 0);
    }

    private static Flowable<Boolean> updateItemObservable(final BaseModel model) {
        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
        BriteDatabase.Transaction transaction = db.newTransaction();
        int val = 0;
        try {
            val = db.update(model.getTableName(), SQLiteDatabase.CONFLICT_REPLACE, model.toContentValues(), model.clause(), model.vals());
            transaction.markSuccessful();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error putting post options into the database", e);
        } finally {
            transaction.end();
        }

        return Flowable.just(val > 0);
    }

    public static class WhereArg {
        public final String where;
        public final Object[] args;

        public WhereArg(String where, Object... args) {
            this.where = where;
            this.args = args;
        }
    }

}
