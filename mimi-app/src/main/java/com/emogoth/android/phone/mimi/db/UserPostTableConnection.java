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


import android.util.Log;

import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.emogoth.android.phone.mimi.app.MimiApplication;
import com.emogoth.android.phone.mimi.db.model.UserPost;
import com.squareup.sqlbrite.BriteDatabase;

import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;

import static com.emogoth.android.phone.mimi.db.ActiveAndroidSqlBriteBridge.runQuery;

public class UserPostTableConnection {
    public static final String LOG_TAG = UserPostTableConnection.class.getSimpleName();

    public static Observable<List<UserPost>> fetchPosts() {
        From query = new Select()
                .from(UserPost.class);

        Log.d(LOG_TAG, "SQL=" + query.toSql());
        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();

        return db.createQuery(UserPost.TABLE_NAME, query.toSql(), query.getArguments())
                .take(1)
                .map(runQuery())
                .flatMap(UserPost.mapper())
                .compose(DatabaseUtils.<List<UserPost>>applySchedulers());
    }

    public static Observable<Boolean> addPost(final String boardName, final int threadId, final int postId) {
        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
                BriteDatabase.Transaction transaction = db.newTransaction();
                long val = 0;

                try {
                    final UserPost userPost = new UserPost();
                    userPost.boardName = boardName;
                    userPost.threadId = threadId;
                    userPost.postId = postId;

                    val = db.update(UserPost.TABLE_NAME, userPost.toContentValues(), userPost.whereClause(), userPost.whereArg());
                    if (val <= 0) {
                        val = db.insert(UserPost.TABLE_NAME, userPost.toContentValues());
                    }
                } catch (Exception e) {
                    val = 0;
                    Log.e(LOG_TAG, "Error adding user post data", e);
                } finally {
                    transaction.end();
                    return Observable.just(val > 0);
                }
            }
        });
    }

    public static Observable<Boolean> prune(int days) {
        final Long deleteTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
                BriteDatabase.Transaction transaction = db.newTransaction();
                long val = -1;

                try {
                    val = db.delete(UserPost.TABLE_NAME, UserPost.KEY_POST_TIME + "<?", deleteTime.toString());
                    transaction.markSuccessful();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error pruning user posts: last update=" + deleteTime, e);
                } finally {
                    transaction.end();
                }

                return Observable.just(val > 0);
            }
        })
                .onErrorReturn(new Func1<Throwable, Boolean>() {
                    @Override
                    public Boolean call(Throwable throwable) {
                        Log.e(LOG_TAG, "Error pruning user posts: last update=" + deleteTime, throwable);
                        return false;
                    }
                })
                .compose(DatabaseUtils.<Boolean>applySchedulers());

    }
}
