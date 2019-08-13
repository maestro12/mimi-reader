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
import android.text.Spanned;
import android.text.TextUtils;

import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.functions.Function;

@Table(name = "History")
public class History extends BaseModel {
    public static final String TABLE_NAME = "History";

    public static final String KEY_ID = "_id";
    public static final String KEY_ORDER_ID = "order_id";
    public static final String KEY_THREAD_ID = "thread_id";
    public static final String KEY_NAME = "board_path";
    public static final String KEY_USER_NAME = "user_name";
    public static final String KEY_POST_TIM = "post_tim"; // thumbnail
    public static final String KEY_POST_TEXT = "post_text"; // preview text
    public static final String KEY_LAST_ACCESS = "last_access";
    public static final String KEY_WATCHED = "watched";
    public static final String KEY_SIZE = "thread_size";
    public static final String KEY_REPLIES = "post_replies";
    public static final String KEY_THREAD_REMOVED = "thread_removed";
    public static final String KEY_LAST_READ_POS = "last_read_position";

    @Column(name = KEY_ID, index = true)
    public Integer id;

    @Column(name = KEY_ORDER_ID)
    public Integer orderId;

    @Column(name = KEY_THREAD_ID)
    public Long threadId;

    @Column(name = KEY_NAME)
    public String boardName;

    @Column(name = KEY_USER_NAME)
    public String userName;

    @Column(name = KEY_LAST_ACCESS)
    public Long lastAccess;

    @Column(name = KEY_POST_TIM)
    public String tim;

    @Column(name = KEY_POST_TEXT)
    public String text;

    @Column(name = KEY_WATCHED)
    public int watched;

    @Column(name = KEY_SIZE)
    public int threadSize;

    @Column(name = KEY_REPLIES)
    public String replies;

    @Column(name = KEY_THREAD_REMOVED)
    public int removed;

    @Column(name = KEY_LAST_READ_POS)
    public int lastReadPosition;

    public CharSequence comment;

    public History() {
        this.threadId = -1L;
        this.lastReadPosition = 0;
        this.threadSize = 0;
    }

    @Override
    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();

        if (id != null) {
            values.put(KEY_ID, id);
        }

        if (orderId != null) {
            values.put(KEY_ORDER_ID, orderId);
        }

        values.put(KEY_THREAD_ID, threadId);
        values.put(KEY_NAME, boardName);
        values.put(KEY_USER_NAME, userName);

        if (lastAccess != null) {
            values.put(KEY_LAST_ACCESS, lastAccess);
        }

        values.put(KEY_POST_TIM, tim);
        values.put(KEY_POST_TEXT, text);
        values.put(KEY_WATCHED, watched);
        values.put(KEY_SIZE, threadSize);

        if (replies != null) {
            values.put(KEY_REPLIES, replies);
        }

        values.put(KEY_THREAD_REMOVED, removed);
        values.put(KEY_LAST_READ_POS, lastReadPosition);
        return values;
    }

    public static Function<Cursor, Flowable<List<History>>> mapper() {
        return cursor -> {
            cursor.moveToPosition(-1);
            List<History> historyList = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                History history = new History();
                history.loadFromCursor(cursor);

                historyList.add(history);
            }
            return Flowable.just(historyList);
        };
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
        if (model instanceof History) {
            History history = (History) model;

            orderId = history.orderId;
            threadId = history.threadId;
            boardName = history.boardName;
            userName = history.userName;
            lastAccess = history.lastAccess;
            tim = history.tim;
            text = history.text;
            watched = history.watched;
            threadSize = history.threadSize;
            replies = history.replies;
            removed = history.removed;
        }
    }

    @Override
    public boolean isEmpty() {
        return TextUtils.isEmpty(boardName) && threadId <= 0;
    }
}
