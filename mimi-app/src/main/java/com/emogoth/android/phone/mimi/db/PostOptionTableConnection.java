package com.emogoth.android.phone.mimi.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.activeandroid.query.Delete;
import com.activeandroid.query.From;
import com.activeandroid.query.Select;
import com.emogoth.android.phone.mimi.app.MimiApplication;
import com.emogoth.android.phone.mimi.db.model.PostOption;
import com.squareup.sqlbrite3.BriteDatabase;

import java.util.List;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;

import static com.emogoth.android.phone.mimi.db.ActiveAndroidSqlBriteBridge.runQuery;

public class PostOptionTableConnection {
    public static final String LOG_TAG = PostOptionTableConnection.class.getSimpleName();

    public static Single<List<PostOption>> fetchPostOptions() {
        return DatabaseUtils.fetchTable(PostOption.class, PostOption.TABLE_NAME);
//        From query = new Select()
//                .all()
//                .from(PostOption.class);
//
//        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
//        return db.createQuery(PostOption.TABLE_NAME, query.toSql(), (Object[]) query.getArguments())
//                .take(1)
//                .map(runQuery())
//                .flatMap(PostOption.mapper())
//                .onErrorReturn(new Function<Throwable, List<PostOption>>() {
//                    @Override
//                    public List<PostOption> call(Throwable throwable) {
//                        return new ArrayList<>();
//                    }
//                })
//                .compose(DatabaseUtils.<List<PostOption>>applySchedulers());

    }

    public static Flowable<Boolean> deletePostOption(final String id) {
        From query = new Delete()
                .from(PostOption.class)
                .where(PostOption.OPTION + "=?", id);

        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
        return db.createQuery(PostOption.TABLE_NAME, query.toSql(), (Object[]) query.getArguments())
                .toFlowable(BackpressureStrategy.BUFFER)
                .take(1)
                .map(runQuery())
                .flatMap(new Function<Cursor, Flowable<Boolean>>() {
                    @Override
                    public Flowable<Boolean> apply(Cursor cursor) {
                        return Flowable.just(cursor != null && cursor.getCount() == 0);
                    }
                })
                .onErrorReturn(new Function<Throwable, Boolean>() {
                    @Override
                    public Boolean apply(Throwable throwable) {
                        Log.e(LOG_TAG, "Error deleting post: id=" + id, throwable);
                        return false;
                    }
                })
                .compose(DatabaseUtils.<Boolean>applySchedulers());

    }

    public static Flowable<Boolean> putPostOption(final String option) {
        From query = new Select()
                .all()
                .from(PostOption.class)
                .where(PostOption.OPTION + "=?", option);

        Log.d(LOG_TAG, "SQL=" + query.toSql());

        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
        return db.createQuery(PostOption.TABLE_NAME, query.toSql(), (Object[]) query.getArguments())
                .toFlowable(BackpressureStrategy.BUFFER)
                .take(1)
                .map(runQuery())
                .flatMap((Function<Cursor, Flowable<PostOption>>) cursor -> {
                    PostOption postOption = null;
                    while (cursor.moveToNext()) {
                        postOption = new PostOption();
                        postOption.loadFromCursor(cursor);
                    }

                    return Flowable.just(postOption);
                })
                .flatMap((Function<PostOption, Flowable<Boolean>>) postOption -> {

                    if (postOption == null) {
                        return insertPostOption(option);
                    }

                    return updatePostOption(postOption, true);
                })
                .onErrorReturn(throwable -> {
                    Log.e(LOG_TAG, "Error updating access count", throwable);
                    return false;
                })
                .compose(DatabaseUtils.applySchedulers());
    }

    private static Flowable<Boolean> insertPostOption(String option) {
        PostOption postOption = new PostOption();
        postOption.option = option;
        postOption.lastUsed = System.currentTimeMillis();

        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
        BriteDatabase.Transaction transaction = db.newTransaction();
        long val = 0;
        try {
            val = db.insert(PostOption.TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, postOption.toContentValues());
            transaction.markSuccessful();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error putting post options into the database", e);
        }
        finally {
            transaction.end();
        }

        return Flowable.just(val > 0);
    }

    private static Flowable<Boolean> updatePostOption(PostOption option, boolean incrementCount) {
        option.lastUsed = System.currentTimeMillis();
        if (incrementCount) {
            option.usedCount += 1;
        }

        BriteDatabase db = MimiApplication.getInstance().getBriteDatabase();
        BriteDatabase.Transaction transaction = db.newTransaction();
        int val = 0;
        try {
            val = db.update(PostOption.TABLE_NAME, SQLiteDatabase.CONFLICT_REPLACE, option.toContentValues(), option.clause(), option.vals());
            transaction.markSuccessful();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error putting post options into the database", e);
        }
        finally {
            transaction.end();
        }

        return Flowable.just(val > 0);
    }
}
