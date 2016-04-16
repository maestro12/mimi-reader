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
import android.support.annotation.NonNull;

import com.emogoth.android.phone.mimi.BuildConfig;
import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.util.MimiPrefs;
import com.emogoth.android.phone.mimi.util.MimiUtil;

import net.rdrei.android.dirchooser.DirectoryChooserConfig;
import net.rdrei.android.dirchooser.DirectoryChooserFragment;

public class GalleryPrefsFragment extends PreferenceFragment {
    private static final String DIR_CHOOSER_TAG = "directory_chooser_tag";

    public GalleryPrefsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.gallery_prefs);

        setupPrefs();
    }

    private void setupPrefs() {
        final Preference saveLocation = findPreference(getString(R.string.image_file_location_pref));
        saveLocation.setSummary(MimiUtil.getSaveDir(getActivity()).getAbsolutePath());
        saveLocation.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final DirectoryChooserConfig.Builder configBuilder = DirectoryChooserConfig.builder();
                configBuilder.initialDirectory(MimiUtil.getSaveDir(getActivity()).getAbsolutePath())
                        .newDirectoryName("Mimi")
                        .allowNewDirectoryNameModification(true);

                final DirectoryChooserFragment chooserDialog = DirectoryChooserFragment.newInstance(configBuilder.build());
                chooserDialog.setDirectoryChooserListener(new DirectoryChooserFragment.OnFragmentInteractionListener() {
                    @Override
                    public void onSelectDirectory(@NonNull String path) {
                        MimiUtil.getInstance().setSaveDir(getActivity(), path);
                        saveLocation.setSummary(path);
                        chooserDialog.dismiss();
                    }

                    @Override
                    public void onCancelChooser() {
                        chooserDialog.dismiss();
                    }
                });

                chooserDialog.show(getActivity().getFragmentManager(), DIR_CHOOSER_TAG);
                return true;
            }
        });
    }

}
