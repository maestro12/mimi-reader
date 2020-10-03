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

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.viewpager.widget.ViewPager;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.activity.MimiActivity;
import com.emogoth.android.phone.mimi.adapter.ThreadPagerAdapter;
import com.emogoth.android.phone.mimi.interfaces.ContentInterface;
import com.emogoth.android.phone.mimi.interfaces.ReplyClickListener;
import com.emogoth.android.phone.mimi.interfaces.ThreadSelectedListener;
import com.emogoth.android.phone.mimi.model.ThreadInfo;
import com.emogoth.android.phone.mimi.util.Extras;
import com.mimireader.chanlib.models.ChanPost;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


/**
 * A fragment representing a single PostItem detail screen.
 * This fragment is either contained in a {@link com.emogoth.android.phone.mimi.activity.PostItemListActivity}
 * in two-pane mode (on tablets) or a {@link com.emogoth.android.phone.mimi.activity.PostItemDetailActivity}
 * on handsets.
 */
public class ThreadPagerFragment extends MimiFragmentBase implements ContentInterface, ReplyClickListener, ThreadSelectedListener {
    public static final String LOG_TAG = ThreadPagerFragment.class.getSimpleName();

    private String boardName;
    private String boardTitle;
    private ArrayList<ThreadInfo> threadList;
    private Integer currentPosition;
    private MimiFragmentBase currentFragment;
    private int currentPage;
    private MenuItem bookmarkCountMenu;
    private int newPostCount = 0;
    private ViewPager threadPager;
    private ThreadPagerAdapter threadPagerAdapter;
    private int[] unreadCountList = null;
    private int viewingHistory;
    private ChanPost initialPost;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ThreadPagerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(false);
    }

    private void extractExtras(final Bundle bundle) {
        if (bundle.containsKey(Extras.EXTRAS_BOARD_NAME)) {
            boardName = bundle.getString(Extras.EXTRAS_BOARD_NAME);
        }
        if (bundle.containsKey(Extras.EXTRAS_BOARD_TITLE)) {
            boardTitle = bundle.getString(Extras.EXTRAS_BOARD_TITLE);
        }
        if (bundle.containsKey(Extras.EXTRAS_THREAD_LIST)) {
            bundle.setClassLoader(ThreadInfo.class.getClassLoader());
            threadList = bundle.getParcelableArrayList(Extras.EXTRAS_THREAD_LIST);
        }
        if (bundle.containsKey(Extras.EXTRAS_POSITION)) {
            currentPosition = bundle.getInt(Extras.EXTRAS_POSITION);
        }
        if (bundle.containsKey(Extras.EXTRAS_PAGE)) {
            currentPage = bundle.getInt(Extras.EXTRAS_PAGE);
        }
        if (bundle.containsKey(Extras.EXTRAS_UNREAD_COUNT)) {
            unreadCountList = bundle.getIntArray(Extras.EXTRAS_UNREAD_COUNT);
        }
        if (bundle.containsKey(Extras.EXTRAS_VIEWING_HISTORY)) {
            viewingHistory = bundle.getInt(Extras.EXTRAS_VIEWING_HISTORY);
        }
        if (bundle.containsKey(Extras.EXTRAS_THREAD_FIRST_POST)) {
            initialPost = bundle.getParcelable(Extras.EXTRAS_THREAD_FIRST_POST);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_postitem_detail, container, false);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            extractExtras(savedInstanceState);
        } else {
            extractExtras(getArguments());
        }

        threadPager = (ViewPager) view.findViewById(R.id.thread_pager);
//        threadPager.setOffscreenPageLimit(2);
        threadPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                currentPosition = position;
                currentFragment = (MimiFragmentBase) threadPagerAdapter.instantiateItem(threadPager, position);
                if (getActivity() != null) {
                    final MimiActivity activity = (MimiActivity) getActivity();
                    activity.getSupportActionBar().setTitle(currentFragment.getTitle());
                    activity.getSupportActionBar().setSubtitle(currentFragment.getSubtitle());

                    currentFragment.initMenu();
                }

            }

            @Override
            public void onPageScrollStateChanged(int position) {

            }
        });

        threadPagerAdapter = new ThreadPagerAdapter(getChildFragmentManager(), threadList, unreadCountList, initialPost, currentPosition);
        threadPager.setAdapter(threadPagerAdapter);
        threadPager.post(new Runnable() {
            @Override
            public void run() {

                final MimiFragmentBase fragment = (MimiFragmentBase) threadPagerAdapter.instantiateItem(threadPager, currentPosition);
                threadPager.setCurrentItem(currentPosition, false);

                if (getActivity() != null) {
                    final MimiActivity activity = (MimiActivity) getActivity();
                    activity.getToolbar().setTitle(fragment.getTitle());
                    activity.getToolbar().setSubtitle(fragment.getSubtitle());

                    fragment.initMenu();
                }
            }
        });

    }

    @Override
    public boolean onBackPressed() {
        final int index = threadPager.getCurrentItem();
        final MimiFragmentBase fragment = (MimiFragmentBase) threadPagerAdapter.instantiateItem(threadPager, index);
        final boolean handled = fragment.onBackPressed();

        return handled;

    }

    @Override
    public void onResume() {
        super.onResume();

        if (threadPagerAdapter != null) {
            final MimiFragmentBase fragment = (MimiFragmentBase) threadPagerAdapter.instantiateItem(threadPager, threadPager.getCurrentItem());
            if (getActivity() != null) {
                final MimiActivity activity = (MimiActivity) getActivity();
                activity.getSupportActionBar().setTitle(fragment.getTitle());
                activity.getSupportActionBar().setSubtitle(fragment.getSubtitle());
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        threadPager.clearOnPageChangeListeners();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public String getSubtitle() {
        return null;
    }

    @Override
    public String getPageName() {
        return "thread_pager";
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(Extras.EXTRAS_BOARD_NAME, boardName);
        outState.putString(Extras.EXTRAS_BOARD_TITLE, boardTitle);
        outState.putInt(Extras.EXTRAS_POSITION, currentPosition);
        outState.putInt(Extras.EXTRAS_PAGE, currentPage);
        outState.putParcelableArrayList(Extras.EXTRAS_THREAD_LIST, threadList);

        if (unreadCountList != null) {
            outState.putIntArray(Extras.EXTRAS_UNREAD_COUNT, unreadCountList);
        }
        super.onSaveInstanceState(outState);
    }

    public void setBookmarkCount(final int count) {
        newPostCount = count;
        if (getActivity() != null) {
            getActivity().supportInvalidateOptionsMenu();
        }
    }

    @Override
    public void onThreadSelected(@NotNull String boardName, long threadId, int position) {
        final ThreadInfo threadInfo = new ThreadInfo(threadId, boardName, "", false);
        final int i = threadList.indexOf(threadInfo);

        if (threadPager != null) {
            if (i >= 0) {
                threadPager.setCurrentItem(i);
            }
        }
    }

    @Override
    public void addContent() {
        if (currentFragment instanceof ContentInterface) {
            ((ContentInterface) currentFragment).addContent();
        }
    }

    @Override
    public void onReplyClicked(@NotNull String boardName, long threadId, long id, @NotNull List<String> replies) {
        if (currentFragment instanceof ReplyClickListener) {
            ReplyClickListener frag = (ReplyClickListener) currentFragment;
            frag.onReplyClicked(boardName, threadId, id, replies);
        }
    }
}
