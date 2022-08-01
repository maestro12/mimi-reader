
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

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.activity.LoginActivity;
import com.emogoth.android.phone.mimi.app.MimiApplication;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;
import com.emogoth.android.phone.mimi.db.HiddenThreadTableConnection;
import com.emogoth.android.phone.mimi.util.HttpClientFactory;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.concurrent.Callable;

import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;


public class AdvancedPrefsFragment extends PreferenceFragmentCompat {
    public static final String LOG_TAG = AdvancedPrefsFragment.class.getSimpleName();

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.advanced_prefs);
        setupPrefs();
    }

    private void setupPrefs() {
        final PreferenceCategory category = (PreferenceCategory) findPreference(getString(R.string.advanced_preference_category));

        final Preference clearHiddenThreadsPref = findPreference(getString(R.string.clear_hidden_threads_pref));
        if (clearHiddenThreadsPref != null) {
            clearHiddenThreadsPref.setOnPreferenceClickListener(preference -> {
                final Disposable dis = HiddenThreadTableConnection.clearAll()
                        .compose(DatabaseUtils.applySingleSchedulers())
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
                final MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(getActivity());
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
