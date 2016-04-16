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


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.util.Log;

import com.activeandroid.Cache;
import com.activeandroid.Model;
import com.activeandroid.TableInfo;
import com.activeandroid.query.Select;
import com.activeandroid.serializer.TypeSerializer;
import com.activeandroid.util.ReflectionUtils;
import com.emogoth.android.phone.mimi.BuildConfig;
import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.app.MimiApplication;
import com.emogoth.android.phone.mimi.db.model.BaseModel;
import com.emogoth.android.phone.mimi.db.model.Board;
import com.mimireader.chanlib.models.ChanBoard;
import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


public class DatabaseUtils {
    private static final String LOG_TAG = DatabaseUtils.class.getSimpleName();
    public static final String TABLE_NAME_PREFIX_SEPARATOR = "___";

    public static BriteDatabase createBriteDatabase() {
        try {
            Class<Cache> clazz = Cache.class;
            Field openHelperField = clazz.getDeclaredField("sDatabaseHelper");
            openHelperField.setAccessible(true);
            SQLiteOpenHelper sqLiteOpenHelper = (SQLiteOpenHelper) openHelperField.get(openHelperField.getType());
            if (sqLiteOpenHelper == null) {
                throw new AssertionError("Could not access the SQLiteOpenHelper from ActiveAndroid");
            }
            return SqlBrite.create().wrapDatabaseHelper(sqLiteOpenHelper);
        } catch (Exception e) {
            Log.e("DatabaseUtils", "Could not access the sqlite open helper", e);
            return null;
        }
    }

    public static Select.Column[] allColumns(@NonNull Class<? extends Model>... modelClasses) {
        List<Select.Column> columns = new ArrayList<>();
        for (Class<? extends Model> modelClass : modelClasses) {
            TableInfo tableInfo = Cache.getTableInfo(modelClass);
            String tableName = tableInfo.getTableName();
            for (Field field : tableInfo.getFields()) {
                String columnName = tableInfo.getColumnName(field);
                if (!BaseColumns._ID.equals(columnName)) {
                    String col = tableName + "." + columnName;
                    String alias = tableName + TABLE_NAME_PREFIX_SEPARATOR + columnName;
                    columns.add(new Select.Column(col, alias));
                }
            }
        }
        return columns.toArray(new Select.Column[columns.size()]);
    }

    public static Func1<SqlBrite.Query, Cursor> convertQueryToCursor() {
        return new Func1<SqlBrite.Query, Cursor>() {
            @Override
            public Cursor call(SqlBrite.Query query) {
                return query.run();
            }
        };
    }

    /**
     * This version of the method should only be used when querying joined tables that contain columns with the
     * same column name in each table. Otherwise, we should use Model#loadFromCursor(cursor).
     * <p/>
     * The difference in this method is that it first checks for the column index using the column name prefixed
     * with the table name and a special separator before falling back to just using the column name alone.
     * <p/>
     * Taken and modified from: https://github.com/pardom/ActiveAndroid/blob/master/src/com/activeandroid/Model.java
     */
    public static <T extends Model> void loadFromCursor(@NonNull T model, @NonNull Cursor cursor) {
        TableInfo tableInfo = Cache.getTableInfo(model.getClass());
        String tableNamePrefix = tableInfo.getTableName() + TABLE_NAME_PREFIX_SEPARATOR;

        for (Field field : tableInfo.getFields()) {
            final String fieldName = tableInfo.getColumnName(field);
            Class<?> fieldType = field.getType();

            // first check the tableName-prefixed version of the field's column name for column name exists multiple times from joins
            int columnIndex = cursor.getColumnIndex(tableNamePrefix + fieldName);
            if (columnIndex < 0) {
                columnIndex = cursor.getColumnIndex(fieldName);
            }

            if (columnIndex < 0) {
                continue;
            }

            field.setAccessible(true);

            try {
                boolean columnIsNull = cursor.isNull(columnIndex);
                TypeSerializer typeSerializer = Cache.getParserForType(fieldType);
                Object value = null;

                if (typeSerializer != null) {
                    fieldType = typeSerializer.getSerializedType();
                }

                // TODO: Find a smarter way to do this? This if block is necessary because we
                // can't know the type until runtime.
                if (columnIsNull) {
                    field = null;
                } else if (fieldType.equals(Byte.class) || fieldType.equals(byte.class)) {
                    value = cursor.getInt(columnIndex);
                } else if (fieldType.equals(Short.class) || fieldType.equals(short.class)) {
                    value = cursor.getInt(columnIndex);
                } else if (fieldType.equals(Integer.class) || fieldType.equals(int.class)) {
                    value = cursor.getInt(columnIndex);
                } else if (fieldType.equals(Long.class) || fieldType.equals(long.class)) {
                    value = cursor.getLong(columnIndex);
                } else if (fieldType.equals(Float.class) || fieldType.equals(float.class)) {
                    value = cursor.getFloat(columnIndex);
                } else if (fieldType.equals(Double.class) || fieldType.equals(double.class)) {
                    value = cursor.getDouble(columnIndex);
                } else if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) {
                    value = cursor.getInt(columnIndex) != 0;
                } else if (fieldType.equals(Character.class) || fieldType.equals(char.class)) {
                    value = cursor.getString(columnIndex).charAt(0);
                } else if (fieldType.equals(String.class)) {
                    value = cursor.getString(columnIndex);
                } else if (fieldType.equals(Byte[].class) || fieldType.equals(byte[].class)) {
                    value = cursor.getBlob(columnIndex);
                } else if (ReflectionUtils.isModel(fieldType)) {
                    final long entityId = cursor.getLong(columnIndex);
                    final Class<? extends Model> entityType = (Class<? extends Model>) fieldType;

                    Model entity = Cache.getEntity(entityType, entityId);
                    if (entity == null) {
                        entity = new Select().from(entityType).where(BaseColumns._ID + "=?", entityId).executeSingle();
                    }

                    value = entity;
                } else if (ReflectionUtils.isSubclassOf(fieldType, Enum.class)) {
                    @SuppressWarnings("rawtypes")
                    final Class<? extends Enum> enumType = (Class<? extends Enum>) fieldType;
                    value = Enum.valueOf(enumType, cursor.getString(columnIndex));
                }

                // Use a deserializer if one is available
                if (typeSerializer != null && !columnIsNull) {
                    value = typeSerializer.deserialize(value);
                }

                // Set the field value
                if (value != null) {
                    field.set(model, value);
                }
            } catch (IllegalArgumentException | IllegalAccessException | SecurityException e) {
                Log.e(LOG_TAG, e.getClass().getName(), e);
            }
        }

        if (model.getId() != null) {
            Cache.addEntity(model);
        }
    }

    public static int update(BriteDatabase db, BaseModel baseModel) {
        return db.update(baseModel.getTableName(), baseModel.toContentValues(), baseModel.whereClause(), baseModel.whereArg());
    }

    public static void insert(BriteDatabase db, BaseModel baseModel) {
        db.insert(baseModel.getTableName(), baseModel.toContentValues(), SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static <T> Observable.Transformer<T, T> applySchedulers() {
        return new Observable.Transformer<T, T>() {
            @Override
            public Observable<T> call(Observable<T> observable) {
                return observable.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread());
            }
        };
    }

}
