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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.Toast;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.activity.GalleryActivity;
import com.emogoth.android.phone.mimi.activity.MimiActivity;
import com.emogoth.android.phone.mimi.adapter.GalleryGridAdapter;
import com.emogoth.android.phone.mimi.adapter.GalleryPagerAdapter;
import com.emogoth.android.phone.mimi.fourchan.FourChanConnector;
import com.emogoth.android.phone.mimi.interfaces.OnThumbnailClickListener;
import com.emogoth.android.phone.mimi.service.DownloadService;
import com.emogoth.android.phone.mimi.util.Extras;
import com.emogoth.android.phone.mimi.util.HttpClientFactory;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.emogoth.android.phone.mimi.util.ThreadRegistry;
import com.mimireader.chanlib.ChanConnector;
import com.mimireader.chanlib.models.ChanPost;
import com.mimireader.chanlib.models.ChanThread;

import java.util.ArrayList;
import java.util.List;

import retrofit2.adapter.rxjava.HttpException;
import rx.Subscription;
import rx.functions.Action1;


public class GalleryGridFragment extends MimiFragmentBase {
    private static final String LOG_TAG = GalleryImageFragment.class.getSimpleName();
    public static final String TAG = "gallery_grid_fragment";

    private RecyclerView galleryGrid;
    private GalleryGridAdapter galleryAdapter;

    private String boardName;
    private String boardTitle;
    private boolean twoPane;
    private int currentPage;
    private int mActivatedPosition;
    private boolean rotated;
    private int threadId;

    private List<ChanPost> posts = new ArrayList<>();
    private ActionMode actionMode;
    private MenuItem downloadMenuItem;
    private MenuItem selectAllMenuItem;
    private MenuItem selectNoneMenuItem;
    private ChanConnector chanConnector;

    private Subscription fetchThreadSubscription;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        setRetainInstance(false);

        if (savedInstanceState != null) {
            extractExtras(savedInstanceState);
        } else {
            extractExtras(getArguments());
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_gallery_grid, container, false);

        chanConnector = new FourChanConnector.Builder()
                .setClient(HttpClientFactory.getInstance().getOkHttpClient())
                .setEndpoint(FourChanConnector.getDefaultEndpoint(MimiUtil.isSecureConnection(getActivity())))
                .setPostEndpoint(FourChanConnector.getDefaultPostEndpoint())
                .build();

        initMenu();

        return v;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (preferences.getBoolean(getString(R.string.show_batch_download_tutorial), true)) {
            showBatchDownloadTutorial();
        }

        galleryGrid = (RecyclerView) view.findViewById(R.id.gallery_grid);
        galleryGrid.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        galleryAdapter = new GalleryGridAdapter(getActivity(), getChildFragmentManager(), boardName, posts, chanConnector);
        galleryGrid.setAdapter(galleryAdapter);

        if (posts == null || posts.size() <= 0) {
            fetchPosts();
        }

        galleryAdapter.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(LOG_TAG, "thumbnail long clicked");
                startActionMode();

