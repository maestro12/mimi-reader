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
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.activeandroid.query.Delete;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.emogoth.android.phone.mimi.app.MimiApplication;
import com.emogoth.android.phone.mimi.db.model.History;
import com.mimireader.chanlib.models.ChanPost;
import com.squareup.sqlbrite3.BriteDatabase;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;

import static com.emogoth.android.phone.mimi.db.ActiveAndroidSqlBriteBridge.runQuery;


public class HistoryTableConnection {
    public static final String LOG_TAG = HistoryTableConnection.class.getSimpleName();

    public static final int HISTORY = 1;
    public static final int BOOKMARKS = 2;

    public static Flowable<History> fetchPost(final String boardName, final long threadId) {
        return watchThread(boardName, threadId).take(1);
    }

    public static Flowable<History> watchThread(final String boardName, final long threadId) {
        From query = new Select()
                .from(History.class)
                .where(History.KEY_NAME + "=?", boardName)
                .where(History.KEY_THREAD_ID + "=?", threadId);

        Log.d(LOG_TAG, "SQL=" + query.toSql());
        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
        return db.createQuery(History.TABLE_NAME, query.toSql(), query.getArguments())
                .toFlowable(BackpressureStrategy.BUFFER)
                .map(runQuery())
                .flatMap(History.mapper())
                .flatMap((Function<List<History>, Flowable<History>>) histories -> {
                    if (histories == null || histories.size() == 0) {
                        return Flowable.just(new History());
                    }

                    return Flowable.just(histories.get(0));
                })
                .onErrorResumeNext((Function<Throwable, Flowable<History>>) throwable -> {
                    Log.e(LOG_TAG, "Error fetching history", throwable);
                    return Flowable.just(new History());
                });
    }

    public static Flowable<List<History>> fetchHistory() {
        return fetchHistory(null);
    }

    public static Flowable<List<History>> fetchHistory(final Boolean watched) {
        return fetchHistory(watched, 0);
    }

    public static Flowable<List<History>> fetchHistory(final Boolean watched, final int count) {
        return fetchHistory(watched, count, null);
    }

    public static Flowable<List<History>> fetchHistory(final Boolean watched, final int count, final Boolean showRemoved) {
        From query = new Select()
                .from(History.class)
                .orderBy(History.KEY_ORDER_ID + " ASC");

        if (showRemoved != null) {
            query = query.where(History.KEY_THREAD_REMOVED + "=?", showRemoved);
        }
        if (watched != null) {
            query = query.where(History.KEY_WATCHED + "=?", watched);
        }
        if (count > 0) {
            query = query.limit(count);
        }

        Log.d(LOG_TAG, "SQL=" + query.toSql());
        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
        return db.createQuery(History.TABLE_NAME, query.toSql(), query.getArguments())
                .toFlowable(BackpressureStrategy.BUFFER)
                .take(1)
                .map(runQuery())
                .flatMap(History.mapper())
                .onErrorReturn(throwable -> {
                    Log.e(LOG_TAG, "Error fetching history: watched=" + watched, throwable);

                    return Collections.emptyList();
                })
                .compose(DatabaseUtils.applySchedulers());
    }

    public static Flowable<Boolean> setHistoryRemovedStatus(final String boardName, final long threadId, final boolean removed) {
        return Flowable.defer((Callable<Flowable<Boolean>>) () -> {

            BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
            BriteDatabase.Transaction transaction = db.newTransaction();

            int val = 0;

            try {
                ContentValues values = new ContentValues();
                values.put(History.KEY_THREAD_REMOVED, removed);

                val = db.update(History.TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, values, History.KEY_THREAD_ID + "=?", String.valueOf(threadId));
                transaction.markSuccessful();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error deleting history: name=" + boardName + ", thread=" + threadId, e);
            } finally {
                transaction.end();
            }

            return Flowable.just(val > 0)
                    .onErrorReturn(throwable -> {
                        Log.e(LOG_TAG, "Error deleting history: name=" + boardName + ", thread=" + threadId, throwable);
                        return false;
                    })
                    .compose(DatabaseUtils.<Boolean>applySchedulers());
        });
    }

    public static Flowable<Boolean> putHistory(final String boardName, final ChanPost firstPost, final Integer postCount, final int lastReadPosition, final boolean isWatched) {
        if (firstPost == null) {
            return Flowable.just(false);
        }

        Log.d(LOG_TAG, "putHistory: name=" + boardName + ", thread=" + firstPost.getNo() + ", current watched=" + firstPost.isWatched() + ", new watched=" + isWatched);

        return Flowable.defer((Callable<Flowable<Boolean>>) () -> {
            BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
            BriteDatabase.Transaction transaction = db.newTransaction();
            long val = -1;

            try {
                final History history = new History();
                history.boardName = boardName;
                history.threadId = firstPost.getNo();
                history.userName = firstPost.getName();
                history.tim = firstPost.getTim();
                history.threadSize = postCount;
                history.lastReadPosition = lastReadPosition;
                history.watched = (byte) (isWatched ? 1 : 0);
                history.orderId = 0;

                final String text;
                if (!TextUtils.isEmpty(firstPost.getSubject())) {
                    text = firstPost.getSubject().toString();
                } else if (!TextUtils.isEmpty(firstPost.getCom())) {
                    text = firstPost.getCom();
                } else {
                    text = null;
                }
                history.text = text;
                history.lastAccess = System.currentTimeMillis();

                val = db.update(History.TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, history.toContentValues(), history.clause(), history.vals());
                if (val <= 0) {
                    val = db.insert(History.TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, history.toContentValues());
                    Log.d(LOG_TAG, "Added history: name=" + boardName + ", thread=" + firstPost.getNo() + ", watched=" + history.watched);
                } else {
                    Log.d(LOG_TAG, "Updated history: name=" + boardName + ", thread=" + firstPost.getNo() + ", watched=" + history.watched);
                }

                transaction.markSuccessful();

            } catch (Exception e) {
                Log.e(LOG_TAG, "Error adding history: name=" + boardName + ", thread=" + firstPost.getNo(), e);
            } finally {
                transaction.end();
            }

            return Flowable.just(val > 0);
        })
                .onErrorReturn(throwable -> {
                    Log.e(LOG_TAG, "Error adding history: name=" + boardName + ", thread=" + firstPost.getNo(), throwable);
                    return false;
                });
    }

