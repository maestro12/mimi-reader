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

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.activity.MimiActivity;
import com.emogoth.android.phone.mimi.activity.StartupActivity;
import com.emogoth.android.phone.mimi.db.BoardTableConnection;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;
import com.emogoth.android.phone.mimi.db.HistoryTableConnection;
import com.emogoth.android.phone.mimi.db.model.History;
import com.emogoth.android.phone.mimi.event.BookmarkClickedEvent;
import com.emogoth.android.phone.mimi.event.CloseTabEvent;
import com.emogoth.android.phone.mimi.event.HomeButtonPressedEvent;
import com.emogoth.android.phone.mimi.event.OpenHistoryEvent;
import com.emogoth.android.phone.mimi.event.UpdateHistoryEvent;
import com.emogoth.android.phone.mimi.fourchan.FourChanCommentParser;
import com.emogoth.android.phone.mimi.prefs.MimiSettings;
import com.emogoth.android.phone.mimi.util.BusProvider;
import com.emogoth.android.phone.mimi.util.LayoutType;
import com.emogoth.android.phone.mimi.util.MimiPrefs;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.emogoth.android.phone.mimi.util.ThreadRegistry;
import com.emogoth.android.phone.mimi.view.DrawerViewHolder;
import com.mimireader.chanlib.models.ChanBoard;
import com.squareup.otto.Subscribe;

import java.util.List;

import rx.Subscription;
import rx.functions.Action1;


public class NavDrawerFragment extends Fragment {
    private static final String LOG_TAG = NavDrawerFragment.class.getSimpleName();

    private SharedPreferences sharedPreferences;

    private TextView loginRow;
    private int themeId;
    private int themeColorId;
    private FrameLayout notificationContainer;

    private MimiActivity activity;
    private View noBookmarksContainer;
    private ViewGroup bookmarksItemContainer;
    private List<History> currentBookmarks;
    private int fontSizeId;
    private String layoutType;

    private Subscription boardInfoSubscription;
    private Subscription bookmarkSubscription;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(false);

        activity = (MimiActivity) getActivity();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        themeId = Integer.valueOf(sharedPreferences.getString(getString(R.string.theme_pref), "0"));
        themeColorId = Integer.valueOf(sharedPreferences.getString(getString(R.string.theme_color_pref), "0"));
        fontSizeId = Integer.valueOf(sharedPreferences.getString(getString(R.string.font_style_pref), "0"));
        layoutType = sharedPreferences.getString(getString(R.string.start_activity_pref), StartupActivity.getDefaultStartupActivity());

