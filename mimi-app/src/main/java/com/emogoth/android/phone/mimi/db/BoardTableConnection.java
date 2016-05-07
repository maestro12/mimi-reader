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


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.app.MimiApplication;
import com.emogoth.android.phone.mimi.db.model.BaseModel;
import com.emogoth.android.phone.mimi.db.model.Board;
import com.mimireader.chanlib.models.ChanBoard;
import com.squareup.sqlbrite.BriteDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

import static com.emogoth.android.phone.mimi.db.ActiveAndroidSqlBriteBridge.runQuery;

public class BoardTableConnection {
    public static final String LOG_TAG = BoardTableConnection.class.getSimpleName();

    public static Observable<List<Board>> fetchBoards(int orderBy) {
        From query = new Select()
                .all()
                .from(Board.class)
                .orderBy(Board.sortOrder(orderBy))
                .and(Board.KEY_VISIBLE + "=?", true);

        Log.d(LOG_TAG, "SQL=" + query.toSql());

        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();

        return db.createQuery(Board.TABLE_NAME, query.toSql(), query.getArguments())
                .take(1)
                .map(runQuery())
                .flatMap(Board.mapper())
                .onErrorReturn(new Func1<Throwable, List<Board>>() {
                    @Override
                    public List<Board> call(Throwable throwable) {
                        Log.e(LOG_TAG, "Error loading boards from the database", throwable);
                        return Collections.emptyList();
                    }
                });
    }

    public static Observable<ChanBoard> fetchBoard(final String boardName) {
        From query = new Select()
                .all()
                .from(Board.class)
                .where(Board.KEY_NAME + "=?", boardName);

        Log.d(LOG_TAG, "SQL=" + query.toSql());

        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
        return db.createQuery(Board.TABLE_NAME, query.toSql(), query.getArguments())
                .take(1)
                .map(runQuery())
                .flatMap(new Func1<Cursor, Observable<ChanBoard>>() {
                    @Override
                    public Observable<ChanBoard> call(Cursor cursor) {
                        ChanBoard chanBoard = null;
                        while (cursor.moveToNext()) {
                            Board board = new Board();
                            board.loadFromCursor(cursor);

                            chanBoard = convertBoardDbModelToBoard(board);
                        }

                        return Observable.just(chanBoard);
                    }
                })
                .onErrorReturn(new Func1<Throwable, ChanBoard>() {
                    @Override
                    public ChanBoard call(Throwable throwable) {
                        Log.e(LOG_TAG, "Error getting board info", throwable);
                        return null;
                    }
                });
    }

