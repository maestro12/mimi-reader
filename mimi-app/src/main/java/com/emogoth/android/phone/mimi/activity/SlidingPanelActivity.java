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
import android.graphics.Color;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.res.ResourcesCompat;
import androidx.slidingpanelayout.widget.SlidingPaneLayout;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.db.HistoryTableConnection;
import com.emogoth.android.phone.mimi.event.BookmarkClickedEvent;
import com.emogoth.android.phone.mimi.event.OpenHistoryEvent;
import com.emogoth.android.phone.mimi.event.UpdateHistoryEvent;
import com.emogoth.android.phone.mimi.fragment.BoardItemListFragment;
import com.emogoth.android.phone.mimi.fragment.HistoryFragment;
import com.emogoth.android.phone.mimi.fragment.MimiFragmentBase;
import com.emogoth.android.phone.mimi.fragment.PostItemsListFragment;
import com.emogoth.android.phone.mimi.fragment.ThreadDetailFragment;
import com.emogoth.android.phone.mimi.interfaces.BoardItemClickListener;
import com.emogoth.android.phone.mimi.interfaces.ContentInterface;
import com.emogoth.android.phone.mimi.interfaces.GalleryMenuItemClickListener;
import com.emogoth.android.phone.mimi.interfaces.IToolbarContainer;
import com.emogoth.android.phone.mimi.interfaces.OnThumbnailClickListener;
import com.emogoth.android.phone.mimi.util.AppRatingUtil;
import com.emogoth.android.phone.mimi.util.Extras;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.Pages;
import com.mimireader.chanlib.models.ChanBoard;
import com.mimireader.chanlib.models.ChanPost;
import com.novoda.simplechromecustomtabs.SimpleChromeCustomTabs;
import com.squareup.otto.Subscribe;

import java.util.List;

