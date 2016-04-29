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

import com.emogoth.android.phone.mimi.db.model.BaseModel;
import com.squareup.sqlbrite.BriteDatabase;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


public class DatabaseUtils {

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
