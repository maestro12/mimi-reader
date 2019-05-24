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
import android.preference.SwitchPreference;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.db.HistoryTableConnection;
import com.emogoth.android.phone.mimi.autorefresh.RefreshScheduler;
import com.emogoth.android.phone.mimi.db.PostTableConnection;
import com.emogoth.android.phone.mimi.db.model.History;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.RxUtil;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.SingleSource;
import io.reactivex.functions.Function;


public class HistoryPrefsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.history_prefs);

        setupPrefs();
    }

    private void setupPrefs() {
        final ListPreference refreshInterval = (ListPreference) findPreference(getString(R.string.app_auto_refresh_time));
        refreshInterval.setSummary(refreshInterval.getEntry());
        refreshInterval.setOnPreferenceChangeListener((preference, newValue) -> {
            int index = 0;
            for (int i = 0; i < refreshInterval.getEntryValues().length; i++) {
                if (refreshInterval.getEntryValues()[i].equals(newValue)) {
                    index = i;
                }
            }

            refreshInterval.setSummary(refreshInterval.getEntries()[index]);

            RefreshScheduler.getInstance().setInterval(Integer.valueOf(newValue.toString()));
            RefreshScheduler.getInstance().scheduleNextRun();

            return true;
        });

        final ListPreference backgroundRefreshInterval = (ListPreference) findPreference(getString(R.string.background_auto_refresh_time));
        backgroundRefreshInterval.setSummary(backgroundRefreshInterval.getEntry());
        backgroundRefreshInterval.setOnPreferenceChangeListener((preference, newValue) -> {
            int index = 0;
            for (int i = 0; i < backgroundRefreshInterval.getEntryValues().length; i++) {
                if (backgroundRefreshInterval.getEntryValues()[i].equals(newValue)) {
                    index = i;
                }
            }

            backgroundRefreshInterval.setSummary(backgroundRefreshInterval.getEntries()[index]);

            return true;
        });

        final ListPreference notificationPref = (ListPreference) findPreference(getString(R.string.background_notification_pref));
        notificationPref.setSummary(notificationPref.getEntry());
        notificationPref.setOnPreferenceChangeListener((preference, o) -> {
            int index = 0;
            for (int i = 0; i < notificationPref.getEntryValues().length; i++) {
                if (notificationPref.getEntryValues()[i].equals(o)) {
                    index = i;
                }
            }

            notificationPref.setSummary(notificationPref.getEntries()[index]);

            return true;
        });

        final SwitchPreference saveHistory = (SwitchPreference) findPreference(getString(R.string.save_history_pref));
        saveHistory.setOnPreferenceChangeListener((preference, newValue) -> {
            if (!((Boolean) newValue)) {
                MimiUtil.pruneHistory(0).subscribe();
            }
            return true;
        });

        final ListPreference historyPruneTime = (ListPreference) findPreference(getString(R.string.history_prune_time_pref));
        historyPruneTime.setSummary(getString(R.string.history_prune_time_summary, historyPruneTime.getEntry()));
        historyPruneTime.setOnPreferenceChangeListener((preference, newValue) -> {
            int index = 0;
            for (int i = 0; i < historyPruneTime.getEntryValues().length; i++) {
                if (historyPruneTime.getEntryValues()[i].equals(newValue)) {
                    index = i;
                }
            }

            preference.setSummary(getString(R.string.history_prune_time_summary, historyPruneTime.getEntries()[index]));

            return true;
        });

        final Preference clearHistory = findPreference(getString(R.string.clear_history_pref));
        clearHistory.setOnPreferenceClickListener(preference -> {
            MimiUtil.pruneHistory(0).subscribe();
            return true;
        });
    }
}
