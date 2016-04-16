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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.util.LayoutType;

import java.util.HashMap;
import java.util.Map;


public class StartupActivity extends Activity {

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
}