public class SlidingPanelActivity extends MimiActivity implements BoardItemClickListener,
        Toolbar.OnClickListener,
        OnThumbnailClickListener,
        GalleryMenuItemClickListener,
        IToolbarContainer {

    public static final int BOARD_LIST_ID = 0;
    public static final int BOOKMARKS_ID = 1;
    public static final int HISTORY_ID = 2;

    private static final String TAG_BOARD_LIST = "board_list_fragment";
    private static final String TAG_POST_LIST = "post_list_fragment";
    private static final String TAG_THREAD_DETAIL = "thread_detail_fragment";

    private int listType = BOARD_LIST_ID;

    private String boardName;
    private String boardTitle;
    private MimiFragmentBase listFragment;
    private MimiFragmentBase detailFragment;
    private MimiFragmentBase boardsFragment;

    private Pages openPage = Pages.NONE;

    private FloatingActionButton addContentFab;
    private SlidingPaneLayout panelLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sliding_panel);

        int sliderFadeColor;
        if (MimiUtil.getInstance().getTheme() == MimiUtil.THEME_LIGHT) {
            sliderFadeColor = ResourcesCompat.getColor(getResources(), R.color.background_light, getTheme());
        } else {
            sliderFadeColor = ResourcesCompat.getColor(getResources(), R.color.background_dark, getTheme());
        }

        int coverFadeColor = sliderFadeColor;
        sliderFadeColor = Color.argb(0xAA, Color.red(sliderFadeColor), Color.green(sliderFadeColor), Color.blue(sliderFadeColor));

        panelLayout = (SlidingPaneLayout) findViewById(R.id.panel_layout);
        panelLayout.setSliderFadeColor(sliderFadeColor);
        panelLayout.setCoveredFadeColor(coverFadeColor);
        panelLayout.setPanelSlideListener(new SlidingPaneLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {

            }

            @Override
            public void onPanelOpened(View panel) {
                if (listFragment != null) {
                    listFragment.initMenu();
                }
            }

            @Override
            public void onPanelClosed(View panel) {
                if (detailFragment != null) {
                    detailFragment.initMenu();
                }
            }
        });
        panelLayout.openPane();

        addContentFab = (FloatingActionButton) findViewById(R.id.fab_add_content);
        addContentFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Fragment fragment;
                if (panelLayout.isOpen()) {
                    fragment = listFragment;
                } else {
                    fragment = detailFragment;
                }

                if (fragment instanceof ContentInterface) {
                    ((ContentInterface) fragment).addContent();
                }
            }
        });

        ViewGroup appRatingContainer = (ViewGroup) findViewById(R.id.app_rater_container);
        AppRatingUtil.init(appRatingContainer);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.mimi_toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(this);
            setToolbar(toolbar);
        }

        extractExtras(getIntent().getExtras());

        initDrawers(R.id.nav_drawer, R.id.nav_drawer_container, true);
        createDrawers(R.id.nav_drawer);

        initFragments();

        if (openPage == Pages.BOOKMARKS) {
            openHistoryPage(new OpenHistoryEvent(true));
        }
    }

    private void initFragments() {

        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        if (listType == BOARD_LIST_ID) {
            final BoardItemListFragment fragment = new BoardItemListFragment();
            final Bundle extras = new Bundle();
            extras.putBoolean(Extras.EXTRAS_OPTIONS_MENU_ENABLED, false);

            fragment.setArguments(extras);
            ft.add(R.id.postitem_list, fragment, TAG_BOARD_LIST);

            ft.commit();

            fragment.setActivateOnItemClick(true);

            detailFragment = fragment;
            listFragment = fragment;
            boardsFragment = fragment;
        } else if (listType == BOOKMARKS_ID) {
            final HistoryFragment fragment = new HistoryFragment();
            final Bundle args = new Bundle();

            args.putInt(Extras.EXTRAS_HISTORY_QUERY_TYPE, HistoryTableConnection.BOOKMARKS);
            args.putInt(Extras.EXTRAS_VIEWING_HISTORY, MimiActivity.VIEWING_BOOKMARKS);
            args.putBoolean(Extras.EXTRAS_OPTIONS_MENU_ENABLED, false);

            fragment.setArguments(args);

            ft.add(R.id.postitem_list, fragment, TAG_BOARD_LIST);

            ft.commit();

            detailFragment = fragment;
            listFragment = fragment;
        } else if (listType == HISTORY_ID) {
            final HistoryFragment fragment = new HistoryFragment();
            final Bundle args = new Bundle();

            args.putInt(Extras.EXTRAS_HISTORY_QUERY_TYPE, HistoryTableConnection.HISTORY);
            args.putInt(Extras.EXTRAS_VIEWING_HISTORY, MimiActivity.VIEWING_HISTORY);
            args.putBoolean(Extras.EXTRAS_OPTIONS_MENU_ENABLED, false);

            fragment.setArguments(args);

            ft.add(R.id.postitem_list, fragment, TAG_BOARD_LIST);

            ft.commit();

            detailFragment = fragment;
            listFragment = fragment;
        }
    }

    public void extractExtras(Bundle extras) {
        if (extras != null) {
            boardName = extras.getString(Extras.EXTRAS_BOARD_NAME);

            if (extras.containsKey(Extras.EXTRAS_LIST_TYPE)) {
                listType = extras.getInt(Extras.EXTRAS_LIST_TYPE);
            }

            if (extras.containsKey(Extras.OPEN_PAGE)) {
                final String page = extras.getString(Extras.OPEN_PAGE);

                if (!TextUtils.isEmpty(page)) {
                    openPage = Pages.valueOf(page);
                }
            }
        }
    }

    private void setFabVisibility(boolean visible) {
        if (addContentFab.isShown() && !visible) {
            addContentFab.hide();
        } else if (!addContentFab.isShown() && visible) {
            addContentFab.show();
        }
    }

    @Override
    protected void onPause() {
        SimpleChromeCustomTabs.getInstance().disconnectFrom(this);
        super.onPause();
    }

    @Override
    protected void onResume() {
        SimpleChromeCustomTabs.getInstance().connectTo(this);
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        if (panelLayout.isOpen()) {
            listFragment.onCreateOptionsMenu(menu, inflater);
        } else {
            detailFragment.onCreateOptionsMenu(menu, inflater);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            toggleNavDrawer();
            return true;
        }

        if (panelLayout.isOpen()) {
            return listFragment.onOptionsItemSelected(item);
        } else {
            return detailFragment.onOptionsItemSelected(item);
        }
    }

    @Override
    protected String getPageName() {
        return "sliding_drawer_activity";
    }

    @Override
    public void onClick(View v) {
        if (listType == BOARD_LIST_ID) {
            toggleNavDrawer();
        } else {
            finish();
        }
    }

    @Override
    public void onBoardItemClick(ChanBoard board, boolean saveBackStack) {
        final Bundle arguments = new Bundle();
        arguments.putString(Extras.EXTRAS_BOARD_NAME, board.getName());
        arguments.putString(Extras.EXTRAS_BOARD_TITLE, board.getTitle());
        arguments.putBoolean(Extras.EXTRAS_OPTIONS_MENU_ENABLED, false);

        PostItemsListFragment fragment = new PostItemsListFragment();
        fragment.setArguments(arguments);

        final FragmentManager fm = getSupportFragmentManager();
        final FragmentTransaction ft = fm.beginTransaction();
        final Fragment catalogItemsFragment = fm.findFragmentByTag(TAG_POST_LIST);
        if (catalogItemsFragment != null) {
            ft.remove(catalogItemsFragment);
        }

        ft.replace(R.id.postitem_list, fragment, TAG_POST_LIST);

//        if (listFragment != null) {
//
//            ft.hide(listFragment);
//        }
//        ft.add(R.id.postitem_list, fragment, TAG_POST_LIST);

        if (saveBackStack) {
            ft.addToBackStack(null);
        }

        ft.commit();

        listFragment = fragment;
        setFabVisibility(fragment.showFab());
    }

    @Override
    public void onGalleryMenuItemClick(String boardPath, long threadId) {
//        final Bundle args = new Bundle();
//
//        args.putInt(Extras.EXTRAS_GALLERY_TYPE, 0);
//        args.putString(Extras.EXTRAS_BOARD_NAME, boardPath);
//        args.putInt(Extras.EXTRAS_THREAD_ID, threadId);
//
//        final Intent intent = new Intent(this, GalleryActivity2.class);
//        intent.putExtras(args);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
//        startActivity(intent);

        GalleryActivity2.start(this, GalleryActivity2.GALLERY_TYPE_GRID, 0, boardPath, threadId, new long[0]);
    }

    @Override
    public void setExpandedToolbar(boolean expanded, boolean animate) {

    }

    @Override
    public void onPostItemClick(View v, List<ChanPost> posts, int position, String boardTitle, String boardName, long threadId) {
        openThread(posts, position, boardName, boardTitle, threadId);
    }

    public void openThread(final List<ChanPost> posts, final int position, final String boardName, final String boardTitle, long threadId) {
        this.boardName = boardName;
        this.boardTitle = boardTitle;

        if (posts != null && posts.size() > position) {
            detailFragment = ThreadDetailFragment.newInstance(posts.get(position).getNo(), boardName, boardTitle, posts.get(position), true, false);
        } else {
            detailFragment = ThreadDetailFragment.newInstance(threadId, boardName, boardTitle, null, true, false);
        }

        final FragmentManager fm = getSupportFragmentManager();
        final FragmentTransaction ft = fm.beginTransaction();

        ft.replace(R.id.postitem_detail, detailFragment, TAG_THREAD_DETAIL);
        ft.commit();

        panelLayout.closePane();
    }

    @Override
    public void onBackPressed() {
        if (panelLayout.isOpen()) {
            if (listFragment instanceof HistoryFragment) {
                final FragmentManager fm = getSupportFragmentManager();
                final FragmentTransaction ft = fm.beginTransaction();

                ft.remove(listFragment).commit();
            }
            listFragment = boardsFragment;
            super.onBackPressed();

            listFragment.initMenu();
        } else {
            panelLayout.openPane();
        }
    }

    @Subscribe
    public void openBookmark(BookmarkClickedEvent event) {
        this.boardName = event.getBoardName();
        this.boardTitle = event.getBoardTitle();

        final FragmentManager fm = getSupportFragmentManager();
        final FragmentTransaction ft = fm.beginTransaction();

        long threadId = event.getThreadId();
        detailFragment = ThreadDetailFragment.newInstance(threadId, boardName, boardTitle, null, false, false);
        ft.replace(R.id.postitem_detail, detailFragment, TAG_THREAD_DETAIL);
        ft.commit();

        if (panelLayout.isOpen()) {
            panelLayout.closePane();
        }
    }

    @Subscribe
    public void openHistoryPage(OpenHistoryEvent event) {
        final FragmentManager fm = getSupportFragmentManager();
        final FragmentTransaction ft = fm.beginTransaction();

        final boolean saveBackStack;
        if (listFragment == boardsFragment) {
            saveBackStack = true;
        } else {
            saveBackStack = false;
        }

        HistoryFragment fragment = HistoryFragment.newInstance(event.watched);
        ft.replace(R.id.postitem_list, fragment, TAG_POST_LIST);

        if (saveBackStack) {
            ft.addToBackStack(null);
        }

        ft.commit();

        listFragment = fragment;
        panelLayout.openPane();
    }

    @Override
    @Subscribe
    public void onAutoRefresh(final UpdateHistoryEvent event) {
        super.onAutoRefresh(event);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String page = intent.getStringExtra(Extras.OPEN_PAGE);
        if (!TextUtils.isEmpty(page)) {
            try {
                Pages pageEnum = Pages.valueOf(page);
                boolean watched;
                watched = pageEnum == Pages.BOOKMARKS;

                openHistoryPage(new OpenHistoryEvent(watched));
            } catch (Exception e) {

            }
        }
    }
}
