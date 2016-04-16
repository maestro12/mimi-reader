
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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.activity.LoginActivity;
import com.emogoth.android.phone.mimi.util.MimiUtil;


public class AdvancedPrefsFragment extends PreferenceFragment {
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
            preventScreenRotation.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final Boolean v = (Boolean) newValue;
                    if (v) {
                        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    } else {
                        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
                    }

                    return true;
                }
            });
        }

        final Preference showAllBoardsPref = findPreference(getString(R.string.show_all_boards));
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final PreferenceCategory category = (PreferenceCategory) findPreference(getString(R.string.advanced_preference_category));
        final boolean enabled = prefs.getBoolean(getString(R.string.full_board_list_enabled), false);

        if (!enabled) {
            category.removePreference(showAllBoardsPref);
        }

        final Preference chanpassLogin = findPreference(getString(R.string.chanpass_login_pref));
        if (MimiUtil.getInstance().isLoggedIn()) {
            chanpassLogin.setTitle(R.string.chanpass_logout);
            chanpassLogin.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
                    dialogBuilder.setTitle(R.string.chanpass_logout);
                    dialogBuilder.setMessage(R.string.are_you_sure);
                    dialogBuilder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            MimiUtil.getInstance().logout();
                            chanpassLogin.setTitle(R.string.chanpass_login);
                        }
                    });
                    dialogBuilder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    dialogBuilder.show();
                    return true;
                }
            });
        } else {
            chanpassLogin.setTitle(R.string.chanpass_login);
            chanpassLogin.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final Intent intent = new Intent(getActivity(), LoginActivity.class);
                    startActivity(intent);

                    getActivity().finish();
                    return true;
                }
            });
        }
    }
}
