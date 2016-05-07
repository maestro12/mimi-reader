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

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.functions.Func1;

@Table(name = "user_posts")
public class UserPost extends BaseModel {
    public static final String TABLE_NAME = "user_posts";

    public static final String KEY_ID = "_id";
    public static final String KEY_THREAD_ID = "thread_id";
    public static final String KEY_POST_ID = "post_id";
    public static final String KEY_PATH = "board_path";
    public static final String KEY_POST_TIME = "post_time";


    @Column(name = KEY_ID, index = true)
    public Integer id;

    @Column(name = KEY_THREAD_ID)
    public Integer threadId;

    @Column(name = KEY_POST_ID)
    public Integer postId;

    @Column(name = KEY_PATH)
    public String boardName;

    @Column(name = KEY_POST_TIME)
    public Long postTime;

    @Override
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();

        if (id != null) {
            values.put(KEY_ID, id);
        }

        if (threadId != null) {
            values.put(KEY_THREAD_ID, threadId);
        }

        if (postId != null) {
            values.put(KEY_POST_ID, postId);
        }

        if (boardName != null) {
            values.put(KEY_PATH, boardName);
        }

        if (postTime != null) {
            values.put(KEY_POST_TIME, postTime);
        }

        return values;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public String whereClause() {
        return KEY_THREAD_ID + "=?";
    }

    @Override
    public String whereArg() {
        return String.valueOf(threadId);
    }

    public static Func1<Cursor, Observable<List<UserPost>>> mapper() {
        return new Func1<Cursor, Observable<List<UserPost>>>() {
            @Override
            public Observable<List<UserPost>> call(Cursor cursor) {
                cursor.moveToPosition(-1);
                List<UserPost> userPostList = new ArrayList<>(cursor.getCount());
                while (cursor.moveToNext()) {
                    UserPost userPost = new UserPost();
                    userPost.loadFromCursor(cursor);

                    userPostList.add(userPost);
                }
                return Observable.just(userPostList);
            }
        };
    }
}
