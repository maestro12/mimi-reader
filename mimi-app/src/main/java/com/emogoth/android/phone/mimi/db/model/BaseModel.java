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

package com.emogoth.android.phone.mimi.db.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import com.activeandroid.Model;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

public abstract class BaseModel extends Model {
    public static final String LOG_TAG = BaseModel.class.getSimpleName();

    public abstract ContentValues toContentValues();

    public abstract String getTableName();

    public abstract DatabaseUtils.WhereArg[] where();

    public abstract void copyValuesFrom(BaseModel model);

    public abstract boolean isEmpty();

    public static <K extends BaseModel>  Function<Cursor, List<K>> mapper(final Class<K> c) {
        return cursor -> {
            cursor.moveToPosition(-1);
            List<K> data = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                try {
                    K obj = c.newInstance();
                    obj.loadFromCursor(cursor);
                    data.add(obj);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error mapping database model", e);
                    e.printStackTrace();
                }

            }
            return data;
        };
    }

    public String clause() {
        StringBuilder clauseBuilder = new StringBuilder(where()[0].where);

        DatabaseUtils.WhereArg[] args = where();
        for (int i = 1; i < args.length; i++) {
            clauseBuilder.append(" AND ").append(args[i].where);
        }

        return clauseBuilder.toString();
    }

    public String[] vals() {
        ArrayList<String> clauseVals = new ArrayList<>();
        DatabaseUtils.WhereArg[] args = where();
        int size = 0;
        for (DatabaseUtils.WhereArg arg : args) {
            for (int i = 0; i < arg.args.length; i++) {
                clauseVals.add(arg.args[i].toString());
                size++;
            }
        }

        return clauseVals.toArray(new String[size]);
    }
}
