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

import android.animation.Animator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;
import android.widget.Toast;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.activity.MimiActivity;
import com.emogoth.android.phone.mimi.activity.TabsActivity;
import com.emogoth.android.phone.mimi.adapter.ThreadListAdapter;
import com.emogoth.android.phone.mimi.async.ProcessThreadTask;
import com.emogoth.android.phone.mimi.db.BoardTableConnection;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;
import com.emogoth.android.phone.mimi.db.HistoryTableConnection;
import com.emogoth.android.phone.mimi.db.UserPostTableConnection;
import com.emogoth.android.phone.mimi.db.model.History;
import com.emogoth.android.phone.mimi.dialog.RepliesDialog;
import com.emogoth.android.phone.mimi.event.CloseTabEvent;
import com.emogoth.android.phone.mimi.event.GalleryPagerScrolledEvent;
import com.emogoth.android.phone.mimi.event.ReplyClickEvent;
import com.emogoth.android.phone.mimi.event.ShowRepliesEvent;
import com.emogoth.android.phone.mimi.event.UpdateHistoryEvent;
import com.emogoth.android.phone.mimi.fourchan.FourChanConnector;
import com.emogoth.android.phone.mimi.interfaces.ContentInterface;
import com.emogoth.android.phone.mimi.interfaces.IToolbarContainer;
import com.emogoth.android.phone.mimi.interfaces.OnThumbnailClickListener;
import com.emogoth.android.phone.mimi.interfaces.ReplyMenuClickListener;
import com.emogoth.android.phone.mimi.interfaces.TabInterface;
import com.emogoth.android.phone.mimi.model.OutsideLink;
import com.emogoth.android.phone.mimi.util.BusProvider;
import com.emogoth.android.phone.mimi.util.Extras;
import com.emogoth.android.phone.mimi.util.HttpClientFactory;
import com.emogoth.android.phone.mimi.util.LayoutType;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.RefreshScheduler;
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.emogoth.android.phone.mimi.util.ThreadRegistry;
import com.emogoth.android.phone.mimi.widget.MimiRecyclerView;
import com.mimireader.chanlib.ChanConnector;
import com.mimireader.chanlib.models.ChanBoard;
import com.mimireader.chanlib.models.ChanPost;
import com.mimireader.chanlib.models.ChanThread;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import xyz.danoz.recyclerviewfastscroller.vertical.VerticalRecyclerViewFastScroller;


