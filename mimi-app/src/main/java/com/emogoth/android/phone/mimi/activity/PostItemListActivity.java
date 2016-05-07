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
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.db.BoardTableConnection;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;
import com.emogoth.android.phone.mimi.db.HistoryTableConnection;
import com.emogoth.android.phone.mimi.db.model.History;
import com.emogoth.android.phone.mimi.event.BookmarkClickedEvent;
import com.emogoth.android.phone.mimi.event.FABVisibilityEvent;
import com.emogoth.android.phone.mimi.event.HomeButtonPressedEvent;
import com.emogoth.android.phone.mimi.event.HttpErrorEvent;
import com.emogoth.android.phone.mimi.event.OpenHistoryEvent;
import com.emogoth.android.phone.mimi.event.RateAppEvent;
import com.emogoth.android.phone.mimi.event.UpdateHistoryEvent;
import com.emogoth.android.phone.mimi.fragment.BoardItemListFragment;
import com.emogoth.android.phone.mimi.fragment.HistoryFragment;
import com.emogoth.android.phone.mimi.fragment.MimiFragmentBase;
import com.emogoth.android.phone.mimi.fragment.PostItemsListFragment;
import com.emogoth.android.phone.mimi.interfaces.BoardItemClickListener;
import com.emogoth.android.phone.mimi.interfaces.ContentInterface;
import com.emogoth.android.phone.mimi.interfaces.GalleryMenuItemClickListener;
import com.emogoth.android.phone.mimi.interfaces.IToolbarContainer;
import com.emogoth.android.phone.mimi.interfaces.OnBoardsUpdatedCallback;
import com.emogoth.android.phone.mimi.interfaces.OnPostItemClickListener;
import com.emogoth.android.phone.mimi.interfaces.OnThumbnailClickListener;
import com.emogoth.android.phone.mimi.model.ThreadInfo;
import com.emogoth.android.phone.mimi.util.AppRatingUtil;
import com.emogoth.android.phone.mimi.util.Extras;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.Pages;
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.emogoth.android.phone.mimi.util.ThreadRegistry;
import com.mimireader.chanlib.models.ChanBoard;
import com.mimireader.chanlib.models.ChanPost;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import rx.Subscription;
import rx.functions.Action1;


/**
 * An activity representing a list of PostItems. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link com.emogoth.android.phone.mimi.activity.PostItemDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p/>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link com.emogoth.android.phone.mimi.fragment.BoardItemListFragment} and the item details
 * (if present) is a {@link com.emogoth.android.phone.mimi.fragment.ThreadPagerFragment}.
 * <p/>
 * This activity also implements the required
 * {@link com.emogoth.android.phone.mimi.interfaces.BoardItemClickListener} interface
 * to listen for item selections.
 */
