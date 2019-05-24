
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

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.util.Log;
import android.widget.Toast;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.activity.LoginActivity;
import com.emogoth.android.phone.mimi.app.MimiApplication;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;
import com.emogoth.android.phone.mimi.db.HiddenThreadTableConnection;
import com.emogoth.android.phone.mimi.util.HttpClientFactory;
import com.emogoth.android.phone.mimi.util.MimiUtil;


public class AdvancedPrefsFragment extends PreferenceFragment {
    public static final String LOG_TAG = AdvancedPrefsFragment.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.advanced_prefs);


        setupPrefs();
    }

    private void setupPrefs() {
        final Preference cacheDirPref = findPreference(getString(R.string.cache_external_pref));
        if (cacheDirPref != null) {
            final String state = Environment.getExternalStorageState();
            if (state.equals(Environment.MEDIA_MOUNTED) && !state.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
                cacheDirPref.setEnabled(true);
            } else {
                cacheDirPref.setEnabled(false);
            }
        }

        final SwitchPreference preventScreenRotation = (SwitchPreference) findPreference(getString(R.string.prevent_screen_rotation_pref));
        if (preventScreenRotation != null) {
            preventScreenRotation.setOnPreferenceChangeListener((preference, newValue) -> {
                final Boolean v = (Boolean) newValue;
                if (v) {
                    getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                } else {
                    getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
                }

                return true;
            });
        }

        final Preference showAllBoardsPref = findPreference(getString(R.string.show_all_boards));
        final PreferenceCategory category = (PreferenceCategory) findPreference(getString(R.string.advanced_preference_category));

        if (showAllBoardsPref != null) {
            category.removePreference(showAllBoardsPref);
        }

        final Preference clearHiddenThreadsPref = findPreference(getString(R.string.clear_hidden_threads_pref));
        if (clearHiddenThreadsPref != null) {
            clearHiddenThreadsPref.setOnPreferenceClickListener(preference -> {
                HiddenThreadTableConnection.clearAll()
                        .compose(DatabaseUtils.applySchedulers())
                        .subscribe(success -> {
                            Log.d(LOG_TAG, "Clearing hidden threads: success=" + success);

                            int msgResId = R.string.all_hidden_threads_cleared;
                            if (!success) {
                                msgResId = R.string.failed_to_clear_hidden_threads;
                            }

                            Toast.makeText(MimiApplication.getInstance(), msgResId, Toast.LENGTH_SHORT).show();

                        }, throwable -> {
                            Log.w(LOG_TAG, "Could not clear hidden threads", throwable);

                            Toast.makeText(MimiApplication.getInstance(), R.string.failed_to_clear_hidden_threads, Toast.LENGTH_SHORT).show();
                        });

                return true;
            });
        }

        final Preference httpBufferSizePref = findPreference(getString(R.string.http_buffer_size_pref));
        httpBufferSizePref.setOnPreferenceChangeListener((preference, o) -> {
            HttpClientFactory.getInstance().reset();
            return true;
        });

        final Preference chanpassLogin = findPreference(getString(R.string.chanpass_login_pref));
        if (MimiUtil.getInstance().isLoggedIn()) {
            chanpassLogin.setTitle(R.string.chanpass_logout);
            chanpassLogin.setOnPreferenceClickListener(preference -> {
                final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
                dialogBuilder.setTitle(R.string.chanpass_logout);
                dialogBuilder.setMessage(R.string.are_you_sure);
                dialogBuilder.setPositiveButton(R.string.yes, (dialog, which) -> {
                    MimiUtil.getInstance().logout();
                    chanpassLogin.setTitle(R.string.chanpass_login);
                });
                dialogBuilder.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());

                dialogBuilder.show();
                return true;
            });
        } else {
            chanpassLogin.setTitle(R.string.chanpass_login);
            chanpassLogin.setOnPreferenceClickListener(preference -> {
                final Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);

                getActivity().finish();
                return true;
            });
        }
    }
}