public class ThreadDetailFragment extends MimiFragmentBase implements
        Toolbar.OnMenuItemClickListener,
        TabInterface,
        ContentInterface {
    private static final String LOG_TAG = ThreadDetailFragment.class.getSimpleName();
    private static final String REPLY_FRAGMENT_TAG = "reply_fragment";
    private static final int LOADER_ID = 2;

    private static final int MAX_RETRIES = 5;

    private static final long FAST_SCROLL_TIMEOUT = 2000;

    private String threadReplyFragmentTag;

    private String postUrl;

    private MimiRecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private ThreadListAdapter threadListAdapter;
    private String boardName;
    private String boardTitle;
    private int threadId;
    private ChanThread currentThread = new ChanThread();
    private View loadingLayout;

    private int[] listViewItemHeight;

    private ViewGroup messageContainer;

    private TextView messageText;
    private boolean doThreadRegistryUpdate = false;

    private String replyComment;
    private PostFragment postFragment;
    private int unreadCount = 0;
    private int loaderId = LOADER_ID;
    private TextView closeMessageButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean createNewPostFragment = true;
    private boolean isWatched;
    private boolean rememberThreadScrollPosition;
    private int postFromExtras = 0;
    private MenuItem bookmarkMenuItem;
    private Toolbar toolbar;
    private boolean wasVisibleToUser;

    private long profileTimer;

    private Handler lastReadHandler = new Handler();
    private Runnable lastReadRunnable = new Runnable() {
        @Override
        public void run() {
            ThreadRegistry.getInstance().setLastReadPosition(threadId, layoutManager.findLastVisibleItemPosition());
            Log.d(LOG_TAG, "set last read position: set=" + layoutManager.findLastVisibleItemPosition() + ", get=" + ThreadRegistry.getInstance().getLastReadPosition(threadId));

            BusProvider.getInstance().post(new UpdateHistoryEvent(boardName, currentThread));
        }
    };

    private Runnable hideFastScrollRunnable = new Runnable() {
        @Override
        public void run() {
            if (fastScrollLayout != null) {
                fastScrollLayout.animate()
                        .alpha(0)
                        .setDuration(200)
                        .setListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                if (fastScrollLayout != null) {
                                    fastScrollLayout.setVisibility(View.INVISIBLE);
                                }
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {

                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {

                            }
                        })
                        .start();
            }
        }
    };
    private Handler hideFastScrollHandler = new Handler();

    //    private CoordinatorLayout coordinatorLayout;
    private Snackbar postingSnackbar;
    private boolean stickyAutoRefresh;
    private ChanConnector chanConnector;
    private VerticalRecyclerViewFastScroller fastScrollLayout;
    private RecyclerView.OnScrollListener fastScrollListener;

    private Subscription threadSubscription;
    private Subscription boardInfoSubscription;

    private View listHeader;
    private View listFooter;

    private Subscription fetchPostSubscription;
    private Subscription historyRemovedSubscription;
    private Subscription removeHistorySubscription;
    private Subscription addPostSubscription;

    public static ThreadDetailFragment newInstance(int threadId, String boardName, String boardTitle, ChanPost firstPost, boolean stickyAutoRefresh) {
        final Bundle args = new Bundle();
        args.putInt(Extras.EXTRAS_THREAD_ID, threadId);
        args.putString(Extras.EXTRAS_BOARD_NAME, boardName);
        args.putString(Extras.EXTRAS_BOARD_TITLE, boardTitle);
        args.putBoolean(Extras.EXTRAS_STICKY_AUTO_REFRESH, stickyAutoRefresh);

        if (firstPost != null) {
            args.putParcelable(Extras.EXTRAS_THREAD_FIRST_POST, firstPost);
        }

        ThreadDetailFragment fragment = new ThreadDetailFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        setRetainInstance(false);

        if (savedInstanceState == null) {
            extractExtras(getArguments());
        } else {
            if (savedInstanceState.containsKey(Extras.EXTRAS_THREAD_ID)
                    && getArguments() != null
                    && getArguments().containsKey(Extras.EXTRAS_THREAD_ID)
                    && savedInstanceState.getInt(Extras.EXTRAS_THREAD_ID) == getArguments().getInt(Extras.EXTRAS_THREAD_ID)) {
                extractExtras(savedInstanceState);
            } else {
                extractExtras(getArguments());
            }

        }

        threadReplyFragmentTag = REPLY_FRAGMENT_TAG + "_" + threadId;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_thread_detail, container, false);

        loadingLayout = view.findViewById(R.id.loading_layout);
        recyclerView = (MimiRecyclerView) view.findViewById(R.id.thread_list);
        messageContainer = (ViewGroup) view.findViewById(R.id.message_container);
        messageText = (TextView) view.findViewById(R.id.message);
        closeMessageButton = (TextView) view.findViewById(R.id.close_message_button);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        rememberThreadScrollPosition = MimiUtil.rememberThreadScrollPosition(getActivity());
        fastScrollLayout = (VerticalRecyclerViewFastScroller) view.findViewById(R.id.fast_scroll_layout);
        fastScrollLayout.setRecyclerView(recyclerView);

        final MimiActivity activity = (MimiActivity) getActivity();
        toolbar = activity.getToolbar();

        if (MimiUtil.getLayoutType(getActivity()) == LayoutType.TABBED) {
            listHeader = inflater.inflate(R.layout.header_layout, container, false);
        } else {
            listHeader = inflater.inflate(R.layout.header_layout_tall, container, false);
        }
        listFooter = inflater.inflate(R.layout.list_footer, container, false);

        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(LOG_TAG, "Inside onViewCreated for thread " + threadId);

        final boolean secure = MimiUtil.isSecureConnection(getActivity());
        chanConnector = new FourChanConnector.Builder()
                .setCacheDirectory(MimiUtil.getInstance().getCacheDir())
                .setEndpoint(FourChanConnector.getDefaultEndpoint(secure))
                .setPostEndpoint(FourChanConnector.getDefaultPostEndpoint())
                .setClient(HttpClientFactory.getInstance().getOkHttpClient())
                .build();

        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        threadListAdapter = new ThreadListAdapter(getActivity(), getChildFragmentManager(), currentThread, boardName);

        setupRepliesDialog();
        if (getActivity() instanceof OnThumbnailClickListener) {
            threadListAdapter.setOnThumbnailClickListener((OnThumbnailClickListener) getActivity());
        }

        recyclerView.setAdapter(threadListAdapter);
        threadListAdapter.addHeader(listHeader);
        threadListAdapter.addFooter(listFooter);

        TypedValue typedValue = new TypedValue();
        getActivity().getTheme().resolveAttribute(R.attr.actionBarSize, typedValue, true);
        int pixels = getResources().getDimensionPixelSize(typedValue.resourceId);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(true);
                refresh(false);
            }
        });

        swipeRefreshLayout.setProgressViewOffset(false, pixels, pixels + 100);

        if (getUserVisibleHint() && MimiUtil.getLayoutType(getActivity()) != LayoutType.SLIDING_PANEL) {
            initMenu();
        }

        if (!TextUtils.isEmpty(boardName)) {

            Log.d(LOG_TAG, "Board name for thread " + threadId + " is " + boardName);
            if (currentThread == null || currentThread.getPosts() == null || currentThread.getPosts().size() == 0) {
                showContent();
            }

            RxUtil.safeUnsubscribe(fetchPostSubscription);
            fetchPostSubscription = HistoryTableConnection.fetchPost(boardName, threadId)
                    .flatMap(new Func1<History, Observable<ChanThread>>() {
                        @Override
                        public Observable<ChanThread> call(History history) {
                            if (getActivity() == null) {
                                return Observable.just(null);
                            }

                            isWatched = history != null && history.watched;

                            if (stickyAutoRefresh) {
                                RefreshScheduler.getInstance().addThread(boardName, threadId, isWatched);
                            }

                            final ChanThread chanThread;
                            if (isWatched) {
                                chanThread = MimiUtil.getInstance().getBookmarkedThread(boardName, threadId);
                                if (chanThread != null) {
                                    currentThread = chanThread;
                                    return Observable.just(chanThread);
                                } else {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            showLoadingLayout();
                                        }
                                    });
                                    return chanConnector.fetchThread(getActivity(), boardName, threadId);
                                }
                            } else {
                                if (currentThread == null || currentThread.getPosts() == null || currentThread.getPosts().size() == 0) {
                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            showLoadingLayout();
                                        }
                                    });
                                }
                                return chanConnector.fetchThread(getActivity(), boardName, threadId);
                            }
                        }
                    })
                    .flatMap(ProcessThreadTask.processThreadFlatMap(getActivity(), boardName, threadId))
                    .compose(DatabaseUtils.<ChanThread>applySchedulers())
                    .subscribe(new Action1<ChanThread>() {
                        @Override
                        public void call(ChanThread chanThread) {
                            if (chanThread != null && chanThread.getPosts() != null && chanThread.getPosts().size() > 0) {
                                final int size;
                                if (isWatched) {
                                    if (currentThread == null || currentThread.getPosts() == null || currentThread.getPosts().size() == 0) {
                                        size = 0;
                                    } else {
                                        size = ThreadRegistry.getInstance().getThreadSize(threadId) - ThreadRegistry.getInstance().getUnreadCount(threadId);
                                    }

//                                    refresh(false);
                                } else {
                                    size = -1;
                                }

                                showThread(chanThread, size, isWatched && rememberThreadScrollPosition);
                            }

                            currentThread = chanThread;

//                            RxUtil.safeUnsubscribe(fetchPostSubscription);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            onErrorResponse(throwable);
//                            RxUtil.safeUnsubscribe(fetchPostSubscription);
                        }
                    });
        } else {
            Log.w(LOG_TAG, "No board name for thread " + threadId);
        }

    }

    @Override
    public void initMenu() {
        super.initMenu();

        if (toolbar != null) {
            toolbar.getMenu().clear();

            if (MimiUtil.getLayoutType(getActivity()) == LayoutType.TABBED) {
                toolbar.inflateMenu(R.menu.detail_tab);
            } else {
                toolbar.inflateMenu(R.menu.detail);
            }
            toolbar.setOnMenuItemClickListener(this);

            if (isWatched && bookmarkMenuItem != null) {
                bookmarkMenuItem.setIcon(R.drawable.ic_bookmark);
            }

            if (TextUtils.isEmpty(boardTitle)) {
                RxUtil.safeUnsubscribe(boardInfoSubscription);
                boardInfoSubscription = BoardTableConnection.fetchBoard(boardName)
                        .compose(DatabaseUtils.<ChanBoard>applySchedulers())
                        .subscribe(new Action1<ChanBoard>() {
                            @Override
                            public void call(ChanBoard chanBoard) {
                                if (chanBoard != null) {
                                    boardTitle = chanBoard.getTitle();

                                    toolbar.setTitle(boardTitle);
                                    toolbar.setSubtitle(String.valueOf(threadId));
                                }

                            }
                        });
            } else {
                toolbar.setTitle(boardTitle);
                toolbar.setSubtitle(String.valueOf(threadId));
            }

            final View spinner = toolbar.findViewById(R.id.board_spinner);
            if (spinner != null) {
                spinner.setVisibility(View.GONE);
            }

            final MenuItem bookmarkItem = toolbar.getMenu().findItem(R.id.bookmark_menu);
            if (bookmarkItem != null) {
                if (isWatched) {
                    bookmarkItem.setIcon(R.drawable.ic_bookmark);
                }
                bookmarkMenuItem = bookmarkItem;
            }

            final MenuItem closeTabItem = toolbar.getMenu().findItem(R.id.close_tab);
            if (closeTabItem != null) {
                if (getActivity() instanceof TabsActivity) {
                    closeTabItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            BusProvider.getInstance().post(new CloseTabEvent(threadId, boardName, boardTitle, false));
                            return true;
                        }
                    });
                } else {
                    closeTabItem.setVisible(false);
                }
            }

            final MenuItem closeOtherTabsItem = toolbar.getMenu().findItem(R.id.close_other_tabs);
            if (closeOtherTabsItem != null) {
                if (getActivity() instanceof TabsActivity) {
                    closeOtherTabsItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            BusProvider.getInstance().post(new CloseTabEvent(threadId, boardName, boardTitle, true));
                            return true;
                        }
                    });
                } else {
                    closeOtherTabsItem.setVisible(false);
                }
            }
        }

    }

    private void createRecyclerViewScrollListeners(final boolean useFastScroll) {
        recyclerView.removeOnScrollListener(fastScrollLayout.getOnScrollListener());
        recyclerView.addOnScrollListener(fastScrollLayout.getOnScrollListener());

        if (useFastScroll) {
            recyclerView.setVerticalScrollBarEnabled(false);
        } else {
            recyclerView.setVerticalFadingEdgeEnabled(true);
            recyclerView.setVerticalScrollBarEnabled(true);
        }

        if (fastScrollListener == null) {
            fastScrollListener = new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    if (dy > 0) {
                        lastReadHandler.removeCallbacks(lastReadRunnable);
                        lastReadHandler.postDelayed(lastReadRunnable, 500);
                    }

                    if (useFastScroll && (Math.abs(dy) > 220 || fastScrollLayout.getVisibility() == View.VISIBLE)) {
                        fastScrollLayout.clearAnimation();
                        fastScrollLayout.setAlpha(1.0F);
                        fastScrollLayout.setVisibility(View.VISIBLE);

                        hideFastScrollHandler.removeCallbacks(hideFastScrollRunnable);
                        hideFastScrollHandler.postDelayed(hideFastScrollRunnable, FAST_SCROLL_TIMEOUT);
                    }
                }
            };
        }

        recyclerView.removeOnScrollListener(fastScrollListener);
        recyclerView.addOnScrollListener(fastScrollListener);
    }

    private void extractExtras(final Bundle bundle) {
        if (bundle != null) {
            if (bundle.containsKey(Extras.EXTRAS_THREAD_ID)) {
                threadId = bundle.getInt(Extras.EXTRAS_THREAD_ID);
            }
            if (bundle.containsKey(Extras.EXTRAS_BOARD_NAME)) {
                boardName = bundle.getString(Extras.EXTRAS_BOARD_NAME);
                if (getActivity() != null) {
                    final String url = getResources().getString(R.string.sys_link);
                    postUrl = "https://" + url + "/" + boardName + "/post";
                }
            }
            if (bundle.containsKey(Extras.EXTRAS_BOARD_TITLE)) {
                boardTitle = bundle.getString(Extras.EXTRAS_BOARD_TITLE);
            }
            if (bundle.containsKey(Extras.EXTRAS_UNREAD_COUNT)) {
                unreadCount = bundle.getInt(Extras.EXTRAS_UNREAD_COUNT);
                Log.i(LOG_TAG, "Unread count=" + unreadCount);
            }

            if (bundle.containsKey(Extras.LOADER_ID)) {
                loaderId = bundle.getInt(Extras.LOADER_ID);
            }

            if (bundle.containsKey(Extras.EXTRAS_THREAD_FIRST_POST)) {
                final ChanPost post = bundle.getParcelable(Extras.EXTRAS_THREAD_FIRST_POST);

                currentThread = new ChanThread();
                currentThread.setThreadId(threadId);
                currentThread.setBoardName(boardName);
                currentThread.setPosts(new ArrayList<ChanPost>());
                currentThread.getPosts().add(post);

                postFromExtras++;
            }

            if (bundle.containsKey(Extras.EXTRAS_STICKY_AUTO_REFRESH)) {
                stickyAutoRefresh = bundle.getBoolean(Extras.EXTRAS_STICKY_AUTO_REFRESH);
            } else {
                stickyAutoRefresh = false;
            }
        }

    }

    private void toggleWatch() {
        if (currentThread != null) {
            final File bookmarkFile = MimiUtil.getBookmarkFile(MimiUtil.getInstance().getBookmarkDir(), boardName, threadId);
            final ChanPost post;


            if (isWatched) {

                if (bookmarkMenuItem != null) {
                    bookmarkMenuItem.setIcon(R.drawable.ic_bookmark_outline);
                }
                RxUtil.safeUnsubscribe(removeHistorySubscription);
                removeHistorySubscription = HistoryTableConnection.removeHistory(boardName, threadId)
                        .doOnNext(new Action1<Boolean>() {
                            @Override
                            public void call(Boolean success) {
                                MimiUtil.getInstance().removeBookmark(boardName, threadId);
                            }
                        })
                        .compose(DatabaseUtils.<Boolean>applySchedulers())
                        .subscribe();
                ThreadRegistry.getInstance().remove(threadId);

            } else {

                if (bookmarkMenuItem != null) {
                    bookmarkMenuItem.setIcon(R.drawable.ic_bookmark);
                }
                ThreadRegistry.getInstance().update(threadId, threadListAdapter.getLastPosition(), true, true);

                try {
                    if (bookmarkFile != null) {
                        MimiUtil.getInstance().saveBookmark(currentThread, new MimiUtil.OperationCompleteListener() {
                            @Override
                            public void onOperationComplete() {
                                Log.d(LOG_TAG, "Saved bookmark successfully: thread=" + threadId);
                            }

                            @Override
                            public void onOperationFailed() {
                                Log.e(LOG_TAG, "Error saving bookmark: thread=" + threadId);
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            isWatched = !isWatched;

            if (currentThread.getPosts().size() > 0) {
                post = currentThread.getPosts().get(0);
                RxUtil.safeUnsubscribe(fetchPostSubscription);
                fetchPostSubscription = HistoryTableConnection.putHistory(boardName, post, currentThread.getPosts().size(), isWatched)
                        .compose(DatabaseUtils.<Boolean>applySchedulers())
                        .subscribe(new Action1<Boolean>() {
                            @Override
                            public void call(Boolean success) {
                                if (success) {
                                    post.setWatched(isWatched);
                                }
                            }
                        });

                BusProvider.getInstance().post(new UpdateHistoryEvent(boardName, currentThread));
            }
        }
    }

    private void saveHistory() {
        if (currentThread != null && currentThread.getPosts() != null) {
            final ChanPost firstPost = getFirstPost(currentThread);
            final int postCount = currentThread.getPosts().size();

            RxUtil.safeUnsubscribe(fetchPostSubscription);
            fetchPostSubscription = HistoryTableConnection.fetchPost(boardName, threadId)
                    .flatMap(new Func1<History, Observable<Boolean>>() {
                        @Override
                        public Observable<Boolean> call(History history) {
                            final boolean watched;
                            if (history == null) {
                                watched = isWatched;
                            } else {
                                watched = history.watched;
                            }

                            return HistoryTableConnection.putHistory(boardName, firstPost, postCount, watched);
                        }
                    })
                    .compose(DatabaseUtils.<Boolean>applySchedulers())
                    .subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean success) {
                            if (success) {
                                Log.d(LOG_TAG, "Successfully updated history");
                            } else {
                                Log.e(LOG_TAG, "Error while updating history");
                            }
                        }
                    });
        }
    }

    private void refresh(final boolean showLoading) {
        Log.d(LOG_TAG, "Refreshing thread " + threadId);
        if (getActivity() == null) {
            return;
        }

        if (showLoading) {
            showLoadingLayout();
        }

        final int oldThreadSize = ThreadRegistry.getInstance().getLastReadPosition(threadId);

        RxUtil.safeUnsubscribe(threadSubscription);
        threadSubscription = refreshObservable()
                .subscribe(showThreadAction(oldThreadSize, showLoading), new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        onErrorResponse(throwable);
                    }
                });

        swipeRefreshLayout.setEnabled(false);
    }

    private Observable<ChanThread> refreshObservable() {
        return chanConnector.fetchThread(getActivity(), boardName, threadId)
                .map(ProcessThreadTask.processThread(getActivity(), boardName, threadId))
                .zipWith(HistoryTableConnection.fetchPost(boardName, threadId), threadLookupZipper(boardName, threadId))
                .compose(DatabaseUtils.<ChanThread>applySchedulers());

    }

    private void showPostFragment() {
        final FragmentManager fm = getChildFragmentManager();

        recyclerView.smoothScrollBy(0, 0);
        if (!createNewPostFragment) {
            createNewPostFragment = false;
            postFragment = (PostFragment) fm.findFragmentByTag(threadReplyFragmentTag);
        }

        if (postFragment == null) {
            final Bundle args = new Bundle();
            args.putInt(Extras.EXTRAS_THREAD_ID, threadId);
            args.putString(Extras.EXTRAS_BOARD_NAME, boardName);
            args.putString(Extras.EXTRAS_BOARD_TITLE, boardTitle);
            args.putInt(Extras.LOADER_ID, loaderId);

            if (!TextUtils.isEmpty(replyComment)) {
                args.putString(Extras.EXTRAS_POST_COMMENT, replyComment);
                replyComment = null;
            }

            postFragment = new PostFragment();
            postFragment.setPostListener(createPostCompleteListener());
            postFragment.setArguments(args);
        } else {
            if (!TextUtils.isEmpty(replyComment)) {
                postFragment.setComment(replyComment);
            }
        }

        postFragment.show(fm, threadReplyFragmentTag);
    }

    private PostFragment.PostListener createPostCompleteListener() {
        return new PostFragment.PostListener() {
            @Override
            public void onStartPost() {
                RxUtil.safeUnsubscribe(fetchPostSubscription);
                fetchPostSubscription = HistoryTableConnection.fetchPost(boardName, threadId)
                        .compose(DatabaseUtils.<History>applySchedulers())
                        .subscribe(new Action1<History>() {
                            @Override
                            public void call(History history) {
                                if (history == null) {
                                    return;
                                }

                                final boolean watched = history.watched;
                                if (!watched) {
                                    toggleWatch();
                                }
                            }
                        });

                showPostProgress();
                onBackPressed();
            }

            @Override
            public void onSuccess(final String postId) {
                if (getActivity() == null) {
                    return;
                }

                Log.i(LOG_TAG, "Post ID: " + postId);
                showPostStatus("Success!");

                final int id = Integer.valueOf(postId);
                RxUtil.safeUnsubscribe(fetchPostSubscription);
                fetchPostSubscription = HistoryTableConnection.fetchPost(boardName, threadId)
                        .compose(DatabaseUtils.<History>applySchedulers())
                        .subscribe(new Action1<History>() {
                            @Override
                            public void call(History history) {
                                if (history != null) {
                                    ThreadRegistry.getInstance().add(boardName, threadId, id, currentThread.getPosts().size(), history.watched);
                                }
                            }
                        });

                RxUtil.safeUnsubscribe(addPostSubscription);
                addPostSubscription = UserPostTableConnection.addPost(boardName, threadId, id)
                        .compose(DatabaseUtils.<Boolean>applySchedulers())
                        .subscribe(new Action1<Boolean>() {
                            @Override
                            public void call(Boolean success) {
                                if (success) {
                                    Log.d(LOG_TAG, "Added post to database: board=" + boardName + ", thread=" + threadId + ", post=" + postId);
                                } else {
                                    Log.e(LOG_TAG, "Error Adding post to database: board=" + boardName + ", thread=" + threadId + ", post=" + postId);
                                }
                            }
                        });

                refresh(false);

                final FragmentTransaction ft = getChildFragmentManager().beginTransaction();
                ft.remove(postFragment);
                ft.commit();

                createNewPostFragment = true;
                postFragment = null;

            }

            @Override
            public void onError(Throwable error) {
                if (getActivity() != null) {
                    error.printStackTrace();
                    showPostStatus(error.getLocalizedMessage());
                }
            }

            @Override
            public void onCanceled() {
                if (getActivity() != null) {
                    final FragmentTransaction ft = getChildFragmentManager().beginTransaction();
                    postFragment.dismiss();
                    ft.remove(postFragment);
                    ft.commit();

                    createNewPostFragment = true;
                    postFragment = null;
                }
            }
        };
    }

    @Subscribe
    public void autoRefreshThread(final UpdateHistoryEvent event) {
        if (event != null && getActivity() != null) {

            if (event.getThread() == null) {
                refresh(false);
                return;
            }

            if (event.getThreadId() == threadId
                    && event.getBoardName().equals(boardName)
                    && (currentThread == null || event.getThread().getPosts().size() != currentThread.getPosts().size())
                    && threadListAdapter != null) {

                refresh(false);
            }
        }
    }

    @Subscribe
    public void onGalleryPagerScrolled(final GalleryPagerScrolledEvent event) {

        if (getUserVisibleHint()) {
            if (currentThread != null) {
                final ChanPost p = new ChanPost();
                p.setNo(event.getPostNumber());
                final int index = currentThread.getPosts().indexOf(p);

                if (index >= 0 && recyclerView != null) {
                    scrollListToPosition(recyclerView, index);
//                    recyclerView.smoothScrollToPosition(index + 2);
                }
            }
        }
    }

    private void showPostProgress() {
        if (getActivity() != null) {
            final View v = getActivity().findViewById(android.R.id.content);
            if (postingSnackbar != null && postingSnackbar.isShown()) {
                postingSnackbar.dismiss();
            }
            postingSnackbar = Snackbar.make(v, R.string.sending, Snackbar.LENGTH_INDEFINITE);
            postingSnackbar.show();
        }
    }

    private void showPostStatus(final String status) {
        if (getActivity() != null) {
            final View v = getActivity().findViewById(android.R.id.content);
            if (postingSnackbar != null && postingSnackbar.isShown()) {
                postingSnackbar.dismiss();
            }
            Snackbar.make(v, status, Snackbar.LENGTH_LONG).show();
        }

    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        outState.putInt(Extras.EXTRAS_THREAD_ID, threadId);
        outState.putString(Extras.EXTRAS_BOARD_NAME, boardName);
        outState.putString(Extras.EXTRAS_BOARD_TITLE, boardTitle);
        outState.putInt(Extras.LOADER_ID, loaderId);
        outState.putBoolean(Extras.EXTRAS_STICKY_AUTO_REFRESH, stickyAutoRefresh);

        super.onSaveInstanceState(outState);
    }

    private void showLoadingLayout() {
        recyclerView.setVisibility(View.GONE);
        loadingLayout.setVisibility(View.VISIBLE);
        messageContainer.setVisibility(View.GONE);
    }

    private void showContent() {
        loadingLayout.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);

        swipeRefreshLayout.setEnabled(true);
        swipeRefreshLayout.setRefreshing(false);
    }

    private void setupRepliesDialog() {
        threadListAdapter.setOnReplyMenuClickListener(new ReplyMenuClickListener() {
            @Override
            public void onReply(View view, int threadId) {
                String comment = null;
                if (postFragment != null) {
                    comment = postFragment.getComment();
                }

                if (TextUtils.isEmpty(comment)) {
                    comment = ">>" + threadId + "\n";
                } else {
                    comment = comment + ">>" + threadId + "\n";
                }

//                comment = (comment == null ? "" : comment + "\n") + ">>" + threadId + "\n";

                replyComment = comment;

                showPostFragment();

            }

            @Override
            public void onQuote(View view, int position, ChanPost post) {

                if (post.getCom() != null) {
                    String comment = null;
                    String quote = Html.fromHtml(post.getCom()).toString();

                    if (postFragment != null) {
                        comment = postFragment.getComment();
                    }

                    if (!TextUtils.isEmpty(quote)) {
                        quote = ">" + quote;
                        quote = quote.replace("\n", "\n>");
                    }

                    if (TextUtils.isEmpty(comment)) {
                        comment = ">>" + threadId + "\n" + quote + "\n";
                    } else {

                        comment = comment + ">>" + threadId + "\n" + quote + "\n";
                    }

//                    comment = (comment == null ? "" : comment + "\n") + ">>" + post.getNo() + "\n" + quote + "\n";

                    replyComment = comment;
                }
                showPostFragment();

            }
        });
    }

    private void closeThread(final int threadId) {
        ThreadRegistry.getInstance().remove(threadId);
    }

    private Func2<ChanThread, History, ChanThread> threadLookupZipper(final String boardName, final int threadId) {
        return new Func2<ChanThread, History, ChanThread>() {
            @Override
            public ChanThread call(ChanThread chanThread, History history) {
                isWatched = history != null && history.watched;
                return chanThread;
            }
        };
    }

    private void showThread(final ChanThread thread, final int originalThreadSize, final boolean scrollToPosition) {
        if (thread == null || thread.getPosts() == null) {
            return;
        }

        currentThread = thread;

        listViewItemHeight = new int[thread.getPosts().size()];
        Arrays.fill(listViewItemHeight, -1);

        if (bookmarkMenuItem != null) {
            if (isWatched) {
                bookmarkMenuItem.setIcon(R.drawable.ic_bookmark);
            } else {
                bookmarkMenuItem.setIcon(R.drawable.ic_bookmark_outline);
            }
        }

        if (threadListAdapter != null) {
            threadListAdapter.setThread(thread);
            if (getUserVisibleHint()) {
                ThreadRegistry.getInstance().update(threadId, thread.getPosts().size(), true, isWatched);
            } else {
                doThreadRegistryUpdate = true;
            }
        }

        if (scrollToPosition) {
            // unnecessary?
            if (originalThreadSize >= 0) {
                recyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollListToPosition(recyclerView, originalThreadSize);
                    }
                });

