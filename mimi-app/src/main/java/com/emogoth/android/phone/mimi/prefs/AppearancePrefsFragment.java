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
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.emogoth.android.phone.mimi.R;


public class AppearancePrefsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.appearance_prefs);

        setupPrefs();
    }

    private void setupPrefs() {
        final ListPreference layoutTypePreference = (ListPreference) findPreference(getString(R.string.start_activity_pref));
        layoutTypePreference.setSummary(layoutTypePreference.getEntry());
        layoutTypePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                int index = 0;
                for (int i = 0; i < layoutTypePreference.getEntryValues().length; i++) {
                    if (layoutTypePreference.getEntryValues()[i].equals(o)) {
                        index = i;
                    }
                }

                layoutTypePreference.setSummary(layoutTypePreference.getEntries()[index]);

                return true;
            }
        });

        final ListPreference fontSizePref = (ListPreference) findPreference(getString(R.string.font_style_pref));
        fontSizePref.setSummary(fontSizePref.getEntry());
        fontSizePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                int index = 0;
                for (int i = 0; i < fontSizePref.getEntryValues().length; i++) {
                    if (fontSizePref.getEntryValues()[i].equals(o)) {
                        index = i;
                    }
                }

                fontSizePref.setSummary(fontSizePref.getEntries()[index]);

                return true;
            }
        });
    }
}
