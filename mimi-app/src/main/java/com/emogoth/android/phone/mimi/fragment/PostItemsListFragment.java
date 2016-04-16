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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spannable;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.activity.MimiActivity;
import com.emogoth.android.phone.mimi.activity.PostItemListActivity;
import com.emogoth.android.phone.mimi.adapter.BoardDropDownAdapter;
import com.emogoth.android.phone.mimi.adapter.PostItemsAdapter;
import com.emogoth.android.phone.mimi.db.BoardTableConnection;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;
import com.emogoth.android.phone.mimi.db.HistoryTableConnection;
import com.emogoth.android.phone.mimi.db.UserPostTableConnection;
import com.emogoth.android.phone.mimi.db.model.Board;
import com.emogoth.android.phone.mimi.db.model.History;
import com.emogoth.android.phone.mimi.event.FABVisibilityEvent;
import com.emogoth.android.phone.mimi.fourchan.FourChanCommentParser;
import com.emogoth.android.phone.mimi.fourchan.FourChanConnector;
import com.emogoth.android.phone.mimi.interfaces.BoardItemClickListener;
import com.emogoth.android.phone.mimi.interfaces.ContentInterface;
import com.emogoth.android.phone.mimi.interfaces.EndlessRecyclerOnScrollListener;
import com.emogoth.android.phone.mimi.interfaces.OnPostItemClickListener;
import com.emogoth.android.phone.mimi.interfaces.TabInterface;
import com.emogoth.android.phone.mimi.util.BusProvider;
import com.emogoth.android.phone.mimi.util.ChanUtil;
import com.emogoth.android.phone.mimi.util.Extras;
import com.emogoth.android.phone.mimi.util.LayoutType;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.emogoth.android.phone.mimi.util.ThreadRegistry;
import com.emogoth.android.phone.mimi.widget.MimiRecyclerView;
import com.mimireader.chanlib.ChanConnector;
import com.mimireader.chanlib.models.ChanBoard;
import com.mimireader.chanlib.models.ChanCatalog;
import com.mimireader.chanlib.models.ChanPost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import autovalue.shaded.com.google.common.common.collect.Lists;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;


