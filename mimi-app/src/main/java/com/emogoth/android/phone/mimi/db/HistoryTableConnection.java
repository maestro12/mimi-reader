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
import android.text.TextUtils;
import android.util.Log;

import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.emogoth.android.phone.mimi.app.MimiApplication;
import com.emogoth.android.phone.mimi.db.model.History;
import com.mimireader.chanlib.models.ChanPost;
import com.squareup.sqlbrite.BriteDatabase;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;

import static com.emogoth.android.phone.mimi.db.ActiveAndroidSqlBriteBridge.runQuery;


public class HistoryTableConnection {
    public static final String LOG_TAG = HistoryTableConnection.class.getSimpleName();

    public static final int HISTORY = 1;
    public static final int BOOKMARKS = 2;

    public static Observable<History> fetchPost(final String boardName, final int threadId) {
        From query = new Select()
                .from(History.class)
                .where(History.KEY_NAME + "=?", boardName)
                .where(History.KEY_THREAD_ID + "=?", threadId);

        Log.d(LOG_TAG, "SQL=" + query.toSql());
        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
        return db.createQuery(History.TABLE_NAME, query.toSql(), query.getArguments())
                .take(1)
                .map(runQuery())
                .flatMap(History.mapper())
                .flatMap(new Func1<List<History>, Observable<History>>() {
                    @Override
                    public Observable<History> call(List<History> histories) {
                        if (histories == null || histories.size() == 0) {
                            return Observable.just(null);
                        }

                        return Observable.just(histories.get(0));
                    }
                })
                .onErrorResumeNext(new Func1<Throwable, Observable<History>>() {
                    @Override
                    public Observable<History> call(Throwable throwable) {
                        Log.e(LOG_TAG, "Error fetching history", throwable);
                        return Observable.just(null);
                    }
                });
    }

    public static Observable<List<History>> fetchHistory() {
        return fetchHistory(null);
    }

    public static Observable<List<History>> fetchHistory(final Boolean watched) {
        return fetchHistory(watched, 0);
    }

    public static Observable<List<History>> fetchHistory(final Boolean watched, final int count) {
        return fetchHistory(watched, count, null);
    }

