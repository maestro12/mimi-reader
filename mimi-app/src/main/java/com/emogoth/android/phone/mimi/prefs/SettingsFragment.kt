package com.emogoth.android.phone.mimi.prefs

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.emogoth.android.phone.mimi.R


class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preference_headers)
    }
}