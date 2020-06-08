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

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;

@Table(name = UserPost.TABLE_NAME)
public class UserPost extends BaseModel {
    public static final String TABLE_NAME = "posts";

    public static final String KEY_THREAD_ID = "thread_id";
    public static final String KEY_POST_ID = "post_id";
    public static final String KEY_PATH = "board_path";
    public static final String KEY_POST_TIME = "post_time";

    @Column(name = KEY_THREAD_ID)
    public long threadId;

    @Column(name = KEY_POST_ID)
    public long postId;

    @Column(name = KEY_PATH)
    public String boardName;

    @Column(name = KEY_POST_TIME)
    public long postTime;

    @Override
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();

        values.put(KEY_THREAD_ID, threadId);
        values.put(KEY_POST_ID, postId);
        values.put(KEY_POST_TIME, postTime);

        if (boardName != null) {
            values.put(KEY_PATH, boardName);
        }
        return values;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public DatabaseUtils.WhereArg[] where() {
        DatabaseUtils.WhereArg[] arg = new DatabaseUtils.WhereArg[1];
        arg[0] = new DatabaseUtils.WhereArg(KEY_THREAD_ID + "=?", String.valueOf(threadId));
        return arg;
    }

    @Override
    public void copyValuesFrom(BaseModel model) {
        if (model instanceof UserPost) {
            UserPost userPost = (UserPost) model;

            threadId = userPost.threadId;
            postId = userPost.postId;
            boardName = userPost.boardName;
            postTime = userPost.postTime;
        }
    }

    @Override
    public boolean isEmpty() {
        return TextUtils.isEmpty(boardName) && threadId <= 0 && postId <= 0;
    }

    public static Function<Cursor, Single<List<UserPost>>> mapper() {
        return cursor -> {
            cursor.moveToPosition(-1);
            List<UserPost> userPostList = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                UserPost userPost = new UserPost();
                userPost.loadFromCursor(cursor);

                userPostList.add(userPost);
            }
            return Single.just(userPostList);
        };
    }

    public static Function<Cursor, Flowable<List<UserPost>>> flowableMapper() {
        return cursor -> {
            cursor.moveToPosition(-1);
            List<UserPost> userPostList = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                UserPost userPost = new UserPost();
                userPost.loadFromCursor(cursor);

                userPostList.add(userPost);
            }
            return Flowable.just(userPostList);
        };
    }
}
