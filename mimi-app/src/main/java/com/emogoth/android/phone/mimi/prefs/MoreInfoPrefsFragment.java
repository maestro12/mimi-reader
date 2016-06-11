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

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.emogoth.android.phone.mimi.BuildConfig;
import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.db.BoardTableConnection;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;
import com.emogoth.android.phone.mimi.db.model.Board;
import com.emogoth.android.phone.mimi.util.MimiUtil;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;


public class MoreInfoPrefsFragment extends PreferenceFragment {
    private static final String LOG_TAG = MoreInfoPrefsFragment.class.getSimpleName();
    private int aboutCounter = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.more_info_prefs);

        setupPrefs();
    }

    private void setupPrefs() {
        final Handler handler = new Handler();
        final Runnable countRunnable = new Runnable() {
            @Override
            public void run() {
                aboutCounter = 0;
            }
        };

        final Preference changelogPreference = findPreference(getString(R.string.changelog_pref));
        if (changelogPreference != null) {
            changelogPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    // Create and show the dialog.
                    LicensesFragment.displayLicensesFragment(getFragmentManager(), R.raw.changelog, "ChangeLog");
                    return true;
                }
            });
        }

        final Preference website = findPreference(getString(R.string.website_pref));
        website.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {

                try {
                    final String url = "http://mimireader.com";
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                } catch (final ActivityNotFoundException e) {
                    Log.e(LOG_TAG, "Could not find browser to open mimireader.com", e);

                    if (getActivity() != null) {
                        Toast.makeText(getActivity(), R.string.error_opening_url, Toast.LENGTH_SHORT).show();
                    }
                }

                return true;
            }
        });

        final Preference rate = findPreference(getString(R.string.rate_pref));
        rate.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                MimiUtil.getInstance().openMarketLink(getActivity());
                return true;
            }
        });

        final Preference feedback = findPreference(getString(R.string.feedback_pref));
        feedback.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                final Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", "eli@mimireader.com", null));
                intent.putExtra(Intent.EXTRA_SUBJECT, "Mimi Feedback");
                Intent mailer = Intent.createChooser(intent, null);
                startActivity(mailer);
                return true;
            }
        });


        final Preference privacy = findPreference(getString(R.string.privacy_pref));
        privacy.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                // Create & show a licenses fragment just as you would any other DialogFragment.
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                Fragment prev = getFragmentManager().findFragmentByTag("privacyDialogFragment");
                if (prev != null) {
                    ft.remove(prev);
                }
                ft.addToBackStack(null);

                // Create and show the dialog.
                DialogFragment newFragment = PrivacyPolicyFragment.newInstance();
                newFragment.show(ft, "privacyDialogFragment");
                return true;
            }
        });

        final Preference license = findPreference(getString(R.string.licenses_pref));
        license.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                // Create & show a licenses fragment just as you would any other DialogFragment.
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                Fragment prev = getFragmentManager().findFragmentByTag("licensesDialogFragment");
                if (prev != null) {
                    ft.remove(prev);
                }
                ft.addToBackStack(null);

                // Create and show the dialog.
                DialogFragment newFragment = LicensesFragment.newInstance(R.raw.licenses);
                newFragment.show(ft, "licensesDialogFragment");
                return true;
            }
        });

        final Preference version = findPreference(getString(R.string.version_pref));
        version.setSummary(BuildConfig.VERSION_NAME);
        version.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                aboutCounter++;

                if (aboutCounter == 7) {
                    final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    final boolean fullBoardEnabled = prefs.getBoolean(getString(R.string.full_board_list_enabled), false);

                    if (fullBoardEnabled) {
                        Toast.makeText(getActivity(), "Full board list is already active", Toast.LENGTH_SHORT).show();
                    } else {
                        BoardTableConnection.fetchBoards(0)
                                .compose(DatabaseUtils.<List<Board>>applySchedulers())
                                .flatMap(new Func1<List<Board>, Observable<Boolean>>() {
                                    @Override
                                    public Observable<Boolean> call(List<Board> boards) {
                                        return BoardTableConnection.setBoardVisibility(null, true);
                                    }
                                })
                                .onErrorResumeNext(new Func1<Throwable, Observable<Boolean>>() {
                                    @Override
                                    public Observable<Boolean> call(Throwable throwable) {
                                        Log.w("MoreInfoFragment", "Error setting board visibility", throwable);
                                        return Observable.just(false);
                                    }
                                })
                                .subscribe(new Action1<Boolean>() {
                                    @Override
                                    public void call(Boolean success) {

                                    }
                                });

                        prefs.edit()
                                .putBoolean(getString(R.string.show_all_boards), true)
                                .apply();

                        Toast.makeText(getActivity(), "Full board list is available", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    handler.removeCallbacks(countRunnable);
                }

                handler.postDelayed(countRunnable, 1000);

                return true;
            }
        });
    }
}
