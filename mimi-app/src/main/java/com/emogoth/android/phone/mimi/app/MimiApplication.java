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

package com.emogoth.android.phone.mimi.app;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.activeandroid.ActiveAndroid;
import com.activeandroid.Configuration;
import com.emogoth.android.phone.mimi.BuildConfig;
import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.autorefresh.RefreshScheduler;
import com.emogoth.android.phone.mimi.db.ActiveAndroidSqlBriteBridge;
import com.emogoth.android.phone.mimi.db.HiddenThreadTableConnection;
import com.emogoth.android.phone.mimi.db.UserPostTableConnection;
import com.emogoth.android.phone.mimi.db.model.Board;
import com.emogoth.android.phone.mimi.db.model.CatalogPostModel;
import com.emogoth.android.phone.mimi.db.model.Filter;
import com.emogoth.android.phone.mimi.db.model.HiddenThread;
import com.emogoth.android.phone.mimi.db.model.History;
import com.emogoth.android.phone.mimi.db.model.PostModel;
import com.emogoth.android.phone.mimi.db.model.PostOption;
import com.emogoth.android.phone.mimi.db.model.UserPost;
import com.emogoth.android.phone.mimi.util.BusProvider;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.ThreadRegistry;
import com.novoda.simplechromecustomtabs.SimpleChromeCustomTabs;
import com.squareup.leakcanary.RefWatcher;
import com.squareup.sqlbrite3.BriteDatabase;

import java.io.File;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;


public class MimiApplication extends Application {

    private static final String LOG_TAG = MimiApplication.class.getSimpleName();
    private static MimiApplication app;
    private BriteDatabase briteDatabase;

    private RefWatcher refWatcher = null;

    @SuppressLint("CheckResult")
    @Override
    public void onCreate() {
        super.onCreate();
        app = this;

        MimiUtil.getInstance().init(this);

        @SuppressWarnings("unchecked")
        Configuration.Builder configurationBuilder = new Configuration.Builder(this)
                .addModelClasses(
                        Board.class,
                        History.class,
                        UserPost.class,
                        HiddenThread.class,
                        PostOption.class,
                        Filter.class,
                        PostModel.class,
                        CatalogPostModel.class
                );

        ActiveAndroid.initialize(configurationBuilder.create());

        try {
            final File fullImageDir = new File(MimiUtil.getInstance().getCacheDir().getAbsolutePath(), "full_images/");
            MimiUtil.deleteRecursive(fullImageDir, false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final int notificationLevel = Integer.valueOf(preferences.getString(getString(R.string.background_notification_pref), "0"));
        final boolean defaultSet = preferences.getBoolean(getString(R.string.crappy_samsung_default_set), false);

        if (!defaultSet) {
            boolean useCrappyVideoPlayer = MimiUtil.isCrappySamsung();
            preferences.edit()
                    .putBoolean(getString(R.string.crappy_samsung_default_set), true)
                    .putBoolean(getString(R.string.use_crappy_video_player), useCrappyVideoPlayer)
                    .apply();
        }

        if (notificationLevel == 0) {
            preferences.edit().putString(getString(R.string.background_notification_pref), "3").apply();
        }

        final int historyPruneDays = Integer.valueOf(preferences.getString(getString(R.string.history_prune_time_pref), "0"));

        MimiUtil.pruneHistory(historyPruneDays)
        .flatMap((Function<Boolean, Flowable<Boolean>>) aBoolean -> HiddenThreadTableConnection.prune(5))
        .flatMap((Function<Boolean, Flowable<Boolean>>) aBoolean -> UserPostTableConnection.prune(7))
        .subscribe(aBoolean -> {
            if (aBoolean) {
                Log.d(LOG_TAG, "Pruned history");
            } else {
                Log.e(LOG_TAG, "Failed to prune history");
            }
        }, throwable -> Log.e(LOG_TAG, "Caught exception while setting up database", throwable));

        ThreadRegistry.getInstance().init();
        BusProvider.getInstance();
        RefreshScheduler.getInstance();

        SimpleChromeCustomTabs.initialize(this);
    }

    public BriteDatabase getBriteDatabase() {
        if (briteDatabase == null) {
            briteDatabase = ActiveAndroidSqlBriteBridge.getBriteDatabase();

//            if (briteDatabase != null && BuildConfig.DEBUG) {
//                briteDatabase.setLoggingEnabled(true);
//            }
        }

        return briteDatabase;
    }

    public RefWatcher getRefWatcher() {
        return refWatcher;
    }

    public void setRefWatcher(RefWatcher refWatcher) {
        this.refWatcher = refWatcher;
    }

    public static MimiApplication getInstance() {
        return app;
    }
}
