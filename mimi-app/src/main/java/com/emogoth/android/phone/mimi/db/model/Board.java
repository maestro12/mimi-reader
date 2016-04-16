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
import com.emogoth.android.phone.mimi.db.DatabaseUtils;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.functions.Func1;

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
    public Integer id;

    @Column(name = KEY_TITLE, notNull = true)
    public String title; // title

    @Column(name = KEY_NAME, notNull = true)
    public String name; // name

    @Column(name = KEY_ACCESS_COUNT)
    public Integer accessCount;

    @Column(name = KEY_POST_COUNT)
    public Integer postCount;

    @Column(name = KEY_CATEGORY)
    public Integer category;

    @Column(name = KEY_LAST_ACCESSED)
    public Long lastAccessed;

    @Column(name = KEY_FAVORITE)
    public Boolean favorite;

    @Column(name = KEY_NSFW)
    public Boolean nsfw; // This value is the opposite of nsfw, but requires a schema change

    @Column(name = KEY_POSTS_PER_PAGE)
    public Integer perPage;

    @Column(name = KEY_NUMBER_OF_PAGES)
    public Integer pages;

    @Column(name = KEY_VISIBLE)
    public Boolean visible;

    @Column(name = KEY_ORDER_INDEX)
    public Integer orderIndex;

    @Column(name = KEY_MAX_FILESIZE)
    public Integer maxFileSize;

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();

        if (id != null) {
            values.put(Board.KEY_ID, id);
        }

        values.put(Board.KEY_NAME, name);
        values.put(Board.KEY_TITLE, title);

        if (accessCount != null) {
            values.put(Board.KEY_ACCESS_COUNT, accessCount);
        }

        if (postCount != null) {
            values.put(Board.KEY_POST_COUNT, postCount);
        }

        values.put(Board.KEY_CATEGORY, category);
        values.put(Board.KEY_LAST_ACCESSED, lastAccessed);
        values.put(Board.KEY_FAVORITE, favorite);
        values.put(Board.KEY_NSFW, !nsfw); // the api reports safe for work
        values.put(Board.KEY_POSTS_PER_PAGE, perPage);
        values.put(Board.KEY_NUMBER_OF_PAGES, pages);

        if (visible != null) {
            values.put(Board.KEY_VISIBLE, visible);
        }
        values.put(Board.KEY_ORDER_INDEX, orderIndex);
        values.put(Board.KEY_MAX_FILESIZE, maxFileSize);

        return values;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public String whereClause() {
        return KEY_NAME + "=?";
    }

    @Override
    public String whereArg() {
        return name;
    }

    public static Func1<Cursor, Observable<List<Board>>> mapper() {
        return new Func1<Cursor, Observable<List<Board>>>() {
            @Override
            public Observable<List<Board>> call(Cursor cursor) {
                cursor.moveToPosition(-1);
                List<Board> boards = new ArrayList<>(cursor.getCount());
                while (cursor.moveToNext()) {
                    Board board = new Board();
                    DatabaseUtils.loadFromCursor(board, cursor);

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
                value = Board.KEY_LAST_ACCESSED;
                break;
            case 6:
                value = Board.KEY_FAVORITE;
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

        public Builder id(Integer val) {
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

        public Builder accessCount(Integer val) {
            values.put(Board.KEY_ACCESS_COUNT, val);
            return this;
        }

        public Builder postCount(Integer val) {
            values.put(Board.KEY_POST_COUNT, val);
            return this;
        }

        public Builder boardCategory(Integer val) {
            values.put(Board.KEY_CATEGORY, val);
            return this;
        }

        public Builder lastAccessed(Long val) {
            values.put(Board.KEY_LAST_ACCESSED, val);
            return this;
        }

        public Builder favorite(Boolean val) {
            values.put(Board.KEY_FAVORITE, val);
            return this;
        }

        public Builder nsfw(Boolean val) {
            values.put(Board.KEY_NSFW, val);
            return this;
        }

        public Builder perPage(Integer val) {
            values.put(Board.KEY_POSTS_PER_PAGE, val);
            return this;
        }

        public Builder pages(Integer val) {
            values.put(Board.KEY_NUMBER_OF_PAGES, val);
            return this;
        }

        public Builder visible(Boolean val) {
            values.put(Board.KEY_VISIBLE, val);
            return this;
        }

        public Builder orderIndex(Integer val) {
            values.put(Board.KEY_ORDER_INDEX, val);
            return this;
        }

        public Builder maxFileSize(Integer val) {
            values.put(Board.KEY_MAX_FILESIZE, val);
            return this;
        }

        public ContentValues build() {
            return values;
        }
    }



    /*
        public static String sortOrder(int orderBy) {
        String value;
        switch (orderBy) {
            case ORDERBY_NONE:
                value = Board.KEY_NAME + " ASC";
                break;
            case ORDERBY_NAME:
                value = Board.KEY_TITLE + " ASC";
                break;
            case ORDERBY_PATH:
                value = Board.KEY_NAME + " ASC";
                break;
            case ORDERBY_CATEGORY:
            case ORDERBY_ACCESS_COUNT:
                value = Board.KEY_ACCESS_COUNT;
                break;
            case ORDERBY_POST_COUNT:
                value = Board.KEY_POST_COUNT;
                break;
            case ORDERBY_LAST_ACCESSED:
                value = Board.KEY_LAST_ACCESSED;
                break;
            case ORDERBY_FAVORITE:
                value = Board.KEY_FAVORITE;
                break;
            case 7:
                value = Board.KEY_ORDER_INDEX + " ASC";
                break;
            default:
                value = Board.KEY_NAME + " ASC";
        }

        return value;
    }
     */
}
