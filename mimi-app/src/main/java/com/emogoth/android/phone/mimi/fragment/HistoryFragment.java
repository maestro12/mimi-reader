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
import android.os.Bundle;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.activity.MimiActivity;
import com.emogoth.android.phone.mimi.activity.PostItemDetailActivity;
import com.emogoth.android.phone.mimi.activity.PostItemListActivity;
import com.emogoth.android.phone.mimi.adapter.HistoryAdapter;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;
import com.emogoth.android.phone.mimi.db.HistoryTableConnection;
import com.emogoth.android.phone.mimi.db.model.History;
import com.emogoth.android.phone.mimi.event.UpdateHistoryEvent;
import com.emogoth.android.phone.mimi.fourchan.FourChanCommentParser;
import com.emogoth.android.phone.mimi.interfaces.IToolbarContainer;
import com.emogoth.android.phone.mimi.model.ThreadInfo;
import com.emogoth.android.phone.mimi.util.BusProvider;
import com.emogoth.android.phone.mimi.util.Extras;
import com.emogoth.android.phone.mimi.util.MimiPrefs;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.emogoth.android.phone.mimi.widget.MimiRecyclerView;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

public class HistoryFragment extends MimiFragmentBase {
    private static final String LOG_TAG = HistoryFragment.class.getSimpleName();

    private MimiRecyclerView historyRecyclerView;
    private HistoryAdapter historyAdapter;
    private ViewGroup emptyListMessageContainer;
    private TextView emptyListMessage;
    private ListView historylist;
    private TextView closeButton;
    private TextView clearAllButton;
    private LinearLayout clearAllContainer;
    private TextView emptylisttext;
    private FrameLayout emptylist;
    private int historyQuery = 0;
    private List<History> postList;
    private int viewingHistory = 0;

    private boolean isBookmarks = false;

