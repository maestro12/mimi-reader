package com.emogoth.android.phone.mimi.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.autorefresh.RefreshScheduler;
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
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.emogoth.android.phone.mimi.util.ThreadRegistry;
import com.mimireader.chanlib.models.ChanPost;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


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
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private boolean showBadge = true;
    private Fragment resultFragment;

    private int bookmarksOrHistory = 0;
    private Toolbar toolbar;

    private Disposable fetchPostSubscription;

    private int savedRequestCode;
    private int savedResultCode;
    private Intent savedIntentData;

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
            Log.e(LOG_TAG, "Caught crash during onPause()", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MimiUtil.setScreenOrientation(this);

        try {
            BusProvider.getInstance().register(this);
        } catch (Exception e) {
            Log.w(LOG_TAG, "Exception caught when registering bus provider", e);
        }
        refreshScheduler.register(this);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (drawerLayout != null && drawerToggle != null) {
            drawerLayout.removeDrawerListener(drawerToggle);
        }
    }

    protected void initDrawers(final int navRes, final int drawerLayoutRes, final boolean drawerIndicator) {
        navDrawerView = findViewById(navRes);

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

        drawerToggle.setDrawerIndicatorEnabled(false);

        if (drawerIndicator) {
            updateBadge(R.drawable.ic_nav_menu);
        } else {
            updateBadge(R.drawable.ic_nav_arrow_back);
        }

        drawerLayout.addDrawerListener(drawerToggle);

    }

    public DrawerLayout getDrawerLayout() {
        return drawerLayout;
    }

    public ActionBarDrawerToggle getDrawerToggle() {
        return drawerToggle;
    }

    public void updateBadge(@DrawableRes final int navDrawable) {
        HistoryTableConnection.fetchHistory(true)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .single(new ArrayList<>())
                .subscribe(new SingleObserver<List<History>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        // no op
                    }

                    @Override
                    public void onSuccess(List<History> histories) {

                        int unread = 0;
                        for (History history : histories) {
                            unread += (history.threadSize - 1 - history.lastReadPosition);
                        }

                        setNavigationIconWithBadge(navDrawable, unread);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(LOG_TAG, "Error setting unread count badge", e);
                    }
                });

    }

    protected void setNavigationIconWithBadge(final int drawableRes, final int count) {

        Log.d(LOG_TAG, "Setting nav icon: count=" + count);

        final Drawable[] layers;
        final LayerDrawable layerDrawable;
        if (drawableRes > 0) {
            if (count > 0) {
                layers = new Drawable[2];
                layers[0] = VectorDrawableCompat.create(getResources(), drawableRes, getTheme());
                layers[1] = ResourcesCompat.getDrawable(getResources(), R.drawable.notification_unread, getTheme());
            } else {
                layers = new Drawable[1];
                layers[0] = VectorDrawableCompat.create(getResources(), drawableRes, getTheme());
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
                    layers[1] = ResourcesCompat.getDrawable(getResources(), R.drawable.notification_unread, getTheme());
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
            drawerToggle.setHomeAsUpIndicator(layerDrawable);
        }

    }

    protected void createDrawers(final int navRes) {
        final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        final NavDrawerFragment navDrawerFragment = new NavDrawerFragment();
        final Bundle args = new Bundle();

        args.putInt(Extras.EXTRAS_VIEWING_HISTORY, bookmarksOrHistory);

        ft.add(navRes, navDrawerFragment, TAG_NAV_DRAWER);
        ft.commit();
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
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    public boolean showBadge() {
        return this.showBadge;
    }

    public void setResultFragment(final Fragment resultFragment, final boolean dispatchResults) {
        this.resultFragment = resultFragment;

        if (dispatchResults && savedIntentData != null) {
            resultFragment.onActivityResult(savedRequestCode, savedResultCode, savedIntentData);
        }
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
        } else {
            savedRequestCode = requestCode;
            savedResultCode = resultCode;
            savedIntentData = data;
        }
    }

    @Override
    public void onThumbnailClick(final List<ChanPost> posts, final long threadId, final int position, final String boardName) {
        final long id;
        try {
        if (position < 0 || posts.size() <= position) {
            Exception e = new Exception("Could not locate post in post list: position=" + position + ", list size=" + posts.size());

                Log.e(LOG_TAG, "Error opening gallery into a post", e);
                id = threadId;
            } else {
                id = posts.get(position).getNo();
            }
            ThreadRegistry.getInstance().setPosts(threadId, posts);
            GalleryActivity2.start(this, GalleryActivity2.GALLERY_TYPE_PAGER, id, boardName, threadId, new long[0]);

        } catch (Exception e) {
            Log.e(LOG_TAG, "Could not open thumbnail: posts.size()=" + posts.size() + ", position=" + position);
            Toast.makeText(this, R.string.error_opening_gallery, Toast.LENGTH_SHORT).show();
        }
    }

    public void onAutoRefresh(final UpdateHistoryEvent event) {

        if (LOG_DEBUG) {
            Log.i(LOG_TAG, "Updating thread registry");
        }

        if (event.isWatched()) {
            updateBadge(0);
        }
    }

    public void onHttpError(final HttpErrorEvent event) {
        Log.e(LOG_TAG, "Error updating thread registry");

        RxUtil.safeUnsubscribe(fetchPostSubscription);
        fetchPostSubscription = HistoryTableConnection.fetchPost(event.getThreadInfo().boardName, event.getThreadInfo().threadId)
                .compose(DatabaseUtils.applySchedulers())
                .subscribe(history -> {
                    if (history.threadId == -1) {
                        return;
                    }

                    if (history.watched == 1) {
                        updateBadge(0);
                    }
                });
    }

    @Override
    public void onPostItemClick(View v, List<ChanPost> posts, int position, String boardTitle, String boardName, long threadId) {
        // no op
    }

    protected abstract String getPageName();

}
