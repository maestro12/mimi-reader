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

package com.emogoth.android.phone.mimi.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import com.activeandroid.query.Select;
import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.db.HistoryTableConnection;
import com.emogoth.android.phone.mimi.db.model.History;
import com.emogoth.android.phone.mimi.event.BookmarkClickedEvent;
import com.emogoth.android.phone.mimi.event.HomeButtonPressedEvent;
import com.emogoth.android.phone.mimi.event.OpenHistoryEvent;
import com.emogoth.android.phone.mimi.event.SelectThreadEvent;
import com.emogoth.android.phone.mimi.event.UpdateHistoryEvent;
import com.emogoth.android.phone.mimi.event.HttpErrorEvent;
import com.emogoth.android.phone.mimi.fragment.MimiFragmentBase;
import com.emogoth.android.phone.mimi.fragment.ThreadDetailFragment;
import com.emogoth.android.phone.mimi.fragment.ThreadPagerFragment;
import com.emogoth.android.phone.mimi.interfaces.ContentInterface;
import com.emogoth.android.phone.mimi.interfaces.IToolbarContainer;
import com.emogoth.android.phone.mimi.model.ThreadInfo;
import com.emogoth.android.phone.mimi.util.Extras;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.emogoth.android.phone.mimi.util.ThreadRegistry;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import rx.Subscription;
import rx.functions.Action1;

/**
 * An activity representing a single PostItem detail screen. This
 * activity is only used on handset devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link PostItemListActivity}.
 * <p>
 * This activity is mostly just a 'shell' activity containing nothing
 * more than a {@link com.emogoth.android.phone.mimi.fragment.ThreadPagerFragment}.
 */
public class PostItemDetailActivity extends MimiActivity implements Toolbar.OnClickListener,
        SharedPreferences.OnSharedPreferenceChangeListener, IToolbarContainer {
    private static final String LOG_TAG = PostItemDetailActivity.class.getSimpleName();

    public static final String POST_DETAIL_FRAGMENT_TAG = "POST_ITEM_DETAIL_FRAGMENT";
    private static final boolean LOG_DEBUG = true;

    private boolean fromUrl = false;
    private boolean singleThread = false;

    private String boardName;
    private MimiFragmentBase threadFragment;
    private AppBarLayout appBarLayout;
    private FloatingActionButton addContentFab;
    private int offsetIndex;

    private Subscription fetchHistorySubscription;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_postitem_detail);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.mimi_toolbar);
        if(toolbar != null) {
            toolbar.setNavigationOnClickListener(this);
//            toolbar.setSubtitleTextColor(getResources().getColor(R.color.toolbar_subtitle_color));
            
            setToolbar(toolbar);
        }

        appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
        addContentFab = (FloatingActionButton) findViewById(R.id.fab_add_content);
        addContentFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(threadFragment instanceof ContentInterface) {
                    ((ContentInterface) threadFragment).addContent();
                }
            }
        });

        final Bundle extras = getIntent().getExtras();

        if(extras != null) {
            if (extras.containsKey(Extras.EXTRAS_BOARD_NAME)) {
                boardName = extras.getString(Extras.EXTRAS_BOARD_NAME);
            }

            int position = -1;
            if (extras.containsKey(Extras.EXTRAS_POSITION)) {
                position = extras.getInt(Extras.EXTRAS_POSITION, -1);
            }

            if (extras.containsKey(Extras.EXTRAS_BOARD_TITLE)) {
                setTitle(extras.getString(Extras.EXTRAS_BOARD_TITLE));
            }

            if (extras.containsKey(Extras.EXTRAS_FROM_URL)) {
                fromUrl = true;
            }

            if (!extras.containsKey(Extras.EXTRAS_THREAD_LIST)) {

                if (position >= 0) {
                    ArrayList<ThreadInfo> threadList = getIntent().getParcelableArrayListExtra(Extras.EXTRAS_THREAD_LIST);
                    if (threadList != null && !TextUtils.isEmpty(threadList.get(position).boardTitle)) {
                        setTitle(threadList.get(position).boardTitle);
                    }
                }
                singleThread = true;
            }
        }

