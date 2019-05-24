package com.emogoth.android.phone.mimi.db;

import android.util.Log;

import com.activeandroid.query.Delete;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.emogoth.android.phone.mimi.app.MimiApplication;
import com.emogoth.android.phone.mimi.db.model.HiddenThread;
import com.squareup.sqlbrite3.BriteDatabase;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.functions.Function;

import static com.emogoth.android.phone.mimi.db.ActiveAndroidSqlBriteBridge.runQuery;

public class HiddenThreadTableConnection {

    public static final String LOG_TAG = HiddenThreadTableConnection.class.getSimpleName();

    public static Flowable<List<HiddenThread>> fetchHiddenThreads(String boardName) {
        From query = new Select()
                .all()
                .from(HiddenThread.class)
                .where(HiddenThread.BOARD_NAME + "=?", boardName);

        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();

        return db.createQuery(HiddenThread.TABLE_NAME, query.toSql(), query.getArguments())
                .toFlowable(BackpressureStrategy.BUFFER)
                .take(1)
                .map(runQuery())
                .flatMap(HiddenThread.mapper())
                .onErrorReturn(throwable -> {
                    Log.e(LOG_TAG, "Error loading hidden threads from the database", throwable);
                    return Collections.emptyList();
                });
    }

    public static Flowable<Boolean> hideThread(final String boardName, final long threadId, final boolean sticky) {
        return Flowable.defer((Callable<Flowable<Boolean>>) () -> {
            long val = 0;
            BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
            BriteDatabase.Transaction transaction = db.newTransaction();

            try {
                HiddenThread hiddenThread = new HiddenThread();
                hiddenThread.boardName = boardName;
                hiddenThread.threadId = threadId;
                hiddenThread.time = System.currentTimeMillis();
                hiddenThread.sticky = (byte) (sticky ? 1 : 0);

                val = DatabaseUtils.insert(db, hiddenThread);
                transaction.markSuccessful();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error hiding thread", e);
            } finally {
                transaction.end();
            }

            return Flowable.just(val > 0);
        })
                .onErrorReturn(throwable -> {
                    Log.w(LOG_TAG, "Thread could not be hidden: board=" + boardName + ", thread=" + threadId, throwable);
                    return false;
                });
    }

    public static Flowable<Boolean> clearAll() {
        From query = new Delete().from(HiddenThread.class);
        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
        return db.createQuery(HiddenThread.TABLE_NAME, query.toSql(), query.getArguments())
                .toFlowable(BackpressureStrategy.BUFFER)
                .take(1)
                .map(runQuery())
                .flatMap(HiddenThread.mapper())
                .flatMap((Function<List<HiddenThread>, Flowable<Boolean>>) hiddenThreads -> Flowable.just(true))
                .onErrorReturn(throwable -> {
                    Log.e(LOG_TAG, "Error clearing hidden threads", throwable);
                    return false;
                });
    }

    public static Flowable<Boolean> prune(final int days) {
        Long oldestTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days);
        From query = new Delete()
                .from(HiddenThread.class)
                .where(HiddenThread.TIME + "<?", oldestTime)
                .and(HiddenThread.STICKY + "=?", false);

        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
        return db.createQuery(HiddenThread.TABLE_NAME, query.toSql(), query.getArguments())
                .toFlowable(BackpressureStrategy.BUFFER)
                .take(1)
                .map(runQuery())
                .flatMap(HiddenThread.mapper())
                .flatMap((Function<List<HiddenThread>, Flowable<Boolean>>) hiddenThreads -> Flowable.just(true))
                .onErrorReturn(throwable -> {
                    Log.e(LOG_TAG, "Error pruning hidden threads from the last " + days + " days", throwable);
                    return false;
                });
    }
}
