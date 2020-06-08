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
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.app.MimiApplication;
import com.emogoth.android.phone.mimi.db.model.BaseModel;
import com.emogoth.android.phone.mimi.db.model.Board;
import com.mimireader.chanlib.models.ChanBoard;
import com.squareup.sqlbrite3.BriteDatabase;

import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

public class BoardTableConnection {
    public static final String LOG_TAG = BoardTableConnection.class.getSimpleName();

    public static Single<List<Board>> fetchBoards(int orderBy) {
        return DatabaseUtils.fetchTable(Board.class, Board.TABLE_NAME, Board.sortOrder(orderBy), Board.KEY_VISIBLE + "=?", true);
    }

    public static Flowable<List<ChanBoard>> observeBoards(int orderBy) {
        return DatabaseUtils.observeTable(Board.class, Board.TABLE_NAME, Board.sortOrder(orderBy), Board.KEY_VISIBLE + "=?", true)
                .flatMap((Function<List<Board>, Publisher<List<ChanBoard>>>) boards -> Flowable.just(convertBoardDbModelsToChanBoards(boards)));
    }

    public static Single<ChanBoard> fetchBoard(final String boardName) {
        if (TextUtils.isEmpty(boardName)) {
            return Single.just(new ChanBoard());
        }
        return DatabaseUtils.fetchTable(Board.class, Board.TABLE_NAME, null, Board.KEY_NAME + "=?", boardName)
                .flatMap((Function<List<Board>, Single<ChanBoard>>) boards -> {
                    if (boards != null && boards.size() > 0) {
                        return Single.just(convertBoardDbModelToBoard(boards.get(0)));
                    }
                    return Single.just(new ChanBoard());
                });
    }

    public static Single<Boolean> setBoardFavorite(final String boardName, final boolean favorite) {
        Board b = new Board();
        b.name = boardName;

        Board.Builder builder = new Board.Builder();
        builder.favorite(favorite);

        b.setValues(builder.build());

        return DatabaseUtils.updateTable(b);
    }

    public static Single<Boolean> incrementAccessCount(final String boardName) {
        return updateBoard(boardName, 2);
    }

    public static Single<Boolean> incrementPostCount(final String boardName) {
        return updateBoard(boardName, 1);
    }

    public static Single<Boolean> updateLastAccess(final String boardName) {
        return updateBoard(boardName, 0);
    }

