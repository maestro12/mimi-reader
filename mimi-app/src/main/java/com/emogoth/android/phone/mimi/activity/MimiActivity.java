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
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;
import com.emogoth.android.phone.mimi.db.HistoryTableConnection;
import com.emogoth.android.phone.mimi.db.model.History;
import com.emogoth.android.phone.mimi.event.HttpErrorEvent;
import com.emogoth.android.phone.mimi.event.UpdateHistoryEvent;
import com.emogoth.android.phone.mimi.fragment.NavDrawerFragment;
import com.emogoth.android.phone.mimi.interfaces.OnPostItemClickListener;
import com.emogoth.android.phone.mimi.interfaces.OnThumbnailClickListener;
import com.emogoth.android.phone.mimi.util.BusProvider;
import com.emogoth.android.phone.mimi.util.Extras;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.RefreshScheduler;
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.emogoth.android.phone.mimi.util.ThreadRegistry;
import com.mimireader.chanlib.models.ChanPost;

import java.util.List;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import rx.Subscription;
import rx.functions.Action1;


public abstract class MimiActivity extends AppCompatActivity implements PreferenceChangeListener,
        OnThumbnailClickListener,
        OnPostItemClickListener {

    protected static final String LOG_TAG = MimiActivity.class.getName();
    private static final boolean LOG_DEBUG = false;

    private static final String TAG_NAV_DRAWER = "nav_drawer";

    public static final int VIEWING_NONE = 0;
    public static final int VIEWING_BOOKMARKS = 1;
    public static final int VIEWING_HISTORY = 2;

    public static final int SETTINGS_ID = 10;
    public static final int RESTART_ACTIVITY_RESULT = 2;

    private RefreshScheduler refreshScheduler = RefreshScheduler.getInstance();
    private View navDrawerView;
    //    private View bookmarkDrawerView;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private int bookmarkCount;
    private View advertContainer;
    private Handler advertHideHandler;
    private boolean showBadge = true;
    private Fragment resultFragment;

    private LayerDrawable navigationDrawable;

    private int bookmarksOrHistory = 0;
    private Toolbar toolbar;

    private Subscription fetchPostSubscription;
//    private Toolbar toolbar;

    protected void onCreate(Bundle savedInstanceState) {
        try {
            final int currentTheme = getPackageManager().getActivityInfo(getComponentName(), 0).theme;
            if (currentTheme != R.style.Theme_Mimi_Gallery) {
                setTheme(MimiUtil.getInstance().getThemeResourceId());
            }
            getTheme().applyStyle(MimiUtil.getFontStyle(this), true);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();

            setTheme(MimiUtil.getInstance().getThemeResourceId());
        }

        MimiUtil.setScreenOrientation(this);

        super.onCreate(savedInstanceState);

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.containsKey(Extras.EXTRAS_SHOW_ACTIONBAR_BADGE)) {
                showBadge = extras.getBoolean(Extras.EXTRAS_SHOW_ACTIONBAR_BADGE, true);
            }
            if (extras.containsKey(Extras.EXTRAS_VIEWING_HISTORY)) {
                bookmarksOrHistory = extras.getInt(Extras.EXTRAS_VIEWING_HISTORY);
            }
        }

        advertHideHandler = new Handler();
        bookmarkCount = ThreadRegistry.getInstance().getUnreadCount();
    }

    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (drawerToggle != null) {
            // Sync the toggle state after onRestoreInstanceState has occurred.
            drawerToggle.syncState();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            BusProvider.getInstance().unregister(this);
            refreshScheduler.unregister(this);
        } catch (final Exception e) {
            Log.e(LOG_TAG, "Error unregistering", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        MimiUtil.setScreenOrientation(this);

        BusProvider.getInstance().register(this);
        refreshScheduler.register(this);

        if (advertContainer != null && advertContainer.getVisibility() == View.GONE) {
            advertContainer.setVisibility(View.VISIBLE);
            advertHideHandler.removeCallbacks(advertHideRunnable);
        }
    }

    protected void drawerItemSelected(final MenuItem item) {
        drawerToggle.onOptionsItemSelected(item);
    }

    public void toggleNavDrawer() {
        if (drawerLayout.isDrawerOpen(navDrawerView)) {
            drawerLayout.closeDrawer(navDrawerView);
        } else {
            drawerLayout.openDrawer(navDrawerView);
        }
    }

    @Override
    public void onBackPressed() {
        try {
            if (drawerLayout != null && navDrawerView != null && drawerLayout.isDrawerOpen(navDrawerView)) {
                toggleNavDrawer();
            } else {
                super.onBackPressed();
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Caught exception in onBackPressed()", e);
        }

    }

    protected void initDrawers(final int navRes, final int drawerLayoutRes, final boolean drawerIndicator) {
        navDrawerView = findViewById(navRes);
//        bookmarkDrawerView = findViewById(bookmarkRes);

        drawerLayout = (DrawerLayout) findViewById(drawerLayoutRes);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.app_name, R.string.app_name) {

            @Override
            public void onDrawerOpened(View drawerView) {
                final NavDrawerFragment nav = (NavDrawerFragment) getSupportFragmentManager().findFragmentByTag(TAG_NAV_DRAWER);
                nav.onAutoRefresh(null);

                supportInvalidateOptionsMenu();
                drawerToggle.syncState();

            }

            @Override
            public void onDrawerClosed(View drawerView) {
                supportInvalidateOptionsMenu();
                drawerToggle.syncState();
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
            }
        };

        final int count = ThreadRegistry.getInstance().getUnreadCount();
        if (drawerIndicator) {
            setNavigationIconWithBadge(R.drawable.ic_action_hamburger, count);
        } else {
            setNavigationIconWithBadge(R.drawable.ic_action_arrow_back, count);
        }
//        drawerToggle.setDrawerIndicatorEnabled(drawerIndicator);

//        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, bookmarkDrawerView);
        drawerLayout.setDrawerListener(drawerToggle);

    }

    public DrawerLayout getDrawerLayout() {
        return drawerLayout;
    }

    public ActionBarDrawerToggle getDrawerToggle() {
        return drawerToggle;
    }

    public void updateBadge(final int count) {
        setNavigationIconWithBadge(0, count);
    }

    protected void setNavigationIconWithBadge(final int drawableRes, final int count) {

        Log.d(LOG_TAG, "Setting nav icon: count=" + count);

        final Drawable[] layers;
        final LayerDrawable layerDrawable;
        if (drawableRes > 0) {
            if (count > 0) {
                layers = new Drawable[2];
                layers[0] = getResources().getDrawable(drawableRes);
                layers[1] = getResources().getDrawable(R.drawable.notification_unread);
            } else {
                layers = new Drawable[1];
                layers[0] = getResources().getDrawable(drawableRes);
            }

            layerDrawable = new LayerDrawable(layers);
        } else {
            final Drawable navDrawable = getToolbar().getNavigationIcon();
            if (navDrawable instanceof LayerDrawable) {
                final LayerDrawable ld = (LayerDrawable) navDrawable;
                final Drawable icon = ld.getDrawable(0);

                if (count > 0) {
                    layers = new Drawable[2];
                    layers[0] = icon;
                    layers[1] = getResources().getDrawable(R.drawable.notification_unread);
                } else {
                    layers = new Drawable[1];
                    layers[0] = icon;
                }

                layerDrawable = new LayerDrawable(layers);
            } else {
                layerDrawable = null;
            }
        }

        if (layerDrawable != null) {
            getToolbar().setNavigationIcon(layerDrawable);
        }

    }

    protected void createDrawers(final int navRes) {
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        final NavDrawerFragment navDrawerFragment = new NavDrawerFragment();
        final Bundle args = new Bundle();
//        bookmarkDrawerFragment = new BookmarkDrawerFragment();
        args.putInt(Extras.EXTRAS_VIEWING_HISTORY, bookmarksOrHistory);

//        bookmarkDrawerFragment.setArguments(args);

        ft.add(navRes, navDrawerFragment, TAG_NAV_DRAWER);

        ft.commit();
    }

    public int getBookmarkCount() {
        return this.bookmarkCount;
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent pce) {
        Log.i(LOG_TAG, "Preference Changed: name=" + pce.getKey() + ", value=" + pce.getNewValue());
    }

    public Toolbar getToolbar() {
        return this.toolbar;
    }

    public void setToolbar(final Toolbar toolbar) {
        this.toolbar = toolbar;

        if (this.toolbar != null) {
            this.toolbar.setLogo(null);
        }
    }

    public boolean showBadge() {
        return this.showBadge;
    }

    public void setResultFragment(final Fragment resultFragment) {
        this.resultFragment = resultFragment;
    }

    public Fragment getResultFragment() {
        return resultFragment;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SETTINGS_ID) {
            if (resultCode == RESTART_ACTIVITY_RESULT) {
                startActivity(new Intent(this, StartupActivity.class));
                finish();
            }
        } else if (resultFragment != null) {
            resultFragment.onActivityResult(requestCode, resultCode, data);
        }

    }

    @Override
    public void onThumbnailClick(List<ChanPost> posts, int threadId, int position, String boardName) {
        final Bundle args = new Bundle();
        args.putInt(Extras.EXTRAS_GALLERY_TYPE, 1);
        ThreadRegistry.getInstance().setPosts(threadId, posts);
//        args.putParcelableArrayList(Extras.EXTRAS_POST_LIST, GalleryPagerAdapter.getPostsWithImages(posts));
        args.putInt(Extras.EXTRAS_THREAD_ID, threadId);
        args.putInt(Extras.EXTRAS_POSITION, position);
        args.putString(Extras.EXTRAS_BOARD_NAME, boardName);

        final Intent galleryIntent = new Intent(this, GalleryActivity.class);
        galleryIntent.putExtras(args);
        galleryIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

        startActivity(galleryIntent);
    }

    final Runnable advertHideRunnable = new Runnable() {
        @Override
        public void run() {
            if (advertContainer != null) {
                advertContainer.setVisibility(View.VISIBLE);
            }
        }
    };

    public void onAutoRefresh(final UpdateHistoryEvent event) {

        if (LOG_DEBUG) {
            Log.i(LOG_TAG, "Updating thread registry");
        }

        RxUtil.safeUnsubscribe(fetchPostSubscription);
        fetchPostSubscription = HistoryTableConnection.fetchPost(event.getBoardName(), event.getThreadId())
                .compose(DatabaseUtils.<History>applySchedulers())
                .subscribe(new Action1<History>() {
                    @Override
                    public void call(History history) {
                        if (history == null) {
                            return;
                        }

                        final boolean watched = history.watched;
                        if (event.getThread() != null && event.getThread().getPosts() != null && event.getThread().getPosts().get(0).isClosed()) {
                            ThreadRegistry.getInstance().remove(event.getThread().getThreadId());
                        } else {
                            ThreadRegistry.getInstance().update(event.getThreadId(), event.getThreadSize(), watched);
                            if (watched) {
                                updateBadge(ThreadRegistry.getInstance().getUnreadCount());
                            }
                        }
                    }
                });
    }

    public void onHttpError(final HttpErrorEvent event) {
        Log.e(LOG_TAG, "Error updating thread registry");

        RxUtil.safeUnsubscribe(fetchPostSubscription);
        fetchPostSubscription = HistoryTableConnection.fetchPost(event.getThreadInfo().boardName, event.getThreadInfo().threadId)
                .compose(DatabaseUtils.<History>applySchedulers())
                .subscribe(new Action1<History>() {
                    @Override
                    public void call(History history) {
                        if (history == null) {
                            return;
                        }

                        final boolean watched = history.watched;
                        ThreadRegistry.getInstance().update(event.getThreadInfo().threadId, -1, watched);
                        if (watched) {
                            setNavigationIconWithBadge(0, ThreadRegistry.getInstance().getUnreadCount());
                        }
                    }
                });
    }

    @Override
    public void onPostItemClick(View v, List<ChanPost> posts, int position, String boardTitle, String boardName, int threadId) {
        // no op
    }

    protected abstract String getPageName();

}
