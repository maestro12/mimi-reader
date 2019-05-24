package com.emogoth.android.phone.mimi.db;

import androidx.sqlite.db.SupportSQLiteOpenHelper;
import android.database.Cursor;

import com.squareup.sqlbrite3.BriteDatabase;
import com.squareup.sqlbrite3.SqlBrite;

import java.lang.reflect.Field;

import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;


public class ActiveAndroidSqlBriteBridge {

    // From: https://github.com/pardom/ActiveAndroid/issues/153
    private static SupportSQLiteOpenHelper getSQLiteOpenHelper() {
        try {
            Field dhField = com.activeandroid.Cache.class.getDeclaredField("sDatabaseHelper");
            dhField.setAccessible(true);
            SupportSQLiteOpenHelper helper = (SupportSQLiteOpenHelper) dhField.get(null);
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
        SupportSQLiteOpenHelper helper = getSQLiteOpenHelper();

        if (helper != null) {
            SqlBrite.Builder builder = new SqlBrite.Builder();
            SqlBrite sqlBrite = builder.build();
            return sqlBrite.wrapDatabaseHelper(helper, Schedulers.io());
        }

        return null;
    }

    public static Function<SqlBrite.Query, Cursor> runQuery() {
        return SqlBrite.Query::run;
    }
}
