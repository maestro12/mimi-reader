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
import android.os.Build;
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
import com.mimireader.chanlib.models.ChanBoard;

import java.io.File;
import java.io.IOException;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;


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
        final Runnable countRunnable = () -> aboutCounter = 0;

        final Preference changelogPreference = findPreference(getString(R.string.changelog_pref));
        if (changelogPreference != null) {
            changelogPreference.setOnPreferenceClickListener(preference -> {
                // Create and show the dialog.
                LicensesFragment.displayLicensesFragment(getFragmentManager(), R.raw.changelog, "ChangeLog");
                return true;
            });
        }

        final Preference website = findPreference(getString(R.string.website_pref));
        website.setOnPreferenceClickListener(preference -> {

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
        });

        final Preference rate = findPreference(getString(R.string.rate_pref));
        rate.setOnPreferenceClickListener(preference -> {
            MimiUtil.getInstance().openMarketLink(getActivity());
            return true;
        });

        final Preference feedback = findPreference(getString(R.string.feedback_pref));
        feedback.setOnPreferenceClickListener(preference -> {
//            final Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
//                    "mailto", "eli@mimireader.com", null));
//            intent.putExtra(Intent.EXTRA_SUBJECT, "Mimi Feedback");
//            Intent mailer = Intent.createChooser(intent, null);
//            startActivity(mailer);
            sendLogcatMail(false, true);
            return true;
        });


        final Preference privacy = findPreference(getString(R.string.privacy_pref));
        privacy.setOnPreferenceClickListener(preference -> {
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
        });

        final Preference license = findPreference(getString(R.string.licenses_pref));
        license.setOnPreferenceClickListener(preference -> {
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
        });

        final Preference version = findPreference(getString(R.string.version_pref));
        version.setSummary(BuildConfig.VERSION_NAME);
        version.setOnPreferenceClickListener(preference -> {
            aboutCounter++;

            if (aboutCounter == 7) {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                final boolean fullBoardEnabled = prefs.getBoolean(getString(R.string.full_board_list_enabled), false);

                if (fullBoardEnabled) {
                    Toast.makeText(getActivity(), "Full board list is already active", Toast.LENGTH_SHORT).show();
                } else {
                    BoardTableConnection.fetchBoards(0)
                            .compose(DatabaseUtils.applySchedulers())
                            .flatMapIterable((Function<List<Board>, Iterable<Board>>) boards -> boards)
                            .flatMap((Function<Board, Flowable<ChanBoard>>) boards -> BoardTableConnection.setBoardVisibility(null, true))
                            .onErrorResumeNext((Function<Throwable, Flowable<ChanBoard>>) throwable -> {
                                Log.w("MoreInfoFragment", "Error setting board visibility", throwable);
                                return Flowable.just(new ChanBoard());
                            })
                            .subscribe();

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
        });
    }

    void sendLogcatMail(boolean attachLogcat, boolean attachDeviceInfo) {

        boolean log = attachLogcat;
        File outputFile = null;
        if (log) {
            // save logcat in file
            outputFile = getActivity().getFileStreamPath("logcat.txt");

            try {
                Runtime.getRuntime().exec(
                        "logcat -f " + outputFile.getAbsolutePath());
            } catch (IOException e) {
                Log.e("Feedback", "Error getting logcat for feedback email", e);
                log = false;
            }
        }

        StringBuilder sb = new StringBuilder("\n\n\n-----\n");
        if (attachDeviceInfo) {
            sb.append("Mimi ")
                    .append(BuildConfig.VERSION_NAME).append(" (")
                    .append(BuildConfig.VERSION_CODE).append('-')
                    .append(BuildConfig.FLAVOR).append(")\n");
            sb.append("Device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n');
            sb.append("Android: ").append(Build.VERSION.CODENAME).append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n");
        }

        //send file using email
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        // Set type to "email"
        emailIntent.setType("vnd.android.cursor.dir/email");
        String[] to = {"eli@mimireader.com"};
        emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
        if (log) {
            // the attachment
            emailIntent.putExtra(Intent.EXTRA_STREAM, outputFile.getAbsolutePath());
        }

        if (attachDeviceInfo) {
            emailIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        }
        // the mail subject
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Mimi Feedback");
        startActivity(Intent.createChooser(emailIntent, "Send Feedback"));
    }
}
