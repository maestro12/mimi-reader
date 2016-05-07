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

package com.emogoth.android.phone.mimi.dialog;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ListView;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.adapter.RepliesListAdapter;
import com.emogoth.android.phone.mimi.async.ProcessThreadTask;
import com.emogoth.android.phone.mimi.event.ReplyClickEvent;
import com.emogoth.android.phone.mimi.model.OutsideLink;

import com.emogoth.android.phone.mimi.util.BusProvider;
import com.emogoth.android.phone.mimi.util.Extras;
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.mimireader.chanlib.models.ChanPost;
import com.mimireader.chanlib.models.ChanThread;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;


public class RepliesDialog extends DialogFragment {
    public static final String DIALOG_TAG = "reply_dialog_tag";

    private String boardName;
    private List<ChanPost> replies;
    private List<OutsideLink> outsideLinks;
    private ChanThread thread;
    private int id;
    private Subscription repliesSubscription;

    public static RepliesDialog newInstance(ChanThread thread, ChanPost postItem) {
        final String boardName = thread.getBoardName();
        final RepliesDialog dialog = new RepliesDialog();
        final Bundle args = new Bundle();

        args.putString(Extras.EXTRAS_BOARD_NAME, boardName);

        final ArrayList<ChanPost> posts = new ArrayList<>(postItem.getRepliesFrom().size());
        for (ChanPost post : postItem.getRepliesFrom()) {
            final int i = thread.getPosts().indexOf(post);

            if (i >= 0) {
                final ChanPost p = thread.getPosts().get(i);
                posts.add(p);
            }

        }
        args.putInt(Extras.EXTRAS_POST_ID, postItem.getNo());
        args.putParcelableArrayList(Extras.EXTRAS_POST_LIST, posts);
        args.putParcelable(Extras.EXTRAS_SINGLE_THREAD, thread);
        dialog.setArguments(args);
        return dialog;
    }

    public static RepliesDialog newInstance(@Nullable  ChanThread thread, String id) {

        if(thread != null && thread.getPosts() != null && thread.getPosts().size() > 0 && !TextUtils.isEmpty(id)) {
            final String boardName = thread.getBoardName();
            final RepliesDialog dialog = new RepliesDialog();
            final Bundle args = new Bundle();
            final ArrayList<ChanPost> posts = new ArrayList<>();

            for (ChanPost post : thread.getPosts()) {
                if(id.equals(post.getId())) {
                    posts.add(post);
                }
            }

            args.putString(Extras.EXTRAS_BOARD_NAME, boardName);
            args.putInt(Extras.EXTRAS_POST_ID, thread.getThreadId());
            args.putParcelableArrayList(Extras.EXTRAS_POST_LIST, posts);
            args.putParcelable(Extras.EXTRAS_SINGLE_THREAD, thread);
            dialog.setArguments(args);
            return dialog;
        }

        return null;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
        extractExtras(getArguments());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        final View v = inflater.inflate(R.layout.dialog_replies, container, false);
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final View closeButton = view.findViewById(R.id.close_button);
        final ListView listView = (ListView) view.findViewById(R.id.replies_list);
        final Toolbar toolbar = (Toolbar) view.findViewById(R.id.reply_dialog_toolbar);

        toolbar.setNavigationIcon(null);
        toolbar.setLogo(null);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ReplyClickEvent event = new ReplyClickEvent(null, -1);
                BusProvider.getInstance().post(event);
                dismiss();
            }
        });

        RxUtil.safeUnsubscribe(repliesSubscription);
        repliesSubscription = Observable.just(replies)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .map(ProcessThreadTask.processPostList(getActivity(), replies, thread, id))
                .subscribe(new Action1<List<ChanPost>>() {
                    @Override
                    public void call(List<ChanPost> posts) {
                        final RepliesListAdapter adapter = new RepliesListAdapter(getActivity(), boardName, posts, outsideLinks, thread);
                        listView.setAdapter(adapter);
                    }
                });
    }

    private void extractExtras(final Bundle bundle) {
        if(bundle.containsKey(Extras.EXTRAS_BOARD_NAME)) {
            boardName = bundle.getString(Extras.EXTRAS_BOARD_NAME);
        }
        if(bundle.containsKey(Extras.EXTRAS_POST_ID)) {
            id = bundle.getInt(Extras.EXTRAS_POST_ID);
        }
        if(bundle.containsKey(Extras.EXTRAS_POST_LIST)) {
            bundle.setClassLoader(ChanPost.class.getClassLoader());
            replies = bundle.getParcelableArrayList(Extras.EXTRAS_POST_LIST);
        }
        if(bundle.containsKey(Extras.EXTRAS_OUTSIDE_LINK_LIST)) {
            outsideLinks = bundle.getParcelableArrayList(Extras.EXTRAS_OUTSIDE_LINK_LIST);
        }
        if(bundle.containsKey(Extras.EXTRAS_SINGLE_THREAD)) {
            thread = bundle.getParcelable(Extras.EXTRAS_SINGLE_THREAD);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        BusProvider.getInstance().unregister(this);
        RxUtil.safeUnsubscribe(repliesSubscription);
    }

    @Override
    public void onResume() {
        super.onResume();

        int deviceWidth = getResources().getDisplayMetrics().widthPixels;
        getDialog().getWindow().setLayout(deviceWidth - 15, ViewGroup.LayoutParams.WRAP_CONTENT);

        BusProvider.getInstance().register(this);
    }

    @Subscribe
    public void onReplyClicked(final ReplyClickEvent event) {
        dismiss();
    }
}