    private Disposable fetchHistorySubscription;
    private Toolbar toolbar;
    private ItemTouchHelper recyclerViewTouchHelper;
    private ItemTouchHelper.Callback touchHelperCallback;
    private RecyclerView.LayoutManager layoutManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(false);
    }

    public static HistoryFragment newInstance(boolean watched) {
        final Bundle args = new Bundle();

        if (watched) {
            args.putInt(Extras.EXTRAS_HISTORY_QUERY_TYPE, HistoryTableConnection.BOOKMARKS);
            args.putInt(Extras.EXTRAS_VIEWING_HISTORY, MimiActivity.VIEWING_BOOKMARKS);
        } else {
            args.putInt(Extras.EXTRAS_HISTORY_QUERY_TYPE, HistoryTableConnection.HISTORY);
            args.putInt(Extras.EXTRAS_VIEWING_HISTORY, MimiActivity.VIEWING_HISTORY);
        }

        HistoryFragment fragment = new HistoryFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_history_list, container, false);

        if (getUserVisibleHint()) {
            initMenu();
        }

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Bundle args = getArguments();
        if (args != null) {
            if (args.containsKey(Extras.EXTRAS_VIEWING_HISTORY)) {
                viewingHistory = args.getInt(Extras.EXTRAS_VIEWING_HISTORY);
            }
            if (args.containsKey(Extras.EXTRAS_HISTORY_QUERY_TYPE)) {
                historyQuery = args.getInt(Extras.EXTRAS_HISTORY_QUERY_TYPE);
            }

            if (viewingHistory == MimiActivity.VIEWING_BOOKMARKS) {
                isBookmarks = true;
            }
        }

        toolbar = ((MimiActivity) getActivity()).getToolbar();

        if (getActivity() instanceof MimiActivity) {
            final MimiActivity activity = (MimiActivity) getActivity();
//            activity.getToolbar().setVisibility(View.VISIBLE);
            activity.getSupportActionBar().setTitle(getTitle());
            activity.getSupportActionBar().setSubtitle(getSubtitle());
        }

        Log.d(LOG_TAG, "Set toolbar title to " + getTitle());
        Log.d(LOG_TAG, "Set toolbar subtitle to " + getSubtitle());

        clearAllContainer = view.findViewById(R.id.clear_all_container);

        historyRecyclerView = view.findViewById(R.id.history_list);
        historyRecyclerView.setItemAnimator(new DefaultItemAnimator());
        layoutManager = new LinearLayoutManager(getActivity());
        historyAdapter = new HistoryAdapter();
        historyAdapter.setHasStableIds(true);

        touchHelperCallback = createTouchHelperCallback();
        recyclerViewTouchHelper = new ItemTouchHelper(createTouchHelperCallback());
        recyclerViewTouchHelper.attachToRecyclerView(historyRecyclerView);
        historyAdapter.setItemClickListener(new HistoryAdapter.HistoryItemClickListener() {
            @Override
            public void onItemLongClick(View v, int position) {
                if (historyAdapter != null) {
                    if (historyAdapter.isEditMode()) {
                        historyAdapter.setEditMode(false);
                        clearAllContainer.setVisibility(View.GONE);
                    } else {
                        historyAdapter.setEditMode(true);
                        clearAllContainer.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onItemClick(View v, int position) {
                if (!historyAdapter.isEditMode()) {
                    openThreadInActivity(position);
                }
            }

            @Override
            public void onItemStartDrag(HistoryAdapter.ViewHolder holder) {
                recyclerViewTouchHelper.startDrag(holder);
            }
        });
        historyRecyclerView.setLayoutManager(layoutManager);
        historyRecyclerView.setAdapter(historyAdapter);

        emptyListMessageContainer = view.findViewById(R.id.empty_list);
        emptyListMessage = view.findViewById(R.id.empty_list_text);

        clearAllButton = view.findViewById(R.id.clear_all_button);
        clearAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (historyAdapter != null) {
                    historyAdapter.setEditMode(false);
                    historyAdapter.clearHistory();

                    historyRecyclerView.setVisibility(View.GONE);
                    emptyListMessageContainer.setVisibility(View.VISIBLE);
                }
            }
        });

        closeButton = view.findViewById(R.id.remove_history_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (historyAdapter != null) {
                    historyAdapter.setEditMode(false);
                }

                if (clearAllContainer != null) {
                    clearAllContainer.setVisibility(View.GONE);
                }
            }
        });

        final int loaderId = MimiUtil.getInstance().getLoaderId(((Object) this).getClass());
        Log.i(LOG_TAG, "Loading history using loader id=" + loaderId);

        if (historyQuery == HistoryTableConnection.BOOKMARKS) {
            emptyListMessage.setText(R.string.no_bookmarks_message);
        } else {
            emptyListMessage.setText(R.string.no_history_message);
        }

        loadHistory(isBookmarks);
    }

    private ItemTouchHelper.Callback createTouchHelperCallback() {
        return new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT) {
            @Override
            public boolean isLongPressDragEnabled() {
                return true;
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return false;
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder from, RecyclerView.ViewHolder to) {
                HistoryAdapter adapter;
                if (recyclerView.getAdapter() instanceof HistoryAdapter) {
                    adapter = (HistoryAdapter) recyclerView.getAdapter();
                    adapter.swapItems(from.getAdapterPosition(), to.getAdapterPosition());
                    return true;
                }

                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {

            }
        };
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.history_menu, menu);
    }

    @Override
    public void initMenu() {
        super.initMenu();

        MimiActivity activity = null;
        if (getActivity() instanceof MimiActivity) {
            activity = (MimiActivity) getActivity();
            toolbar = activity.getToolbar();
        }

        if (toolbar != null) {

            Spinner toolbarSpinner = toolbar.findViewById(R.id.board_spinner);
            if (toolbarSpinner != null) {
                toolbarSpinner.setVisibility(View.GONE);
            }

            activity.getSupportActionBar().setTitle(getTitle());
            activity.getSupportActionBar().setSubtitle(null);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            if (getActivity() != null) {
                toolbar = ((MimiActivity) getActivity()).getToolbar();
            }

            loadHistory(isBookmarks);

            if (getActivity() instanceof IToolbarContainer) {
                IToolbarContainer activity = (IToolbarContainer) getActivity();
                activity.setExpandedToolbar(true, true);
            }
        } else {
            if (historyAdapter != null && historyAdapter.isEditMode()) {
                historyAdapter.setEditMode(false);
                clearAllContainer.setVisibility(View.GONE);
            }
        }

    }

    private void loadHistory(boolean watched) {
        RxUtil.safeUnsubscribe(fetchHistorySubscription);
        fetchHistorySubscription = HistoryTableConnection.fetchHistory(watched)
                .flatMap((Function<List<History>, Flowable<List<History>>>) histories -> {
                    FourChanCommentParser.Builder builder = new FourChanCommentParser.Builder();
                    builder.setContext(getActivity())
                            .setQuoteColor(MimiUtil.getInstance().getQuoteColor())
                            .setReplyColor(MimiUtil.getInstance().getReplyColor())
                            .setHighlightColor(MimiUtil.getInstance().getHighlightColor())
                            .setLinkColor(MimiUtil.getInstance().getLinkColor())
                            .setEnableEmoji(MimiPrefs.isEmojiEnabled());

                    for (History history : histories) {
                        history.comment = builder.setComment(history.text).build().parse();
                    }

                    return Flowable.just(histories);
                })
                .compose(DatabaseUtils.applySchedulers())
                .subscribe(historyList -> {
                    if (historyList != null && getActivity() != null) {
                        postList = historyList;
                        if (historyList.size() > 0) {
                            if (historyAdapter != null) {
                                historyAdapter.setHistory(historyList);
                            }

                            historyRecyclerView.setVisibility(View.VISIBLE);
                            emptyListMessageContainer.setVisibility(View.GONE);
                        } else {
                            historyRecyclerView.setVisibility(View.GONE);
                            emptyListMessageContainer.setVisibility(View.VISIBLE);
                        }
                    } else {
                        Log.i(LOG_TAG, "Cursor is null!");
                    }
                });
    }

    private void openThreadInActivity(final int position) {

        if (postList == null || position >= postList.size()) {
            return;
        }

        if (getActivity() instanceof PostItemListActivity) {
            final Bundle args = new Bundle();
            final Class clazz;
            if (historyQuery == HistoryTableConnection.BOOKMARKS) {
                args.putBoolean(Extras.EXTRAS_USE_BOOKMARKS, true);
                args.putInt(Extras.EXTRAS_VIEWING_HISTORY, MimiActivity.VIEWING_BOOKMARKS);
            }

            if (historyQuery == HistoryTableConnection.HISTORY) {
                args.putBoolean(Extras.EXTRAS_USE_HISTORY, true);
                args.putInt(Extras.EXTRAS_VIEWING_HISTORY, MimiActivity.VIEWING_HISTORY);
            }

            args.putString(Extras.EXTRAS_BOARD_NAME, postList.get(position).boardName);
            args.putLong(Extras.EXTRAS_THREAD_ID, postList.get(position).threadId);
            args.putInt(Extras.EXTRAS_POSITION, position);
            args.putIntArray(Extras.EXTRAS_UNREAD_COUNT, historyAdapter.getUnreadCountList());

            final ArrayList<ThreadInfo> threadList = new ArrayList<ThreadInfo>(postList.size());

            for (final History post : postList) {
                final ThreadInfo threadInfo = new ThreadInfo(post.threadId, post.boardName, null, post.watched == 1);
                threadList.add(threadInfo);
            }

            args.putParcelableArrayList(Extras.EXTRAS_THREAD_LIST, threadList);

            if (getResources().getBoolean(R.bool.two_pane)) {
                clazz = PostItemListActivity.class;
            } else {
                clazz = PostItemDetailActivity.class;
            }

            final Intent intent = new Intent(getActivity(), clazz);
            intent.putExtras(args);
            startActivity(intent);
        } else if (getActivity() instanceof MimiActivity) {
            MimiActivity activity = (MimiActivity) getActivity();
            History history = postList.get(position);
            activity.onPostItemClick(null, null, position, null, history.boardName, history.threadId);
        }

//        if(viewingHistory > 0 && getActivity() != null) {
//            getActivity().finish();
//        }

    }

    @Override
    public void onPause() {
        super.onPause();

        BusProvider.getInstance().unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        BusProvider.getInstance().register(this);

        if (historyAdapter != null) {
            historyAdapter.notifyDataSetChanged();
        }
    }

    @Subscribe
    public void onAutoRefresh(final UpdateHistoryEvent event) {
//        ThreadRegistry.getInstance().update(event.getThread().getThreadId(), event.getThread().getPosts().size());
//        setBookmarkCount(ThreadRegistry.getInstance().getUnreadCount());

//        if(historyAdapter != null) {
//            Log.i(LOG_TAG, "Refreshing history list");
//            historyAdapter.notifyDataSetChanged();
//        }

        loadHistory(isBookmarks);
    }

    @Override
    public boolean showFab() {
        return false;
    }

    @Override
    public String getTitle() {
        if (viewingHistory == MimiActivity.VIEWING_HISTORY) {
            return getString(R.string.history_title);
        } else if (viewingHistory == MimiActivity.VIEWING_BOOKMARKS) {
            return getString(R.string.bookmarks);
        }

        return null;
    }

    @Override
    public String getSubtitle() {
        return null;
    }

    @Override
    public String getPageName() {
        if (viewingHistory == MimiActivity.VIEWING_HISTORY) {
            return "history_tab";
        } else if (viewingHistory == MimiActivity.VIEWING_BOOKMARKS) {
            return "bookmarks_tab";
        }

        return "history_tab";
    }
}
