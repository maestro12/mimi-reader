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

package com.emogoth.android.phone.mimi.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.util.Log;

import com.emogoth.android.phone.mimi.event.RateAppEvent;


public class AppRater {
    private final static String APP_TITLE = "Mimi";// App Name
//    private final static String APP_PNAME = "com.example.name";// Package Name

    private static final int DAYS_UNTIL_PROMPT = 3;//Min number of days
    private static final int LAUNCHES_UNTIL_PROMPT = 5;//Min number of launches
    private static final String LOG_TAG = AppRater.class.getSimpleName();

    public static void appLaunched(final Context context) {

        final SharedPreferences prefs = context.getSharedPreferences("apprater", 0);
        final SharedPreferences.Editor editor = prefs.edit();
        final Integer storedVersion = prefs.getInt("currentversion", 0);
        Integer currentVersion = 0;

        try {
            final PackageInfo manager = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            currentVersion = manager.versionCode;
        } catch(final Exception e) {
            Log.e(LOG_TAG, "Could not get version code", e);
        }

        if(currentVersion > storedVersion) {
            editor.putInt("currentversion", currentVersion).apply();
//            editor.putBoolean("dontshowagain", false);
//            editor.commit();
        }

        if (prefs.getBoolean("dontshowagain", false)) { return ; }

        // Increment launch counter
        final long launchCount = prefs.getLong("launch_count", 0) + 1;
        editor.putLong("launch_count", launchCount);

        // Get date of first launch
        Long dateFirstLaunch = prefs.getLong("date_firstlaunch", 0);
        if (dateFirstLaunch == 0) {
            dateFirstLaunch = System.currentTimeMillis();
            editor.putLong("date_firstlaunch", dateFirstLaunch);
        }

        // Wait at least n days before opening
        if (launchCount >= LAUNCHES_UNTIL_PROMPT) {
            if (System.currentTimeMillis() >= dateFirstLaunch +
                    (DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000)) {

                final String applicationName = context.getPackageName();

                if(applicationName.contains("amazon")) {
                    editor.putBoolean("dontshowagain", true);
                }
                else {
                    final RateAppEvent event = new RateAppEvent();
                    event.setAction(RateAppEvent.OPEN);
                    BusProvider.getInstance().post(event);
                }
//                showRateDialog(mContext, editor);
            }
        }

        editor.apply();
    }

    public static SharedPreferences getAppRaterPrefs(final Context context) {
        return context.getSharedPreferences("apprater", 0);
    }

    public static void dontShowAgain(final Context context) {
        final SharedPreferences.Editor editor = context.getSharedPreferences("apprater", 0).edit();
        if (editor != null) {
            editor.putBoolean("dontshowagain", true).apply();
        }
    }
}
