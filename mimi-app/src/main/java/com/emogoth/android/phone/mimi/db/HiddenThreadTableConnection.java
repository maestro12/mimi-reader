package com.emogoth.android.phone.mimi.db;

import android.content.ContentValues;
import android.util.Log;

import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.emogoth.android.phone.mimi.app.MimiApplication;
import com.emogoth.android.phone.mimi.db.model.HiddenThread;
import com.emogoth.android.phone.mimi.db.model.History;
import com.squareup.sqlbrite.BriteDatabase;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;

import static com.emogoth.android.phone.mimi.db.ActiveAndroidSqlBriteBridge.runQuery;

public class HiddenThreadTableConnection {

    public static final String LOG_TAG = HiddenThreadTableConnection.class.getSimpleName();

    public static Observable<List<HiddenThread>> fetchHiddenThreads(String boardName) {
        From query = new Select()
                .all()
                .from(HiddenThread.class)
                .where(HiddenThread.BOARD_NAME + "=?", boardName);

        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();

        return db.createQuery(HiddenThread.TABLE_NAME, query.toSql(), query.getArguments())
                .take(1)
                .map(runQuery())
                .flatMap(HiddenThread.mapper())
                .onErrorReturn(new Func1<Throwable, List<HiddenThread>>() {
                    @Override
                    public List<HiddenThread> call(Throwable throwable) {
                        Log.e(LOG_TAG, "Error loading hidden threads from the database", throwable);
                        return Collections.emptyList();
                    }
                });
    }

    public static Observable<Boolean> hideThread(final String boardName, final int threadId, final boolean sticky) {
        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                long val = 0;
                BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
                BriteDatabase.Transaction transaction = db.newTransaction();
                ContentValues values = new ContentValues();

                try {
                    values.put(HiddenThread.BOARD_NAME, boardName);
                    values.put(HiddenThread.THREAD_ID, threadId);
                    values.put(HiddenThread.TIME, System.currentTimeMillis());
                    values.put(HiddenThread.STICKY, sticky);

                    val = db.insert(HiddenThread.TABLE_NAME, values);
                    Log.d(LOG_TAG, "Thread hidden: val=" + val);
                    transaction.markSuccessful();
                } catch (Exception e) {
                    Log.w(LOG_TAG, "Exception while hiding thread: board=" + boardName + ", thread=" + threadId, e);
                } finally {
                    transaction.end();
                }
                return Observable.just(val > 0);
            }
        })
                .onErrorReturn(new Func1<Throwable, Boolean>() {
                    @Override
                    public Boolean call(Throwable throwable) {
                        Log.w(LOG_TAG, "Thread could not be hidden: board=" + boardName + ", thread=" + threadId, throwable);
                        return false;
                    }
                });
    }

    public static Observable<Boolean> clearAll() {
        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
                BriteDatabase.Transaction transaction = db.newTransaction();
                long val = -1;

                try {
                    val = db.delete(HiddenThread.TABLE_NAME, null);
                    transaction.markSuccessful();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Caught exception while deleting all hidden threads", e);
                } finally {
                    transaction.end();
                }

                return Observable.just(val > 0);
            }
        })
                .onErrorReturn(new Func1<Throwable, Boolean>() {
                    @Override
                    public Boolean call(Throwable throwable) {
                        Log.e(LOG_TAG, "Error deleting all hidden threads", throwable);
                        return false;
                    }
                })
                .compose(DatabaseUtils.<Boolean>applySchedulers());
    }

    public static Observable<Boolean> prune(final int days) {
        return Observable.defer(new Func0<Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call() {
                BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
                BriteDatabase.Transaction transaction = db.newTransaction();
                long val = -1;

                try {
                    Long oldestTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
                    val = db.delete(HiddenThread.TABLE_NAME, HiddenThread.TIME + "<? and " + HiddenThread.STICKY + "=?",
                            oldestTime.toString(),
                            "0");

                    transaction.markSuccessful();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Caught exception while pruning hidden threads from the last " + days + " days", e);
                } finally {
                    transaction.end();
                }

                return Observable.just(val > 0);
            }
        })
                .onErrorReturn(new Func1<Throwable, Boolean>() {
                    @Override
                    public Boolean call(Throwable throwable) {
                        Log.e(LOG_TAG, "Error pruning hidden threads from the last " + days + " days", throwable);
                        return false;
                    }
                })
                .compose(DatabaseUtils.<Boolean>applySchedulers());
    }
}
