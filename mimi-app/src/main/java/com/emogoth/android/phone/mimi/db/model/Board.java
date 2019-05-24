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

import io.reactivex.Observable;
import io.reactivex.functions.Function;

@Table(name = "Boards")
public class Board extends BaseModel {

    public static final String TABLE_NAME = "Boards";

    public static final String KEY_ID = "_id";
    public static final String KEY_TITLE = "board_name";
    public static final String KEY_NAME = "board_path";
    public static final String KEY_CATEGORY = "board_category";
    public static final String KEY_ACCESS_COUNT = "access_count";
    public static final String KEY_POST_COUNT = "post_count";
    public static final String KEY_LAST_ACCESSED = "last_accessed";
    public static final String KEY_FAVORITE = "favorite"; // 1 for favorite
    public static final String KEY_NSFW = "nsfw"; // 1 for sfw
    public static final String KEY_POSTS_PER_PAGE = "per_page";
    public static final String KEY_NUMBER_OF_PAGES = "pages";
    public static final String KEY_VISIBLE = "visible";
    public static final String KEY_ORDER_INDEX = "order_index";
    public static final String KEY_MAX_FILESIZE = "max_file_size";

    public static final int ORDERBY_NONE = 0;
    public static final int ORDERBY_NAME = 1;
    public static final int ORDERBY_PATH = 2;
    public static final int ORDERBY_CATEGORY = 3; // Don't use this yet
    public static final int ORDERBY_ACCESS_COUNT = 4;
    public static final int ORDERBY_POST_COUNT = 5;
    public static final int ORDERBY_LAST_ACCESSED = 6;
    public static final int ORDERBY_FAVORITE = 7; // Order by path as a secondary method


    @Column(name = KEY_ID, index = true)
    public int id = -1;

    @Column(name = KEY_TITLE, notNull = true)
    public String title; // title

    @Column(name = KEY_NAME, notNull = true)
    public String name; // name

    @Column(name = KEY_ACCESS_COUNT)
    public int accessCount = 0;

    @Column(name = KEY_POST_COUNT)
    public int postCount = 0;

    @Column(name = KEY_CATEGORY)
    public int category;

    @Column(name = KEY_LAST_ACCESSED)
    public long lastAccessed;

    @Column(name = KEY_FAVORITE)
    public int favorite;

    @Column(name = KEY_NSFW)
    public int nsfw; // This value is the opposite of nsfw, but requires a schema change

    @Column(name = KEY_POSTS_PER_PAGE)
    public int perPage;

    @Column(name = KEY_NUMBER_OF_PAGES)
    public int pages;

    @Column(name = KEY_VISIBLE)
    public int visible = 0;

    @Column(name = KEY_ORDER_INDEX)
    public int orderIndex;

    @Column(name = KEY_MAX_FILESIZE)
    public int maxFileSize;

    ContentValues values = null;

    public ContentValues toContentValues() {
        if (this.values == null) {
            ContentValues values = new ContentValues();

            if (id != -1) {
                values.put(Board.KEY_ID, id);
            }

            values.put(Board.KEY_NAME, name);
            values.put(Board.KEY_TITLE, title);

            values.put(Board.KEY_CATEGORY, category);
            values.put(Board.KEY_NSFW, nsfw == 0 ? 1 : 0); // the api reports safe for work
            values.put(Board.KEY_POSTS_PER_PAGE, perPage);
            values.put(Board.KEY_NUMBER_OF_PAGES, pages);

            values.put(Board.KEY_MAX_FILESIZE, maxFileSize);

            values.put(Board.KEY_FAVORITE, favorite);

            return values;
        }

        return this.values;
    }