    private static Single<Boolean> updateBoard(final String boardName, final int type) {
        return DatabaseUtils.fetchTable(Board.class, Board.TABLE_NAME, null, Board.KEY_NAME + "=?", boardName)
                .flatMap((Function<List<Board>, Single<Boolean>>) boards -> {
                    if (boards != null && boards.size() > 0) {
                        Board board = boards.get(0);
                        ContentValues values = null;
                        if (type == 0) {
                            values = updateLastAccessed();
                        } else if (type == 1) {
                            values = updatePostCount(board);
                        } else if (type == 2) {
                            values = updateAccessCount(board);
                        }

                        if (values != null) {
                            board.setValues(values);
                            return DatabaseUtils.updateTable(board);
                        }
                    }

                    return Single.just(false);
                });
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

    public static Single<Boolean> resetStats() {
        return Single.defer((Callable<SingleSource<Boolean>>) () -> {
            BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
            BriteDatabase.Transaction transaction = db.newTransaction();

            ContentValues values = new ContentValues();

            values.put(Board.KEY_ACCESS_COUNT, 0);
            values.put(Board.KEY_LAST_ACCESSED, 0);
            values.put(Board.KEY_POST_COUNT, 0);

            int val = 0;
            try {
                val = db.update(Board.TABLE_NAME, SQLiteDatabase.CONFLICT_IGNORE, values, null, null);
                transaction.markSuccessful();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error putting post options into the database", e);
            } finally {
                transaction.end();
            }

            return Single.just(val > 0);
        });

    }

    public static Flowable<Boolean> updateBoardOrder(final List<ChanBoard> boards) {
        return Flowable.defer((Callable<Flowable<Boolean>>) () -> {
            int val;
            boolean success = false;
            BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
            BriteDatabase.Transaction transaction = db.newTransaction();
            ContentValues values = new ContentValues();

            try {
                for (int i = 0; i < boards.size(); i++) {
                    values.put(Board.KEY_ORDER_INDEX, i);
                    val = db.update(Board.TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values, Board.KEY_NAME + "=?", boards.get(i).getName());

                    success = val > 0;
                    values.clear();
                }

                transaction.markSuccessful();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error updating board order", e);
            } finally {
                transaction.end();
                return Flowable.just(success).onErrorReturn(throwable -> {
                    Log.e(LOG_TAG, "Error updating board order", throwable);
                    return false;
                });
            }

        })
                .compose(DatabaseUtils.applySchedulers());
    }

    public static ChanBoard convertBoardDbModelToBoard(final Board oldBoard) {

        final ChanBoard newBoard = new ChanBoard();
        final String boardName = oldBoard.name; // Path is now Name
        final String boardTitle = oldBoard.title; // Name is now Title
        int ws = 1;
        if (oldBoard.nsfw == 1) {
            ws = 0;
        }
        final boolean favorite = oldBoard.favorite == 1;
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
        board.favorite = (byte) (chanBoard.isFavorite() ? 1 : 0);
        board.nsfw = (byte) (chanBoard.getWsBoard() == 1 ? 1 : 0);
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

    public static Single<List<ChanBoard>> initDefaultBoards(final Context context) {
        return Single
                .defer((Callable<Single<String[]>>) () -> Single.just(context.getResources().getStringArray(R.array.boards)))
                .map(strings -> {
                    List<ChanBoard> boards = new ArrayList<>(strings.length);
                    for (String boardName : strings) {
                        ChanBoard board = new ChanBoard();
                        board.setName(boardName);
                        board.setTitle("");
                        board.setWsBoard(1);
                        boards.add(board);
                    }
                    return boards;
                })
                .doOnSuccess(BoardTableConnection.saveBoards())
                .toFlowable()
                .flatMapIterable((Function<List<ChanBoard>, Iterable<ChanBoard>>) board -> board)
                .flatMap((Function<ChanBoard, Flowable<ChanBoard>>) s -> setBoardVisibility(s.getName(), true).toFlowable())
                .toList();
    }

    public static Consumer<? extends BaseModel> saveToDatabase() {
        return (Consumer<BaseModel>) baseModel -> {
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
        };
    }

    public static Consumer<List<ChanBoard>> saveBoards() {
        return BoardTableConnection::saveBoards;
    }

    public static void saveBoards(List<ChanBoard> chanBoards) {
        if (chanBoards == null || chanBoards.size() == 0) {
            return;
        }

        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
        BriteDatabase.Transaction transaction = db.newTransaction();

        try {
            for (ChanBoard board : chanBoards) {
                int updateVal = DatabaseUtils.update(db, convertChanBoardToDbModel(board));

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

    public static Single<ChanBoard> setBoardVisibility(final String boardPath, final Boolean visible) {
        if (TextUtils.isEmpty(boardPath)) {
            return Single.just(new ChanBoard());
        }

        return Single.defer((Callable<Single<Boolean>>) () -> {
            BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
            BriteDatabase.Transaction transaction = db.newTransaction();

            int val = 0;

            try {
                ContentValues contentValues = new ContentValues();
                contentValues.put(Board.KEY_VISIBLE, visible ? 1 : 0);

                String name = boardPath.replaceAll("/", "");
                val = db.update(Board.TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, contentValues, Board.KEY_NAME + "=?", name);

                transaction.markSuccessful();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Database update failed", e);
            } finally {
                transaction.end();
            }

            return Single.just(val > 0);
        })
                .flatMap((Function<Boolean, Single<ChanBoard>>) success -> {
                    if (success) {
                        return fetchBoard(boardPath);
                    }

                    return Single.just(new ChanBoard());
                })
                .compose(DatabaseUtils.applySingleSchedulers());
    }

}
