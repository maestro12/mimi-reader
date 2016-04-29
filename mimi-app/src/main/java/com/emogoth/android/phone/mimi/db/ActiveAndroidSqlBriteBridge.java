package com.emogoth.android.phone.mimi.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;

import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;

import java.lang.reflect.Field;

import rx.functions.Func1;
import rx.schedulers.Schedulers;


public class ActiveAndroidSqlBriteBridge {

    // From: https://github.com/pardom/ActiveAndroid/issues/153
    private static SQLiteOpenHelper getSQLiteOpenHelper() {
        try {
            Field dhField = com.activeandroid.Cache.class.getDeclaredField("sDatabaseHelper");
            dhField.setAccessible(true);
            SQLiteOpenHelper helper = (SQLiteOpenHelper) dhField.get(null);
            if (helper == null) {
                throw new IllegalStateException("Could not get SQLiteOpenHelper from Active Android");
            } else {
                return helper;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static BriteDatabase getBriteDatabase() {
        SQLiteOpenHelper helper = getSQLiteOpenHelper();

        if (helper != null) {
            return SqlBrite.create().wrapDatabaseHelper(helper, Schedulers.io());
        }

        return null;
    }

    public static Func1<SqlBrite.Query, Cursor> runQuery() {
        return new Func1<SqlBrite.Query, Cursor>() {
            @Override
            public Cursor call(SqlBrite.Query query) {
                return query.run();
            }
        };
    }
}
