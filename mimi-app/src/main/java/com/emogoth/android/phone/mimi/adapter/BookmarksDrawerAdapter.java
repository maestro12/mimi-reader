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

package com.emogoth.android.phone.mimi.adapter;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.db.HistoryTableConnection;
import com.emogoth.android.phone.mimi.fragment.HistoryFragment;
import com.emogoth.android.phone.mimi.util.Extras;
import com.emogoth.android.phone.mimi.util.MimiUtil;


public class BookmarksDrawerAdapter extends FragmentStatePagerAdapter {
    private static final String LOG_TAG = BookmarksDrawerAdapter.class.getSimpleName();
    private final Fragment[] pagerTabs;

    private final String bookmarkTabName;
    private final String historyTabName;

    public BookmarksDrawerAdapter(final FragmentManager fm, final Context context, final int viewType) {
        super(fm);

        if (MimiUtil.historyEnabled(context)) {
            pagerTabs = new Fragment[2];
        } else {
            pagerTabs = new Fragment[1];
        }

        pagerTabs[0] = new HistoryFragment();
        final Bundle bookmarkBundle = new Bundle();
        bookmarkBundle.putInt(Extras.EXTRAS_HISTORY_QUERY_TYPE, HistoryTableConnection.BOOKMARKS);
        bookmarkBundle.putInt(Extras.EXTRAS_VIEWING_HISTORY, viewType);
        Log.i(LOG_TAG, "Bookmark type=" + viewType);
        pagerTabs[0].setArguments(bookmarkBundle);

        if (MimiUtil.historyEnabled(context)) {
            pagerTabs[1] = new HistoryFragment();
            final Bundle historyBundle = new Bundle();
            historyBundle.putInt(Extras.EXTRAS_HISTORY_QUERY_TYPE, HistoryTableConnection.HISTORY);
            historyBundle.putInt(Extras.EXTRAS_VIEWING_HISTORY, viewType);
            Log.i(LOG_TAG, "History type=" + viewType);
            pagerTabs[1].setArguments(historyBundle);
        }

        bookmarkTabName = context.getString(R.string.bookmarks_tab);
        historyTabName = context.getString(R.string.history_tab);

    }

    @Override
    public Fragment getItem(int position) {
        return pagerTabs[position];
    }

    @Override
    public int getCount() {
        return pagerTabs.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return bookmarkTabName;
            case 1:
                return historyTabName;

        }

        return "BAD_TAB_TITLE";
    }
}