    public static Observable<Boolean> setBoardFavorite(final String boardName, final boolean favorite) {
        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                int val = 0;
                BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
                BriteDatabase.Transaction transaction = db.newTransaction();
                ContentValues values = new ContentValues();

                try {
                    values.put(Board.KEY_FAVORITE, favorite);
                    val = db.update(Board.TABLE_NAME, values, Board.KEY_NAME + "=?", boardName);
                    transaction.markSuccessful();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error setting favorite: board=" + boardName + ", favorite=" + favorite);
                } finally {
                    transaction.end();
                }

                return Observable.just(val > 0);
            }
        });
    }

    public static Observable<Boolean> incrementAccessCount(final String boardName) {
        return updateBoard(boardName, 2);
    }

    public static Observable<Boolean> incrementPostCount(final String boardName) {
        return updateBoard(boardName, 1);
    }

    public static Observable<Boolean> updateLastAccess(final String boardName) {
        return updateBoard(boardName, 0);
    }

    private static Observable<Boolean> updateBoard(final String boardName, final int type) {
        From query = new Select()
                .all()
                .from(Board.class)
                .where(Board.KEY_NAME + "=?", boardName);

        Log.d(LOG_TAG, "SQL=" + query.toSql());

        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
        return db.createQuery(Board.TABLE_NAME, query.toSql(), query.getArguments())
                .take(1)
                .map(runQuery())
                .flatMap(new Func1<Cursor, Observable<Board>>() {
                    @Override
                    public Observable<Board> call(Cursor cursor) {
                        Board board = new Board();
                        while (cursor.moveToNext()) {
                            board.loadFromCursor(cursor);
                        }

                        return Observable.just(board);
                    }
                })
                .flatMap(new Func1<Board, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(Board board) {
                        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
                        BriteDatabase.Transaction transaction = db.newTransaction();

                        int val = 0;
                        try {

                            ContentValues values = null;
                            if (type == 0) {
                                values = updateLastAccessed();
                            } else if (type == 1) {
                                values = updatePostCount(board);
                            } else if (type == 2) {
                                values = updateAccessCount(board);
                            }

                            if (values != null) {
                                val = db.update(Board.TABLE_NAME, values, board.whereClause(), board.whereArg());
                            }
                            transaction.markSuccessful();
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "Error updating access count", e);
                        } finally {
                            transaction.end();
                        }

                        return Observable.just(val > 0);
                    }
                })
                .onErrorReturn(new Func1<Throwable, Boolean>() {
                    @Override
                    public Boolean call(Throwable throwable) {
                        Log.e(LOG_TAG, "Error updating access count", throwable);
                        return null;
                    }
                })
                .compose(DatabaseUtils.<Boolean>applySchedulers());
    }

    private static ContentValues updateLastAccessed() {
        ContentValues values = new ContentValues();
        values.put(Board.KEY_LAST_ACCESSED, System.currentTimeMillis());

        return values;
    }

    private static ContentValues updatePostCount(Board board) {
        int count = board.postCount == 0 ? 1 : board.postCount + 1;

        ContentValues values = new ContentValues();
        values.put(Board.KEY_POST_COUNT, count);

        return values;
    }

    private static ContentValues updateAccessCount(Board board) {
        int count = board.accessCount == 0 ? 1 : board.accessCount + 1;

        ContentValues values = new ContentValues();
        values.put(Board.KEY_ACCESS_COUNT, count);

        return values;
    }

    public static Observable<Boolean> updateBoardOrder(final List<ChanBoard> boards) {
        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                int val;
                boolean success = false;
                BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
                BriteDatabase.Transaction transaction = db.newTransaction();
                ContentValues values = new ContentValues();

                try {
                    for (int i = 0; i < boards.size(); i++) {
                        values.put(Board.KEY_ORDER_INDEX, i);
                        val = db.update(Board.TABLE_NAME, values, Board.KEY_NAME + "=?", boards.get(i).getName());

                        success = val > 0;
                        values.clear();
                    }

                    transaction.markSuccessful();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error updating board order", e);
                } finally {
                    transaction.end();
                    return Observable.just(success).onErrorReturn(new Func1<Throwable, Boolean>() {
                        @Override
                        public Boolean call(Throwable throwable) {
                            Log.e(LOG_TAG, "Error updating board order", throwable);
                            return false;
                        }
                    });
                }

            }
        })
                .compose(DatabaseUtils.<Boolean>applySchedulers());
    }

    public static ChanBoard convertBoardDbModelToBoard(final Board oldBoard) {

        final ChanBoard newBoard = new ChanBoard();
        final String boardName = oldBoard.name; // Path is now Name
        final String boardTitle = oldBoard.title; // Name is now Title
        int ws = 1;
        if (oldBoard.nsfw) {
            ws = 0;
        }
        final boolean favorite = oldBoard.favorite;
        final int postsPerPage = oldBoard.perPage;
        final int pages = oldBoard.pages;

        newBoard.setName(boardName);
        newBoard.setTitle(boardTitle);
        newBoard.setWsBoard(ws);
        newBoard.setFavorite(favorite);
        newBoard.setPerPage(postsPerPage);
        newBoard.setPages(pages);

        return newBoard;
    }

    public static List<ChanBoard> convertBoardDbModelsToChanBoards(final List<Board> boards) {
        if (boards == null || boards.size() == 0) {
            return Collections.emptyList();
        }

        List<ChanBoard> chanBoards = new ArrayList<>(boards.size());
        for (Board board : boards) {
            chanBoards.add(convertBoardDbModelToBoard(board));
        }

        return chanBoards;
    }

    public static Board convertChanBoardToDbModel(ChanBoard chanBoard) {
        Board board = new Board();
        board.title = chanBoard.getTitle();
        board.name = chanBoard.getName();
        board.favorite = chanBoard.isFavorite();
        board.nsfw = chanBoard.getWsBoard() == 1;
        board.pages = chanBoard.getPages();
        board.perPage = chanBoard.getPerPage();
        board.maxFileSize = chanBoard.getMaxFilesize();

        return board;
    }

    public static List<ChanBoard> filterVisibleBoards(Context context, List<ChanBoard> boards) {
        final ArrayList<String> visibleBoardNames = new ArrayList<>();
        if (boards != null && boards.size() > 0) {
            final String[] boardsArray = context.getResources().getStringArray(R.array.boards);

            for (int i = 0; i < boardsArray.length; i++) {
                visibleBoardNames.add(boardsArray[i].replaceAll("/", ""));
            }

            List<ChanBoard> filteredBoards = new ArrayList<>(boards.size());
            for (ChanBoard board : boards) {
                if (visibleBoardNames.indexOf(board.getName()) >= 0) {
                    filteredBoards.add(board);
                }
            }


            return filteredBoards;

        }

        return Collections.emptyList();
    }

    public static Observable<List<Boolean>> initDefaultBoards(final Context context) {
        return Observable
                .defer(new Func0<Observable<String[]>>() {
                    @Override
                    public Observable<String[]> call() {
                        return Observable.just(context.getResources().getStringArray(R.array.boards));
                    }
                })
                .flatMapIterable(new Func1<String[], Iterable<String>>() {
                    @Override
                    public Iterable<String> call(String[] strings) {
                        return Arrays.asList(strings);
                    }
                })
                .flatMap(new Func1<String, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(String s) {
                        return setBoardVisibility(s, true);
                    }
                })
                .toList();
    }

    public static Action1<? extends BaseModel> saveToDatabase() {
        return new Action1<BaseModel>() {
            @Override
            public void call(BaseModel baseModel) {
                BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
                BriteDatabase.Transaction transaction = db.newTransaction();
                try {
                    DatabaseUtils.update(db, baseModel);
                    transaction.markSuccessful();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Database update failed", e);
                } finally {
                    transaction.end();
                }
            }
        };
    }

    public static Action1<List<ChanBoard>> saveBoards() {
        return new Action1<List<ChanBoard>>() {
            @Override
            public void call(List<ChanBoard> chanBoards) {
                saveBoards(chanBoards);
            }
        };
    }

    public static void saveBoards(List<ChanBoard> chanBoards) {
        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
        BriteDatabase.Transaction transaction = db.newTransaction();

        try {
            for (ChanBoard board : chanBoards) {
                int updateVal = DatabaseUtils.update(db, convertChanBoardToDbModel(board));
                Log.d(LOG_TAG, "Update return value: " + updateVal);

                if (updateVal == 0) {
                    DatabaseUtils.insert(db, convertChanBoardToDbModel(board));
                }
            }
            transaction.markSuccessful();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Database update failed", e);
        } finally {
            transaction.end();
        }
    }

    public static Observable<Boolean> setBoardVisibility(final String boardPath, final Boolean visible) {
        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
                BriteDatabase.Transaction transaction = db.newTransaction();

                int val = 0;

                try {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(Board.KEY_VISIBLE, visible);

                    String name = boardPath.replaceAll("/", "");

                    val = db.update(Board.TABLE_NAME, contentValues, Board.KEY_NAME + "=?", name);
                    transaction.markSuccessful();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Database update failed", e);
                } finally {
                    transaction.end();
                }

                return Observable.just(val > 0);
            }
        })
                .compose(DatabaseUtils.<Boolean>applySchedulers());
    }

}
