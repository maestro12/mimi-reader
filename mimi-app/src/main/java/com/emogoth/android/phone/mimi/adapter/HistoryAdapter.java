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

package com.emogoth.android.phone.mimi.adapter;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;
import com.emogoth.android.phone.mimi.db.HistoryTableConnection;
import com.emogoth.android.phone.mimi.db.model.History;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.RefreshScheduler;
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.emogoth.android.phone.mimi.util.ThreadRegistry;
import com.nhaarman.listviewanimations.util.Swappable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Subscription;
import rx.functions.Action1;


public class HistoryAdapter extends BaseAdapter implements Swappable {

    private static final String LOG_TAG = HistoryAdapter.class.getSimpleName();
    private final LayoutInflater inflater;
    private final ArrayList<History> historyList;
    private final FragmentActivity activity;

    private int[] unreadCountList;

    private final FragmentManager fm;

    private boolean isEditMode = false;

    private LinkedList<CharSequence> timeList;
    private Subscription updateHistoryOrderSubscription;
    private Subscription fetchPostSubscription;
    private Subscription removeHistorySubscription;

    public HistoryAdapter(final FragmentActivity activity, final FragmentManager fm, final List<History> data) {
        this.activity = activity;
        this.fm = fm;
        this.historyList = new ArrayList<>(data);
        this.inflater = LayoutInflater.from(activity);

        init();
    }

    private void init() {
        unreadCountList = new int[historyList.size()];
        for (int i = 0; i < historyList.size(); i++) {
            final int count = ThreadRegistry.getInstance().getUnreadCount(historyList.get(i).threadId);
            unreadCountList[i] = count;
        }

        buildTimeMap();
    }

    public void setHistory(final List<History> list) {
        historyList.clear();
        historyList.addAll(list);

        init();
    }

    public void addThread(final History thread) {
        historyList.add(thread);
        notifyDataSetChanged();
    }

    public void removeThread(int threadId) {
        for (History model : historyList) {
            if (model.threadId == threadId) {
                historyList.remove(model);
            }
        }

        notifyDataSetChanged();
    }

    private void buildTimeMap() {
        this.timeList = new LinkedList<>();
        for (int i = 0; i < historyList.size(); i++) {
            timeList.add(DateUtils.getRelativeTimeSpanString(
                    historyList.get(i).lastAccess,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
            ));
        }
    }

    public void setEditMode(final boolean enabled) {
        isEditMode = enabled;
        notifyDataSetChanged();
    }

    public boolean isEditMode() {
        return isEditMode;
    }

    public void clearHistory() {
        if (historyList.size() > 0) {

            RxUtil.safeUnsubscribe(fetchPostSubscription);
            fetchPostSubscription = HistoryTableConnection.fetchPost(historyList.get(0).boardName, historyList.get(0).threadId)
                    .subscribe(new Action1<History>() {
                        @Override
                        public void call(History history) {
                            if (history == null) {
                                return;
                            }

                            final boolean watched = history.watched;
                            for (int position = 0; position < historyList.size(); position++) {
                                final History historyItem = historyList.get(position);

                                RefreshScheduler.getInstance().removeThread(historyItem.boardName, historyItem.threadId);

                                historyList.remove(position);
                                timeList.remove(position);

                            }

                            ThreadRegistry.getInstance().clear();
                            HistoryTableConnection.removeAllHistory(watched).subscribe();

                            isEditMode = false;
                            notifyDataSetChanged();
                        }
                    });
        }
    }

    public int[] getUnreadCountList() {
        return unreadCountList;
    }

    @Override
    public boolean isEnabled(int position) {
        return !isEditMode;
    }

    @Override
    public int getCount() {
        return historyList.size();
    }

    @Override
    public History getItem(int position) {
        return historyList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return historyList.get(position).threadId;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        final History historyItem = historyList.get(position);
        final ViewHolder viewHolder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.history_item, parent, false);

            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.text.setText(historyList.get(position).comment);

