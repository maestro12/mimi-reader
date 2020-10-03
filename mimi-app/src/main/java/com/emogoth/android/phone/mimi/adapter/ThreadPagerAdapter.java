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

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import android.util.Log;

import com.emogoth.android.phone.mimi.fragment.MimiFragmentBase;
import com.emogoth.android.phone.mimi.fragment.ThreadDetailFragment;
import com.emogoth.android.phone.mimi.model.ThreadInfo;
import com.emogoth.android.phone.mimi.util.Extras;
import com.mimireader.chanlib.models.ChanPost;

import java.util.List;


public class ThreadPagerAdapter extends FragmentStatePagerAdapter {
    private static final String LOG_TAG = ThreadPagerAdapter.class.getSimpleName();

    private final List<ThreadInfo> threadList;
    private final int[] unreadCountList;
    private final int postPosition;
    private int sizeWithAds;

    private ChanPost initialPost;

    public ThreadPagerAdapter(final FragmentManager fm, final List<ThreadInfo> threadList, final int[] unreadCountList, final ChanPost initialPost, final int postPosition) {
        super(fm);
        this.threadList = threadList;
        this.unreadCountList = unreadCountList;
        this.initialPost = initialPost;
        this.postPosition = postPosition;

        this.sizeWithAds = threadList.size();
    }

    @Override
    public Fragment getItem(final int position) {
        final MimiFragmentBase fragment = new ThreadDetailFragment();
        final Bundle args = new Bundle();
        if (position < threadList.size()) {

                args.putString(Extras.EXTRAS_BOARD_NAME, threadList.get(position).getBoardName());
                args.putString(Extras.EXTRAS_BOARD_TITLE, threadList.get(position).getBoardTitle());
                args.putLong(Extras.EXTRAS_THREAD_ID, threadList.get(position).getThreadId());
                args.putInt(Extras.LOADER_ID, position % 3);

                if (unreadCountList != null) {
                    args.putInt(Extras.EXTRAS_UNREAD_COUNT, unreadCountList[position]);
                }

                if (position == postPosition && initialPost != null) {
                    args.putParcelable(Extras.EXTRAS_THREAD_FIRST_POST, initialPost);
                    initialPost = null;
                }

        }

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public int getCount() {
        return sizeWithAds;
    }

}