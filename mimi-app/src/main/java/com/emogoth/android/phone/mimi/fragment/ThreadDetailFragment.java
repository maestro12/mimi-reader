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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.AlphaAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.legacy.widget.Space;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.emogoth.android.phone.mimi.BuildConfig;
import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.activity.MimiActivity;
import com.emogoth.android.phone.mimi.activity.TabsActivity;
import com.emogoth.android.phone.mimi.activity.WebActivity;
import com.emogoth.android.phone.mimi.adapter.ThreadListAdapter;
import com.emogoth.android.phone.mimi.app.MimiApplication;
import com.emogoth.android.phone.mimi.autorefresh.RefreshJobService;
import com.emogoth.android.phone.mimi.autorefresh.RefreshScheduler;
import com.emogoth.android.phone.mimi.db.BoardTableConnection;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;
import com.emogoth.android.phone.mimi.db.HistoryTableConnection;
import com.emogoth.android.phone.mimi.db.model.History;
import com.emogoth.android.phone.mimi.dialog.RepliesDialog;
import com.emogoth.android.phone.mimi.event.CloseTabEvent;
import com.emogoth.android.phone.mimi.event.GalleryPagerScrolledEvent;
import com.emogoth.android.phone.mimi.event.ReplyClickEvent;
import com.emogoth.android.phone.mimi.event.ShowRepliesEvent;
import com.emogoth.android.phone.mimi.event.UpdateHistoryEvent;
import com.emogoth.android.phone.mimi.exceptions.ChanPostException;
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
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.emogoth.android.phone.mimi.util.ThreadRegistry;
import com.emogoth.android.phone.mimi.view.DividerItemDecoration;
import com.emogoth.android.phone.mimi.viewmodel.ThreadViewModel;
import com.emogoth.android.phone.mimi.widget.MimiRecyclerView;
import com.google.android.material.snackbar.Snackbar;
import com.mimireader.chanlib.ChanConnector;
import com.mimireader.chanlib.models.ChanBoard;
import com.mimireader.chanlib.models.ChanPost;
import com.mimireader.chanlib.models.ChanThread;
import com.mimireader.chanlib.models.ErrorChanThread;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import retrofit2.adapter.rxjava2.HttpException;