                return true;
            }
        });

        galleryAdapter.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (galleryAdapter != null) {
                    if (galleryAdapter.isBatchDownload()) {
                        galleryAdapter.toggleSelectedItem(position);

                    } else {
                        if (posts != null && getActivity() != null && isAdded()) {
                            ((OnThumbnailClickListener) getActivity()).onThumbnailClick(posts, threadId, position, boardName);
                        }
                    }
                }
            }
        });

    }

    private ActionMode.Callback getActionModeCallback() {
        return new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(final ActionMode mode, final Menu menu) {
                final MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.batch_download, menu);

                downloadMenuItem = menu.findItem(R.id.download);
                selectAllMenuItem = menu.findItem(R.id.select_all);
                selectNoneMenuItem = menu.findItem(R.id.select_none);

                actionMode = mode;

                return true;
            }

            @Override
            public boolean onPrepareActionMode(final ActionMode actionMode, final Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(final ActionMode actionMode, final MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.download) {
                    downloadIfPermission();
                    return true;
                } else if (menuItem.getItemId() == R.id.select_all) {
                    if (galleryAdapter != null) {
                        selectAllMenuItem.setVisible(false);
                        selectNoneMenuItem.setVisible(true);

                        galleryAdapter.selectAll();

                        return true;
                    }

                    return false;
                } else if (menuItem.getItemId() == R.id.select_none) {
                    if (galleryAdapter != null) {
                        selectAllMenuItem.setVisible(true);
                        selectNoneMenuItem.setVisible(false);

                        galleryAdapter.selectNone();

                        return true;
                    }

                    return false;
                } else if (menuItem.getItemId() == R.id.invert) {
                    if (galleryAdapter != null) {

                        galleryAdapter.invertSelection();

                        return true;
                    }

                    return false;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(final ActionMode actionMode) {
                if (getActivity() instanceof MimiActivity) {
                    final MimiActivity activity = (MimiActivity) getActivity();

                    if (activity.getToolbar() != null) {
                        activity.getToolbar().setVisibility(View.VISIBLE);
                        Log.i(LOG_TAG, "onDestroyActionMode - Setting toolbar visible");
                    }
                }

                if (galleryAdapter != null) {
                    galleryAdapter.setBatchDownload(false);
                }
            }
        };
    }

    private void startActionMode() {
        if (getActivity() instanceof MimiActivity) {
            final MimiActivity activity = (MimiActivity) getActivity();

            if (galleryAdapter != null) {
                if (galleryAdapter.isBatchDownload()) {

                    if (actionMode != null) {
                        actionMode.finish();
                        actionMode = null;
                    }
                } else {
                    activity.startSupportActionMode(getActionModeCallback());
                    if (activity.getToolbar() != null) {
                        activity.getToolbar().setVisibility(View.GONE);
                        Log.i(LOG_TAG, "onItemLongClick - Setting toolbar gone");
                    }
                    galleryAdapter.setBatchDownload(true);

                }

            }

        }
    }

    private void downloadIfPermission() {
        if (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                Snackbar.make(galleryGrid, R.string.app_needs_your_permission_to_save, Snackbar.LENGTH_LONG).show();

            } else {

                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        GalleryActivity.PERMISSIONS_REQUEST_EXTERNAL_STORAGE);

            }
        } else {
            startBatchDownload();
        }
    }

    private void startBatchDownload() {
        if (galleryAdapter != null && getActivity() != null && isAdded()) {
            final ArrayList<ChanPost> selectedPosts = galleryAdapter.getSelectedPosts();

            if (selectedPosts.size() > 0) {
                final Intent downloadIntent = new Intent(getActivity(), DownloadService.class);
                final Bundle extras = new Bundle();

                ThreadRegistry.getInstance().setPosts(DownloadService.REGISTRY_ID, selectedPosts);

//                extras.putParcelableArrayList(DownloadService.DATA_KEY, selectedPosts);
                extras.putString(DownloadService.COMMAND_SAVE, MimiUtil.getSaveDir(getActivity()).getAbsolutePath());
                extras.putString(DownloadService.BOARD_KEY, boardName);
                extras.putInt(DownloadService.THREAD_KEY, threadId);
                extras.putInt(DownloadService.DOWNLOAD_TYPE_KEY, DownloadService.DOWNLOAD_BATCH);

                downloadIntent.putExtras(extras);
                getActivity().startService(downloadIntent);

                actionMode.finish();
            } else {
                Toast.makeText(getActivity(), R.string.no_images_selected, Toast.LENGTH_SHORT).show();
            }

        }
    }

    @Override
    public void initMenu() {
        super.initMenu();

        if (getActivity() != null && getActivity() instanceof MimiActivity) {
            final Toolbar toolbar = ((MimiActivity) getActivity()).getToolbar();
            if (toolbar != null) {
                toolbar.getMenu().clear();
                toolbar.inflateMenu(R.menu.gallery_grid);
                toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        startActionMode();
                        return true;
                    }
                });
            }
        }
    }

    private void fetchPosts() {
        if (getActivity() == null) {
            return;
        }

        RxUtil.safeUnsubscribe(fetchThreadSubscription);
        fetchThreadSubscription = chanConnector.fetchThread(getActivity(), boardName, threadId, ChanConnector.CACHE_DEFAULT)
                .subscribe(new Action1<ChanThread>() {
                    @Override
                    public void call(ChanThread thread) {

                        if (thread != null) {
                            posts = GalleryPagerAdapter.getPostsWithImages(thread.getPosts());
                            if (galleryGrid != null) {
                                galleryAdapter.setPosts(posts);
                            }
                        } else if (getActivity() != null) {
                            Toast.makeText(getActivity(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        if (throwable instanceof HttpException) {
                            HttpException error = (HttpException) throwable;
                            Log.i(LOG_TAG, "Error receiving response: " + error.getLocalizedMessage() + ", " + error.code());
                        }

                        if (getActivity() != null) {
                            Toast.makeText(getActivity(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }

    private void showBatchDownloadTutorial() {
        final LayoutInflater inflater = LayoutInflater.from(getActivity());
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        final View dialogView = inflater.inflate(R.layout.dialog_batch_download_tutorial, null, false);
        final CheckBox dontShow = (CheckBox) dialogView.findViewById(R.id.batch_download_dont_show);

        dialogBuilder.setTitle(R.string.batch_download)
                .setView(dialogView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        pref.edit().putBoolean(getString(R.string.show_batch_download_tutorial), !dontShow.isChecked()).apply();
                    }
                })
                .show();

    }

    private void extractExtras(final Bundle bundle) {
        if (bundle == null) {
            return;
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
        if (bundle.containsKey(Extras.EXTRAS_TWOPANE)) {
            twoPane = bundle.getBoolean(Extras.EXTRAS_TWOPANE);
        }
        if (bundle.containsKey(Extras.EXTRAS_PAGE)) {
            currentPage = bundle.getInt(Extras.EXTRAS_PAGE);
        }
        if (bundle.containsKey("rotated")) {
            rotated = bundle.getBoolean("rotated");
        }

        if (bundle.containsKey(Extras.EXTRAS_POST_LIST)) {
            bundle.setClassLoader(ChanPost.class.getClassLoader());
            posts = bundle.getParcelableArrayList(Extras.EXTRAS_POST_LIST);
        } else {
            posts = ThreadRegistry.getInstance().getPosts(threadId);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(Extras.EXTRAS_BOARD_TITLE, boardTitle);
        outState.putString(Extras.EXTRAS_BOARD_NAME, boardName);
        outState.putInt(Extras.EXTRAS_THREAD_ID, threadId);
//        outState.putParcelableArrayList(Extras.EXTRAS_POST_LIST, posts);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getActivity() != null) {
            final MimiActivity activity = ((MimiActivity) getActivity());
            activity.getToolbar().setTitle(getTitle());
            activity.getToolbar().setSubtitle(getSubtitle());
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        RxUtil.safeUnsubscribe(fetchThreadSubscription);
    }

    @Override
    public String getTitle() {
        return getString(R.string.gallery);
    }

    @Override
    public String getSubtitle() {
        return "/" + boardName + "/" + threadId;
    }

    @Override
    public String getPageName() {
        return "gallery_grid";
    }

    @Override
    public boolean onBackPressed() {

        if (!super.onBackPressed() && actionMode != null && galleryAdapter != null && galleryAdapter.isBatchDownload()) {
            actionMode.finish();
            actionMode = null;

            if (galleryAdapter != null) {
                galleryAdapter.setBatchDownload(false);
            }

            return true;
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case GalleryActivity.PERMISSIONS_REQUEST_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    startBatchDownload();

                } else {

                    Snackbar.make(galleryGrid, R.string.save_file_permission_denied, Snackbar.LENGTH_LONG).show();
                }
            }
        }
    }
}
