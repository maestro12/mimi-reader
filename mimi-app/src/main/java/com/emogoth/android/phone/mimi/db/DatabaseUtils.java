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
import android.database.sqlite.SQLiteException;
import android.text.TextUtils;
import android.util.Log;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.Model;
import com.activeandroid.TableInfo;
import com.activeandroid.query.Delete;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.emogoth.android.phone.mimi.BuildConfig;
import com.emogoth.android.phone.mimi.app.MimiApplication;
import com.emogoth.android.phone.mimi.db.model.BaseModel;
import com.squareup.sqlbrite3.BriteDatabase;
import com.squareup.sqlbrite3.QueryObservable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.Single;
import io.reactivex.SingleTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
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

    public static long delete(BriteDatabase db, BaseModel baseModel) {
        return db.delete(baseModel.getTableName(), baseModel.clause(), baseModel.vals());
    }

    public static <T> FlowableTransformer<T, T> applySchedulers() {
        return f -> f.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public static <T> SingleTransformer<T, T> applySingleSchedulers() {
        return f -> f.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public static <K extends BaseModel> Single<List<K>> fetchTable(final Class<K> c, String tableName, String order, String whereClause, Object... whereArgs) {
        return observeTable(c, tableName, order, whereClause, whereArgs).first(Collections.emptyList());
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

        if (BuildConfig.DEBUG) {
            StringBuilder sb = new StringBuilder(query.toSql()).append(": Values=");
            sb.append("[");
            for (String argument : query.getArguments()) {
                sb.append(argument).append(",");
            }
            sb.append("]");

            Log.d(LOG_TAG, sb.toString());
        }

        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
        return db.createQuery(tableName, query.toSql(), (Object[]) query.getArguments())
                .toFlowable(BackpressureStrategy.BUFFER)
                .map(runQuery())
                .map(BaseModel.mapper(c))
                .onErrorReturn(throwable -> {
                    Log.e(LOG_TAG, "Error fetching rows from table: " + c.getSimpleName(), throwable);
                    return new ArrayList<>();
                })
                .compose(DatabaseUtils.applySchedulers());

    }

    public static <K extends BaseModel> Single<List<K>> fetchTable(final Class<K> c, String tableName, String whereClause, Object... whereArgs) {
        return fetchTable(c, tableName, null, whereClause, whereArgs);
    }

    public static <K extends BaseModel> Single<List<K>> fetchTable(final Class<K> c, String tableName) {
        return fetchTable(c, tableName, null);
    }

    public static <K extends BaseModel> Single<Boolean> updateTable(final K model) {
        return Single.defer(() -> {
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

            return Single.just(val > 0);
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
        return db.createQuery(model.getTableName(), query.toSql(), (Object[]) query.getArguments())
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
            long val = insertModels(models);
            return Flowable.just(val > 0);
        });
    }

    public static <K extends BaseModel> long insertModels(final List<K> models) {
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

        return val;
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
        Flowable<Boolean> executor = db.createQuery(model.getTableName(), query.toSql(), (Object[]) query.getArguments())
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
                    .compose(DatabaseUtils.applySchedulers());
        }

        return executor.compose(DatabaseUtils.applySchedulers());
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

    public enum COLUMN_TYPE {
        STRING("VARCHAR", ""), LONG("LONG", ""), INT("INT", "");

        private final String name;
        private final String defaultValue;
        COLUMN_TYPE(String name, String defaultValue) {
            this.name = name;
            this.defaultValue = defaultValue;
        }

        public String getName() {
            return this.name;
        }
    }

    public static boolean createIfNeedColumn(Class<? extends Model> model, String column, COLUMN_TYPE type, boolean nullable) {
        boolean isFound = false;
        TableInfo tableInfo = new TableInfo(model);

        Collection<Field> columns = tableInfo.getFields();
        for (Field f : columns) {
            if (column.equals(f.getName())) {
                isFound = true;
                break;
            }
        }
        if (!isFound) {
            // ALTER TABLE History ADD COLUMN unread_count INT NOT NULL DEFAULT(0);
            final StringBuilder sb = new StringBuilder("ALTER TABLE");
            sb.append(tableInfo.getTableName()).append(" ADD COLUMN ").append(column).append(" ").append(type.name);
            if (!nullable) {
                final String defaultVal;
                if (type == COLUMN_TYPE.STRING) {
                    defaultVal = "''";
                } else {
                    defaultVal = type.defaultValue;
                }
                sb.append(" NOT NULL DEFAULT(").append(defaultVal).append(")");
            }
            final String sql = "ALTER TABLE " + tableInfo.getTableName() + " ADD COLUMN " + column + " " + type.name;
            try {
                ActiveAndroid.execSQL(sql);
            } catch (SQLiteException e) {
                Log.w(LOG_TAG, "Skipping column creation for " + tableInfo.getTableName() + " table because " + column + " already exists");
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error while executing sql: " + sql, e);
            }
        }
        return isFound;
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
