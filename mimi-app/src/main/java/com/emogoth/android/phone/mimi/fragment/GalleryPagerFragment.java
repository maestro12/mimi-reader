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

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.activity.GalleryActivity;
import com.emogoth.android.phone.mimi.activity.MimiActivity;
import com.emogoth.android.phone.mimi.adapter.GalleryPagerAdapter;
import com.emogoth.android.phone.mimi.event.GalleryGridButtonEvent;
import com.emogoth.android.phone.mimi.event.GalleryImageTouchEvent;
import com.emogoth.android.phone.mimi.event.GalleryPagerScrolledEvent;
import com.emogoth.android.phone.mimi.fourchan.FourChanConnector;
import com.emogoth.android.phone.mimi.util.BusProvider;
import com.emogoth.android.phone.mimi.util.Extras;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.emogoth.android.phone.mimi.util.ThreadRegistry;
import com.mimireader.chanlib.ChanConnector;
import com.mimireader.chanlib.models.ChanPost;
import com.mimireader.chanlib.models.ChanThread;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.util.List;

import rx.Subscription;
import rx.functions.Action1;


public class GalleryPagerFragment extends MimiFragmentBase implements Toolbar.OnMenuItemClickListener {
    public static final String TAG = "gallery_pager_fragment";
    private static final String LOG_TAG = GalleryPagerFragment.class.getSimpleName();

    private static final int AD_SPACING = 9;

    private List<ChanPost> posts;
    private int threadId;
    private int postNumber;
    private String boardName;
    private String boardTitle;

    private ViewPager galleryPager;
    private GalleryPagerAdapter galleryPagerAdapter;
    private int pagerPosition;

    private GalleryImageBase currentImageFragment;
    private Toolbar toolbar;

    private TextView fileSizeTextView;
    private TextView fileNameTextView;
    private ViewGroup galleryToolbar;

    private GalleryImageBase permissionsRequstedFragment;
    private Subscription fetchPostsSubscription;
    private boolean scrollThreadWithGallery;
    private boolean closeOnClick;
    private ViewPager.OnPageChangeListener galleryPageChangeListener;


    public GalleryPagerFragment() {
        setHasOptionsMenu(true);
        setRetainInstance(false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        pagerPosition = 0;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final MimiActivity activity = (MimiActivity) getActivity();

        final Bundle args = getArguments();
        if (savedInstanceState != null) {
            extractExtras(savedInstanceState);
        } else {
            extractExtras(args);
        }

        if (threadId == 0 || TextUtils.isEmpty(boardName)) {
            throw new IllegalArgumentException("One or more required arguments was not set for the gallery");
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        scrollThreadWithGallery = prefs.getBoolean(getString(R.string.scroll_thread_with_gallery_pref), true);
        closeOnClick = prefs.getBoolean(getString(R.string.close_gallery_on_click_pref), true);

        toolbar = activity.getToolbar();
        toolbar.setTitle(R.string.gallery);

        initMenu();

        final View v = inflater.inflate(R.layout.fragment_gallery_pager, container, false);
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ImageView gridButton = (ImageView) view.findViewById(R.id.grid_button);
        gridButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final GalleryGridButtonEvent event = new GalleryGridButtonEvent();
                event.setButton(v);
                BusProvider.getInstance().post(event);
            }
        });

        galleryToolbar = (ViewGroup) view.findViewById(R.id.gallery_toolbar);

        fileNameTextView = (TextView) view.findViewById(R.id.file_name);
        fileSizeTextView = (TextView) view.findViewById(R.id.file_size);

        updateToolbar(pagerPosition);

        galleryPager = (ViewPager) view.findViewById(R.id.gallery_pager);
//        galleryPager.setOffscreenPageLimit(4);

        if (posts == null || posts.size() == 0) {
            fetchPosts();
        } else {
            Log.i(LOG_TAG, "[onResponse] Creating pager adapter");

            galleryPagerAdapter = new GalleryPagerAdapter(getChildFragmentManager(), posts, boardName, null);
            galleryPager.setAdapter(galleryPagerAdapter);
        }

        galleryPager.addOnPageChangeListener(createPageChangeListener());

