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
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.adapter.TabPagerAdapter;
import com.emogoth.android.phone.mimi.autorefresh.RefreshScheduler;
import com.emogoth.android.phone.mimi.db.HistoryTableConnection;
import com.emogoth.android.phone.mimi.event.ActionModeEvent;
import com.emogoth.android.phone.mimi.event.BookmarkClickedEvent;
import com.emogoth.android.phone.mimi.event.CloseTabEvent;
import com.emogoth.android.phone.mimi.event.FABVisibilityEvent;
import com.emogoth.android.phone.mimi.event.HomeButtonPressedEvent;
import com.emogoth.android.phone.mimi.event.HttpErrorEvent;
import com.emogoth.android.phone.mimi.event.OpenHistoryEvent;
import com.emogoth.android.phone.mimi.event.RateAppEvent;
import com.emogoth.android.phone.mimi.event.UpdateHistoryEvent;
import com.emogoth.android.phone.mimi.fragment.MimiFragmentBase;
import com.emogoth.android.phone.mimi.fragment.PostItemsListFragment;
import com.emogoth.android.phone.mimi.interfaces.BoardItemClickListener;
import com.emogoth.android.phone.mimi.interfaces.ContentInterface;
import com.emogoth.android.phone.mimi.interfaces.GalleryMenuItemClickListener;
import com.emogoth.android.phone.mimi.interfaces.IToolbarContainer;
import com.emogoth.android.phone.mimi.interfaces.OnPostItemClickListener;
import com.emogoth.android.phone.mimi.model.ThreadRegistryModel;
import com.emogoth.android.phone.mimi.util.AppRatingUtil;
import com.emogoth.android.phone.mimi.util.Extras;
import com.emogoth.android.phone.mimi.util.Pages;
import com.emogoth.android.phone.mimi.util.ThreadRegistry;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.mimireader.chanlib.models.ChanBoard;
import com.mimireader.chanlib.models.ChanPost;
import com.novoda.simplechromecustomtabs.SimpleChromeCustomTabs;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;


