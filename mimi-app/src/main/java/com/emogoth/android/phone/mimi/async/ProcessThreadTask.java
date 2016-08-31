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

package com.emogoth.android.phone.mimi.async;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.text.Html;
import android.text.Spannable;

import com.emogoth.android.phone.mimi.fourchan.FourChanCommentParser;
import com.emogoth.android.phone.mimi.util.FourChanUtil;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.ThreadRegistry;
import com.mimireader.chanlib.models.ChanPost;
import com.mimireader.chanlib.models.ChanThread;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.functions.Func1;

public class ProcessThreadTask extends AsyncTaskLoader<ChanThread> {
    private static final String LOG_TAG = ProcessThreadTask.class.getSimpleName();

    private final ChanThread outThread;
    private final String boardName;

    public ProcessThreadTask(final Context activity, final String boardName, final ChanThread inThread) {
        super(activity);

        this.boardName = boardName;
        this.outThread = inThread;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    public ChanThread loadInBackground() {
        if (outThread == null) {
            return null;
        }

        return processThread(getContext(), outThread.getPosts(), boardName, outThread.getThreadId());
    }

    public static Func1<ChanThread, ChanThread> processThread(final Context context, final String boardName, final int threadId) {
        return new Func1<ChanThread, ChanThread>() {
            @Override
            public ChanThread call(ChanThread thread) {
                if (thread == null) {
                    return null;
                }

                return processThread(context, thread.getPosts(), boardName, threadId);
            }
        };
    }

    public static Func1<ChanThread, Observable<ChanThread>> processThreadFlatMap(final Context context, final String boardName, final int threadId) {
        return new Func1<ChanThread, Observable<ChanThread>>() {
            @Override
            public Observable<ChanThread> call(ChanThread chanThread) {
                if (chanThread == null) {
                    return Observable.just(null);
                }

                return Observable.just(processThread(context, chanThread.getPosts(), boardName, threadId));
            }
        };

    }

    public static ChanThread processThread(final Context context, List<ChanPost> posts, final String boardName, final int threadId) {
        if (posts == null || context == null) {
            return null;
        }

        ChanThread updatedThread = new ChanThread();
        updatedThread.setBoardName(boardName);
        updatedThread.setThreadId(threadId);

        List<ChanPost> updatedPosts = updatePostList(context, posts, boardName, threadId, 0);
        updatedThread.setPosts(updatedPosts);

        return updatedThread;
    }

    public static Func1<List<ChanPost>, List<ChanPost>> processPostList(final Context context, final List<ChanPost> posts, final ChanThread thread, final int highlightedPost) {
        return new Func1<List<ChanPost>, List<ChanPost>>() {
            @Override
            public List<ChanPost> call(List<ChanPost> postList) {
                return updatePostList(context, posts, thread.getBoardName(), thread.getThreadId(), highlightedPost);
            }
        };
    }

    public static Func1<List<ChanPost>, List<ChanPost>> processPostList(final Context context, final List<ChanPost> posts, final ChanThread thread) {
        return processPostList(context, posts, thread, 0);
    }

    private static List<ChanPost> updatePostList(final Context context, final List<ChanPost> posts, final String boardName, final int threadId, final int highlightedPost) {
        List<ChanPost> updatedPosts = new ArrayList<>(posts.size());
        List<Integer> userPosts = new ArrayList<>(ThreadRegistry.getInstance().getUserPosts(boardName, threadId));
        List<Integer> hightlightedPosts = new ArrayList<>();
        if (highlightedPost > 0) {
            hightlightedPosts.add(highlightedPost);
        }

        FourChanCommentParser.Builder parserBuilder = new FourChanCommentParser.Builder();
        parserBuilder.setContext(context)
                .setQuoteColor(MimiUtil.getInstance().getQuoteColor())
                .setReplyColor(MimiUtil.getInstance().getReplyColor())
                .setHighlightColor(MimiUtil.getInstance().getHighlightColor())
                .setLinkColor(MimiUtil.getInstance().getLinkColor());

        for (int i = 0; i < posts.size(); i++) {
            final ChanPost post = new ChanPost(posts.get(i));
            final Spannable nameSpan = FourChanUtil.getUserName(
                    context,
                    post.getName(),
                    post.getCapcode()
            );
            post.setDisplayedName(nameSpan);

            if (post.getCom() != null) {
                parserBuilder.setBoardName(boardName)
                        .setComment(post.getCom())
                        .setThreadId(post.getResto())
                        .setReplies(post.getRepliesTo())
                        .setUserPostIds(userPosts)
                        .setHighlightedPosts(hightlightedPosts);
                post.setComment(parserBuilder.build().parse());
            }

            if (post.getRepliesTo() != null) {
                final ChanPost searchPost = new ChanPost();
                for (final String replyPostId : post.getRepliesTo()) {
                    try {
                        searchPost.setNo(Integer.valueOf(replyPostId));
                        final int index = posts.indexOf(searchPost);
                        if (index >= 0) {
                            if (!posts.get(index).getRepliesFrom().contains(post)) {
                                posts.get(index).addReplyFrom(post);
                            }
                        }
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
            }

            if (post.getSub() != null) {
                post.setSubject(Html.fromHtml(post.getSub()));
            }

            updatedPosts.add(post);
        }

        return updatedPosts;
    }

    @Override
    public void deliverResult(ChanThread data) {
        super.deliverResult(data);
    }
}
