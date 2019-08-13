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
import android.content.res.Resources;
import android.text.Html;

import com.emogoth.android.phone.mimi.app.MimiApplication;
import com.emogoth.android.phone.mimi.fourchan.FourChanCommentParser;
import com.emogoth.android.phone.mimi.util.FourChanUtil;
import com.emogoth.android.phone.mimi.util.MimiPrefs;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.mimireader.chanlib.models.ChanPost;
import com.mimireader.chanlib.models.ChanThread;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.functions.Function;

public class ProcessThreadTask {
    private static final String LOG_TAG = ProcessThreadTask.class.getSimpleName();

    private final ChanThread outThread;
    private final String boardName;

    public ProcessThreadTask(final String boardName, final ChanThread inThread) {
        this.boardName = boardName;
        this.outThread = inThread;
    }

    public static Function<ChanThread, ChanThread> processThread(final List<Long> userPosts, final String boardName, final long threadId) {
        return thread -> {
            if (thread == null) {
                return ChanThread.empty();
            }

            return processThread(thread.getPosts(), userPosts, boardName, threadId);
        };
    }

    public static Function<ChanThread, Observable<ChanThread>> processThreadFlatMap(final List<Long> userPosts, final String boardName, final long threadId) {
        return chanThread -> {
            if (chanThread == null) {
                return Observable.just(ChanThread.empty());
            }

            return Observable.just(processThread(chanThread.getPosts(), userPosts, boardName, threadId));
        };

    }

    public static ChanThread processThread(List<ChanPost> posts, final List<Long> userPosts, final String boardName, final long threadId) {
        return processThread(posts, userPosts, boardName, threadId, 0L);
    }

    public static ChanThread processThread(List<ChanPost> posts, final List<Long> userPosts, final String boardName, final long threadId, final long highlightedPost) {
        final Resources res = MimiApplication.getInstance().getResources();
        if (posts == null || res == null) {
            return ChanThread.empty();
        }

        ChanThread updatedThread = new ChanThread();
        updatedThread.setBoardName(boardName);
        updatedThread.setThreadId(threadId);

        List<ChanPost> updatedPosts = updatePostList(posts, userPosts, boardName, threadId, highlightedPost);
        updatedThread.setPosts(updatedPosts);

        return updatedThread;
    }

    public static Function<List<ChanPost>, List<ChanPost>> processPostList(final List<ChanPost> posts, final List<Long> userPosts, final ChanThread thread, final long highlightedPost) {
        return postList -> updatePostList(posts, userPosts, thread.getBoardName(), thread.getThreadId(), highlightedPost);
    }

    public static Function<List<ChanPost>, List<ChanPost>> processPostList(final List<ChanPost> posts, final List<Long> userPosts, final ChanThread thread) {
        return processPostList(posts, userPosts, thread, 0);
    }

    public static ChanPost updatePost(final ChanPost post, List<Long> userPosts, final String boardName, final long highlightedPost) {
        final Context context = MimiApplication.getInstance().getApplicationContext();
        ChanPost updatedPost = new ChanPost(post);
        List<Long> hightlightedPosts = new ArrayList<>();
        if (highlightedPost > 0) {
            hightlightedPosts.add(highlightedPost);
        }

        FourChanCommentParser.Builder parserBuilder = new FourChanCommentParser.Builder();
        parserBuilder.setContext(context)
                .setQuoteColor(MimiUtil.getInstance().getQuoteColor())
                .setReplyColor(MimiUtil.getInstance().getReplyColor())
                .setHighlightColor(MimiUtil.getInstance().getHighlightColor())
                .setLinkColor(MimiUtil.getInstance().getLinkColor())
                .setEnableEmoji(MimiPrefs.isEmojiEnabled());

        final CharSequence nameSpan = FourChanUtil.getUserName(
                context.getResources(),
                post.getName(),
                post.getCapcode()
        );
        updatedPost.setDisplayedName(nameSpan);

        if (updatedPost.getCom() != null) {
            parserBuilder.setBoardName(boardName)
                    .setComment(updatedPost.getCom())
                    .setThreadId(updatedPost.getResto())
                    .setReplies(updatedPost.getRepliesTo())
                    .setUserPostIds(userPosts)
                    .setHighlightedPosts(hightlightedPosts);
            updatedPost.setComment(parserBuilder.build().parse());
        }

        return updatedPost;

    }

    private static List<ChanPost> updatePostList(final List<ChanPost> posts, List<Long> userPosts, final String boardName, final long threadId, final long highlightedPost) {
        final Context context = MimiApplication.getInstance().getApplicationContext();
        List<ChanPost> updatedPosts = new ArrayList<>(posts.size());
        List<Long> hightlightedPosts = new ArrayList<>();
        if (highlightedPost > 0) {
            hightlightedPosts.add(highlightedPost);
        }

        FourChanCommentParser.Builder parserBuilder = new FourChanCommentParser.Builder();
        parserBuilder.setContext(context)
                .setQuoteColor(MimiUtil.getInstance().getQuoteColor())
                .setReplyColor(MimiUtil.getInstance().getReplyColor())
                .setHighlightColor(MimiUtil.getInstance().getHighlightColor())
                .setLinkColor(MimiUtil.getInstance().getLinkColor())
                .setEnableEmoji(MimiPrefs.isEmojiEnabled());

        for (int i = 0; i < posts.size(); i++) {
            final ChanPost post = new ChanPost(posts.get(i));
            final CharSequence nameSpan = FourChanUtil.getUserName(
                    context.getResources(),
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
                for (final String replyPostId : post.getRepliesTo()) {
                    try {
                        final int index = MimiUtil.findPostPositionById(Integer.valueOf(replyPostId), posts);
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
}