public class ThreadDetailFragment extends MimiFragmentBase implements
        TabInterface,
        ContentInterface {
    private static final String LOG_TAG = ThreadDetailFragment.class.getSimpleName();
    private static final boolean LOG_DEBUG = true;
    private static final String REPLY_FRAGMENT_TAG = "reply_fragment";
    private static final int LOADER_ID = 2;

    private static final int MAX_RETRIES = 5;

    private String threadReplyFragmentTag;

    private String postUrl;

    private MimiRecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private ThreadListAdapter threadListAdapter;
    private String boardName;
    private String boardTitle;
    private long threadId;
    private ChanThread currentThread = ChanThread.empty();
    private View loadingLayout;

    private int[] listViewItemHeight;

    private ViewGroup messageContainer;

    private TextView messageText;
//    private boolean doThreadRegistryUpdate = false;

    private String replyComment;
    private PostFragment postFragment;
    private int unreadCount = 0;
    private int loaderId = LOADER_ID;
    private TextView closeMessageButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean createNewPostFragment = false;
    //    private boolean isWatched;
    private boolean rememberThreadScrollPosition;
    private int postFromExtras = 0;
    private MenuItem bookmarkMenuItem;
    private Toolbar toolbar;
    private boolean wasVisibleToUser;

    private RecyclerView.ItemDecoration recyclerViewDivider;

    private long profileTimer;

    private Handler lastReadHandler = new Handler();
    private LastReadRunnable lastReadRunnable;

    private Snackbar postingSnackbar;
    private boolean stickyAutoRefresh;
    private ChanConnector chanConnector;
    private RecyclerView.OnScrollListener recyclerViewScrollListener;
    private ViewStub findViewStub;
    private View findView;

    private Disposable threadSubscription;
    private Disposable boardInfoSubscription;
    private Disposable threadWatcherSubscription;

    private View listHeader;
    private View listFooter;

    private ThreadViewModel viewModel;

    private Disposable fetchPostSubscription;
    private Disposable historyRemovedSubscription;
    private Disposable putHistorySubscription;
    private Disposable fetchHistorySubscription;
    private Disposable removeHistorySubscription;
    private Disposable addPostSubscription;

    private Handler textFindHandler;
    private Runnable updateAdapterRunnable;
    private Space topSpacer;
    private Bundle postState;

    public static ThreadDetailFragment newInstance(long threadId, String boardName, String boardTitle, ChanPost firstPost, boolean stickyAutoRefresh, boolean hasOptionsMenu) {
        final Bundle args = new Bundle();
        args.putLong(Extras.EXTRAS_THREAD_ID, threadId);
        args.putString(Extras.EXTRAS_BOARD_NAME, boardName);
        args.putString(Extras.EXTRAS_BOARD_TITLE, boardTitle);
        args.putBoolean(Extras.EXTRAS_STICKY_AUTO_REFRESH, stickyAutoRefresh);
        args.putBoolean(Extras.EXTRAS_OPTIONS_MENU_ENABLED, hasOptionsMenu);

        if (firstPost != null) {
            args.putParcelable(Extras.EXTRAS_THREAD_FIRST_POST, firstPost);
        }

        ThreadDetailFragment fragment = new ThreadDetailFragment();
        fragment.setArguments(args);

        return fragment;
    }

    public static ThreadDetailFragment newInstance(long threadId, String boardName, String boardTitle, ChanPost firstPost, boolean stickyAutoRefresh) {
        return newInstance(threadId, boardName, boardTitle, firstPost, stickyAutoRefresh, true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(false);

        if (LOG_DEBUG) {
            Log.d(LOG_TAG, "Entered onCreate()");
        }

        if (savedInstanceState == null) {
            extractExtras(getArguments());
        } else {
            if (savedInstanceState.containsKey(Extras.EXTRAS_THREAD_ID)
                    && getArguments() != null
                    && getArguments().containsKey(Extras.EXTRAS_THREAD_ID)
                    && savedInstanceState.getLong(Extras.EXTRAS_THREAD_ID) == getArguments().getLong(Extras.EXTRAS_THREAD_ID)) {
                extractExtras(savedInstanceState);
            } else {
                extractExtras(getArguments());
            }

        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefs.edit().remove(RefreshJobService.NOTIFICATIONS_KEY_THREAD_SIZE + "." + threadId).apply();

        lastReadRunnable = new LastReadRunnable(threadId, boardName);
        threadReplyFragmentTag = REPLY_FRAGMENT_TAG + "_" + threadId;

        ViewModelProvider.Factory factory = new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new ThreadViewModel(boardName, threadId);
            }
        };
        viewModel = ViewModelProviders
                .of(this, factory)
                .get(ThreadViewModel.class);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        if (LOG_DEBUG) {
            Log.d(LOG_TAG, "Entered onCreateView()");
        }

        final View view = inflater.inflate(R.layout.fragment_thread_detail, container, false);

        loadingLayout = view.findViewById(R.id.loading_layout);
        recyclerView = view.findViewById(R.id.thread_list);
        messageContainer = view.findViewById(R.id.message_container);
        messageText = view.findViewById(R.id.message);
        closeMessageButton = view.findViewById(R.id.close_message_button);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        rememberThreadScrollPosition = MimiUtil.rememberThreadScrollPosition(getActivity());
        findViewStub = view.findViewById(R.id.find_bar_stub);
        topSpacer = (Space) view.findViewById(R.id.thread_list_top_spacer);

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
    public void onViewCreated(@NonNull View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (LOG_DEBUG) {
            Log.d(LOG_TAG, "Entered onViewCreated() for thread " + threadId);
        }

        chanConnector = new FourChanConnector.Builder()
                .setCacheDirectory(MimiUtil.getInstance().getCacheDir())
                .setEndpoint(FourChanConnector.getDefaultEndpoint())
                .setPostEndpoint(FourChanConnector.getDefaultPostEndpoint())
                .setClient(HttpClientFactory.getInstance().getClient())
                .build();

        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), LinearLayoutManager.VERTICAL));

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

        swipeRefreshLayout.setOnRefreshListener(() -> {
            swipeRefreshLayout.setRefreshing(true);
            refresh(false);
        });

        swipeRefreshLayout.setProgressViewOffset(false, pixels, pixels + 100);

        if (getUserVisibleHint() && MimiUtil.getLayoutType(getActivity()) != LayoutType.SLIDING_PANEL) {
            initMenu();
        }

        if (!TextUtils.isEmpty(boardName)) {
            initPosts();
        } else {
            Log.w(LOG_TAG, "No board name for thread " + threadId);
        }

    }

    private void initPosts() {
        threadWatcherSubscription = viewModel.watchThread()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(chanThread -> {
                    Log.d(LOG_TAG, "Flowable onNext() called; thread size= " + chanThread.getPosts().size());
                    if (chanThread.getPosts().size() > 0 && threadId == chanThread.getThreadId()) {
                        final int pos;
                        if (!viewModel.getFirstFetchComplete()) {
                            pos = viewModel.lastReadPos();
                            RefreshScheduler.getInstance().addThread(boardName, threadId, viewModel.bookmarked());
                        } else {
                            pos = 0;
                        }

                        ThreadRegistry.getInstance().setThreadSize(chanThread.getThreadId(), chanThread.getPosts().size());
                        viewModel.setFirstFetchComplete(true);

                        currentThread = chanThread;
                        showThread(chanThread, pos > 0 && rememberThreadScrollPosition);

                    }
                })
                .onErrorResumeNext(throwable -> {
                    Log.e(LOG_TAG, "Error occurred while watching thread", throwable);
                    return Flowable.just(new ErrorChanThread(new ChanThread(boardName, threadId, Collections.emptyList()), throwable));
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();

        viewModel.fetchThread(false)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(t -> {
                    if (t instanceof ErrorChanThread) {
                        Log.e(LOG_TAG, "Error while fetching thread", ((ErrorChanThread) t).getError());
                        onErrorResponse(((ErrorChanThread) t).getError());
                    }
                })
                .doOnError(this::onErrorResponse)
                .subscribe();
    }

    @Override
    public void initMenu() {
        super.initMenu();

        if (toolbar != null) {
            toolbar.getMenu().clear();
            if (viewModel.bookmarked() && bookmarkMenuItem != null) {
                bookmarkMenuItem.setIcon(R.drawable.ic_bookmark);
            }

            if (TextUtils.isEmpty(boardTitle)) {
                RxUtil.safeUnsubscribe(boardInfoSubscription);
                boardInfoSubscription = BoardTableConnection.fetchBoard(boardName)
                        .compose(DatabaseUtils.<ChanBoard>applySchedulers())
                        .subscribe(chanBoard -> {
                            if (!TextUtils.isEmpty(chanBoard.getName())) {
                                boardTitle = chanBoard.getTitle();

                                if (getActivity() instanceof MimiActivity) {
                                    MimiActivity activity = (MimiActivity) getActivity();
                                    activity.getSupportActionBar().setTitle(boardTitle);
                                    activity.getSupportActionBar().setSubtitle(String.valueOf(threadId));
                                }
                            }

                        });
            } else {
                if (getActivity() instanceof MimiActivity) {
                    MimiActivity activity = (MimiActivity) getActivity();
                    activity.getSupportActionBar().setTitle(boardTitle);
                    activity.getSupportActionBar().setSubtitle(String.valueOf(threadId));
                }
            }

            final View spinner = toolbar.findViewById(R.id.board_spinner);
            if (spinner != null) {
                spinner.setVisibility(View.GONE);
            }
        }

        if (getActivity() != null) {
            getActivity().supportInvalidateOptionsMenu();
        }

    }

    @Override
    public int getMenuRes() {
        if (MimiUtil.getLayoutType(getActivity()) == LayoutType.TABBED) {
            return R.menu.detail_tab;
        } else {
            return R.menu.detail;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        int menuRes = getMenuRes();
        inflater.inflate(menuRes, menu);

        final MenuItem bookmarkItem = menu.findItem(R.id.bookmark_menu);
        if (bookmarkItem != null) {
            if (viewModel.bookmarked()) {
                bookmarkItem.setIcon(R.drawable.ic_bookmark);
            }
            bookmarkMenuItem = bookmarkItem;
        }

        final MenuItem closeTabItem = menu.findItem(R.id.close_tab);
        if (!(getActivity() instanceof TabsActivity) && closeTabItem != null) {
            closeTabItem.setVisible(false);
        }

        final MenuItem closeOtherTabsItem = menu.findItem(R.id.close_other_tabs);
        if (!(getActivity() instanceof TabsActivity) && closeOtherTabsItem != null) {
            closeOtherTabsItem.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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
                            MimiUtil.https() + getResources().getString(R.string.board_link) + getResources().getString(R.string.raw_thread_path, getBoardName(), getThreadId()));
                    sendIntent.setType("text/plain");
                    startActivity(sendIntent);
                    return true;

                case R.id.find_menu:
                    showFindView();
                    return true;

                case R.id.close_tab:
                    BusProvider.getInstance().post(new CloseTabEvent(threadId, boardName, boardTitle, false));
                    return true;

                case R.id.close_other_tabs:
                    BusProvider.getInstance().post(new CloseTabEvent(threadId, boardName, boardTitle, true));
                    return true;

                case R.id.quick_top:
                    if (recyclerView != null) {
                        recyclerView.scrollToPosition(0);
                    }
                    return true;

                case R.id.quick_bottom:
                    if (recyclerView != null && currentThread != null && currentThread.getPosts() != null) {
                        recyclerView.scrollToPosition(currentThread.getPosts().size());
                    }
                    return true;
            }

            return false;

        }

        return false;
    }

    private void showFindView() {
        if (textFindHandler == null) {
            textFindHandler = new Handler();
        }
        if (findView != null && topSpacer != null) {
            findView.setVisibility(View.VISIBLE);
            topSpacer.setVisibility(View.VISIBLE);
            AppCompatEditText input = findView.findViewById(R.id.find_text);
            if (input != null) {
                input.requestFocus();
                MimiUtil.showKeyboard();
            }
            return;
        }

        findViewStub.setLayoutResource(R.layout.find_bar_view);
        findViewStub.setOnInflateListener((stub, inflated) -> {
            findView = inflated;
            if (topSpacer != null) {
                topSpacer.setVisibility(View.VISIBLE);
            }

            final AppCompatTextView foundText = inflated.findViewById(R.id.number_found);
            final AppCompatEditText input = inflated.findViewById(R.id.find_text);
            input.requestFocus();
            MimiUtil.showKeyboard();
            input.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    findNext();
                    return true;
                }
                return false;
            });
            input.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(final CharSequence s, int start, int before, int count) {
                    if (threadListAdapter != null) {
                        textFindHandler.removeCallbacks(updateAdapterRunnable);

                        if (!TextUtils.isEmpty(s)) {
                            textFindHandler.postDelayed(updateAdapterRunnable, 500);
                            updateAdapterRunnable = () -> {
                                if (threadListAdapter != null) {
                                    threadListAdapter.getFilter().filter(s);
                                    int count1 = threadListAdapter.getFilterCount();
                                    foundText.setText(inflated.getResources().getString(R.string.found_number, count1));
                                }
                            };
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });

            View nextButton = inflated.findViewById(R.id.find_next);
            nextButton.setOnClickListener(v -> findNext());

            View prevButton = inflated.findViewById(R.id.find_prev);
            prevButton.setOnClickListener(v -> findPrevious());

            View closeButton = inflated.findViewById(R.id.close_find_bar);
            closeButton.setOnClickListener(v -> {
                if (threadListAdapter != null) {
                    threadListAdapter.clearFilter();
                }

                if (input != null) {
                    input.setText("");
                    MimiUtil.hideKeyboard(input);
                }

                if (inflated != null) {
                    inflated.setVisibility(View.GONE);
                }

                if (topSpacer != null) {
                    topSpacer.setVisibility(View.GONE);
                }
            });

            threadListAdapter.setOnFilterUpdateCallback((filteredString, count) -> foundText.setText(inflated.getResources().getString(R.string.found_number, count)));
        });
        findViewStub.inflate();
    }

    protected void findNext() {
        int pos = threadListAdapter.getNextFoundStringPosition();
        if (pos >= 0) {
            layoutManager.scrollToPositionWithOffset(pos, 20);
        }

        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "Position = " + pos);
        }
    }

    protected void findPrevious() {
        int pos = threadListAdapter.getPrevFoundStringPosition();
        if (pos >= 0) {
            layoutManager.scrollToPositionWithOffset(pos, 20);
        }

        if (BuildConfig.DEBUG) {
            Log.d(LOG_TAG, "Position = " + pos);
        }
    }

    private void createRecyclerViewScrollListeners(final boolean useFastScroll) {
        if (recyclerViewScrollListener == null) {
            recyclerViewScrollListener = new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (dy > 0) {
                        lastReadHandler.removeCallbacks(lastReadRunnable);
                        lastReadHandler.postDelayed(lastReadRunnable, 500);
                    }
                }
            };
        }

        recyclerView.removeOnScrollListener(recyclerViewScrollListener);
        recyclerView.addOnScrollListener(recyclerViewScrollListener);
    }

    private void extractExtras(final Bundle bundle) {
        if (bundle != null) {
            if (bundle.containsKey(Extras.EXTRAS_THREAD_ID)) {
                threadId = bundle.getLong(Extras.EXTRAS_THREAD_ID);
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

                List<ChanPost> posts = new ArrayList<>(1);
                posts.add(post);

                currentThread = new ChanThread();
                currentThread.setThreadId(threadId);
                currentThread.setBoardName(boardName);
                currentThread.setPosts(posts);

                postFromExtras++;
            }

            if (bundle.containsKey(Extras.EXTRAS_STICKY_AUTO_REFRESH)) {
                stickyAutoRefresh = bundle.getBoolean(Extras.EXTRAS_STICKY_AUTO_REFRESH);
            } else {
                stickyAutoRefresh = false;
            }

            if (bundle.containsKey(Extras.EXTRAS_POST_STATE)) {
                postState = bundle.getBundle(Extras.EXTRAS_POST_STATE);
            }
        }

    }

    private void toggleWatch() {
        final boolean bookmarked = viewModel.bookmarked();
        if (currentThread != null) {
            final ChanPost post;
            if (viewModel.bookmarked()) {

                if (bookmarkMenuItem != null) {
                    bookmarkMenuItem.setIcon(R.drawable.ic_bookmark_outline);
                }
                RxUtil.safeUnsubscribe(removeHistorySubscription);
                removeHistorySubscription = HistoryTableConnection.removeHistory(boardName, threadId)
                        .compose(DatabaseUtils.<Boolean>applySchedulers())
                        .subscribe();
//                ThreadRegistry.getInstance().remove(threadId);

            } else if (bookmarkMenuItem != null) {
                bookmarkMenuItem.setIcon(R.drawable.ic_bookmark);
            }
//                ThreadRegistry.getInstance().update(threadId, threadListAdapter.getLastPosition(), true, true);


            if (currentThread.getPosts().size() > 0) {
                if (!bookmarked) {
                    post = currentThread.getPosts().get(0);
                    RxUtil.safeUnsubscribe(putHistorySubscription);
                    putHistorySubscription = HistoryTableConnection.putHistory(boardName, post, currentThread.getPosts().size(), viewModel.lastReadPos(), !bookmarked)
                            .compose(DatabaseUtils.<Boolean>applySchedulers())
                            .subscribe(success -> {
                                if (success) {
                                    BusProvider.getInstance().post(new UpdateHistoryEvent(currentThread.getThreadId(), boardName, currentThread.getPosts().size(), currentThread.getPosts().get(0).isClosed()));
                                    post.setWatched(bookmarked);
                                }
                            });
                } else {
                    BusProvider.getInstance().post(new UpdateHistoryEvent(currentThread.getThreadId(), boardName, currentThread.getPosts().size(), currentThread.getPosts().get(0).isClosed()));
                }


            }

            viewModel.setBookmarked(!bookmarked)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe();
        }
    }

    private void saveHistory() {
        if (currentThread != null && currentThread.getPosts() != null) {
            final ChanPost firstPost = getFirstPost(currentThread);
            final int postCount = currentThread.getPosts().size();

//            ThreadRegistryModel threadInfo = ThreadRegistry.getInstance().getThread(threadId);
//            if (threadInfo != null) {
//                isWatched = threadInfo.isBookmarked();
//            }

            RxUtil.safeUnsubscribe(fetchHistorySubscription);
            fetchHistorySubscription = HistoryTableConnection.fetchPost(boardName, threadId)
                    .flatMap((Function<History, Flowable<Boolean>>) history -> {
                        final boolean watched;
                        if (history.threadId > -1) {
                            watched = viewModel.bookmarked();
                        } else {
                            watched = history.watched == 1;
                        }

                        if (firstPost.getNo() > -1 && history.threadSize != postCount) {
                            return HistoryTableConnection.putHistory(boardName, firstPost, postCount, viewModel.lastReadPos(), watched);
                        } else {
                            return Flowable.just(false);
                        }
                    })
                    .compose(DatabaseUtils.<Boolean>applySchedulers())
                    .subscribe(success -> {
                        if (success) {
                            Log.d(LOG_TAG, "Successfully updated history");
                        } else {
                            Log.e(LOG_TAG, "Error while updating history");
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

//        final int oldThreadSize = ThreadRegistry.getInstance().getLastReadPosition(threadId);

        RxUtil.safeUnsubscribe(threadSubscription);
        threadSubscription = refreshObservable()
                .subscribe(success -> Log.d(LOG_TAG, "Refreshed thread: /" + boardName + "/" + threadId), this::onErrorResponse);

        swipeRefreshLayout.setEnabled(false);
    }

    private Single<ChanThread> refreshObservable() {
        return viewModel.fetchThread(true)
                .doOnSuccess(chanThread -> {
                    if (chanThread instanceof ErrorChanThread) {
                        onErrorResponse(((ErrorChanThread) chanThread).getError());
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void showPostFragment() {

        if (getActivity() == null || !isAdded()) {
            return;
        }

        try {
            final FragmentManager fm = getChildFragmentManager();

            recyclerView.smoothScrollBy(0, 0);
            final int threadSize;
            if (currentThread != null && currentThread.getPosts() != null) {
                threadSize = currentThread.getPosts().size();
            } else {
                threadSize = 0;
            }

            final Bundle args;
            if (postState == null || createNewPostFragment) {
                createNewPostFragment = false;

                args = new Bundle();
                args.putLong(Extras.EXTRAS_THREAD_ID, threadId);
                args.putString(Extras.EXTRAS_BOARD_NAME, boardName);
                args.putString(Extras.EXTRAS_BOARD_TITLE, boardTitle);
                args.putInt(Extras.EXTRAS_THREAD_SIZE, threadSize);

            } else {
                args = postState;
            }

            if (!TextUtils.isEmpty(replyComment)) {
                args.putString(Extras.EXTRAS_POST_REPLY, replyComment);
                replyComment = null;
            }

            postFragment = new PostFragment();
            postFragment.setPostListener(createPostCompleteListener());
            postFragment.setArguments(args);

            postFragment.show(fm, threadReplyFragmentTag);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error showing post form", e);
        }
    }

    private PostFragment.PostListener createPostCompleteListener() {
        return new PostFragment.PostListener() {
            @Override
            public void onStartPost() {
                RxUtil.safeUnsubscribe(fetchHistorySubscription);
                fetchHistorySubscription = HistoryTableConnection.fetchPost(boardName, threadId)
                        .compose(DatabaseUtils.<History>applySchedulers())
                        .subscribe(history -> {
                            if (history.threadId == -1) {
                                return;
                            }

                            final boolean watched = history.watched == 1;
                            if (!watched) {
                                toggleWatch();
                            }
                        });

                showPostProgress();
                onBackPressed();
            }

            @Override
            public void onSuccess(final String postId) {
                try {
                    if (threadWatcherSubscription == null || threadWatcherSubscription.isDisposed()) {
                        return;
                    }

                    Log.i(LOG_TAG, "Post ID: " + postId);

                    if (getActivity() == null) {
                        return;
                    }

                    showPostStatus("Success!", null);
                    refresh(false);

                    final FragmentTransaction ft = getChildFragmentManager().beginTransaction();
                    ft.remove(postFragment);
                    ft.commit();

                    createNewPostFragment = true;
                    postFragment = null;
                    replyComment = null;
                    postState = null;
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error creating post", e);
                }
            }

            @Override
            public void onError(Throwable error) {
                if (getActivity() != null) {
                    error.printStackTrace();
                    final String html;
                    if (error instanceof ChanPostException) {
                        html = ((ChanPostException) error).getHtml();
                    } else {
                        html = null;
                    }
                    showPostStatus(error.getLocalizedMessage(), html);
                }
            }

            @Override
            public void onDismiss() {
                if (postFragment != null) {
                    postState = postFragment.saveState();
                }
            }

            @Override
            public void onCanceled() {
                if (getActivity() != null && postFragment != null) {
                    final FragmentTransaction ft = getChildFragmentManager().beginTransaction();
                    postFragment.dismiss();
                    ft.remove(postFragment);
                    ft.commit();

                    createNewPostFragment = true;
                    postFragment = null;
                    replyComment = null;
                    postState = null;
                }
            }
        };
    }

//    @Subscribe
//    public void autoRefreshThread(final UpdateHistoryEvent event) {
//        if (event != null && getActivity() != null) {
//
//            if (event.getThreadSize() == 0) {
//                refresh(false);
//                return;
//            }
//
//            if (event.getThreadId() == threadId
//                    && event.getBoardName().equals(boardName)
//                    && (currentThread == null || event.getThreadSize() != currentThread.getPosts().size())
//                    && threadListAdapter != null) {
//
//                refresh(false);
//            }
//        }
//    }

    @Subscribe
    public void onGalleryPagerScrolled(final GalleryPagerScrolledEvent event) {

        if (getUserVisibleHint()) {
            if (currentThread != null) {
                final int index = MimiUtil.findPostPositionById(event.getPostNumber(), currentThread.getPosts());
                if (index >= 0 && recyclerView != null) {
                    scrollListToPosition(recyclerView, index);
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

    private void showPostStatus(final String status, final String html) {
        if (getActivity() != null) {
            final View v = getActivity().findViewById(android.R.id.content);
            if (postingSnackbar != null && postingSnackbar.isShown()) {
                postingSnackbar.dismiss();
            }
            Snackbar snackbar = Snackbar.make(v, status, Snackbar.LENGTH_LONG);
            if (!TextUtils.isEmpty(html)) {
                snackbar.setAction(R.string.view,
                        view -> WebActivity
                                .start(getActivity(), html))
                        .setActionTextColor(ResourcesCompat.getColor(getResources(), R.color.md_green_400, getActivity().getTheme()));
            }

            snackbar.show();
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        outState.putLong(Extras.EXTRAS_THREAD_ID, threadId);
        outState.putString(Extras.EXTRAS_BOARD_NAME, boardName);
        outState.putString(Extras.EXTRAS_BOARD_TITLE, boardTitle);
        outState.putInt(Extras.LOADER_ID, loaderId);
        outState.putBoolean(Extras.EXTRAS_STICKY_AUTO_REFRESH, stickyAutoRefresh);

        if (postState != null) {
            outState.putBundle(Extras.EXTRAS_POST_STATE, postState);
        }

        super.onSaveInstanceState(outState);
    }

    private void showLoadingLayout() {
        if (recyclerView != null) {
            recyclerView.setVisibility(View.GONE);
        }

        if (loadingLayout != null) {
            loadingLayout.setVisibility(View.VISIBLE);
        }

        if (messageContainer != null) {
            messageContainer.setVisibility(View.GONE);
        }
    }

    private void showContent() {
        if (loadingLayout != null) {
            loadingLayout.setVisibility(View.GONE);
        }

        if (recyclerView != null) {
            recyclerView.setVisibility(View.VISIBLE);
        }

        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setEnabled(true);
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void setupRepliesDialog() {
        threadListAdapter.setOnReplyMenuClickListener(new ReplyMenuClickListener() {
            @Override
            public void onReply(View view, long threadId) {
                replyComment = ">>" + threadId + "\n";
                showPostFragment();
            }

            @Override
            public void onQuote(View view, ChanPost post) {

                if (post.getCom() != null) {
                    String quote = post.getComment().toString();

                    if (!TextUtils.isEmpty(quote)) {
                        quote = ">" + quote;
                        quote = quote.replace("\n", "\n>");
                    }
                    replyComment = ">>" + post.getNo() + "\n" + quote + "\n";
                }
                showPostFragment();
            }

            @Override
            public void onQuoteSelection(View view, ChanPost post, int start, int end) {
                if (post.getCom() != null) {
                    String quote = post.getComment().toString();
                    if (!TextUtils.isEmpty(quote) && start < quote.length() && end <= quote.length()) {
                        quote = ">" + quote.substring(start, end);
                        quote = quote.replace("\n", "\n>");
                    }
                    replyComment = ">>" + post.getNo() + "\n" + quote + "\n";
                }

                showPostFragment();
            }
        });
    }

    private void showThread(final ChanThread thread, final boolean scrollToPosition) {
        if (thread == null || thread.getPosts() == null || !isAdded()) {
            return;
        }

//        currentThread = thread;

        listViewItemHeight = new int[thread.getPosts().size()];
        Arrays.fill(listViewItemHeight, -1);

        if (bookmarkMenuItem != null) {
            if (viewModel.bookmarked()) {
                bookmarkMenuItem.setIcon(R.drawable.ic_bookmark);
            } else {
                bookmarkMenuItem.setIcon(R.drawable.ic_bookmark_outline);
            }
        }

        if (threadListAdapter != null) {
            if (recyclerView.isComputingLayout()) {
                Log.d(LOG_TAG, "RecyclerView is computing layout");
            }

            threadListAdapter.setThread(thread);

//            if (getUserVisibleHint()) {
//                ThreadRegistry.getInstance().update(threadId, thread.getPosts().size(), true, isWatched);
//            } else {
//                doThreadRegistryUpdate = true;
//            }
        }

        if (scrollToPosition) {
            recyclerView.post(() -> scrollListToPosition(recyclerView, viewModel.lastReadPos()));
            // unnecessary?
//            if (originalThreadSize >= 0) {
//
//            } else {
//                recyclerView.post(() -> scrollListToPosition(recyclerView, 0));
//            }
        }

//        final ThreadRegistry threadRegistry = ThreadRegistry.getInstance();
//        ThreadRegistryModel threadInfo = threadRegistry.getThread(threadId);
//        if (threadInfo == null) {
//            Log.d(LOG_TAG, "Thread does not exist in registry: Adding thread");
//            threadRegistry.add(boardName, threadId, currentThread.getPosts().size(), isWatched);
//        } else {
//            Log.d(LOG_TAG, "Thread already exists in registry: Not adding thread");
//            isWatched = viewModel.bookmarked();
//        }

        if (postFromExtras > 1) {
            Log.d(LOG_TAG, "Showing footer refresh icon");
            postFromExtras = 0;
        }
        showContent();

        final FragmentManager fm = getChildFragmentManager();
        final PostFragment fragment = (PostFragment) fm.findFragmentByTag(threadReplyFragmentTag);

        if (fragment != null && fragment.getUserVisibleHint()) {
            postFragment = fragment;
            postFragment.setPostListener(createPostCompleteListener());
        } else if (MimiUtil.historyEnabled(MimiApplication.getInstance().getApplicationContext())) {
            saveHistory();
        }

    }

    public void onErrorResponse(final Throwable error) {
        Log.e(LOG_TAG, "Error: " + error.getLocalizedMessage(), error);

        showContent();

        if (error instanceof HttpException) {
            HttpException exception = (HttpException) error;

            if (exception.code() == 404) {
                if (currentThread == null) {
                    if (closeMessageButton != null) {
                        closeMessageButton.setVisibility(View.INVISIBLE);
                    }
                } else {
                    if (closeMessageButton != null) {
                        closeMessageButton.setVisibility(View.VISIBLE);
                        closeMessageButton.setOnClickListener(v -> {
                            if (messageContainer != null) {
                                final AlphaAnimation animation = new AlphaAnimation(1.0F, 0.0F);
                                animation.setDuration(100);
                                messageContainer.setAnimation(animation);
                                messageContainer.setVisibility(View.GONE);
                            }
                        });
                    }
                }
                if (messageText != null) {
                    messageText.setText(R.string.error_404);
                }

                if (messageContainer != null) {
                    messageContainer.setVisibility(View.VISIBLE);
                }

                if (viewModel.bookmarked()) {
                    RxUtil.safeUnsubscribe(historyRemovedSubscription);
                    historyRemovedSubscription = HistoryTableConnection.setHistoryRemovedStatus(boardName, threadId, true)
                            .compose(DatabaseUtils.<Boolean>applySchedulers())
                            .subscribe();
                }
            }
        }

        Log.d(LOG_TAG, "Exception while accessing network", error);
    }

    private ChanPost getFirstPost(final ChanThread thread) {
        if (thread != null && thread.getPosts().size() > 0) {
            return thread.getPosts().get(0);
        }

        return new ChanPost();
    }

    @Override
    public void onStop() {
        super.onStop();

        BusProvider.getInstance().unregister(this);

        RxUtil.safeUnsubscribe(threadSubscription);
        RxUtil.safeUnsubscribe(boardInfoSubscription);
        RxUtil.safeUnsubscribe(putHistorySubscription);
        RxUtil.safeUnsubscribe(fetchHistorySubscription);
        RxUtil.safeUnsubscribe(historyRemovedSubscription);
        RxUtil.safeUnsubscribe(removeHistorySubscription);
        RxUtil.safeUnsubscribe(addPostSubscription);
        RxUtil.safeUnsubscribe(threadWatcherSubscription);

        threadWatcherSubscription = null;

        viewModel.setFirstFetchComplete(false);

        if (!viewModel.bookmarked()) {
            if (!stickyAutoRefresh) {
                RefreshScheduler.getInstance().removeThread(boardName, threadId);
            }
        } else if (recyclerView != null) {
            ThreadRegistry.getInstance().setLastReadPosition(threadId, layoutManager.findFirstVisibleItemPosition() - 1);
            viewModel.setLastReadPosition(layoutManager.findFirstVisibleItemPosition() - 1)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe();

        }

        recyclerView.clearOnScrollListeners();
    }

    @Override
    public void onStart() {
        super.onStart();

        BusProvider.getInstance().register(this);

        if (recyclerView != null) {
            createRecyclerViewScrollListeners(false);
        }

        if (threadWatcherSubscription == null && !TextUtils.isEmpty(boardName)) {
            initPosts();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!viewModel.bookmarked() && MimiUtil.getLayoutType(getActivity()) != LayoutType.TABBED) {
            RefreshScheduler.getInstance().removeThread(boardName, threadId);
        }

        if (threadListAdapter != null) {
            threadListAdapter.setOnFilterUpdateCallback(null);
        }

        RxUtil.safeUnsubscribe(fetchPostSubscription);
        fetchPostSubscription = null;

        recyclerView = null;
        layoutManager = null;
        threadListAdapter = null;
        loadingLayout = null;
        messageContainer = null;
        closeMessageButton = null;
        messageText = null;
        swipeRefreshLayout = null;
        bookmarkMenuItem = null;
        toolbar = null;
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
            if (isVisibleToUser && getActivity() instanceof IToolbarContainer) {
                IToolbarContainer activity = (IToolbarContainer) getActivity();
                activity.setExpandedToolbar(true, true);
            }

            if (!isVisibleToUser && !viewModel.bookmarked() && !stickyAutoRefresh) {
                RefreshScheduler.getInstance().removeThread(boardName, threadId);
            }
        }

        wasVisibleToUser = isVisibleToUser;

    }

    private void scrollListToPosition(RecyclerView recyclerView, int position) {
        if (recyclerView == null) {
            return;
        }

        try {
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            final int headerCount = threadListAdapter.headerCount();
            final int pos = (position + headerCount) >= threadListAdapter.getItemCount() && threadListAdapter.getItemCount() > 0 ? threadListAdapter.getItemCount() - 1 : position + headerCount;
            if (layoutManager instanceof LinearLayoutManager) {
                ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(pos, 0);
            } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                ((StaggeredGridLayoutManager) layoutManager).scrollToPositionWithOffset(pos, 0);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Could not scroll thread", e);
        }
    }

    @Subscribe
    public void showRepliesDialog(final ShowRepliesEvent event) {
        if (event.getThreadId() == threadId) {
            Log.i(LOG_TAG, "Showing replies");
            if (getActivity() != null) {

                final Handler handler = new Handler(getActivity().getMainLooper());
                handler.post(() -> {
                    if (getActivity() == null) {
                        return;
                    }

                    final FragmentManager fm = getActivity().getSupportFragmentManager();
                    final RepliesDialog repliesDialog = new RepliesDialog();
                    final Bundle args = new Bundle();

                    final ArrayList<OutsideLink> outsideLinks = new ArrayList<>();
                    final ArrayList<String> postNumbers = new ArrayList<>();
                    for (final String id : event.getReplies()) {

                        if (TextUtils.isDigitsOnly(id)) {
                            int index = -1;
                            try {
                                index = MimiUtil.findPostPositionById(Integer.valueOf(id), currentThread.getPosts());
                                ChanPost post = currentThread.getPosts().get(index);

                                if (post != null) {
                                    postNumbers.add(String.valueOf(post.getNo()));
                                }
                            } catch (ArrayIndexOutOfBoundsException e) {
                                Log.e(LOG_TAG, "post id: " + id);
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

                    if (postNumbers.size() > 0 || outsideLinks.size() > 0) {

                        args.putString(Extras.EXTRAS_BOARD_NAME, boardName);
                        args.putStringArrayList(Extras.EXTRAS_POST_LIST, postNumbers);
                        args.putParcelableArrayList(Extras.EXTRAS_OUTSIDE_LINK_LIST, outsideLinks);
                        args.putLong(Extras.EXTRAS_THREAD_ID, currentThread.getThreadId());

                        ThreadRegistry.getInstance().setPosts(currentThread.getThreadId(), currentThread.getPosts());

                        repliesDialog.setArguments(args);
                        repliesDialog.show(fm, RepliesDialog.DIALOG_TAG);
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
                final long index = MimiUtil.findPostPositionById(event.getPost().getNo(), currentThread.getPosts());

                if (index >= 0) {
                    scrollListToPosition(recyclerView, (int) index);
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

    public long getThreadId() {
        return threadId;
    }

    @Override
    public int getTabId() {
        return (int) threadId;
    }

    @Override
    public void addContent() {
        showPostFragment();
    }

    private class LastReadRunnable implements Runnable {

        public final long threadId;
        public final String boardName;

        public boolean closed;
        private int size;

        private LastReadRunnable(long threadId, String boardName) {
            this.threadId = threadId;
            this.boardName = boardName;
        }

        public long getThreadId() {
            return threadId;
        }

        public String getBoardName() {
            return boardName;
        }

        public int getSize() {
            return size;
        }

        public LastReadRunnable setSize(int size) {
            this.size = size;
            return this;
        }

        public boolean isClosed() {
            return closed;
        }

        public LastReadRunnable setClosed(boolean closed) {
            this.closed = closed;
            return this;
        }

        @Override
        public void run() {
            if (layoutManager != null) {
//                ThreadRegistry.getInstance().setLastReadPosition(getThreadId(), layoutManager.findLastVisibleItemPosition());
                ThreadRegistry.getInstance().setLastReadPosition(threadId, layoutManager.findLastVisibleItemPosition());
                viewModel.setLastReadPosition(layoutManager.findLastVisibleItemPosition())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe();
//                int threadSize = ThreadRegistry.getInstance().getThreadSize(getThreadId());
//                ThreadRegistry.getInstance().setUnreadCount(getThreadId(), threadSize - layoutManager.findFirstVisibleItemPosition());
                Log.d(LOG_TAG, "set last read position: set=" + layoutManager.findLastVisibleItemPosition() + ", get=" + viewModel.unread());
            }

            BusProvider.getInstance().post(new UpdateHistoryEvent(threadId, boardName, size, false));
        }
    }
}