public class PostItemsListFragment extends MimiFragmentBase implements
        Spinner.OnItemSelectedListener,
        Toolbar.OnMenuItemClickListener,
        TabInterface,
        ContentInterface {

    private final static String LOG_TAG = PostItemsListFragment.class.getSimpleName();
    private static final String POST_FRAGMENT_TAG = "post_fragment";

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    private static final String CATALOG_PREF_STRING = "catalog_grid";
    private static final String SORT_PREF_STRING = "sort_type_pref";
    private static final String INVERT_SORT_PREF_STRING = "invert_sort_pref";

    private static final int SORT_TYPE_DEFAULT = 0;
    private static final int SORT_TYPE_THREAD_ID = 1;
    private static final int SORT_TYPE_IMAGE_COUNT = 2;
    private static final int SORT_TYPE_REPLY_COUNT = 3;

    public static final int TAB_ID = 101;

    private int mActivatedPosition = ListView.INVALID_POSITION;
    private boolean activateOnItemClick;

    private MimiRecyclerView listView;
    private List<ChanBoard> boards;

    private String boardName;
    private String boardTitle;

    private PostItemsAdapter postItemsAdapter;

    private boolean isFirstRun;

    private BoardItemClickListener boardItemClickListener;
    private CatalogItemClickListener postItemClickListener;
    private View loadingLayout;
    private View errorContainer;
    private View headerContainer;
    private TextView errorText;

    private int totalPages;
    private int currentPage = 1;
    private boolean twoPane;
    private boolean rotated = false;
    private int currentPositionY = 0;

    private boolean useCatalog;

    private boolean animating = false;

    private MenuItem bookmarkCountMenu;
    private int newPostCount = 0;

    private boolean isSearching;
    private MenuItem searchMenu;
    private SearchView searchView;
    private View listFooter;
    private int viewSwitcherChild;
    private PostFragment postFragment;
    private ArrayList<ChanPost> postList = new ArrayList<>();
    private boolean useFastScroll;
    private int sortType;
    private boolean invertSort;
    private TextView errorRefreshButton;
    private SwipeRefreshLayout listRefreshLayout;

    private PostItemsAdapter.ManagerType adapterType;
    private View footerLoading;

    private int retryCount = 0;
    private Spinner toolbarSpinner;
    private Toolbar toolbar;
    private AdapterView.OnItemClickListener listItemClickListener;
    private OnPostItemClickListener clickListener;
    private ChanConnector chanConnector;
    private EndlessRecyclerOnScrollListener scrollListener;

    private Subscription pageSubscription;
    private Subscription catalogSubscription;
    private Subscription boardInfoSubscription;
    private Subscription pagedResponseSubscription;
    private Subscription fetchBoardsSubscription;
    private Subscription fetchHistorySubscription;
    private Subscription lastAccessSubscription;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        setRetainInstance(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        isFirstRun = true;
        final View v = inflater.inflate(R.layout.fragment_catalog_list, container, false);

        listRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.list_refresh_layout);
        listView = (MimiRecyclerView) v.findViewById(R.id.catalog_list);
        errorText = (TextView) v.findViewById(R.id.error_text);
        errorRefreshButton = (TextView) v.findViewById(R.id.refresh_on_error_button);

        listFooter = inflater.inflate(R.layout.list_footer, container, false);
        footerLoading = listFooter.findViewById(R.id.progressBar);
        footerLoading.setVisibility(View.INVISIBLE);

        if (MimiUtil.getLayoutType(getActivity()) == LayoutType.TABBED) {
            headerContainer = inflater.inflate(R.layout.header_layout_catalog, container, false);
        } else {
            headerContainer = inflater.inflate(R.layout.header_layout_catalog_tall, container, false);
        }
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final boolean isHttps = MimiUtil.isSecureConnection(getActivity());
        chanConnector = new FourChanConnector.Builder()
                .setCacheDirectory(MimiUtil.getInstance().getCacheDir())
                .setEndpoint(FourChanConnector.getDefaultEndpoint(isHttps))
                .setPostEndpoint(FourChanConnector.getDefaultPostEndpoint())
                .build();

        if (savedInstanceState == null) {
            extractExtras(getArguments());
            RxUtil.safeUnsubscribe(lastAccessSubscription);
            lastAccessSubscription = BoardTableConnection.updateLastAccess(boardName)
                    .compose(DatabaseUtils.<Boolean>applySchedulers())
                    .subscribe();
        } else {
            if (savedInstanceState.containsKey(Extras.EXTRAS_BOARD_NAME)
                    && getArguments() != null
                    && getArguments().containsKey(Extras.EXTRAS_BOARD_NAME)
                    && TextUtils.equals(savedInstanceState.getString(Extras.EXTRAS_BOARD_NAME), getArguments().getString(Extras.EXTRAS_BOARD_NAME))) {
                extractExtras(savedInstanceState);
            } else {
                extractExtras(getArguments());
                RxUtil.safeUnsubscribe(lastAccessSubscription);
                lastAccessSubscription = BoardTableConnection.updateLastAccess(boardName)
                        .compose(DatabaseUtils.<Boolean>applySchedulers())
                        .subscribe();
            }
        }

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        useFastScroll = preferences.getBoolean(getString(R.string.use_fast_scroll_pref), false);
        sortType = preferences.getInt(SORT_PREF_STRING, SORT_TYPE_DEFAULT);
        invertSort = preferences.getBoolean(INVERT_SORT_PREF_STRING, false);

        TypedValue typedValue = new TypedValue();
        getActivity().getTheme().resolveAttribute(R.attr.actionBarSize, typedValue, true);
        int pixels = getResources().getDimensionPixelSize(typedValue.resourceId);

        listItemClickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (getActivity() instanceof PostItemListActivity && postItemsAdapter != null) {
                    final PostItemListActivity mimiActivity = (PostItemListActivity) getActivity();
                    final int pos = position - 1;
                    final List<ChanPost> posts = postItemsAdapter.getPosts();

                    if (pos >= 0) {
                        mimiActivity.openThread(posts, pos, boardName, boardTitle);
                    }
                }
            }
        };

        if (getActivity() instanceof OnPostItemClickListener) {
            clickListener = (OnPostItemClickListener) getActivity();
        }

        listRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                listRefreshLayout.setRefreshing(true);
                refreshBoard(false);
            }
        });

        listRefreshLayout.setProgressViewOffset(false, pixels, pixels + 170);
        if (currentPositionY > 0) {
            listView.post(new Runnable() {
                @Override
                public void run() {
                    listView.scrollTo(0, currentPositionY);
                }
            });

        }

        final boolean isGrid = preferences.getBoolean(CATALOG_PREF_STRING, false);
        if (isGrid) {
            RecyclerView.LayoutManager layoutManager = listView.getLayoutManager();
            if (layoutManager instanceof LinearLayoutManager || layoutManager == null) {
                layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
                ((StaggeredGridLayoutManager) layoutManager).setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
                listView.setLayoutManager(layoutManager);
            }
            adapterType = PostItemsAdapter.ManagerType.STAGGERED_GRID;
        } else {
            RecyclerView.LayoutManager layoutManager = listView.getLayoutManager();
            if (layoutManager instanceof StaggeredGridLayoutManager || layoutManager == null) {
                layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
                listView.setLayoutManager(layoutManager);
            }
            adapterType = PostItemsAdapter.ManagerType.LIST;
            listRefreshLayout.setVisibility(View.VISIBLE);
        }

        loadingLayout = view.findViewById(R.id.loading_layout);
        errorContainer = view.findViewById(R.id.error_container);

        errorRefreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshBoard(true);
            }
        });

        if (postList.size() == 0) {
            if (adapterType == PostItemsAdapter.ManagerType.GRID || adapterType == PostItemsAdapter.ManagerType.STAGGERED_GRID) {
                useCatalog = true;
            } else {
                useCatalog = preferences.getBoolean(getString(R.string.use_catalog_pref), true);
            }

            if (useCatalog) {
                fetchCatalog();

                Log.i(LOG_TAG, "Fetching catalog");
                showLoadingLayout();
            } else {

                RxUtil.safeUnsubscribe(boardInfoSubscription);
                boardInfoSubscription = BoardTableConnection.fetchBoard(boardName)
                        .compose(DatabaseUtils.<ChanBoard>applySchedulers())
                        .subscribe(new Action1<ChanBoard>() {
                            @Override
                            public void call(ChanBoard chanBoard) {
                                if (chanBoard != null) {
                                    totalPages = chanBoard.getPages();
                                }

                                if (totalPages == 0) {
                                    totalPages = 15;
                                }

                                Log.i(LOG_TAG, "Fetching page: " + currentPage);
                                fetchPage(currentPage);

                                showLoadingLayout();
                            }
                        });

            }
        } else {
            showContent();

            if (postItemsAdapter == null) {
                postItemsAdapter = new PostItemsAdapter(getActivity(), boardName, boardTitle, postList, chanConnector, clickListener);
                postItemsAdapter.setLayoutManager(listView.getLayoutManager());
                listView.setAdapter(postItemsAdapter);

                postItemsAdapter.addHeader(headerContainer);
                postItemsAdapter.addFooter(listFooter);
            }
        }

        if (getScrollListener(listView) != null) {
            listView.addOnScrollListener(getScrollListener(listView));
        }

        if (getUserVisibleHint()) {
            initMenu();
        }

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
                setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
            }

        }

    }

    private void switchToList() {
        if (getActivity() == null) {
            return;
        }

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        preferences.edit().putBoolean(CATALOG_PREF_STRING, false).apply();

        adapterType = PostItemsAdapter.ManagerType.LIST;
        postItemsAdapter = null;
        RecyclerView.LayoutManager layoutManager = listView.getLayoutManager();
        if (layoutManager instanceof StaggeredGridLayoutManager || layoutManager == null) {
            layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
            listView.setLayoutManager(layoutManager);

            refreshBoard(true);
        }
    }

    private void switchToGrid() {
        if (getActivity() == null) {
            return;
        }

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        preferences.edit().putBoolean(CATALOG_PREF_STRING, true).apply();

        adapterType = PostItemsAdapter.ManagerType.STAGGERED_GRID;
        RecyclerView.LayoutManager layoutManager = listView.getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager || layoutManager == null) {

            layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
            ((StaggeredGridLayoutManager) layoutManager).setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
            listView.setLayoutManager(layoutManager);

            refreshBoard(true);
        }
    }

    private EndlessRecyclerOnScrollListener getScrollListener(RecyclerView recyclerView) {
        if (scrollListener == null && !useCatalog) {
            scrollListener = new EndlessRecyclerOnScrollListener(recyclerView.getLayoutManager()) {
                @Override
                public void onLoadMore(int currentPage) {
                    fetchPage(currentPage);
                }
            };
        }

        return scrollListener;
    }

    private void catalogResponse(final ChanCatalog catalog) {
        if (getActivity() != null) {
            listRefreshLayout.setRefreshing(false);

            try {
                if (getActivity() != null) {
                    postList.clear();

                    RxUtil.safeUnsubscribe(fetchHistorySubscription);
                    fetchHistorySubscription = HistoryTableConnection.fetchHistory()
                            .compose(DatabaseUtils.<List<History>>applySchedulers())
                            .subscribe(new Action1<List<History>>() {
                                @Override
                                public void call(List<History> histories) {
                                    List<ChanPost> posts = catalog.getPosts();
                                    for (History history : histories) {
                                        for (ChanPost post : posts) {
                                            if (post.getNo() == history.threadId) {
                                                post.setWatched(history.watched);
                                            }
                                        }
                                    }

                                    postList.addAll(posts);

                                    if (postItemsAdapter == null) {
                                        postItemsAdapter = new PostItemsAdapter(getActivity(), boardName, boardTitle, postList, chanConnector, clickListener);
                                        postItemsAdapter.setLayoutManager(listView.getLayoutManager());

                                        postItemsAdapter.addHeader(headerContainer);
                                        postItemsAdapter.addFooter(listFooter);

                                        listView.setAdapter(postItemsAdapter);

                                    } else {
                                        postItemsAdapter.setLayoutManager(listView.getLayoutManager());
                                        postItemsAdapter.setPosts(postList, boardName, boardTitle);
                                    }

                                    scrollListToTop(listView);
                                    showContent();
                                }
                            });


                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error loading board", e);
                showError(getString(R.string.generic_board_load_error));
            }

        }
    }

    private void pagedResponse(ChanCatalog catalog) {
        retryCount = 0;

        if (catalog == null || catalog.getPosts() == null || getActivity() == null) {
            return;
        }

        listRefreshLayout.setRefreshing(false);

        RxUtil.safeUnsubscribe(pagedResponseSubscription);
        pagedResponseSubscription = Observable.just(catalog.getPosts())
                .subscribeOn(Schedulers.newThread())
                .zipWith(HistoryTableConnection.fetchHistory(), new Func2<List<ChanPost>, List<History>, List<ChanPost>>() {
                    @Override
                    public List<ChanPost> call(List<ChanPost> chanPosts, List<History> histories) {
                        for (History history : histories) {
                            for (ChanPost post : chanPosts) {
                                if (post.getNo() == history.threadId) {
                                    post.setWatched(history.watched);
                                }
                            }
                        }
                        return chanPosts;
                    }
                })
                .map(new Func1<List<ChanPost>, List<ChanPost>>() {
                    @Override
                    public List<ChanPost> call(List<ChanPost> chanPosts) {

                        final LinkedHashSet<ChanPost> postSet = new LinkedHashSet<>(postList);
                        for (ChanPost post : chanPosts) {
                            postSet.add(post);
                        }

                        return new ArrayList<>(postSet);
                    }
                })
                .unsubscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(DatabaseUtils.<List<ChanPost>>applySchedulers())
                .subscribe(new Action1<List<ChanPost>>() {
                    @Override
                    public void call(List<ChanPost> chanPosts) {
                        postList.clear();
                        postList.addAll(chanPosts);

                        if (postItemsAdapter == null) {
                            postItemsAdapter = new PostItemsAdapter(getActivity(), boardName, boardTitle, postList, chanConnector, clickListener);
                            postItemsAdapter.setLayoutManager(listView.getLayoutManager());

                            listView.setAdapter(postItemsAdapter);

                            postItemsAdapter.addHeader(headerContainer);
                            postItemsAdapter.addFooter(listFooter);

                            scrollListToTop(listView);
                        } else {
                            postItemsAdapter.addPosts(postList);

                            if (currentPage == 1) {
                                scrollListToTop(listView);
                            }

                        }

                        if (twoPane && currentPage == 1) {
                            postItemClickListener.onPostItemClick(postItemsAdapter.getPosts(), 0, boardName, boardTitle);
                        }

                        showContent();

                    }
                });
    }

    private void scrollListToTop(final RecyclerView recyclerView) {
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                recyclerView.scrollToPosition(0);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PostFragment.PICK_IMAGE) {
            final FragmentManager fm = getChildFragmentManager();
            final PostFragment fragment = (PostFragment) fm.findFragmentByTag(POST_FRAGMENT_TAG);

            if (fragment != null) {
                fragment.onActivityResult(requestCode, resultCode, data);
            }

        }

        Log.i(LOG_TAG, "Activity result called");
    }

    public void refreshBoard(final boolean showLoading) {

        if (getActivity() == null) {
            return;
        }

        if (showLoading) {
            showLoadingLayout();
        }

        if (getScrollListener(listView) != null) {
            getScrollListener(listView).reset();
        }

        currentPage = 1;
        if (postList != null) {
            postList.clear();

            if (postItemsAdapter != null) {
                postItemsAdapter.clear();
            }
        }

        if (adapterType == PostItemsAdapter.ManagerType.GRID || adapterType == PostItemsAdapter.ManagerType.STAGGERED_GRID) {
            useCatalog = true;
        } else {
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            useCatalog = preferences.getBoolean(getString(R.string.use_catalog_pref), true);
        }

        if (useCatalog) {
            fetchCatalog();
        } else {
            fetchPage(currentPage);
        }

    }

    public void setUnreadCount(final int count) {
        newPostCount = count;
        getActivity().supportInvalidateOptionsMenu();
    }

    private void extractExtras(final Bundle bundle) {
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
        if (bundle.containsKey(STATE_ACTIVATED_POSITION)) {
            mActivatedPosition = bundle.getInt(STATE_ACTIVATED_POSITION);
        }
        if (bundle.containsKey(Extras.EXTRAS_POSITION)) {
            currentPositionY = bundle.getInt(Extras.EXTRAS_POSITION);
            Log.i(LOG_TAG, "scroll position=" + currentPositionY);
        }
        if (bundle.containsKey(Extras.EXTRAS_CATALOG)) {
            useCatalog = bundle.getBoolean(Extras.EXTRAS_CATALOG);
        }
        if (bundle.containsKey("rotated")) {
            rotated = bundle.getBoolean("rotated");
        }
        if (bundle.containsKey(Extras.EXTRAS_POST_LIST)) {
            bundle.setClassLoader(ChanPost.class.getClassLoader());
            postList = bundle.getParcelableArrayList(Extras.EXTRAS_POST_LIST);
        }
    }

    public void setBoard(final String boardName) {
        this.boardName = boardName;
    }

    private ChanCatalog processCatalog(ChanCatalog catalog) {
        List<ChanPost> posts = catalog.getPosts();
        if (posts != null) {
            FourChanCommentParser.Builder parserBuilder = new FourChanCommentParser.Builder();
            parserBuilder.setContext(getActivity())
                    .setBoardName(boardName)
                    .setQuoteColor(MimiUtil.getInstance().getQuoteColor())
                    .setReplyColor(MimiUtil.getInstance().getReplyColor())
                    .setHighlightColor(MimiUtil.getInstance().getHighlightColor())
                    .setLinkColor(MimiUtil.getInstance().getLinkColor());
            for (int i = 0; i < posts.size(); i++) {

                if (posts.get(i).getCom() != null) {
                    final List<Integer> userPosts = ThreadRegistry.getInstance().getUserPosts(boardName, posts.get(i).getResto());
                    parserBuilder.setComment(posts.get(i).getCom())
                            .setThreadId(posts.get(i).getResto())
                            .setReplies(posts.get(i).getRepliesTo())
                            .setUserPostIds(userPosts);

                    posts.get(i).setComment(parserBuilder.build().parse());
                }

                if (posts.get(i).getName() != null) {
                    final Spannable nameSpan = ChanUtil.getUserName(getActivity(), posts.get(i).getName(), posts.get(i).getCapcode());
                    posts.get(i)
                            .setDisplayedName(nameSpan);
                }

                if (posts.get(i).getSub() != null) {
                    posts.get(i).setSubject(Html.fromHtml(posts.get(i).getSub()));
                }
            }
        }

        catalog.setPosts(posts);
        return catalog;
    }

    private void fetchPage(final int page) {

        if (getScrollListener(listView) != null) {
            getScrollListener(listView).lock();
        }

        if (page <= totalPages) {

            final Handler handler = new Handler();

            currentPage = page;

            RxUtil.safeUnsubscribe(pageSubscription);
            pageSubscription = chanConnector.fetchPage(getActivity(), page, boardName, boardTitle)
                    .map(new Func1<ChanCatalog, ChanCatalog>() {
                        @Override
                        public ChanCatalog call(ChanCatalog catalog) {
                            if (catalog != null) {
                                return processCatalog(catalog);
                            }

                            return null;
                        }
                    })
                    .compose(DatabaseUtils.<ChanCatalog>applySchedulers())
                    .subscribe(new Action1<ChanCatalog>() {
                        @Override
                        public void call(ChanCatalog catalog) {

                            if (catalog != null) {
                                pagedResponse(catalog);
                                Log.e(LOG_TAG, "Loading page: number=" + page);
                            }

                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (getScrollListener(listView) != null) {
                                        getScrollListener(listView).unlock();
                                    }
                                }
                            }, 100);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Log.e(LOG_TAG, "Error fetching page: number=" + page, throwable);
                            if (getScrollListener(listView) != null) {
                                getScrollListener(listView).unlock();
                            }
                            showError(getString(R.string.error_loading_board));

                            if (getUserVisibleHint()) {
                                BusProvider.getInstance().post(new FABVisibilityEvent(false));
                            }
                        }
                    });
        } else {
            Log.i(LOG_TAG, "Reached the last page: " + totalPages);
            showContent();

            if (getScrollListener(listView) != null) {
                getScrollListener(listView).unlock();
            }
        }

    }

    private void fetchCatalog() {
        RxUtil.safeUnsubscribe(catalogSubscription);
        catalogSubscription = chanConnector.fetchCatalog(getActivity(), boardName, boardTitle)
                .flatMap(sortPosts())
                .map(new Func1<ChanCatalog, ChanCatalog>() {
                    @Override
                    public ChanCatalog call(ChanCatalog catalog) {
                        if (catalog != null) {
                            return processCatalog(catalog);
                        }

                        return null;
                    }
                })
                .compose(DatabaseUtils.<ChanCatalog>applySchedulers())
                .subscribe(new Action1<ChanCatalog>() {
                    @Override
                    public void call(ChanCatalog catalog) {
                        if (catalog != null) {
                            catalogResponse(catalog);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        showError(getString(R.string.error_loading_board));
                        Log.e(LOG_TAG, "Error fetching catalog", throwable);
                    }
                });
    }

    private Func1<ChanCatalog, Observable<ChanCatalog>> sortPosts() {
        return new Func1<ChanCatalog, Observable<ChanCatalog>>() {
            @Override
            public Observable<ChanCatalog> call(ChanCatalog chanCatalog) {
                if (chanCatalog != null) {
                    List<ChanPost> posts = chanCatalog.getPosts();
                    chanCatalog.setPosts(sortPosts(posts, sortType, invertSort));
                }

                return Observable.just(chanCatalog);
            }
        };
    }

    private List<ChanPost> sortPosts(List<ChanPost> posts, int type, boolean invert) {
        if (posts == null || posts.size() == 0) {
            return Collections.emptyList();
        }

        final List<ChanPost> updatedPostList = new ArrayList<>(posts);
        if (type == SORT_TYPE_THREAD_ID) {
            Collections.sort(updatedPostList, new ChanPost.ThreadIdComparator());
        } else if (type == SORT_TYPE_IMAGE_COUNT) {
            Collections.sort(updatedPostList, new ChanPost.ImageCountComparator());
        } else if (type == SORT_TYPE_REPLY_COUNT) {
            Collections.sort(updatedPostList, new ChanPost.ReplyCountComparator());
        }

        if (invert) {
            return Lists.reverse(updatedPostList);
        }

        return updatedPostList;

    }

    private void showLoadingLayout() {
        listRefreshLayout.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
        if (loadingLayout != null) {
            loadingLayout.setVisibility(View.VISIBLE);
        }

    }

    private void showContent() {
        if (getActivity() == null) {
            return;
        }
        listRefreshLayout.setRefreshing(false);
        listRefreshLayout.setVisibility(View.VISIBLE);

        if (errorContainer != null) {
            errorContainer.setVisibility(View.GONE);
        }

        if (loadingLayout != null) {
            loadingLayout.setVisibility(View.GONE);
        }

    }

    private void showError(final String msg) {
        if (loadingLayout != null) {
            loadingLayout.setVisibility(View.GONE);
        }

        if (errorContainer != null) {
            errorText.setText(msg);
            errorContainer.setVisibility(View.VISIBLE);
        }

        listRefreshLayout.setRefreshing(false);
        listRefreshLayout.setVisibility(View.GONE);
    }

    @Override
    public void initMenu() {
        super.initMenu();

        setupActionBar(true);
    }

    private void setupActionBar(final boolean fullSetup) {
        if (getActivity() != null && ((MimiActivity) getActivity()).getToolbar() != null) {
            toolbar = ((MimiActivity) getActivity()).getToolbar();
            toolbar.getMenu().clear();
            toolbar.setSubtitle(null);
            toolbar.inflateMenu(R.menu.post_list);
            toolbar.setOnMenuItemClickListener(this);

            if (toolbarSpinner == null) {
                toolbarSpinner = (Spinner) toolbar.findViewById(R.id.board_spinner);
                toolbarSpinner.setOnItemSelectedListener(this);

                if (boards != null) {
                    int boardPos = -1;
                    for (int i = 0; i < boards.size(); i++) {
                        if (boards.get(i).getName().equals(boardName)) {
                            boardPos = i;
                        }
                    }

                    isFirstRun = true;
                    toolbarSpinner.setSelection(boardPos);
                }
            }

            if (toolbarSpinner != null && MimiUtil.getLayoutType(getActivity()) != LayoutType.TABBED) {
                toolbar.setTitle(null);
                toolbarSpinner.setVisibility(View.VISIBLE);
            } else {
                toolbar.setTitle(boardTitle);

                if (toolbarSpinner != null) {
                    toolbarSpinner.setVisibility(View.GONE);
                }
            }

            toolbar.setSubtitle(null);

            final MenuItem searchItem = toolbar.getMenu().findItem(R.id.search_menu);
            setupSearchMenu(searchItem);

            final MenuItem catalogItem = toolbar.getMenu().findItem(R.id.catalog_menu);
            final MenuItem listItem = toolbar.getMenu().findItem(R.id.list_menu);
            setupListType(catalogItem, listItem);

            final MenuItem invertSortItem = toolbar.getMenu().findItem(R.id.invert_sort_menu);
            invertSortItem.setChecked(invertSort);
            invertSortItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (getActivity() == null) {
                        return false;
                    }

                    invertSort = !invertSort;
                    invertSortItem.setChecked(invertSort);

                    PreferenceManager.getDefaultSharedPreferences(getActivity())
                            .edit()
                            .putBoolean(INVERT_SORT_PREF_STRING, invertSort)
                            .apply();

                    if (postItemsAdapter != null) {
                        postList = new ArrayList<>(Lists.reverse(postItemsAdapter.getPosts()));
                        postItemsAdapter.setPosts(postList, boardName, boardTitle);
                        listView.smoothScrollToPosition(0);
                    }

                    return true;
                }
            });

            final MenuItem sortMenu = toolbar.getMenu().findItem(R.id.sort_menu);
            final MenuItem[] sortMenuItems = new MenuItem[4];
            sortMenuItems[SORT_TYPE_DEFAULT] = toolbar.getMenu().findItem(R.id.sort_default);
            sortMenuItems[SORT_TYPE_THREAD_ID] = toolbar.getMenu().findItem(R.id.sort_time);
            sortMenuItems[SORT_TYPE_IMAGE_COUNT] = toolbar.getMenu().findItem(R.id.sort_image_count);
            sortMenuItems[SORT_TYPE_REPLY_COUNT] = toolbar.getMenu().findItem(R.id.sort_reply_count);

            if (useCatalog) {
                setupSortType(sortMenuItems);
            } else {
                sortMenu.setVisible(false);
                invertSortItem.setVisible(false);
            }

            if (fullSetup) {
                if (boards == null) {
                    final boolean showAllBoards = PreferenceManager
                            .getDefaultSharedPreferences(getActivity())
                            .getBoolean(getString(R.string.show_all_boards), false);
                    final int boardOrder = MimiUtil.getBoardOrder(getActivity());

                    RxUtil.safeUnsubscribe(fetchBoardsSubscription);
                    fetchBoardsSubscription = BoardTableConnection.fetchBoards(boardOrder, showAllBoards)
                            .flatMap(new Func1<List<Board>, Observable<List<ChanBoard>>>() {
                                @Override
                                public Observable<List<ChanBoard>> call(List<Board> boards) {
                                    return Observable.just(BoardTableConnection.convertBoardDbModelsToChanBoards(boards));
                                }
                            })
                            .compose(DatabaseUtils.<List<ChanBoard>>applySchedulers())
                            .subscribe(new Action1<List<ChanBoard>>() {
                                @Override
                                public void call(List<ChanBoard> chanBoards) {
                                    boards = chanBoards;
                                    initSpinner();
                                }
                            });
                } else {
                    initSpinner();
                }
            }
        }
    }

    private void setupSortType(final MenuItem[] sortItems) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sortType = preferences.getInt(SORT_PREF_STRING, SORT_TYPE_DEFAULT);

        sortItems[sortType].setChecked(true);
        for (int i = 0; i < sortItems.length; i++) {
            final int type = i;
            sortItems[i].setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (getActivity() != null && type != sortType) {
                        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        sortType = type;

                        preferences.edit().putInt(SORT_PREF_STRING, sortType).apply();
                        item.setChecked(true);

                        if (sortType != SORT_TYPE_DEFAULT) {
                            if (postItemsAdapter != null) {
                                postItemsAdapter.setPosts(sortPosts(postItemsAdapter.getPosts(), sortType, invertSort), boardName, boardTitle);
                                listView.smoothScrollToPosition(0);
                            }
                        } else {
                            showLoadingLayout();
                            fetchCatalog();
                        }
                    }
                    return true;
                }
            });
        }
    }

    private void initSpinner() {
        if (toolbarSpinner.getAdapter() == null) {
            int boardPos = -1;
            for (int i = 0; i < boards.size(); i++) {
                if (boards.get(i).getName().equals(boardName)) {
                    boardPos = i;
                }
            }

            Log.d(LOG_TAG, "Setting up toolbar spinner");

            final BoardDropDownAdapter adapter = new BoardDropDownAdapter(getActivity(), R.layout.board_spinner_item, boards, BoardDropDownAdapter.MODE_ACTIONBAR);
            toolbarSpinner.setAdapter(adapter);

            if (boardPos != -1) {
                toolbarSpinner.setSelection(boardPos);
            }
        }
    }

    private void setupSearchMenu(MenuItem searchItem) {
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String s) {
                    if (TextUtils.isEmpty(s)) {
                        isSearching = false;
                        listRefreshLayout.setEnabled(true);

                    } else {
                        isSearching = true;
                        listRefreshLayout.setEnabled(false);
                    }

                    if (postItemsAdapter != null) {
                        postItemsAdapter.getFilter().filter(s);
                    }

                    return true;
                }

                @Override
                public boolean onQueryTextChange(String s) {
                    if (TextUtils.isEmpty(s)) {
                        isSearching = false;
                        listRefreshLayout.setEnabled(true);
                    } else {
                        isSearching = true;
                        listRefreshLayout.setEnabled(false);
                    }

                    if (postItemsAdapter != null) {
                        postItemsAdapter.getFilter().filter(s);
                    }

                    return true;
                }
            });
        }
    }

    private void setupListType(MenuItem catalogItem, MenuItem listItem) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final boolean isGrid = preferences.getBoolean(CATALOG_PREF_STRING, false);

        if (isGrid) {
            catalogItem.setChecked(true);
        } else {
            listItem.setChecked(true);
        }

        catalogItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (!item.isChecked()) {
                    item.setChecked(true);
                    switchToGrid();
                }
                return true;
            }
        });

        listItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (!item.isChecked()) {
                    item.setChecked(true);
                    switchToList();
                }
                return true;
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof BoardItemClickListener) {
            boardItemClickListener = (BoardItemClickListener) context;
        } else {
            throw new IllegalStateException("Activity must inherit BoardItemClickListener");
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        RxUtil.safeUnsubscribe(catalogSubscription);
        RxUtil.safeUnsubscribe(pageSubscription);
        RxUtil.safeUnsubscribe(boardInfoSubscription);
        RxUtil.safeUnsubscribe(pagedResponseSubscription);
        RxUtil.safeUnsubscribe(fetchBoardsSubscription);
        RxUtil.safeUnsubscribe(fetchHistorySubscription);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }

        if (listView != null) {
            outState.putInt(Extras.EXTRAS_POSITION, listView.getScrollY());
        }
        outState.putString(Extras.EXTRAS_BOARD_NAME, boardName);
        outState.putString(Extras.EXTRAS_BOARD_TITLE, boardTitle);
        outState.putInt(Extras.EXTRAS_PAGE, currentPage);
        outState.putParcelableArrayList(Extras.EXTRAS_POST_LIST, postList);
        outState.putBoolean(Extras.EXTRAS_CATALOG, useCatalog);

        outState.putBoolean("rotated", true);
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(final boolean activateOnItemClick) {
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.

        this.activateOnItemClick = activateOnItemClick;

    }

    private void showPostForm() {
        listView.smoothScrollBy(0, 0);

        final FragmentManager fm = getChildFragmentManager();
        postFragment = (PostFragment) fm.findFragmentByTag(POST_FRAGMENT_TAG);
        if (postFragment == null) {
            final Bundle args = new Bundle();
            args.putString(Extras.EXTRAS_BOARD_NAME, boardName);
            args.putString(Extras.EXTRAS_BOARD_TITLE, boardTitle);
            args.putBoolean(Extras.EXTRAS_POST_NEW, true);

            postFragment = new PostFragment();
            postFragment.setPostListener(createPostCompleteListener());
            postFragment.setArguments(args);
        }

        postFragment.show(fm, POST_FRAGMENT_TAG);
    }

    private PostFragment.PostListener createPostCompleteListener() {
        return new PostFragment.PostListener() {
            @Override
            public void onStartPost() {
                showPostProgress();
                if (postFragment != null) {
                    postFragment.dismiss();
                }
            }

            @Override
            public void onSuccess(final String postId) {
                Log.i(LOG_TAG, "Post ID: " + postId);
                showPostStatus(getString(R.string.success));

                final int id = Integer.valueOf(postId);

//                RefreshScheduler.getInstance().addThread(boardName, id);
                ThreadRegistry.getInstance().add(boardName, id, 1, true);

                ThreadRegistry.getInstance().addUserPost(boardName, id, id);
                UserPostTableConnection.addPost(boardName, id, id)
                        .compose(DatabaseUtils.<Boolean>applySchedulers())
                        .subscribe(new Action1<Boolean>() {
                            @Override
                            public void call(Boolean success) {
                                if (success) {
                                    Log.d(LOG_TAG, "Added post to database: board=" + boardName + ", thread=" + id + ", post=" + postId);
                                } else {
                                    Log.e(LOG_TAG, "Error Adding post to database: board=" + boardName + ", thread=" + id + ", post=" + postId);
                                }
                            }
                        });

                postFragment = null;

                final Handler handler = new Handler();
                final Runnable statusRunnable = new Runnable() {
                    @Override
                    public void run() {
                        showReplyText();
                    }
                };

                handler.postDelayed(statusRunnable, 200);

            }

            @Override
            public void onError(Throwable error) {
                error.printStackTrace();
                showPostStatus(error.getLocalizedMessage());
            }

            @Override
            public void onCanceled() {
                final FragmentTransaction fm = getChildFragmentManager().beginTransaction();
                if (postFragment != null) {
                    postFragment.dismiss();
                    fm.remove(postFragment).commit();
                    postFragment = null;
                }
            }
        };
    }

    private void showPostProgress() {
        Snackbar.make(listRefreshLayout, R.string.sending, Snackbar.LENGTH_SHORT).show();
    }

    private void showPostStatus(final String status) {
        Snackbar.make(listRefreshLayout, status, Snackbar.LENGTH_SHORT).show();

    }

    public void showReplyText() {

    }

    private void setActivatedPosition(int position) {
        mActivatedPosition = position;
    }

    public void setBoards(final List<ChanBoard> boards) {
        this.boards = boards;
    }

    public void setOnBoardItemClickListener(final BoardItemClickListener listener) {
        boardItemClickListener = listener;
    }

    @Override
    public boolean onBackPressed() {
        if (toolbar != null && toolbar.hasExpandedActionView()) {
            toolbar.collapseActionView();

            return true;
        }

        return false;
    }

    @Override
    public int getTabId() {
        return TAB_ID;
    }

    @Override
    public String getTitle() {
        return boardTitle;
    }

    @Override
    public String getSubtitle() {
        return null;
    }

    @Override
    public String getPageName() {
        return "post_list";
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        ChanBoard b = boards.get(position);
        boardName = b.getName();
        boardTitle = b.getTitle();

        refreshBoard(true);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.refresh_menu:
                refreshBoard(true);
                return true;

        }
        return false;
    }

    @Override
    public void addContent() {
        showPostForm();
    }

    public interface CatalogItemClickListener {
        void onPostItemClick(final List<ChanPost> Posts, final int position, final String boardName, final String boardTitle);
    }
}