    public static Observable<List<History>> fetchHistory(final Boolean watched, final int count, final Boolean showRemoved) {
        From query = new Select()
                .from(History.class)
                .orderBy(History.KEY_ORDER_ID + " ASC");

        if(showRemoved != null) {
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
                .take(1)
                .map(runQuery())
                .flatMap(History.mapper())
                .onErrorReturn(new Func1<Throwable, List<History>>() {
                    @Override
                    public List<History> call(Throwable throwable) {
                        Log.e(LOG_TAG, "Error fetching history: watched=" + watched, throwable);

                        return Collections.emptyList();
                    }
                })
                .compose(DatabaseUtils.<List<History>>applySchedulers());
    }

    public static Observable<Boolean> setHistoryRemovedStatus(final String boardName, final int threadId, final boolean removed) {
        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {

                BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
                BriteDatabase.Transaction transaction = db.newTransaction();

                int val = 0;

                try {
                    ContentValues values = new ContentValues();
                    values.put(History.KEY_THREAD_REMOVED, removed);

                    val = db.update(History.TABLE_NAME, values, History.KEY_THREAD_ID + "=?", String.valueOf(threadId));
                    transaction.markSuccessful();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error deleting history: name=" + boardName + ", thread=" + threadId, e);
                } finally {
                    transaction.end();
                }

                return Observable.just(val > 0)
                        .onErrorReturn(new Func1<Throwable, Boolean>() {
                            @Override
                            public Boolean call(Throwable throwable) {
                                Log.e(LOG_TAG, "Error deleting history: name=" + boardName + ", thread=" + threadId, throwable);
                                return false;
                            }
                        })
                        .compose(DatabaseUtils.<Boolean>applySchedulers());
            }
        });
    }

    public static Observable<Boolean> putHistory(final String boardName, final ChanPost firstPost, final Integer postCount, final boolean isWatched) {
        if (firstPost == null) {
            return Observable.just(false);
        }

        Log.d(LOG_TAG, "putHistory: name=" + boardName + ", thread=" + firstPost.getNo() + ", current watched=" + firstPost.isWatched() + ", new watched=" + isWatched);

        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
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
                    history.watched = isWatched;
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

                    val = db.update(History.TABLE_NAME, history.toContentValues(), history.whereClause(), history.whereArg());
                    if (val <= 0) {
                        val = db.insert(History.TABLE_NAME, history.toContentValues());
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

                return Observable.just(val > 0);
            }
        })
                .onErrorReturn(new Func1<Throwable, Boolean>() {
                    @Override
                    public Boolean call(Throwable throwable) {
                        Log.e(LOG_TAG, "Error adding history: name=" + boardName + ", thread=" + firstPost.getNo(), throwable);
                        return false;
                    }
                })
                .compose(DatabaseUtils.<Boolean>applySchedulers());
    }

    public static Observable<Boolean> removeHistory(final String boardName, final int threadId) {

        if (boardName == null || threadId <= 0) {
            return Observable.just(false);
        }

        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
                BriteDatabase.Transaction transaction = db.newTransaction();
                long val = -1;

                try {
                    val = db.delete(History.TABLE_NAME, History.KEY_THREAD_ID + "=?", String.valueOf(threadId));
                    transaction.markSuccessful();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error deleting history: name=" + boardName + ", thread=" + threadId, e);
                } finally {
                    transaction.end();
                }

                return Observable.just(val > 0);
            }
        })
                .onErrorReturn(new Func1<Throwable, Boolean>() {
                    @Override
                    public Boolean call(Throwable throwable) {
                        Log.e(LOG_TAG, "Error deleting history: name=" + boardName + ", thread=" + threadId, throwable);
                        return false;
                    }
                })
                .compose(DatabaseUtils.<Boolean>applySchedulers());
    }

    public static Observable<Boolean> removeAllHistory(final boolean watched) {
        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
                BriteDatabase.Transaction transaction = db.newTransaction();
                long val = -1;

                try {
                    val = db.delete(History.TABLE_NAME, History.KEY_WATCHED + "=?", String.valueOf(watched));
                    transaction.markSuccessful();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error deleting history: watched=" + watched, e);
                } finally {
                    transaction.end();
                }

                return Observable.just(val > 0);
            }
        })
                .onErrorReturn(new Func1<Throwable, Boolean>() {
                    @Override
                    public Boolean call(Throwable throwable) {
                        Log.e(LOG_TAG, "Error deleting history: watched=" + watched, throwable);
                        return false;
                    }
                })
                .compose(DatabaseUtils.<Boolean>applySchedulers());
    }

    public static Observable<Boolean> updateHistoryOrder(final List<History> historyList) {
        if (historyList == null || historyList.size() == 0) {
            Log.e(LOG_TAG, "Cannot update history order: history list is blank");
            return Observable.just(false);
        }

        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
                BriteDatabase.Transaction transaction = db.newTransaction();
                int val;
                boolean success = false;

                try {
                    ContentValues values = new ContentValues();

                    for (int i = 0; i < historyList.size(); i++) {
                        values.put(History.KEY_ORDER_ID, i);
                        val = db.update(History.TABLE_NAME, values, History.KEY_THREAD_ID + "=?", String.valueOf(historyList.get(i).threadId));
                        success = val > 0;

                        values.clear();
                    }
                    transaction.markSuccessful();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Failed to set the last read position", e);
                } finally {
                    transaction.end();
                }
                return Observable.just(success);
            }
        })
                .onErrorReturn(new Func1<Throwable, Boolean>() {
                    @Override
                    public Boolean call(Throwable throwable) {
                        Log.e(LOG_TAG, "Error updating history order: size=" + historyList.size(), throwable);
                        return false;
                    }
                })
                .compose(DatabaseUtils.<Boolean>applySchedulers());
    }

    public static Observable<Boolean> pruneHistory(final int days) {
        final Long oldestHistoryTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
                BriteDatabase.Transaction transaction = db.newTransaction();
                long val = -1;

                try {
                    val = db.delete(History.TABLE_NAME, History.KEY_LAST_ACCESS + "<? and " + History.KEY_WATCHED + "=?", oldestHistoryTime.toString(), "0");
                    transaction.markSuccessful();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error pruning history: last update=" + oldestHistoryTime, e);
                } finally {
                    transaction.end();
                }

                return Observable.just(val > 0);
            }
        })
                .onErrorReturn(new Func1<Throwable, Boolean>() {
                    @Override
                    public Boolean call(Throwable throwable) {
                        Log.e(LOG_TAG, "Error pruning history: last update=" + oldestHistoryTime, throwable);
                        return false;
                    }
                })
                .compose(DatabaseUtils.<Boolean>applySchedulers());
    }

    public static Observable<Boolean> setLastReadPost(final int threadId, final int pos) {
        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
                BriteDatabase.Transaction transaction = db.newTransaction();
                int val = 0;

                try {
                    ContentValues values = new ContentValues();
                    values.put(History.KEY_SIZE, pos);

                    val = db.update(History.TABLE_NAME, values, History.KEY_THREAD_ID + "=?", String.valueOf(threadId));
                    transaction.markSuccessful();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Failed to set the last read position", e);
                } finally {
                    transaction.end();
                }

                return Observable.just(val > 0);
            }
        })
                .onErrorReturn(new Func1<Throwable, Boolean>() {
                    @Override
                    public Boolean call(Throwable throwable) {
                        Log.e(LOG_TAG, "Failed to set the last read position", throwable);
                        return false;
                    }
                })
                .compose(DatabaseUtils.<Boolean>applySchedulers());
    }
}
