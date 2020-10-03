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

package com.emogoth.android.phone.mimi.activity;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.autorefresh.RefreshNotification;
import com.emogoth.android.phone.mimi.autorefresh.RefreshScheduler2;
import com.emogoth.android.phone.mimi.db.ArchiveTableConnection;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;
import com.emogoth.android.phone.mimi.db.RefreshQueueTableConnection;
import com.emogoth.android.phone.mimi.fourchan.FourChanConnector;
import com.emogoth.android.phone.mimi.util.HttpClientFactory;
import com.emogoth.android.phone.mimi.util.LayoutType;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.mimireader.chanlib.ChanConnector;
import com.mimireader.chanlib.models.ChanArchive;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.ObservableSource;
import io.reactivex.SingleObserver;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;


public class StartupActivity extends Activity {
    public static final String LOG_TAG = StartupActivity.class.getSimpleName();

    public static final LayoutType DEFAULT_LAYOUT_TYPE = LayoutType.TABBED;

    public static final String BROWSER_ACTIVITY = "browser";
    public static final String TABBED_ACTIVITY = "tabbed";
    public static final String SLIDING_PANEL_ACTIVITY = "sliding_panel";
    private final static Map<String, Class> startupActivities = new HashMap<>();

    static {
        startupActivities.put(BROWSER_ACTIVITY, PostItemListActivity.class);
        startupActivities.put(TABBED_ACTIVITY, TabsActivity.class);
        startupActivities.put(SLIDING_PANEL_ACTIVITY, SlidingPanelActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(RefreshNotification.NOTIFICATION_ID);

//        cleanAutoRefreshQueue();
        fetchArchivesFromNetwork();

        final String startActivityPref = getString(R.string.start_activity_pref);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        final String act = prefs.getString(startActivityPref, getDefaultStartupActivity());
        final Class c = startupActivities.get(act) == null ? startupActivities.get(getDefaultStartupActivity()) : startupActivities.get(act);

        final Bundle args = getIntent().getExtras();
        final Intent i = new Intent(this, c);

        if (args != null) {
            i.putExtras(args);
        }
        startActivity(i);

        finish();
    }

    public static String getDefaultStartupActivity() {
        return TABBED_ACTIVITY;
    }

    private void fetchArchivesFromNetwork() {
        ChanConnector chanConnector = new FourChanConnector
                .Builder()
                .setCacheDirectory(MimiUtil.getInstance().getCacheDir())
                .setEndpoint(FourChanConnector.getDefaultEndpoint())
                .setClient(HttpClientFactory.getInstance().getClient())
                .setPostEndpoint(FourChanConnector.getDefaultPostEndpoint())
                .build();

        chanConnector.fetchArchives()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io()) // We can observe on the background because we're not updating UI
                .doOnSuccess(chanArchives -> ArchiveTableConnection.clear()
                        .flatMap((Function<Boolean, SingleSource<Boolean>>) success ->
                                ArchiveTableConnection.putChanArchives(chanArchives))
                        .subscribe())
                .subscribe(new SingleObserver<List<ChanArchive>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        // no op
                    }

                    @Override
                    public void onSuccess(List<ChanArchive> chanArchives) {
                        Log.d(LOG_TAG, "Chan archives saved to database");
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e(LOG_TAG, "Error while processing archives", throwable);
                    }
                });
    }

    private void cleanAutoRefreshQueue() {
        RefreshQueueTableConnection.removeCompleted()
                .compose(DatabaseUtils.applySingleSchedulers())
                .subscribe(new SingleObserver<Integer>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onSuccess(@NonNull Integer integers) {

                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                });
    }
}