    public void setValues(ContentValues values) {
        this.values = values;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public DatabaseUtils.WhereArg[] where() {
        DatabaseUtils.WhereArg[] arg = new DatabaseUtils.WhereArg[1];
        arg[0] = new DatabaseUtils.WhereArg(KEY_NAME + "=?", name);
        return arg;
    }

    @Override
    public void copyValuesFrom(BaseModel model) {
        if (model instanceof Board) {
            Board board = (Board) model;
            title = board.title;
            name = board.name;
            accessCount = board.accessCount;
            postCount = board.postCount;
            category = board.category;
            lastAccessed = board.lastAccessed;
            favorite = board.favorite;
            nsfw = board.nsfw;
            perPage = board.perPage;
            pages = board.pages;
            visible = board.visible;
            orderIndex = board.orderIndex;
            maxFileSize = board.maxFileSize;
        }
    }

    @Override
    public boolean isEmpty() {
        return TextUtils.isEmpty(title) && TextUtils.isEmpty(name);
    }

    public static Function<Cursor, Observable<List<Board>>> mapper() {
        return new Function<Cursor, Observable<List<Board>>>() {
            @Override
            public Observable<List<Board>> apply(Cursor cursor) {
                cursor.moveToPosition(-1);
                List<Board> boards = new ArrayList<>(cursor.getCount());
                while (cursor.moveToNext()) {
                    Board board = new Board();
                    board.loadFromCursor(cursor);

                    boards.add(board);
                }
                return Observable.just(boards);
            }
        };
    }

    public static String sortOrder(int orderBy) {
        String value;
        switch (orderBy) {
            case 0:
                value = Board.KEY_NAME + " ASC";
                break;
            case 1:
                value = Board.KEY_TITLE + " ASC";
                break;
            case 2:
                value = Board.KEY_NAME + " ASC";
                break;
            case 3:
                value = Board.KEY_ACCESS_COUNT;
                break;
            case 4:
                value = Board.KEY_POST_COUNT;
                break;
            case 5:
                value = Board.KEY_LAST_ACCESSED + " DESC";
                break;
            case 6:
                value = Board.KEY_FAVORITE + " DESC";
                break;
            case 7:
                value = Board.KEY_ORDER_INDEX + " ASC";
                break;
            default:
                value = Board.KEY_NAME + " ASC";
        }

        return value;
    }

    public static final class Builder {

        private final ContentValues values = new ContentValues();

        public Builder() {
        }

        public Builder id(int val) {
            values.put(Board.KEY_ID, val);
            return this;
        }

        public Builder name(String val) {
            values.put(Board.KEY_NAME, val);
            return this;
        }

        public Builder title(String val) {
            values.put(Board.KEY_TITLE, val);
            return this;
        }

        public Builder accessCount(int val) {
            values.put(Board.KEY_ACCESS_COUNT, val);
            return this;
        }

        public Builder postCount(int val) {
            values.put(Board.KEY_POST_COUNT, val);
            return this;
        }

        public Builder boardCategory(int val) {
            values.put(Board.KEY_CATEGORY, val);
            return this;
        }

        public Builder lastAccessed(long val) {
            values.put(Board.KEY_LAST_ACCESSED, val);
            return this;
        }

        public Builder favorite(boolean val) {
            values.put(Board.KEY_FAVORITE, val);
            return this;
        }

        public Builder nsfw(boolean val) {
            values.put(Board.KEY_NSFW, val);
            return this;
        }

        public Builder perPage(int val) {
            values.put(Board.KEY_POSTS_PER_PAGE, val);
            return this;
        }

        public Builder pages(int val) {
            values.put(Board.KEY_NUMBER_OF_PAGES, val);
            return this;
        }

        public Builder visible(boolean val) {
            values.put(Board.KEY_VISIBLE, val);
            return this;
        }

        public Builder orderIndex(int val) {
            values.put(Board.KEY_ORDER_INDEX, val);
            return this;
        }

        public Builder maxFileSize(int val) {
            values.put(Board.KEY_MAX_FILESIZE, val);
            return this;
        }

        public ContentValues build() {
            return values;
        }
    }
}