        if (historyItem.watched) {
            final int count = ThreadRegistry.getInstance().getUnreadCount(historyItem.threadId);
            unreadCountList[position] = count;
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
        if (!TextUtils.isEmpty(historyItem.tim)) {
            final String url = MimiUtil.httpOrHttps(activity) + activity.getString(R.string.thumb_link) + activity.getString(R.string.thumb_path, historyItem.boardName, historyItem.tim);

            viewHolder.image.setVisibility(View.VISIBLE);

            Glide.with(activity)
                    .load(url)
                    .crossFade()
                    .error(R.drawable.placeholder_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(viewHolder.image);
        } else {
            viewHolder.image.setVisibility(View.INVISIBLE);
            Glide.clear(viewHolder.image);
        }

        viewHolder.threadinfo.setText("/" + historyItem.boardName + "/" + historyItem.threadId);
        viewHolder.opname.setText(historyItem.userName);
        viewHolder.lastviewed.setText(timeList.get(position));

        if (isEditMode) {
            viewHolder.deletehistory.setVisibility(View.VISIBLE);
        } else {
            viewHolder.deletehistory.setVisibility(View.GONE);
        }

        viewHolder.deletehistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {

                RxUtil.safeUnsubscribe(removeHistorySubscription);
                removeHistorySubscription = HistoryTableConnection.removeHistory(historyItem.boardName, historyItem.threadId)
                        .doOnNext(new Action1<Boolean>() {
                            @Override
                            public void call(Boolean aBoolean) {
                                if (historyItem.watched) {
                                    MimiUtil.getInstance().removeBookmark(historyItem.boardName, historyItem.threadId);
                                }
                            }
                        })
                        .compose(DatabaseUtils.<Boolean>applySchedulers())
                        .subscribe(new Action1<Boolean>() {
                            @Override
                            public void call(Boolean success) {
                                if (success) {
                                    Log.d(LOG_TAG, "Removed history: " + "board name=" + historyItem.boardName + ", thread id=" + historyItem.threadId);

                                    RefreshScheduler.getInstance().removeThread(historyItem.boardName, historyItem.threadId);
                                    ThreadRegistry.getInstance().remove(historyItem.threadId);
                                    historyList.remove(position);
                                    timeList.remove(position);
                                    if (historyList.size() == 0) {
                                        isEditMode = false;
                                    }
                                    notifyDataSetChanged();

                                    startUpdateTimer(historyList);
                                } else {
                                    Log.e(LOG_TAG, "Error removing history: " + "board name=" + historyItem.boardName + ", thread id=" + historyItem.threadId);
                                }
                            }
                        });
            }
        });

        return viewHolder.root;
    }

    @Override
    public void swapItems(int from, int to) {
        final History item = historyList.set(from, getItem(to));
        notifyDataSetChanged();

        historyList.set(to, item);
        startUpdateTimer(historyList);
    }

    private void startUpdateTimer(List<History> histories) {
        RxUtil.safeUnsubscribe(updateHistoryOrderSubscription);
        updateHistoryOrderSubscription = HistoryTableConnection.updateHistoryOrder(histories)
                .delay(500, TimeUnit.MILLISECONDS)
                .subscribe();
    }

    public static class ViewHolder {
        public final ImageView image;
        public final TextView threadinfo;
        public final TextView opname;
        public final TextView lastviewed;
        public final TextView text;
        public final TextView unreadcount;
        public final ImageView deletehistory;
        public final View root;

        public ViewHolder(View root) {
            image = (ImageView) root.findViewById(R.id.image);
            threadinfo = (TextView) root.findViewById(R.id.thread_info);
            opname = (TextView) root.findViewById(R.id.op_name);
            lastviewed = (TextView) root.findViewById(R.id.last_viewed);
            text = (TextView) root.findViewById(R.id.text);
            unreadcount = (TextView) root.findViewById(R.id.unread_count);
            deletehistory = (ImageView) root.findViewById(R.id.delete_history);
            this.root = root;
        }
    }
}