public class TabsActivity extends MimiActivity implements BoardItemClickListener,
        Toolbar.OnClickListener,
        OnPostItemClickListener,
        IToolbarContainer,
        GalleryMenuItemClickListener {

    private TabLayout tabLayout;
    private ViewPager tabPager;
    private TabPagerAdapter tabPagerAdapter;
    private MimiFragmentBase postListFragment;
    private Toolbar toolbar;
    private AppBarLayout appBarLayout;
    private MimiFragmentBase currentFragment;
    private FloatingActionButton addContentFab;
    private ViewGroup rateAppContainer;

    private boolean closeTabOnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tabs);

        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        closeTabOnBack = sp.getBoolean(getString(R.string.close_tab_on_back_pref), false);

        tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabPager = (ViewPager) findViewById(R.id.tabs_pager);
        toolbar = (Toolbar) findViewById(R.id.mimi_toolbar);
        appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
        rateAppContainer = (ViewGroup) findViewById(R.id.app_rater_container);

        setToolbar(toolbar);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleNavDrawer();
            }
        });

        final ArrayList<TabPagerAdapter.TabItem> tabItems;
        if (savedInstanceState != null && savedInstanceState.containsKey("tabItems")) {
            tabItems = savedInstanceState.getParcelableArrayList("tabItems");
            tabPagerAdapter = new TabPagerAdapter(getSupportFragmentManager(), tabItems);
        } else {
            tabItems = null;
            tabPagerAdapter = new TabPagerAdapter(getSupportFragmentManager());
        }

        tabPager.setAdapter(tabPagerAdapter);
        tabPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(final int position) {

                if (currentFragment == null) {
                    return;
                }

                tabPager.post(new Runnable() {
                    @Override
                    public void run() {
                        if (position < tabPagerAdapter.getCount()) {
                            currentFragment = (MimiFragmentBase) tabPagerAdapter.instantiateItem(tabPager, position);
                            currentFragment.initMenu();
                        }
                    }
                });

                setFabVisibility(currentFragment.showFab());
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        tabPager.post(new Runnable() {
            @Override
            public void run() {
                currentFragment = (MimiFragmentBase) tabPagerAdapter.instantiateItem(tabPager, 0);
                currentFragment.initMenu();
            }
        });

        // Hack to stop crashing
        // https://code.google.com/p/android/issues/detail?id=201827
//        TabLayout.Tab uselessTab;
//        for (int j = 0; j < 17; j++) {
//            uselessTab = tabLayout.newTab();
//        }

        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabLayout.setupWithViewPager(tabPager);
        tabLayout.setTabsFromPagerAdapter(tabPagerAdapter);

        if (savedInstanceState != null && tabItems != null) {
            int count = tabLayout.getTabCount();
            for (int i = 1; i < count; i++) {
                TabLayout.Tab tab = tabLayout.getTabAt(i);
                if (tab != null) {
                    TabPagerAdapter.TabItem item = tabItems.get(i);

                    if (i == 1 && item.getTabType() == TabPagerAdapter.TabType.POSTS) {
                        tab.setText("/" + item.getTitle() + "/");
                    } else if (i == 1 && item.getTabType() == TabPagerAdapter.TabType.HISTORY) {
                        tab.setText(item.getTitle());
                    } else {
                        Bundle args = item.getBundle();
                        if (args != null) {
                            long threadId = args.getLong(Extras.EXTRAS_THREAD_ID, 0);
                            String boardName = args.getString(Extras.EXTRAS_BOARD_NAME, "");
                            View tabView = createTabView(threadId, boardName);
                            tab.setCustomView(tabView);
                        }
                    }
                }
            }
        }

        final TabLayout.Tab boardsTab = tabLayout.getTabAt(0);
        if (boardsTab != null) {
            boardsTab.setText(R.string.boards);
        }

        final Toolbar toolbar = (Toolbar) findViewById(R.id.mimi_toolbar);
        if (toolbar != null) {
//            toolbar.setSubtitleTextColor(getResources().getColor(R.color.toolbar_subtitle_color));
        }

        addContentFab = (FloatingActionButton) findViewById(R.id.fab_add_content);
        addContentFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentFragment instanceof ContentInterface) {
                    ((ContentInterface) currentFragment).addContent();
                }
            }
        });

        AppRatingUtil.init(rateAppContainer);

        initDrawers(R.id.nav_drawer, R.id.nav_drawer_container, true);
        createDrawers(R.id.nav_drawer);

        final Bundle extras = getIntent().getExtras();
        Pages openPage = Pages.NONE;
        if (extras != null && extras.containsKey(Extras.OPEN_PAGE)) {
            final String page = extras.getString(Extras.OPEN_PAGE);

            if (!TextUtils.isEmpty(page)) {
                openPage = Pages.valueOf(page);
            }
        }

        if (openPage == Pages.BOOKMARKS) {
            openHistory(new OpenHistoryEvent(true));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (tabPager != null) {
            tabPager.clearOnPageChangeListeners();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SimpleChromeCustomTabs.getInstance().connectTo(this);

        if (currentFragment != null) {
            setFabVisibility(currentFragment.showFab());
        }
    }

    @Override
    protected void onPause() {
        SimpleChromeCustomTabs.getInstance().disconnectFrom(this);
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        ArrayList<TabPagerAdapter.TabItem> tabItems = new ArrayList<>(tabPagerAdapter.getItems());
        outState.putParcelableArrayList("tabItems", tabItems);
    }

    @Override
    protected String getPageName() {
        return "tabs_activity";
    }

    @Override
    public void onBoardItemClick(ChanBoard board, boolean saveBackStack) {
        final Bundle arguments = new Bundle();
        arguments.putString(Extras.EXTRAS_BOARD_NAME, board.getName());
        arguments.putString(Extras.EXTRAS_BOARD_TITLE, board.getTitle());
        arguments.putBoolean(Extras.EXTRAS_TWOPANE, false);
        arguments.putBoolean(Extras.EXTRAS_STICKY_AUTO_REFRESH, true);

        final TabPagerAdapter.TabItem tabItem = new TabPagerAdapter.TabItem(TabPagerAdapter.TabType.POSTS, arguments, PostItemsListFragment.TAB_ID, board.getName(), null);
        if (tabPagerAdapter.getCount() == 1) {
            final TabLayout.Tab postListTab = tabLayout.newTab();
            postListTab.setText("/" + board.getName().toLowerCase() + "/");

            tabPagerAdapter.addItem(tabItem);
            tabLayout.addTab(postListTab);
        } else {
            final TabLayout.Tab postListTab = tabLayout.getTabAt(1);
            if (postListTab != null) {
                TabLayout.Tab newTab = tabLayout.newTab();
                newTab.setText("/" + board.getName().toLowerCase() + "/");
                tabLayout.removeTabAt(1);
                tabLayout.addTab(newTab, 1);
            }

            tabPagerAdapter.setItemAtIndex(1, tabItem);

            if (postListFragment == null) {
                postListFragment = (PostItemsListFragment) tabPagerAdapter.instantiateItem(tabPager, 1);
            }

            if (postListFragment instanceof PostItemsListFragment) {
                PostItemsListFragment frag = (PostItemsListFragment) postListFragment;
                frag.setBoard(board.getName());
                frag.refreshBoard(true);
            }
        }

        tabPager.setCurrentItem(1, true);
    }

    private void setFabVisibility(boolean shouldShow) {
        if (addContentFab.isShown() && !shouldShow) {
            addContentFab.hide();
        } else if (!addContentFab.isShown() && shouldShow) {
            addContentFab.show();
        }
    }

    @Override
    public void setExpandedToolbar(boolean expanded, boolean animate) {
        if (appBarLayout != null) {
            appBarLayout.setExpanded(expanded, animate);
        }
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onBackPressed() {
        boolean handled = false;
//        if(isDrawerOpen(ToggleDrawerEvent.DrawerType.BOOKMARKS)) {
//            toggleBookmarkDrawer();
//            handled = true;
//        } else
        final int pos = tabPager.getCurrentItem();
        if (currentFragment != null) {
            handled = currentFragment.onBackPressed();
        }

        if (!handled && pos > 0) {
            handled = true;

            if (closeTabOnBack && pos > 1) {
                closeTab(pos, false);
            } else {
                tabPager.setCurrentItem(pos - 1, true);
            }
        }

        if (!handled) {
            super.onBackPressed();
            supportInvalidateOptionsMenu();
        }
    }

    @Override
    public void onPostItemClick(View v, List<ChanPost> posts, final int position, final String boardTitle, final String boardName, final long threadId) {
        final TabLayout.Tab threadTab = tabLayout.newTab();
        final TabPagerAdapter.TabItem threadTabItem;
        final Bundle args = new Bundle();

        args.putLong(Extras.EXTRAS_THREAD_ID, threadId);
        args.putString(Extras.EXTRAS_BOARD_NAME, boardName);
        args.putString(Extras.EXTRAS_BOARD_TITLE, boardTitle);
        args.putBoolean(Extras.EXTRAS_STICKY_AUTO_REFRESH, true);

        if (posts != null && posts.size() > position) {
            ChanPost firstPost = posts.get(position);
            if (firstPost != null) {
                args.putParcelable(Extras.EXTRAS_THREAD_FIRST_POST, firstPost);
            }
        }

        View tabView = createTabView(threadId, boardName);
        threadTab.setCustomView(tabView);

        threadTabItem = new TabPagerAdapter.TabItem(TabPagerAdapter.TabType.THREAD, args, threadId, boardName, String.valueOf(threadId));
        final int itemCount = tabPagerAdapter.getCount();
        final int pos = tabPagerAdapter.addItem(threadTabItem);

        if (pos < 0) {
            return;
        } else if (pos >= itemCount) {
            tabLayout.addTab(threadTab);
        }
        tabPager.setCurrentItem(pos, true);
    }

    private View createTabView(final long threadId, final String boardName) {
        final View tabView = View.inflate(this, R.layout.tab_default, null);
        final TextView title = (TextView) tabView.findViewById(R.id.title);
        final TextView subTitle = (TextView) tabView.findViewById(R.id.subtitle);

        tabView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                PopupMenu popupMenu = new PopupMenu(TabsActivity.this, v);
                popupMenu.inflate(R.menu.tab_popup_menu);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        try {
                            if (item.getItemId() == R.id.close_tab) {
                                closeTab(threadId, boardName, true);
                            } else if (item.getItemId() == R.id.close_other_tabs) {
                                closeOtherTabs(tabPagerAdapter.getPositionById(threadId));
                            }
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "Error closing tab", e);
                        }
                        return true;
                    }
                });
                popupMenu.show();

                return true;
            }
        });

        tabView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int pos = tabPagerAdapter.getIndex(threadId);
                tabPager.setCurrentItem(pos, false);
            }
        });

        title.setText("/" + boardName.toLowerCase() + "/");
        subTitle.setText(String.valueOf(threadId));

        return tabView;
    }

    private void closeOtherTabs(int position) {
        int size = tabPagerAdapter.getCount();
        for (int i = size - 1; i >= 2; i--) {
            if (i != position) {
                Log.d(LOG_TAG, "Closing tab at position " + i);
                TabPagerAdapter.TabItem t = tabPagerAdapter.getTab(i);

                if (t != null) {
                    Integer id = Integer.valueOf(t.getSubtitle());
                    closeTab(id, t.getTitle().replaceAll("/", null), true);
                }
            }
        }
    }

    private void closeTab(long threadId, String boardName, boolean showSnackbar) {
        final int pos = tabPagerAdapter.getPositionById(threadId);
        closeTab(pos, threadId, boardName, showSnackbar);
    }


    private void closeTab(int pos, boolean showSnackbar) {
        closeTab(pos, -1, null, showSnackbar);
    }

    private void closeTab(int pos, long threadId, String boardName, boolean showSnackbar) {
        try {

            final int pagerPos = tabPager.getCurrentItem();
            final int newPos;
            if (pos > pagerPos) {
                newPos = pagerPos;
            } else {
                newPos = pagerPos - 1;
            }

            long id = threadId;
            String name = boardName;
            if (threadId <= 0 || boardName == null) {
                TabPagerAdapter.TabItem item = tabPagerAdapter.getTabItem(pos);
                if (item != null) {
                    Bundle args = item.getBundle();

                    if (args != null) {
                        id = args.getLong(Extras.EXTRAS_THREAD_ID, -1);
                        name = args.getString(Extras.EXTRAS_BOARD_NAME, null);
                    }
                }
            }

            if (newPos >= 0) {
                Log.d(LOG_TAG, "Removing tab: position=" + pos);

                ThreadRegistryModel threadInfo = ThreadRegistry.getInstance().getThread(id);

                if (threadInfo == null || !threadInfo.isBookmarked()) {
                    RefreshScheduler.getInstance().removeThread(name, id);
                }

                tabLayout.removeTabAt(pos);
                tabPagerAdapter.removeItemAtIndex(pos);
                tabPagerAdapter.notifyDataSetChanged();
//                tabPager.setAdapter(tabPagerAdapter);
                tabPager.setCurrentItem(newPos, false);

                if (showSnackbar) {
                    Snackbar.make(tabPager, R.string.closing_tab, Snackbar.LENGTH_SHORT).show();
                }
            } else {
                Log.e(LOG_TAG, "Position is invalid: pos=" + newPos);
            }
        } catch (Exception e) {
            Snackbar.make(tabPager, R.string.error_occurred, Snackbar.LENGTH_SHORT).show();
            Log.e(LOG_TAG, "Error closing tab", e);
        }
    }

    @Subscribe
    public void fabVisibilityEvent(FABVisibilityEvent event) {
        setFabVisibility(event.isVisible());
    }

    @Subscribe
    public void onRateApp(final RateAppEvent event) {
        Log.i(LOG_TAG, "onRateApp called: action=" + event.getAction());
        if (rateAppContainer != null) {
            if (event.getAction() == RateAppEvent.OPEN) {
                rateAppContainer.setVisibility(View.VISIBLE);
            }

            if (event.getAction() == RateAppEvent.CLOSE) {
                rateAppContainer.setVisibility(View.GONE);
            }
        }
    }

    @Subscribe
    public void actionModeChanged(ActionModeEvent event) {
        if (tabLayout != null) {
            if (event.isEnabled()) {
                tabLayout.setVisibility(View.INVISIBLE);
            } else {
                tabLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    @Subscribe
    public void homeButtonPressed(HomeButtonPressedEvent event) {
        tabPager.setCurrentItem(0, false);
    }

    @Subscribe
    public void openBookmark(BookmarkClickedEvent event) {

        try {
            ChanBoard board = new ChanBoard();
            board.setName(event.getBoardName());
            board.setTitle(event.getBoardTitle());

            if (tabPagerAdapter.getCount() == 1) {
                onBoardItemClick(board, false);
            }
            onPostItemClick(null, null, event.getPosition(), event.getBoardTitle(), event.getBoardName(), event.getThreadId());
        } catch (Exception e) {
            Toast.makeText(TabsActivity.this, R.string.error_opening_bookmark, Toast.LENGTH_SHORT).show();
            Log.e(LOG_TAG, "Error opening bookmark", e);
        }
    }

    @Override
    @Subscribe
    public void onAutoRefresh(final UpdateHistoryEvent event) {
        super.onAutoRefresh(event);

        if (tabPagerAdapter != null && tabLayout != null) {
            int tabIndex = tabPagerAdapter.getIndex(event.getThreadId());
            if (tabIndex >= 0) {
                TabLayout.Tab t = tabLayout.getTabAt(tabIndex);
                if (t != null) {
                    View v = t.getCustomView();
                    if (v != null) {
                        View highlightView = v.findViewById(R.id.highlight);
                        if (highlightView != null) {
                            if (event.getThreadSize() - 1 > event.getLastReadPosition() && event.getLastReadPosition() > 0) {
                                highlightView.setVisibility(View.VISIBLE);
                            } else {
                                highlightView.setVisibility(View.GONE);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    @Subscribe
    public void onHttpError(final HttpErrorEvent event) {
        super.onHttpError(event);
    }

    @Subscribe
    public void onActionModeChanged(ActionModeEvent event) {
        if (tabPager != null) {
            tabPager.setEnabled(!event.isEnabled());
        }
    }

    @Subscribe
    public void onTabClosed(CloseTabEvent event) {
        final long threadId = event.getId();
        final String boardName = event.getBoardName();

        if (event.isCloseOthers()) {
            closeOtherTabs(tabPagerAdapter.getPositionById(threadId));
        } else {
            closeTab(threadId, boardName, true);
        }
    }

    @Override
    public void onGalleryMenuItemClick(String boardPath, long threadId) {
        GalleryActivity2.start(this, 0, 0, boardPath, threadId, new long[0]);
    }

    @Subscribe
    public void openHistory(OpenHistoryEvent event) {
        final Bundle args = new Bundle();
        final String historyFragmentName;

        if (event.watched) {
            historyFragmentName = getString(R.string.bookmarks);

            args.putInt(Extras.EXTRAS_HISTORY_QUERY_TYPE, HistoryTableConnection.BOOKMARKS);
            args.putInt(Extras.EXTRAS_VIEWING_HISTORY, MimiActivity.VIEWING_BOOKMARKS);
        } else {
            historyFragmentName = getString(R.string.history);

            args.putInt(Extras.EXTRAS_HISTORY_QUERY_TYPE, HistoryTableConnection.HISTORY);
            args.putInt(Extras.EXTRAS_VIEWING_HISTORY, MimiActivity.VIEWING_HISTORY);
        }

        final TabPagerAdapter.TabItem tabItem = new TabPagerAdapter.TabItem(TabPagerAdapter.TabType.HISTORY, args, PostItemsListFragment.TAB_ID, historyFragmentName, null);

        if (tabPagerAdapter.getCount() == 1) {
            final TabLayout.Tab postListTab = tabLayout.newTab();
            postListTab.setText(historyFragmentName);

            tabPagerAdapter.addItem(tabItem);
            tabLayout.addTab(postListTab);
        } else {
            final TabLayout.Tab postListTab = tabLayout.getTabAt(1);
            if (postListTab != null) {
                TabLayout.Tab newTab = tabLayout.newTab();
                newTab.setText(historyFragmentName);
                tabLayout.removeTabAt(1);
                tabLayout.addTab(newTab, 1);
            }

            tabPagerAdapter.setItemAtIndex(1, tabItem);

            if (postListFragment == null) {
                postListFragment = (MimiFragmentBase) tabPagerAdapter.instantiateItem(tabPager, 1);
            }
        }

        tabPager.setCurrentItem(1, false);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        String page = intent.getStringExtra(Extras.OPEN_PAGE);
        if (!TextUtils.isEmpty(page)) {
            try {
                Pages pageEnum = Pages.valueOf(page);
                boolean watched;
                if (pageEnum == Pages.BOOKMARKS) {
                    watched = true;
                } else {
                    watched = false;
                }

                openHistory(new OpenHistoryEvent(watched));
            } catch (Exception e) {

            }
        }
    }
}
