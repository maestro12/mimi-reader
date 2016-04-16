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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.event.GalleryGridButtonEvent;
import com.emogoth.android.phone.mimi.event.GalleryImageTouchEvent;
import com.emogoth.android.phone.mimi.fragment.GalleryGridFragment;
import com.emogoth.android.phone.mimi.fragment.GalleryPagerFragment;
import com.emogoth.android.phone.mimi.fragment.MimiFragmentBase;
import com.emogoth.android.phone.mimi.interfaces.OnThumbnailClickListener;
import com.emogoth.android.phone.mimi.util.Extras;
import com.emogoth.android.phone.mimi.util.ThreadRegistry;
import com.mimireader.chanlib.models.ChanPost;
import com.squareup.otto.Subscribe;

import java.util.List;


public class GalleryActivity extends MimiActivity implements OnThumbnailClickListener, View.OnClickListener {
    private static final String LOG_TAG = GalleryActivity.class.getSimpleName();
    private static final boolean LOG_DEBUG = false;
    private static final String GRID_BACKSTACK_TAG = "gallery_grid";
    private static final String GALLERY_FRAGMENT_TAG = "gallery_fragment";

    public static final int GALLERY_TYPE_GRID = 0;
    public static final int GALLERY_TYPE_PAGER = 1;

    public static final int PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 100;

    private View decorView;
    private boolean systemUiVisible = true;
    private int defaultSystemUiVisibility;

    //    private View advertContainer;
    private Handler advertHideHandler;

    private MimiFragmentBase currentFragment;
    private int threadId;
//    private boolean configChange = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        decorView = getWindow().getDecorView();
        defaultSystemUiVisibility = decorView.getSystemUiVisibility();

        setContentView(R.layout.activity_gallery);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.mimi_toolbar);
        if (toolbar != null) {
            setToolbar(toolbar);

            toolbar.setNavigationIcon(R.drawable.ic_action_arrow_back);
            toolbar.setNavigationOnClickListener(this);
        }

        final FragmentManager fm = getSupportFragmentManager();
        final FragmentTransaction ft = fm.beginTransaction();
        if (savedInstanceState == null) {
            final int galleryType;
            final Bundle args = getIntent().getExtras();

            threadId = args.getInt(Extras.EXTRAS_THREAD_ID, 0);
            if (args != null) {
                galleryType = args.getInt(Extras.EXTRAS_GALLERY_TYPE, 0);
            } else {
                galleryType = 0;
            }

            final MimiFragmentBase frag;
            final String tag;
            if (galleryType == GALLERY_TYPE_PAGER) {
                frag = new GalleryPagerFragment();
                tag = GalleryPagerFragment.TAG;
            } else {
                frag = new GalleryGridFragment();
                tag = GalleryGridFragment.TAG;
            }

            frag.setArguments(args);

            ft.add(R.id.gallery_container, frag, tag);
            ft.commit();

            currentFragment = frag;
        } else {
            MimiFragmentBase frag = (MimiFragmentBase) fm.findFragmentByTag(GalleryPagerFragment.TAG);

            if (frag == null) {
                frag = (MimiFragmentBase) fm.findFragmentByTag(GalleryGridFragment.TAG);
            }

            if (frag != null) {
                ft.show(frag);
                ft.commit();
            }

            currentFragment = frag;
        }

        advertHideHandler = new Handler();

    }

    public void toggleFullscreen() {
        if (systemUiVisible) {
            systemUiVisible = false;
            hideSystemUI();
        } else {
            systemUiVisible = true;
            showSystemUI();
        }
    }

    public void setFullscreen(final boolean show) {
        systemUiVisible = show;
        if (show) {
            showSystemUI();
        } else {
            hideSystemUI();
        }
    }

    // This snippet hides the system bars.
    private void hideSystemUI() {


        getToolbar().setVisibility(View.GONE);
    }

    private void showSystemUI() {
        decorView.setSystemUiVisibility(defaultSystemUiVisibility);
        getToolbar().setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onThumbnailClick(List<ChanPost> posts, int threadId, int position, String boardName) {
        final Bundle args = new Bundle();
        ThreadRegistry.getInstance().setPosts(threadId, posts);
//        args.putParcelableArrayList(Extras.EXTRAS_POST_LIST, posts);
        args.putInt(Extras.EXTRAS_THREAD_ID, threadId);
        args.putInt(Extras.EXTRAS_POSITION, position);
//        Log.i(LOG_TAG, "thumbnail click position=" + position);
        args.putString(Extras.EXTRAS_BOARD_NAME, boardName);

        final GalleryPagerFragment fragment = new GalleryPagerFragment();
        fragment.setArguments(args);

        final FragmentManager fm = getSupportFragmentManager();
        final FragmentTransaction ft = fm.beginTransaction();

        if (LOG_DEBUG) {
            Log.i(LOG_TAG, "Showing gallery");
        }

        ft.add(R.id.gallery_container, fragment, GalleryPagerFragment.TAG);
        ft.addToBackStack(GRID_BACKSTACK_TAG);
        ft.commit();

        currentFragment = fragment;
    }

    @Override
    protected void onStop() {
        super.onStop();
        ThreadRegistry.getInstance().clearPosts(threadId);
    }

    @Subscribe
    public void onGalleryImageTouchEvent(final GalleryImageTouchEvent event) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean closeOnClick = prefs.getBoolean(getString(R.string.close_gallery_on_click_pref), true);

        if(closeOnClick) {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (currentFragment != null) {
            if (!currentFragment.onBackPressed()) {
                super.onBackPressed();

                if (currentFragment instanceof GalleryPagerFragment) {
                    currentFragment = (MimiFragmentBase) getSupportFragmentManager().findFragmentByTag(GalleryGridFragment.TAG);

                    if (currentFragment != null) {
                        currentFragment.initMenu();
                    }
                }
            }
        } else {
            super.onBackPressed();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (currentFragment != null) {
            currentFragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Subscribe
    public void onGalleryGridButtonEvent(final GalleryGridButtonEvent event) {
        final FragmentManager fm = getSupportFragmentManager();
        final int backstackCount = fm.getBackStackEntryCount();
        if (backstackCount > 0) {
            fm.popBackStack();
        } else {
            final Bundle args = getIntent().getExtras();
            final GalleryGridFragment frag = new GalleryGridFragment();
            final Fragment pagerFragment = fm.findFragmentByTag(GalleryPagerFragment.TAG);
            final FragmentTransaction ft = fm.beginTransaction();

            frag.setArguments(args);

            ft.remove(pagerFragment);
            ft.add(R.id.gallery_container, frag, GalleryGridFragment.TAG);
            ft.commit();

            currentFragment = frag;
        }
    }

    @Override
    protected String getPageName() {
        return null;
    }

    @Override
    public void onClick(View v) {
        onBackPressed();
    }
}
