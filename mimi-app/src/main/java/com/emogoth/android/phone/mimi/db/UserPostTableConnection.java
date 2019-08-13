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

import com.activeandroid.query.Delete;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.emogoth.android.phone.mimi.app.MimiApplication;
import com.emogoth.android.phone.mimi.db.model.UserPost;
import com.squareup.sqlbrite3.BriteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;

import static com.emogoth.android.phone.mimi.db.ActiveAndroidSqlBriteBridge.runQuery;

public class UserPostTableConnection {
    public static final String LOG_TAG = UserPostTableConnection.class.getSimpleName();

    public static Flowable<List<UserPost>> fetchPosts(final String boardName, final long threadId) {
        return watchPosts(boardName, threadId).take(1);
    }
    public static Flowable<List<UserPost>> watchPosts(final String boardName, final long threadId) {
        From query = new Select()
                .from(UserPost.class)
                .where(UserPost.KEY_PATH + "=?", boardName)
                .and(UserPost.KEY_THREAD_ID + "=?", threadId);

        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();

        return db.createQuery(UserPost.TABLE_NAME, query.toSql(), (Object[]) query.getArguments())
                .toFlowable(BackpressureStrategy.BUFFER)
                .map(runQuery())
                .flatMap(UserPost.mapper())
                .compose(DatabaseUtils.<List<UserPost>>applySchedulers());
    }

    public static Flowable<Boolean> addPost(final String boardName, final long threadId, final long postId) {

        return Flowable.defer((Callable<Flowable<Boolean>>) () -> {
            long val = 0;
            BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
            BriteDatabase.Transaction transaction = db.newTransaction();

            try {
                UserPost userPost = new UserPost();
                userPost.boardName = boardName;
                userPost.threadId = threadId;
                userPost.postId = postId;
                userPost.postTime = System.currentTimeMillis();

                val = DatabaseUtils.insert(db, userPost);

                transaction.markSuccessful();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error saving user post data", e);
            } finally {
                transaction.end();
            }
            return Flowable.just(val > 0);
        });
    }

    public static Flowable<Boolean> removePost(final String boardName, final long threadId, final long postId) {

        return Flowable.defer((Callable<Flowable<Boolean>>) () -> {
            long val = 0;
            BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
            BriteDatabase.Transaction transaction = db.newTransaction();

            try {
                UserPost userPost = new UserPost();
                userPost.boardName = boardName;
                userPost.threadId = threadId;
                userPost.postId = postId;
                userPost.postTime = System.currentTimeMillis();

                val = DatabaseUtils.delete(db, userPost);

                transaction.markSuccessful();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error saving user post data", e);
            } finally {
                transaction.end();
            }
            return Flowable.just(val > 0);
        });
    }

    public static Flowable<Boolean> prune(int days) {
        final Long deleteTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
        From query = new Delete()
                .from(UserPost.class)
                .where(UserPost.KEY_POST_TIME + "<?", deleteTime);

        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();

        return db.createQuery(UserPost.TABLE_NAME, query.toSql(), query.getArguments())
                .toFlowable(BackpressureStrategy.BUFFER)
                .take(1)
                .map(runQuery())
                .flatMap(UserPost.mapper())
                .map(userPosts -> true)
                .onErrorReturn(throwable -> false);
    }

    public static List<Long> postIdList(List<UserPost> userPosts) {
        ArrayList<Long> postIds = new ArrayList<>(userPosts.size());
        for (UserPost userPost : userPosts) {
            postIds.add(userPost.postId);
        }

        return postIds;
    }
}