        if (pagerPosition >= 0 && getView() != null) {
            getView().post(new Runnable() {
                @Override
                public void run() {
                    try {
                        galleryPager.setCurrentItem(pagerPosition, false);
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "Could not set gallery pager position", e);
                    }
                }
            });
        }

        if (galleryPagerAdapter != null) {
            currentImageFragment = (GalleryImageBase) galleryPagerAdapter.instantiateItem(galleryPager, pagerPosition);
            currentImageFragment.initMenu();
        }

    }

    private ViewPager.OnPageChangeListener createPageChangeListener() {
        if (galleryPageChangeListener == null) {
            galleryPageChangeListener = new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
                    if (currentImageFragment == null && galleryPagerAdapter != null) {
                        currentImageFragment = (GalleryImageBase) galleryPagerAdapter.instantiateItem(galleryPager, position);
                    }
                }

                @Override
                public void onPageSelected(final int position) {
                    if (galleryPagerAdapter != null) {
                        final GalleryImageBase fragment = (GalleryImageBase) galleryPagerAdapter.instantiateItem(galleryPager, position);
                        if (fragment != currentImageFragment) {
                            fragment.initMenu();

                            currentImageFragment = fragment;

                            if (scrollThreadWithGallery && position < posts.size()) {
                                BusProvider.getInstance().post(new GalleryPagerScrolledEvent(posts.get(position).getNo()));
                            }
                        }

                        if (posts != null) {
                            if (posts.size() > 1) {
                                toolbar.setSubtitle((position + 1) + " / " + posts.size());
                            }
                            updateToolbar(position);
                        }

                        pagerPosition = position;
                    }
                }

                @Override
                public void onPageScrollStateChanged(final int i) {

                }
            };
        }

        return galleryPageChangeListener;
    }

    @Override
    public void initMenu() {
        super.initMenu();

        if (toolbar != null) {
            toolbar.getMenu().clear();
            toolbar.inflateMenu(R.menu.image_menu);
            toolbar.setOnMenuItemClickListener(this);
        }
    }

    private void fetchPosts() {
        if (getActivity() == null) {
            return;
        }

        final ChanConnector chanConnector = new FourChanConnector.Builder()
                .setEndpoint(FourChanConnector.getDefaultEndpoint(MimiUtil.isSecureConnection(getActivity())))
                .setPostEndpoint(FourChanConnector.getDefaultPostEndpoint())
                .build();

        RxUtil.safeUnsubscribe(fetchPostsSubscription);
        fetchPostsSubscription = chanConnector
                .fetchThread(getActivity(), boardName, threadId)
                .subscribe(processResponse(), processError());
    }

    private Action1<ChanThread> processResponse() {
        return new Action1<ChanThread>() {
            @Override
            public void call(ChanThread thread) {
                if (getActivity() != null && thread != null) {
                    posts = GalleryPagerAdapter.getPostsWithImages(thread);

                    Log.i(LOG_TAG, "[onResponse] Creating pager adapter");
                    galleryPagerAdapter = new GalleryPagerAdapter(getChildFragmentManager(), posts, boardName, null);
                    galleryPager.setAdapter(galleryPagerAdapter);

                    if (posts != null) {
                        if (posts.size() > 1) {
                            toolbar.setSubtitle("1 / " + posts.size());
                        }
                        updateToolbar(0);
                    }
                }
            }
        };
    }

    private Action1<Throwable> processError() {
        return new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                Log.e(LOG_TAG, "Error fetching thread images", throwable);
                Toast.makeText(getActivity(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void extractExtras(final Bundle bundle) {
        if (bundle.containsKey(Extras.EXTRAS_POSITION)) {
            pagerPosition = bundle.getInt(Extras.EXTRAS_POSITION);
        }
        if (bundle.containsKey(Extras.EXTRAS_THREAD_ID)) {
            threadId = bundle.getInt(Extras.EXTRAS_THREAD_ID);
        }
        if (bundle.containsKey(Extras.EXTRAS_BOARD_NAME)) {
            boardName = bundle.getString(Extras.EXTRAS_BOARD_NAME);
        }
        if (bundle.containsKey(Extras.EXTRAS_BOARD_TITLE)) {
            boardTitle = bundle.getString(Extras.EXTRAS_BOARD_TITLE);
        }
        if (bundle.containsKey(Extras.EXTRAS_POST_LIST)) {
            bundle.setClassLoader(ChanPost.class.getClassLoader());
            posts = bundle.getParcelableArrayList(Extras.EXTRAS_POST_LIST);
        } else {
            posts = ThreadRegistry.getInstance().getPosts(threadId);
        }

    }

    private void updateToolbar(final int pos) {
        if (posts != null && posts.size() > 0) {
            try {
                final String fileName = posts.get(pos).getFilename() + posts.get(pos).getExt();
                fileNameTextView.setText(fileName);

                final String fileSize = MimiUtil.humanReadableByteCount(posts.get(pos).getFsize(), true);
                fileSizeTextView.setText(fileSize);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error updating toolbar", e);
            }
        }
    }

//    @Subscribe
//    public void onGalleryImageTouchEvent(final GalleryImageTouchEvent event) {
//        if (galleryToolbar.getVisibility() == View.VISIBLE) {
//            galleryToolbar.setVisibility(View.INVISIBLE);
//        } else {
//            galleryToolbar.setVisibility(View.VISIBLE);
//        }
//    }

    @Override
    public void onStop() {
        super.onStop();

        if (toolbar != null) {
            toolbar.getMenu().clear();
        }

        galleryPager.clearOnPageChangeListeners();

        if (fetchPostsSubscription != null && !fetchPostsSubscription.isUnsubscribed()) {
            fetchPostsSubscription.unsubscribe();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        toolbar.setSubtitle(null);
        BusProvider.getInstance().unregister(this);

        if (galleryPager != null) {
            galleryPager.removeOnPageChangeListener(createPageChangeListener());
        }

        RxUtil.safeUnsubscribe(fetchPostsSubscription);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() != null && posts != null && posts.size() > 0) {
            final MimiActivity activity = ((MimiActivity) getActivity());
            activity.getToolbar().setTitle(R.string.gallery);
            activity.getToolbar().setSubtitle((pagerPosition + 1) + " / " + posts.size());

            if (galleryPager != null) {
                galleryPager.removeOnPageChangeListener(createPageChangeListener());
                galleryPager.addOnPageChangeListener(createPageChangeListener());
            }

            if (currentImageFragment != null) {
                currentImageFragment.initMenu();
            }
        }

        BusProvider.getInstance().register(this);
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        menu.clear();
        inflater.inflate(R.menu.image_menu, menu);

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
//        outState.putParcelableArrayList(Extras.EXTRAS_POST_LIST, posts);
        outState.putInt(Extras.EXTRAS_POSITION, pagerPosition);
        outState.putString(Extras.EXTRAS_BOARD_NAME, boardName);
        outState.putInt(Extras.EXTRAS_THREAD_ID, threadId);

        super.onSaveInstanceState(outState);
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
        return "gallery_pager";
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        if (item.getItemId() == R.id.save_menu) {
            if (galleryPager != null && galleryPagerAdapter != null) {
                final GalleryImageBase fragment = (GalleryImageBase) galleryPagerAdapter.instantiateItem(galleryPager, galleryPager.getCurrentItem());
                saveFile(fragment);
            }

            return true;
        } else if (item.getItemId() == R.id.share_menu) {

            if (getActivity() == null) {
                return true;
            }

            final GalleryImageBase fragment = (GalleryImageBase) galleryPagerAdapter.instantiateItem(galleryPager, galleryPager.getCurrentItem());
            final View menuView = getActivity().findViewById(R.id.share_menu);
            PopupMenu popupMenu = new PopupMenu(getActivity(), menuView);
            popupMenu.inflate(R.menu.share_popup);
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (getActivity() == null) {
                        return true;
                    }

                    final Intent shareIntent = new Intent();
                    if (item.getItemId() == R.id.share_link) {
                        shareIntent.setAction(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_TEXT,
                                MimiUtil.httpOrHttps(getActivity()) + getResources().getString(R.string.image_link) + getResources().getString(R.string.full_image_path,
                                        boardName,
                                        fragment.getTim(),
                                        fragment.getFileExt()));
                        shareIntent.setType("text/plain");
                    } else {
                        final File shareFile = fragment.getImageFileLocation();

                        if (shareFile != null && shareFile.exists()) {
                            final Uri uri = FileProvider.getUriForFile(getActivity(), getString(R.string.fileprovider_authority), shareFile);
                            final String type;
                            if (shareFile.getName().endsWith(".webm")) {
                                type = "video/webm";
                            } else {
                                type = "image/*";
                            }

                            shareIntent.setAction(Intent.ACTION_SEND);
                            shareIntent.setDataAndType(uri, type);
                            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                        }
                    }

                    startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
                    return true;
                }
            });

            popupMenu.show();

            return true;
        }

        return false;
    }

    private void saveFile(final GalleryImageBase fragment) {
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                Snackbar.make(galleryPager, R.string.app_needs_your_permission_to_save, Snackbar.LENGTH_LONG).show();

            } else {

                permissionsRequstedFragment = fragment;
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        GalleryActivity.PERMISSIONS_REQUEST_EXTERNAL_STORAGE);

            }
        } else {
            fragment.safeSaveFile(true);
        }
    }

    @Override
    public boolean onBackPressed() {
        return super.onBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case GalleryActivity.PERMISSIONS_REQUEST_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (permissionsRequstedFragment != null) {
                        permissionsRequstedFragment.safeSaveFile(true);
                        permissionsRequstedFragment = null;
                    }

                } else {

                    Snackbar.make(galleryPager, R.string.save_file_permission_denied, Snackbar.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}
