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

package com.emogoth.android.phone.mimi.prefs;

import android.os.Bundle;
import android.util.Log;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.emogoth.android.phone.mimi.BuildConfig;
import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.util.MimiPrefs;

public class GeneralPrefsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.general_prefs);

        if (BuildConfig.DEBUG) {
            addPreferencesFromResource(R.xml.debug_prefs);
        }

        setupPrefs();

        Log.d("GeneralPrefsFragment", "name=" + this.getClass().getName());
    }

    private void setupPrefs() {
        final ListPreference bookmarkCount = (ListPreference) findPreference(getString(R.string.nav_drawer_bookmark_count_pref));
        bookmarkCount.setSummary(String.valueOf(MimiPrefs.navDrawerBookmarkCount(getActivity())));
        bookmarkCount.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String val = String.valueOf(newValue);
                bookmarkCount.setSummary(val);

                return true;
            }
        });
    }

}
