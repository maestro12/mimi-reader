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

package com.emogoth.android.phone.mimi.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.AbsListView;

import com.emogoth.android.phone.mimi.activity.MimiActivity;


public abstract class MimiFragmentBase extends Fragment {
    private static final String LOG_TAG = MimiFragmentBase.class.getSimpleName();
    private static final boolean LOG_DEBUG = true;
    private AbsListView absListView;
    private MimiActivity activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        activity = (MimiActivity) getActivity();

    }

    public boolean onBackPressed() {
        return false;
    }

    public abstract String getTitle();

    public abstract String getSubtitle();

    public abstract String getPageName(); // for logging

    public void initMenu() {
        // empty
    }

    public boolean showFab() {
        return true;
    }
}