        final View v = inflater.inflate(R.layout.fragment_nav_drawer, container, false);
        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final View settingsRow = view.findViewById(R.id.settings_row);
        settingsRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(getActivity(), MimiSettings.class), MimiActivity.SETTINGS_ID);
                toggleDrawer();
            }

        });

        notificationContainer = (FrameLayout) view.findViewById(R.id.notification_container);
        notificationContainer.addView(MimiUtil.getInstance().createActionBarNotification(getActivity().getLayoutInflater(), notificationContainer, ThreadRegistry.getInstance().getUnreadCount()));

        final View homeRow = view.findViewById(R.id.home_row);
        homeRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BusProvider.getInstance().post(new HomeButtonPressedEvent());
                toggleDrawer();
            }
        });

        final View closeTabsContainer = view.findViewById(R.id.close_tabs_container);
        if (MimiUtil.getLayoutType(getActivity()) != LayoutType.TABBED) {
            closeTabsContainer.setVisibility(View.GONE);
        } else {
            final View closeTabsRow = view.findViewById(R.id.close_tabs_row);
            closeTabsRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closeAllTabs();
                }
            });
        }

        final View bookmarksRow = view.findViewById(R.id.bookmarks_row);
        bookmarksRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleDrawer();
                BusProvider.getInstance().post(new OpenHistoryEvent(true));
            }
        });

        bookmarksItemContainer = (ViewGroup) view.findViewById(R.id.bookmark_items);
        noBookmarksContainer = view.findViewById(R.id.no_bookmarks);
        populateBookmarks();

        final View historyRow = view.findViewById(R.id.history_row);
        historyRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleDrawer();
                BusProvider.getInstance().post(new OpenHistoryEvent(false));
            }
        });
    }

    private void toggleDrawer() {
        if (getActivity() instanceof MimiActivity) {
            ((MimiActivity) getActivity()).toggleNavDrawer();
        }
    }

    private void closeAllTabs() {
        final LayoutInflater inflater = LayoutInflater.from(getActivity());
        final android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(getActivity());
        final View dialogView = inflater.inflate(R.layout.dialog_close_tabs_prompt, null, false);
        final CheckBox dontShow = (CheckBox) dialogView.findViewById(R.id.dialog_dont_show);
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        final boolean shouldShowDialog = pref.getBoolean(getString(R.string.close_all_tabs_prompt_pref), true);

        if (shouldShowDialog) {
            dialogBuilder.setTitle(R.string.close_all_tabs)
                    .setView(dialogView)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                            pref.edit().putBoolean(getString(R.string.close_all_tabs_prompt_pref), !dontShow.isChecked()).apply();

                            BusProvider.getInstance().post(new CloseTabEvent(true));
                            toggleDrawer();
                        }
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        } else {
            BusProvider.getInstance().post(new CloseTabEvent(true));
            toggleDrawer();
        }

    }

    public void populateBookmarks() {
        final int bookmarkCount = MimiPrefs.navDrawerBookmarkCount(getActivity());

//        HistoryTableConnection.fetchPost(12345)
//                .subscribe();
        RxUtil.safeUnsubscribe(bookmarkSubscription);
        bookmarkSubscription = HistoryTableConnection.fetchHistory(true, bookmarkCount)
                .subscribe(new Action1<List<History>>() {
                    @Override
                    public void call(List<History> bookmarks) {
                        currentBookmarks = bookmarks;

                        bookmarksItemContainer.removeAllViews();
                        bookmarksItemContainer.addView(noBookmarksContainer);

                        if (bookmarks != null && bookmarks.size() > 0) {
                            notificationContainer.setVisibility(View.VISIBLE);
                            noBookmarksContainer.setVisibility(View.GONE);
                            for (int i = 0; i < bookmarks.size(); i++) {
                                final History bookmark = bookmarks.get(i);
                                bookmark.orderId = i;

                                final View row = getActivity().getLayoutInflater().inflate(R.layout.bookmark_row_item, bookmarksItemContainer, false);
                                row.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        sendBookmarkClickedEvent(bookmark);
                                        toggleDrawer();
                                    }
                                });
                                bookmarksItemContainer.addView(createBookmarkRow(bookmark, row));
                            }
                        } else {
                            notificationContainer.setVisibility(View.INVISIBLE);
                            noBookmarksContainer.setVisibility(View.VISIBLE);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e(LOG_TAG, "Error loading bookmarks", throwable);
                    }
                });
    }

    private void sendBookmarkClickedEvent(final History bookmark) {
        RxUtil.safeUnsubscribe(boardInfoSubscription);
        boardInfoSubscription = BoardTableConnection.fetchBoard(bookmark.boardName)
                .compose(DatabaseUtils.<ChanBoard>applySchedulers())
                .subscribe(new Action1<ChanBoard>() {
                    @Override
                    public void call(ChanBoard chanBoard) {
                        if (chanBoard != null) {
                            BusProvider.getInstance().post(new BookmarkClickedEvent(bookmark.threadId, chanBoard.getName(), chanBoard.getTitle(), bookmark.orderId));
                        } else if (getActivity() != null) {
                            Toast.makeText(getActivity(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private View createBookmarkRow(final History bookmark, final View row) {

        final CharSequence time = DateUtils.getRelativeTimeSpanString(
                bookmark.lastAccess,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE);
        final DrawerViewHolder viewHolder = new DrawerViewHolder(row);
        final FourChanCommentParser.Builder parserBuilder = new FourChanCommentParser.Builder();
        parserBuilder.setContext(getActivity())
                .setComment(bookmark.text)
                .setBoardName(bookmark.boardName)
                .setThreadId(bookmark.threadId)
                .setQuoteColor(MimiUtil.getInstance().getQuoteColor())
                .setReplyColor(MimiUtil.getInstance().getReplyColor())
                .setHighlightColor(MimiUtil.getInstance().getHighlightColor())
                .setLinkColor(MimiUtil.getInstance().getLinkColor());

        viewHolder.text.setText(parserBuilder.build().parse());

        if (bookmark.watched) {
            final int count = ThreadRegistry.getInstance().getUnreadCount(bookmark.threadId);

            if (count > 0) {
                viewHolder.unreadcount.setText(String.valueOf(count));
                viewHolder.unreadcount.setVisibility(View.VISIBLE);
            } else {
                viewHolder.unreadcount.setVisibility(View.GONE);
            }
        } else {
            viewHolder.unreadcount.setVisibility(View.GONE);
        }

        viewHolder.image.setVisibility(View.INVISIBLE);
        if (!TextUtils.isEmpty(bookmark.tim)) {
            final String url = MimiUtil.httpOrHttps(activity) + activity.getString(R.string.thumb_link) + activity.getString(R.string.thumb_path, bookmark.boardName, bookmark.tim);

            Glide.with(activity)
                    .load(url)
                    .crossFade()
                    .error(R.drawable.placeholder_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(viewHolder.image);

            viewHolder.image.setVisibility(View.VISIBLE);

//            viewHolder.image.setDefaultImageResId(R.drawable.ic_content_picture);
//            viewHolder.image.setErrorImageResId(R.drawable.ic_content_picture);
//            viewHolder.image.setImageUrl(url, imageLoader);

        } else {
            viewHolder.image.setVisibility(View.INVISIBLE);
            Glide.clear(viewHolder.image);
        }

        viewHolder.boardName.setText("/" + bookmark.boardName + "/");
        viewHolder.threadId.setText(String.valueOf(bookmark.threadId));
        viewHolder.lastviewed.setText(time);

        return row;
    }

//    @Subscribe
//    public void openThreadInActivity(final HistoryDbModel historyItem) {
//        final Bundle args = new Bundle();
//        final Class clazz;
//        args.putBoolean(Extras.EXTRAS_USE_BOOKMARKS, true);
//        args.putInt(Extras.EXTRAS_VIEWING_HISTORY, MimiActivity.VIEWING_BOOKMARKS);
//
//        args.putString(Extras.EXTRAS_BOARD_NAME, historyItem.getBoardName());
//        args.putInt(Extras.EXTRAS_THREAD_ID, historyItem.getThreadId());
//        args.putInt(Extras.EXTRAS_POSITION, historyItem.getOrderId());
//
//        final List<HistoryDbModel> bookmarks = MimiUtil.getInstance().getBookmarks();
//        final ArrayList<ThreadInfo> threadList = new ArrayList<>(bookmarks.size());
//
//        for (final HistoryDbModel post : bookmarks) {
//            final ThreadInfo threadInfo = new ThreadInfo(post.getThreadId(), post.getBoardName(), null);
//            threadList.add(threadInfo);
//        }
//
//        args.putParcelableArrayList(Extras.EXTRAS_THREAD_LIST, threadList);
//
//        if (getResources().getBoolean(R.bool.two_pane)) {
//            clazz = PostItemListActivity.class;
//        } else {
//            clazz = PostItemDetailActivity.class;
//        }
//
//        toggleDrawer();
//
//        final Intent intent = new Intent(getActivity(), clazz);
//        intent.putExtras(args);
//        startActivity(intent);
//
//    }

    @Override
    public void onPause() {
        super.onPause();

        BusProvider.getInstance().unregister(this);
        RxUtil.safeUnsubscribe(boardInfoSubscription);
    }

    @Override
    public void onResume() {
        super.onResume();
        BusProvider.getInstance().register(this);

        final int themePref = Integer.valueOf(sharedPreferences.getString(getString(R.string.theme_pref), "0"));
        final int themeColorPref = Integer.valueOf(sharedPreferences.getString(getString(R.string.theme_color_pref), "0"));

        if (themeId != themePref || themeColorPref != themeColorId) {
            themeId = themePref;
            themeColorId = themeColorPref;

            MimiUtil.getInstance().setCurrentTheme(themePref, themeColorPref);
            Activity activity = getActivity();
            Intent intent = new Intent(activity, activity.getClass());
            activity.finish();
            startActivity(intent);

        }

        final int fontSizePref = Integer.valueOf(sharedPreferences.getString(getString(R.string.font_style_pref), "0"));
        if (fontSizeId != fontSizePref) {
            Activity activity = getActivity();
            Intent intent = new Intent(activity, activity.getClass());
            activity.finish();
            startActivity(intent);
        }

        final String layoutTypePref = sharedPreferences.getString(getString(R.string.start_activity_pref), StartupActivity.getDefaultStartupActivity());
        if (!TextUtils.equals(layoutType, layoutTypePref)) {
            Activity activity = getActivity();
            Intent intent = new Intent(activity, StartupActivity.class);
            activity.finish();
            startActivity(intent);
        }

        if (loginRow != null && MimiUtil.getInstance().isLoggedIn()) {
            loginRow.setText(R.string.chanpass_logout);
        }

    }

    @Subscribe
    public void onAutoRefresh(final UpdateHistoryEvent event) {
        populateBookmarks();

        notificationContainer.removeAllViews();
        notificationContainer.addView(MimiUtil.getInstance().createActionBarNotification(getActivity().getLayoutInflater(), notificationContainer, ThreadRegistry.getInstance().getUnreadCount()));
    }

}