//                recyclerView.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        layoutManager.scrollToPosition(originalThreadSize);
//                        Log.d(LOG_TAG, "[processThread] Setting list selection: id=" + threadId + ", position=" + originalThreadSize + ", current thread size=" + thread.getPosts().size() + ", last visible position=" + (originalThreadSize + layoutManager.findLastVisibleItemPosition()));
//                    }
//                });
            } else {
                recyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollListToPosition(recyclerView, 0);
                    }
                });

//                recyclerView.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        layoutManager.scrollToPosition(0);
//                    }
//                });
            }
        }

        final ThreadRegistry threadRegistry = ThreadRegistry.getInstance();
        threadRegistry.add(boardName, threadId, currentThread.getPosts().size(), isWatched);

        if (postFromExtras > 1) {
            Log.d(LOG_TAG, "Showing footer refresh icon");
            postFromExtras = 0;
        }
        showContent();

        if (isWatched) {
            try {
                final File bookmarkFile = MimiUtil.getBookmarkFile(MimiUtil.getInstance().getBookmarkDir(), boardName, threadId);
                if (bookmarkFile != null) {
                    if (currentThread.getPosts().size() > ThreadRegistry.getInstance().getThreadSize(threadId)) {
                        MimiUtil.getInstance().saveBookmark(currentThread, new MimiUtil.OperationCompleteListener() {
                                    @Override
                                    public void onOperationComplete() {
                                        Log.d(LOG_TAG, "Saved bookmark successfully: thread=" + threadId);
                                    }

                                    @Override
                                    public void onOperationFailed() {
                                        Log.e(LOG_TAG, "Error saving bookmark: thread=" + threadId);
                                    }
                                }
                        );
                        Log.i(LOG_TAG, "Saving bookmark for thread: thread=" + threadId);
                    }
                }
            } catch (final Exception authFailureError) {
                authFailureError.printStackTrace();
            }
        } else if (getUserVisibleHint() && MimiUtil.historyEnabled(getActivity())) {
            saveHistory();
        }
    }

    private Action1<ChanThread> showThreadAction(final int oldThreadSize, final boolean scrollToPosition) {

        return new Action1<ChanThread>() {
            @Override
            public void call(ChanThread thread) {
                showThread(thread, oldThreadSize, scrollToPosition);
            }
        };

    }

    public void onErrorResponse(final Throwable error) {
        Log.e(LOG_TAG, "Error: " + error.getLocalizedMessage(), error);

        showContent();

        if (error instanceof HttpException) {
            HttpException exception = (HttpException) error;

            if (exception.code() == 404) {
                if (currentThread == null) {
                    closeMessageButton.setVisibility(View.INVISIBLE);
                } else {
                    closeMessageButton.setVisibility(View.VISIBLE);
                    closeMessageButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final AlphaAnimation animation = new AlphaAnimation(1.0F, 0.0F);
                            animation.setDuration(100);
                            messageContainer.setAnimation(animation);
                            messageContainer.setVisibility(View.GONE);
                        }
                    });
                }
                messageText.setText(R.string.error_404);
                messageContainer.setVisibility(View.VISIBLE);
                if (isWatched) {
                    RxUtil.safeUnsubscribe(historyRemovedSubscription);
                    historyRemovedSubscription = HistoryTableConnection.setHistoryRemovedStatus(boardName, threadId, true)
                            .compose(DatabaseUtils.<Boolean>applySchedulers())
                            .subscribe();
                }
            }
        }

        Log.d(LOG_TAG, "Exception while accessing network", error);
    }

    private ChanPost getLastPost(final ChanThread thread) {
        if (thread != null && thread.getPosts() != null) {
            return thread.getPosts().get(thread.getPosts().size() - 1);
        }

        return null;
    }

    private ChanPost getFirstPost(final ChanThread thread) {
        if (thread != null && thread.getPosts().size() > 0) {
            return thread.getPosts().get(0);
        }

        return null;
    }

    @Override
    public void onPause() {
        super.onPause();

        RxUtil.safeUnsubscribe(fetchPostSubscription);
        RxUtil.safeUnsubscribe(threadSubscription);
        RxUtil.safeUnsubscribe(boardInfoSubscription);
        RxUtil.safeUnsubscribe(historyRemovedSubscription);
        RxUtil.safeUnsubscribe(removeHistorySubscription);
        RxUtil.safeUnsubscribe(addPostSubscription);

        if (!isWatched) {
            if (!stickyAutoRefresh) {
                RefreshScheduler.getInstance().removeThread(boardName, threadId);
            }
        } else {
            if (recyclerView != null) {
                ThreadRegistry.getInstance().setLastReadPosition(threadId, layoutManager.findLastVisibleItemPosition());
            }
            HistoryTableConnection.setLastReadPost(threadId, ThreadRegistry.getInstance().getLastReadPosition(threadId))
                    .compose(DatabaseUtils.<Boolean>applySchedulers())
                    .subscribe();
        }

        recyclerView.clearOnScrollListeners();
    }

    @Override
    public void onStop() {
        super.onStop();

        BusProvider.getInstance().unregister(this);
    }

    @Override
    public void onStart() {
        super.onStart();

        BusProvider.getInstance().register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (recyclerView != null && getActivity() != null) {
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            final boolean useFastScroll = preferences.getBoolean(getString(R.string.use_fast_scroll_pref), true);
            createRecyclerViewScrollListeners(useFastScroll);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!isWatched && MimiUtil.getLayoutType(getActivity()) != LayoutType.TABBED) {
            RefreshScheduler.getInstance().removeThread(boardName, threadId);
        }
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (getActivity() != null) {
            if (toolbar == null) {
                toolbar = ((MimiActivity) getActivity()).getToolbar();
            }
            if (isVisibleToUser) {

                if (getActivity() instanceof IToolbarContainer) {
                    IToolbarContainer activity = (IToolbarContainer) getActivity();
                    activity.setExpandedToolbar(true, true);
                }

                if (doThreadRegistryUpdate) {
                    ThreadRegistry.getInstance().update(threadId, currentThread.getPosts().size(), true, isWatched);
                    doThreadRegistryUpdate = false;
                }
            } else if (!isWatched && !stickyAutoRefresh) {
                RefreshScheduler.getInstance().removeThread(boardName, threadId);
            }
        }

        wasVisibleToUser = isVisibleToUser;

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PostFragment.PICK_IMAGE) {
            final FragmentManager fm = getChildFragmentManager();
            final PostFragment fragment = (PostFragment) fm.findFragmentByTag(threadReplyFragmentTag);

            if (fragment != null) {
                fragment.onActivityResult(requestCode, resultCode, data);
            }

        }

        Log.i(LOG_TAG, "Activity result called");
    }

    private void scrollListToPosition(RecyclerView recyclerView, int position) {
        if(recyclerView == null) {
            return;
        }

        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        if(layoutManager instanceof LinearLayoutManager) {
            ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(position, 10);
        } else if(layoutManager instanceof StaggeredGridLayoutManager) {
            ((StaggeredGridLayoutManager) layoutManager).scrollToPositionWithOffset(position, 10);
        }
    }

    @Subscribe
    public void showRepliesDialog(final ShowRepliesEvent event) {
        if (event.getThreadId() == threadId) {
            Log.i(LOG_TAG, "Showing replies");
            if (getActivity() != null) {

                final Handler handler = new Handler(getActivity().getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (getActivity() == null) {
                            return;
                        }

                        final FragmentManager fm = getActivity().getSupportFragmentManager();
                        final RepliesDialog repliesDialog = new RepliesDialog();
                        final Bundle args = new Bundle();

                        final ArrayList<OutsideLink> outsideLinks = new ArrayList<>();
                        final ArrayList<ChanPost> postList = new ArrayList<>();
                        for (final String id : event.getReplies()) {

                            ChanPost post = new ChanPost();
                            if (TextUtils.isDigitsOnly(id)) {
                                int index = -1;
                                try {
                                    post.setNo(Integer.valueOf(id));
                                    index = currentThread.getPosts().indexOf(post);
                                    post = currentThread.getPosts().get(index);

                                    if (post != null) {
                                        postList.add(post);
                                    }
                                } catch (ArrayIndexOutOfBoundsException e) {
                                    Log.e(LOG_TAG, "post id: " + post.getNo());
                                    Log.e(LOG_TAG, "string id: " + id);
                                    Log.i(LOG_TAG, "index: " + index);

                                    e.printStackTrace();

                                    Toast.makeText(getActivity(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                if (id.contains("/")) {
                                    final String[] params = id.split("/");

                                    if (params.length > 1) {
                                        final String board = params[1];
                                        final String thread;
                                        final OutsideLink link = new OutsideLink();

                                        link.setBoardName(board);

                                        if (params.length > 2) {
                                            thread = params[2];
                                            Log.i(LOG_TAG, "Found board link: board=" + board + ", thread=" + thread);
                                        } else {
                                            thread = null;
                                            Log.i(LOG_TAG, "Found board link: board=" + board);
                                        }

                                        link.setThreadId(thread);
                                        outsideLinks.add(link);
                                    }
                                }
                            }
                        }

                        if (postList.size() > 0 || outsideLinks.size() > 0) {
                            args.putString(Extras.EXTRAS_BOARD_NAME, boardName);
                            args.putParcelableArrayList(Extras.EXTRAS_POST_LIST, postList);
                            args.putParcelableArrayList(Extras.EXTRAS_OUTSIDE_LINK_LIST, outsideLinks);
                            args.putParcelable(Extras.EXTRAS_SINGLE_THREAD, currentThread);

                            repliesDialog.setArguments(args);
                            repliesDialog.show(fm, RepliesDialog.DIALOG_TAG);
                        }
                    }
                });

            }
        }
    }

    @Subscribe
    public void onReplyClicked(final ReplyClickEvent event) {
        if (event.getPost() == null) {
            return;
        }

        try {
            if (getUserVisibleHint() && currentThread != null) {
                final int index = currentThread.getPosts().indexOf(event.getPost());

                if (index >= 0) {
                    scrollListToPosition(recyclerView, index);
                }
            }
        } catch (Exception e) {
            if (getActivity() != null) {
                Toast.makeText(getActivity(), R.string.unknown_error, Toast.LENGTH_SHORT).show();
            }

            if (currentThread == null) {
                Log.e(LOG_TAG, "Current thread is null");
            } else {
                if (currentThread.getPosts() == null) {
                    Log.e(LOG_TAG, "Thread posts are null");
                } else if (event == null) {
                    Log.e(LOG_TAG, "Event object is null");
                }
            }

            Log.e(LOG_TAG, "Caught exception when scrolling to post from reply dialog", e);
        }

    }

    public String getBoardName() {
        return boardName;
    }

    @Override
    public String getTitle() {
        return boardTitle;
    }

    public String getSubtitle() {
        if (threadId > 0) {
            return String.valueOf(threadId);
        }

        return null;
    }

    @Override
    public String getPageName() {
        return "thread_detail";
    }

    public String getThreadId() {
        return String.valueOf(threadId);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (getActivity() != null) {

            switch (item.getItemId()) {
                case R.id.refresh_menu:
                    refresh(true);

                    return true;

                case R.id.bookmark_menu:
                    toggleWatch();

                    return true;

                case R.id.share_menu:
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT,
                            MimiUtil.httpOrHttps(getActivity()) + getResources().getString(R.string.board_link) + getResources().getString(R.string.raw_thread_path, getBoardName(), getThreadId()));
                    sendIntent.setType("text/plain");
                    startActivity(sendIntent);

                    return true;

            }

            return false;

        }

        return false;
    }

    @Override
    public int getTabId() {
        return threadId;
    }

    @Override
    public void addContent() {
        showPostFragment();
    }
}