    public static Flowable<Boolean> removeHistory(final String boardName, final long threadId) {

        if (boardName == null || threadId <= 0) {
            return Flowable.just(false);
        }

        From query = new Delete().from(History.class).where(History.KEY_THREAD_ID + "=?", threadId);
        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
        return db.createQuery(History.TABLE_NAME, query.toSql(), query.getArguments())
                .toFlowable(BackpressureStrategy.BUFFER)
                .take(1)
                .map(runQuery())
                .flatMap(History.mapper())
                .flatMap(validateHistoryDeleted())
                .onErrorReturn(throwable -> {
                    Log.e(LOG_TAG, "Error deleting history: name=" + boardName + ", thread=" + threadId, throwable);
                    return false;
                });
    }

    public static Flowable<Boolean> removeAllHistory(final boolean watched) {
        From query = new Delete().from(History.class).where(History.KEY_WATCHED + "=?", watched);
        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
        return db.createQuery(History.TABLE_NAME, query.toSql(), query.getArguments())
                .toFlowable(BackpressureStrategy.BUFFER)
                .take(1)
                .map(runQuery())
                .flatMap(History.mapper())
                .flatMap(validateHistoryDeleted())
                .onErrorReturn(throwable -> {
                    Log.e(LOG_TAG, "Error deleting all history", throwable);
                    return false;
                });
    }

    public static Flowable<Boolean> updateHistoryOrder(final List<History> historyList) {
        if (historyList == null || historyList.size() == 0) {
            Log.e(LOG_TAG, "Cannot update history order: history list is blank");
            return Flowable.just(false);
        }

        return Flowable.defer((Callable<Flowable<List<History>>>) () -> Flowable.just(historyList))
                .flatMapIterable((Function<List<History>, Iterable<History>>) histories -> {
            for (int i = 0; i < histories.size(); i++) {
                histories.get(i).orderId = i;
            }
            return histories;
        }).flatMap((Function<History, Flowable<Boolean>>) history -> updateHistory(history))
                .onErrorReturn(throwable -> {
            Log.e(LOG_TAG, "Error updating history order: size=" + historyList.size(), throwable);
            return false;
        });
    }

    public static Flowable<Boolean> updateHistory(final History history) {
        return DatabaseUtils.updateTable(history);
    }

    public static Flowable<Boolean> pruneHistory(final int days) {
        final Long oldestHistoryTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
        From query = new Delete()
                .from(History.class)
                .where(History.KEY_LAST_ACCESS + "<?", oldestHistoryTime)
                .and(History.KEY_WATCHED + "=?", false);

        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
        return db.createQuery(History.TABLE_NAME, query.toSql(), query.getArguments())
                .toFlowable(BackpressureStrategy.BUFFER)
                .take(1)
                .map(runQuery())
                .flatMap(History.mapper())
                .flatMap(validateHistoryDeleted())
                .onErrorReturn(throwable -> {
                    Log.e(LOG_TAG, "Error deleting all history", throwable);
                    return false;
                });
    }

    public static Flowable<List<History>> getHistoryToPrune(final int days) {
        final Long oldestHistoryTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
        From query = new Select()
                .from(History.class)
                .where(History.KEY_LAST_ACCESS + "<?", oldestHistoryTime)
                .and(History.KEY_WATCHED + "=?", false);

        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
        return db.createQuery(History.TABLE_NAME, query.toSql(), query.getArguments())
                .toFlowable(BackpressureStrategy.BUFFER)
                .take(1)
                .map(runQuery())
                .flatMap(History.mapper())
                .onErrorReturn(throwable -> {
                    Log.e(LOG_TAG, "Error deleting all history", throwable);
                    return Collections.emptyList();
                });
    }

    public static Flowable<Boolean> setLastReadPost(final long threadId, final int pos) {
        From query = new Select().from(History.class).where(History.KEY_THREAD_ID + "=?", threadId);
        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
        return db.createQuery(History.TABLE_NAME, query.toSql(), query.getArguments())
                .toFlowable(BackpressureStrategy.BUFFER)
                .take(1)
                .map(runQuery())
                .flatMap(History.mapper())
                .flatMapIterable((Function<List<History>, Iterable<History>>) histories -> histories)
                .flatMap((Function<History, Flowable<Boolean>>) history -> {
                    history.lastReadPosition = pos;
                    return DatabaseUtils.insertOrUpdateRow(history);
                })
                .onErrorReturn(throwable -> {
                    Log.e(LOG_TAG, "Failed to set the last read position", throwable);
                    return false;
                })
                .compose(DatabaseUtils.applySchedulers());

    }

    private static Function<List<History>, Flowable<Boolean>> validateHistoryDeleted() {
        return histories -> Flowable.just(histories != null && histories.size() == 0);
    }
}