//        setAdContainer(R.id.advert_container, MimiUtil.adsEnabled(this));

        initDrawers(R.id.nav_drawer, R.id.nav_drawer_container, false);
        createDrawers(R.id.nav_drawer);


        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            final Bundle arguments = getIntent().getExtras();

            if(singleThread) {
                threadFragment = new ThreadDetailFragment();
            }
            else {
                threadFragment = new ThreadPagerFragment();
            }

            threadFragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.postitem_detail_container, threadFragment, POST_DETAIL_FRAGMENT_TAG)
                    .commit();

        }
        else {
            threadFragment = (MimiFragmentBase) getSupportFragmentManager().findFragmentByTag(POST_DETAIL_FRAGMENT_TAG);
        }
    }

    @Override
    public void setExpandedToolbar(boolean expanded, boolean animate) {
        if(appBarLayout != null) {
            appBarLayout.setExpanded(expanded, animate);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {


            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        final boolean handled;
        if(threadFragment != null) {
            handled = threadFragment.onBackPressed();
        }
        else {
            handled = false;
        }

        if(!handled) {
            super.onBackPressed();
            supportInvalidateOptionsMenu();
        }
    }

    @Override
    public File getCacheDir() {
        return MimiUtil.getInstance().getCacheDir();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        setNavigationIconWithBadge(0, ThreadRegistry.getInstance().getUnreadCount());
    }

    @Override
    protected void onPause() {
        super.onPause();
        RxUtil.safeUnsubscribe(fetchHistorySubscription);
    }

    @Override
    protected String getPageName() {
        return "post_detail";
    }

    @Subscribe
    public void openBookmark(final BookmarkClickedEvent event) {
        if(threadFragment instanceof ThreadPagerFragment && threadFragment.isAdded()) {
            SelectThreadEvent selectThreadEvent = new SelectThreadEvent(event.getBoardName(), event.getThreadId(), event.getPosition());
            ((ThreadPagerFragment)threadFragment).onThreadSelected(selectThreadEvent);
        } else {
            RxUtil.safeUnsubscribe(fetchHistorySubscription);
            fetchHistorySubscription = HistoryTableConnection.fetchHistory(true)
                    .subscribe(new Action1<List<History>>() {
                        @Override
                        public void call(List<History> bookmarks) {
                            final Bundle args = new Bundle();
                            final Class clazz;

                            args.putBoolean(Extras.EXTRAS_USE_BOOKMARKS, true);
                            args.putInt(Extras.EXTRAS_VIEWING_HISTORY, MimiActivity.VIEWING_BOOKMARKS);

                            if (!TextUtils.isEmpty(event.getBoardTitle())) {
                                args.putString(Extras.EXTRAS_BOARD_NAME, event.getBoardTitle());
                            }

                            args.putString(Extras.EXTRAS_BOARD_NAME, event.getBoardName());
                            args.putInt(Extras.EXTRAS_THREAD_ID, event.getThreadId());
                            args.putInt(Extras.EXTRAS_POSITION, event.getPosition());
                            final ArrayList<ThreadInfo> threadList = new ArrayList<>(bookmarks.size());

                            for (final History post : bookmarks) {
                                final ThreadInfo threadInfo = new ThreadInfo(post.threadId, post.boardName, null, post.watched);
                                threadList.add(threadInfo);
                            }

                            args.putParcelableArrayList(Extras.EXTRAS_THREAD_LIST, threadList);

//                        if (getResources().getBoolean(R.bool.two_pane)) {
//                            clazz = PostItemListActivity.class;
//                        } else {
//                            clazz = PostItemDetailActivity.class;
//                        }

                            final Intent intent = new Intent(PostItemDetailActivity.this, PostItemListActivity.class);
                            intent.putExtras(args);
                            startActivity(intent);
                        }
                    });
        }
    }

    @Override
    @Subscribe
    public void onAutoRefresh(final UpdateHistoryEvent event) {
        super.onAutoRefresh(event);
    }

    @Override
    @Subscribe
    public void onHttpError(final HttpErrorEvent event) {
        super.onHttpError(event);
    }

    @Override
    public void onClick(View v) {
        // This ID represents the Home or Up button. In the case of this
        // activity, the Up button is shown. Use NavUtils to allow users
        // to navigate up one level in the application structure. For
        // more details, see the Navigation pattern on Android Design:
        //
        // http://developer.android.com/design/patterns/navigation.html#up-vs-back
        //
        final Intent listIntent = new Intent(this, PostItemListActivity.class);
        if(NavUtils.shouldUpRecreateTask(this, listIntent)) {
            final TaskStackBuilder taskStack = TaskStackBuilder.create(this);
            taskStack.addNextIntent(listIntent);
            taskStack.startActivities();
        }
        else {
            NavUtils.navigateUpTo(this, listIntent);
        }
    }

    @Subscribe
    public void homeButtonPressed(HomeButtonPressedEvent event) {
        NavUtils.navigateUpTo(this, new Intent(this, PostItemListActivity.class));
    }

    @Subscribe
    public void openHistory(OpenHistoryEvent event) {
        final int bookmarkId;
        if(event.watched) {
            bookmarkId = PostItemListActivity.BOOKMARKS_ID;
        } else {
            bookmarkId = PostItemListActivity.HISTORY_ID;
        }

        final Intent intent = new Intent(this, PostItemListActivity.class);
        final Bundle args = new Bundle();

        args.putInt(Extras.EXTRAS_LIST_TYPE, bookmarkId);
        intent.putExtras(args);
        startActivity(intent);
    }

//    @Override
//    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
//        threadFragment.setSwipeRefreshEnabled(false);
//        offsetIndex = i;
//    }

//    @Override
//    public boolean dispatchTouchEvent(MotionEvent ev) {
//        final int action = MotionEventCompat.getActionMasked(ev);
//        switch (action) {
//            case MotionEvent.ACTION_DOWN:
//            case MotionEvent.ACTION_UP:
//            case MotionEvent.ACTION_CANCEL:
//
//                if (threadFragment != null) {
//                    if (offsetIndex == 0) {
//                        threadFragment.setSwipeRefreshEnabled(true);
//                    } else {
//                        threadFragment.setSwipeRefreshEnabled(false);
//                    }
//                }
//                break;
//        }
//        return super.dispatchTouchEvent(ev);
//    }

}