public class PostItemListActivity extends MimiActivity implements BoardItemClickListener,
        Toolbar.OnClickListener,
        OnPostItemClickListener,
        OnBoardsUpdatedCallback,
        OnThumbnailClickListener,
        GalleryMenuItemClickListener,
        IToolbarContainer {

    private static final String LOG_TAG = PostItemListActivity.class.getSimpleName();
    private static final boolean LOG_DEBUG = true;

    public static final int BOARD_LIST_ID = 0;
    public static final int BOOKMARKS_ID = 1;
    public static final int HISTORY_ID = 2;

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;

    private static final String TAG_BOARD_LIST = "board_list_fragment";
    private static final String TAG_POST_LIST = "post_list_fragment";
    private static final String TAG_THREAD_DETAIL = "thread_detail_fragment";

    private int listType = BOARD_LIST_ID;

    private List<ChanBoard> boards;

    private String boardName;
    private boolean fetchCatalog = false;

    private MimiFragmentBase currentFragment;
    private String boardTitle;
    private ViewGroup rateAppContainer;
    private FloatingActionButton addContentFab;

    private Stack<MimiFragmentBase> fragmentList;
    private MimiFragmentBase homeFragment;
    private Pages openPage = Pages.NONE;
    private AppBarLayout appBarLayout;
    private Subscription boardInfoSubscription;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_postitem_list);

        fragmentList = new Stack<>();

        appBarLayout = (AppBarLayout) findViewById(R.id.appbar);

        addContentFab = (FloatingActionButton) findViewById(R.id.fab_add_content);
        addContentFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentFragment instanceof ContentInterface) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        int centerX = Math.round((float) (v.getRight() - v.getLeft()) / 2.0F);
                        int centerY = Math.round((float) (v.getBottom() - v.getTop()) / 2.0F);
                        int radius = v.getRight() - v.getLeft();

                        ViewAnimationUtils.createCircularReveal(v, centerX, centerY, radius, radius * 2).start();
                    }
                    ((ContentInterface) currentFragment).addContent();
                }
            }
        });
        addContentFab.hide();

        final Toolbar toolbar = (Toolbar) findViewById(R.id.mimi_toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(this);
            setToolbar(toolbar);
        }

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {

            boardName = extras.getString(Extras.EXTRAS_BOARD_NAME);

            if (extras.containsKey(Extras.EXTRAS_LIST_TYPE)) {
                listType = extras.getInt(Extras.EXTRAS_LIST_TYPE);
            }

            if (extras.containsKey(Extras.EXTRAS_CATALOG)) {
                fetchCatalog = true;
            }

            if (extras.containsKey(Extras.OPEN_PAGE)) {
                final String page = extras.getString(Extras.OPEN_PAGE);

                if (!TextUtils.isEmpty(page)) {
                    openPage = Pages.valueOf(page);
                }
            }
        }

        if (listType == BOARD_LIST_ID) {
            rateAppContainer = (ViewGroup) findViewById(R.id.app_rater_container);
            AppRatingUtil.init(rateAppContainer);
        }


        if (findViewById(R.id.postitem_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
//            ((BoardItemListFragment) getSupportFragmentManager()
//                    .findFragmentById(R.id.postitem_list))
//                    .setActivateOnItemClick(true);

        }

        initDrawers(R.id.nav_drawer, R.id.nav_drawer_container, listType == BOARD_LIST_ID);

        if (savedInstanceState == null) {
            createDrawers(R.id.nav_drawer);
            initFragments();

            if (boardName != null) {
                RxUtil.safeUnsubscribe(boardInfoSubscription);
                boardInfoSubscription = BoardTableConnection.fetchBoard(boardName)
                        .compose(DatabaseUtils.<ChanBoard>applySchedulers())
                        .subscribe(new Action1<ChanBoard>() {
                            @Override
                            public void call(ChanBoard board) {
                                if (board != null) {
                                    onBoardItemClick(board, false);
                                }
                            }
                        });
            }
        }

        if (openPage != Pages.NONE) {
            final Intent intent = new Intent(this, PostItemListActivity.class);
            final Bundle args = new Bundle();

            if (openPage == Pages.BOOKMARKS) {
                args.putInt(Extras.EXTRAS_LIST_TYPE, PostItemListActivity.BOOKMARKS_ID);
                intent.putExtras(args);
            }

            startActivity(intent);
        }

    }

    private void initFragments() {

        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        if (listType == BOARD_LIST_ID) {
            final BoardItemListFragment fragment = new BoardItemListFragment();
            fragment.setBoardsListener(this);
            ft.add(R.id.postitem_list, fragment, TAG_BOARD_LIST);

            ft.commit();

            fragment.setActivateOnItemClick(true);

            homeFragment = fragment;
            currentFragment = fragment;
        } else if (listType == BOOKMARKS_ID) {
            final HistoryFragment fragment = new HistoryFragment();
            final Bundle args = new Bundle();

            args.putInt(Extras.EXTRAS_HISTORY_QUERY_TYPE, HistoryTableConnection.BOOKMARKS);
            args.putInt(Extras.EXTRAS_VIEWING_HISTORY, MimiActivity.VIEWING_BOOKMARKS);

            fragment.setArguments(args);

            ft.add(R.id.postitem_list, fragment, TAG_BOARD_LIST);

            ft.commit();

            homeFragment = fragment;
            currentFragment = fragment;
        } else if (listType == HISTORY_ID) {
            final HistoryFragment fragment = new HistoryFragment();
            final Bundle args = new Bundle();

            args.putInt(Extras.EXTRAS_HISTORY_QUERY_TYPE, HistoryTableConnection.HISTORY);
            args.putInt(Extras.EXTRAS_VIEWING_HISTORY, MimiActivity.VIEWING_HISTORY);

            fragment.setArguments(args);

            ft.add(R.id.postitem_list, fragment, TAG_BOARD_LIST);

            ft.commit();

            homeFragment = fragment;
            currentFragment = fragment;
        }
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//
//        switch(item.getItemId()) {
//            case android.R.id.home:
//
//
//                return true;
//
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("rotated", true);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBoardItemClick(ChanBoard board, boolean saveBackStack) {

        final Bundle arguments = new Bundle();
        arguments.putString(Extras.EXTRAS_BOARD_NAME, board.getName());
        arguments.putString(Extras.EXTRAS_BOARD_TITLE, board.getTitle());
        arguments.putBoolean(Extras.EXTRAS_TWOPANE, mTwoPane);
        arguments.putBoolean(Extras.EXTRAS_CATALOG, true);

        PostItemsListFragment fragment = new PostItemsListFragment();
        fragment.setArguments(arguments);
        fragment.setBoards(boards);

        final FragmentManager fm = getSupportFragmentManager();
        final FragmentTransaction ft = fm.beginTransaction();
        final Fragment catalogItemsFragment = fm.findFragmentByTag(TAG_POST_LIST);
        if (catalogItemsFragment != null) {
            ft.remove(catalogItemsFragment);
        }

        if (currentFragment != null) {
            ft.hide(currentFragment);
        }
        ft.add(R.id.postitem_list, fragment, TAG_POST_LIST);

        if (saveBackStack) {
            ft.addToBackStack(null);
        }

        ft.commit();

        currentFragment = fragment;
        setFabVisibility(currentFragment.showFab());
    }

    @Override
    public void onBoardsUpdated(List<ChanBoard> boards) {
        this.boards = boards;
    }

    @Override
    public void onBackPressed() {
        final boolean handled;
//        if(isDrawerOpen(ToggleDrawerEvent.DrawerType.BOOKMARKS)) {
//            toggleBookmarkDrawer();
//            handled = true;
//        } else
        if (currentFragment != null && currentFragment.getUserVisibleHint()) {
            handled = currentFragment.onBackPressed();
        } else {
            handled = false;
        }

        if (!handled) {
//            final int i = getSupportFragmentManager().getBackStackEntryCount() - 1;
//            currentFragment = (MimiFragmentBase) getSupportFragmentManager().findFragmentById(id);

            if (fragmentList != null && fragmentList.size() > 0) {
                currentFragment = fragmentList.pop();
            } else {
                currentFragment = homeFragment;
                if (currentFragment != null) {
                    setFabVisibility(currentFragment.showFab());
                }
            }

            if (currentFragment instanceof BoardItemListFragment) {
                currentFragment.onResume();
                currentFragment.initMenu();

                setExpandedToolbar(true, true);
            }

            super.onBackPressed();
            supportInvalidateOptionsMenu();

//            if (getToolbar() != null) {
//                getToolbar().setVisibility(View.VISIBLE);
//            }

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
    public boolean onPrepareOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public File getCacheDir() {
        return MimiUtil.getInstance().getCacheDir();
    }

    @Override
    public void onGalleryMenuItemClick(String boardPath, int threadId) {
        final Bundle args = new Bundle();

        args.putInt(Extras.EXTRAS_GALLERY_TYPE, 0);
        args.putString(Extras.EXTRAS_BOARD_NAME, boardPath);
        args.putInt(Extras.EXTRAS_THREAD_ID, threadId);

        final Intent intent = new Intent(this, GalleryActivity.class);
        intent.putExtras(args);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setNavigationIconWithBadge(0, ThreadRegistry.getInstance().getUnreadCount());
    }

    @Override
    protected void onStop() {
        super.onStop();
        RxUtil.safeUnsubscribe(boardInfoSubscription);
    }

    @Override
    public void setExpandedToolbar(boolean expanded, boolean animate) {
        if (appBarLayout != null) {
            appBarLayout.setExpanded(expanded, animate);
        }
    }

    @Override
    protected String getPageName() {
        return "post_list";
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
    public void openBookmark(final BookmarkClickedEvent event) {
        HistoryTableConnection.fetchHistory(true)
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

                        if (getResources().getBoolean(R.bool.two_pane)) {
                            clazz = PostItemListActivity.class;
                        } else {
                            clazz = PostItemDetailActivity.class;
                        }

                        final Intent intent = new Intent(PostItemListActivity.this, clazz);
                        intent.putExtras(args);
                        startActivity(intent);
                    }
                });
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
        if (listType == BOARD_LIST_ID) {
            toggleNavDrawer();
        } else {
            finish();
        }
    }

    @Override
    public void onPostItemClick(View v, List<ChanPost> posts, int position, String boardTitle, String boardName, int threadId) {
        openThread(posts, position, boardName, boardTitle);
    }

    public void openThread(final List<ChanPost> posts, final int position, final String boardName, final String boardTitle) {
        this.boardName = boardName;
        this.boardTitle = boardTitle;

        final ArrayList<ThreadInfo> threadList = new ArrayList<>(posts.size());
        for (final ChanPost post : posts) {
            final ThreadInfo threadInfo = new ThreadInfo(post.getNo(), boardName, boardTitle, false);
            threadList.add(threadInfo);
        }

        final Intent detailIntent = new Intent(this, PostItemDetailActivity.class);
        detailIntent.putExtra(Extras.EXTRAS_THREAD_LIST, threadList);
        detailIntent.putExtra(Extras.EXTRAS_POSITION, position);
        startActivity(detailIntent);
    }

    @Subscribe
    public void homeButtonPressed(HomeButtonPressedEvent event) {
        final Intent intent = new Intent(this, PostItemListActivity.class);
        startActivity(intent);
        finish();
    }

    @Subscribe
    public void openHistory(OpenHistoryEvent event) {
        final int bookmarkId;
        if (event.watched) {
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
}
