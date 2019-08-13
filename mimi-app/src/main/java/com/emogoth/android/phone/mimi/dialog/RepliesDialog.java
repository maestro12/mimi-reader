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

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.activity.GalleryActivity2;
import com.emogoth.android.phone.mimi.activity.PostItemDetailActivity;
import com.emogoth.android.phone.mimi.activity.PostItemListActivity;
import com.emogoth.android.phone.mimi.adapter.RepliesListAdapter2;
import com.emogoth.android.phone.mimi.async.ProcessThreadTask;
import com.emogoth.android.phone.mimi.db.PostTableConnection;
import com.emogoth.android.phone.mimi.db.UserPostTableConnection;
import com.emogoth.android.phone.mimi.db.model.UserPost;
import com.emogoth.android.phone.mimi.event.ReplyClickEvent;
import com.emogoth.android.phone.mimi.model.OutsideLink;
import com.emogoth.android.phone.mimi.util.BusProvider;
import com.emogoth.android.phone.mimi.util.Extras;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.emogoth.android.phone.mimi.util.ThreadRegistry;
import com.emogoth.android.phone.mimi.view.gallery.GalleryPagerAdapter;
import com.mimireader.chanlib.models.ChanPost;
import com.mimireader.chanlib.models.ChanThread;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class RepliesDialog extends DialogFragment {
    public static final String LOG_TAG = RepliesDialog.class.getSimpleName();
    public static final String DIALOG_TAG = "reply_dialog_tag";

    private String boardName;
    private long threadId;
    private ArrayList<String> replies;
    private List<OutsideLink> outsideLinks;
    private ChanThread thread;
    private long id;
    private Disposable repliesSubscription;


    public RepliesDialog() {
        Log.d(LOG_TAG, "New replies dialog created");
    }

    public static RepliesDialog newInstance(ChanThread thread, ChanPost postItem) {
        final String boardName = thread.getBoardName();
        final RepliesDialog dialog = new RepliesDialog();
        final Bundle args = new Bundle();

        args.putString(Extras.EXTRAS_BOARD_NAME, boardName);

        final ArrayList<String> postNumbers = new ArrayList<>(postItem.getRepliesFrom().size());
        for (ChanPost post : postItem.getRepliesFrom()) {
            postNumbers.add(String.valueOf(post.getNo()));
        }

        args.putLong(Extras.EXTRAS_POST_ID, postItem.getNo());
        args.putStringArrayList(Extras.EXTRAS_POST_LIST, postNumbers);
        args.putLong(Extras.EXTRAS_THREAD_ID, thread.getThreadId());

        ThreadRegistry.getInstance().setPosts(thread.getThreadId(), thread.getPosts());
        dialog.setArguments(args);
        return dialog;
    }

    public static RepliesDialog newInstance(@Nullable ChanThread thread, String id) {

        if (thread != null && thread.getPosts() != null && thread.getPosts().size() > 0 && !TextUtils.isEmpty(id)) {
            final String boardName = thread.getBoardName();
            final RepliesDialog dialog = new RepliesDialog();
            final Bundle args = new Bundle();
            final ArrayList<String> postNumbers = new ArrayList<>();

            for (ChanPost post : thread.getPosts()) {
                if (id.equals(post.getId())) {
                    postNumbers.add(String.valueOf(post.getNo()));
                }
            }

            args.putString(Extras.EXTRAS_BOARD_NAME, boardName);

            if (TextUtils.isDigitsOnly(id)) {
                args.putLong(Extras.EXTRAS_POST_ID, Long.valueOf(id));
            }

            args.putStringArrayList(Extras.EXTRAS_POST_LIST, postNumbers);
            args.putLong(Extras.EXTRAS_THREAD_ID, thread.getThreadId());

            ThreadRegistry.getInstance().setPosts(thread.getThreadId(), thread.getPosts());
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
        final RecyclerView listView = view.findViewById(R.id.replies_list);
        final Toolbar toolbar = view.findViewById(R.id.reply_dialog_toolbar);

        toolbar.setNavigationIcon(null);
        toolbar.setLogo(null);

        closeButton.setOnClickListener(v -> {
            final ReplyClickEvent event = new ReplyClickEvent(null, -1);
            BusProvider.getInstance().post(event);
            dismiss();
        });

        RxUtil.safeUnsubscribe(repliesSubscription);

        repliesSubscription = Flowable.zip(UserPostTableConnection.fetchPosts(boardName, threadId),
                PostTableConnection.watchThread(thread.getThreadId()), (userPosts, chanThread) -> {
                    List<Long> posts = new ArrayList<>();
                    for (UserPost userPost : userPosts) {
                        if (userPost.boardName.equals(thread.getBoardName()) && userPost.threadId == thread.getThreadId()) {
                            posts.add(userPost.postId);
                        }
                    }

                    List<ChanPost> chanPosts = new ArrayList<>();
                    List<ChanPost> postModels = PostTableConnection.convertDbPostsToChanThread(boardName, thread.getThreadId(), chanThread).getPosts();
                    List<ChanPost> processedPosts = ProcessThreadTask.processThread(postModels, posts, thread.getBoardName(), thread.getThreadId(), id).getPosts();

                    for (String reply : replies) {
                        for (ChanPost postModel : processedPosts) {
                            if (postModel.getNo() == Integer.valueOf(reply)) {
                                chanPosts.add(postModel);
                            }
                        }
                    }

                    return chanPosts;
//                    return ProcessThreadTask.processThread(chanPosts, posts, thread.getBoardName(), thread.getThreadId(), id).getPosts();
                })
                .first(Collections.emptyList())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(posts -> {
//                    final RepliesListAdapter adapter = new RepliesListAdapter(getActivity(), boardName, posts, outsideLinks, thread);
                    final RepliesListAdapter2 adapter = new RepliesListAdapter2(posts, outsideLinks, thread);
                    adapter.setLinkClickListener(outsideLink -> {
                        final Intent intent;
                        final String id = outsideLink.getThreadId();

                        if (id != null && TextUtils.isDigitsOnly(id)) {
                            intent = new Intent(getActivity(), PostItemDetailActivity.class);
                            intent.putExtra(Extras.EXTRAS_THREAD_ID, Long.valueOf(id));
                            intent.putExtra(Extras.EXTRAS_SINGLE_THREAD, true);
                        } else {
                            intent = new Intent(getActivity(), PostItemListActivity.class);
                        }

                        intent.putExtra(Extras.EXTRAS_BOARD_NAME, outsideLink.getBoardName());
                        getActivity().startActivity(intent);

                        return null;
                    });
                    adapter.setRepliesTextClickListener(chanPost -> {
                        if (getActivity() != null) {
                            RepliesDialog.newInstance(thread, chanPost).show(getActivity().getSupportFragmentManager(), RepliesDialog.DIALOG_TAG);
                        }

                        return null;
                    });
                    adapter.setThumbClickListener(chanPost -> {
                        if (getActivity() != null) {
                            final ArrayList<ChanPost> postsWithImages = GalleryPagerAdapter.getPostsWithImages(posts);

                            ThreadRegistry.getInstance().setPosts(chanPost.getNo(), postsWithImages);
                            long[] ids = new long[postsWithImages.size()];
                            for (int i = 0; i < postsWithImages.size(); i++) {
                                ids[i] = postsWithImages.get(i).getNo();
                            }
                            GalleryActivity2.start(getActivity(), GalleryActivity2.GALLERY_TYPE_PAGER, chanPost.getNo(), boardName, thread.getThreadId(), ids);
                        }
                        return null;
                    });
                    listView.setLayoutManager(new LinearLayoutManager(getActivity()));
                    listView.setAdapter(adapter);
                });
    }

    private void extractExtras(final Bundle bundle) {
        if (bundle.containsKey(Extras.EXTRAS_BOARD_NAME)) {
            boardName = bundle.getString(Extras.EXTRAS_BOARD_NAME);
        }
        if (bundle.containsKey(Extras.EXTRAS_POST_ID)) {
            id = bundle.getLong(Extras.EXTRAS_POST_ID);
        }
        if (bundle.containsKey(Extras.EXTRAS_POST_LIST)) {
            replies = bundle.getStringArrayList(Extras.EXTRAS_POST_LIST);
        }
        if (bundle.containsKey(Extras.EXTRAS_OUTSIDE_LINK_LIST)) {
            outsideLinks = bundle.getParcelableArrayList(Extras.EXTRAS_OUTSIDE_LINK_LIST);
        } else {
            outsideLinks = Collections.emptyList();
        }
        if (bundle.containsKey(Extras.EXTRAS_SINGLE_THREAD)) {
            thread = bundle.getParcelable(Extras.EXTRAS_SINGLE_THREAD);
        }
        if (bundle.containsKey(Extras.EXTRAS_THREAD_ID)) {
            long id = bundle.getLong(Extras.EXTRAS_THREAD_ID);
            List<ChanPost> threadPosts = ThreadRegistry.getInstance().getPosts(id);
            thread = new ChanThread(boardName, id, threadPosts);

            this.threadId = id;

            ThreadRegistry.getInstance().clearPosts(id);
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
