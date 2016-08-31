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

package com.mimireader.chanlib.util;

import android.content.Context;
import android.text.Spannable;

import java.util.List;
import java.util.regex.Pattern;


public abstract class CommentParser {
    protected static Pattern YOUTUBE_PATTERN = Pattern.compile(".*(?:youtu.be\\/|v\\/|u\\/\\w\\/|embed\\/|watch\\?v=)([^#\\&\\?]*).*");

    protected final List<String> replies;
    protected final List<Integer> userPostIds;
    protected final List<Integer> highlightedPosts;
    protected final Context context;
    protected final CharSequence comment;
    protected final String boardName;
    protected final String opTag;
    protected final String youTag;
    protected final int threadId;
    protected final boolean demoMode;

    protected int replyColor;
    protected int highlightReplyColor;
    protected int quoteColor;
    protected int linkColor;

    public CommentParser(List<String> replies, List<Integer> userPostIds, List<Integer> highlightedPosts, Context context, CharSequence comment, String boardName, String opTag, String youTag, int threadId, int replyColor, int highlightReplyColor, int quoteColor, int linkColor, boolean demoMode) {
        this.replies = replies;
        this.userPostIds = userPostIds;
        this.highlightedPosts = highlightedPosts;
        this.context = context;
        this.comment = comment;
        this.boardName = boardName;
        this.opTag = opTag;
        this.youTag = youTag;
        this.threadId = threadId;
        this.replyColor = replyColor;
        this.highlightReplyColor = highlightReplyColor;
        this.quoteColor = quoteColor;
        this.linkColor = linkColor;
        this.demoMode = demoMode;
    }

    public abstract static class Builder {
        protected List<String> replies = null;
        protected List<Integer> userPostIds = null;
        protected List<Integer> highlightedPosts = null;
        protected Context context = null;
        protected CharSequence comment = null;
        protected String boardName = null;
        protected String opTag = " (OP)";
        protected String youTag = " (You)";
        protected int threadId = 0;

        protected int replyColor = -1;
        protected int highlightColor = -1;
        protected int quoteColor = -1;
        protected int linkColor = -1;

        protected boolean demoMode = false;

        public Builder setReplies(List<String> replies) {
            this.replies = replies;
            return this;
        }

        public Builder setUserPostIds(List<Integer> userPostIds) {
            this.userPostIds = userPostIds;
            return this;
        }

        public Builder setHighlightedPosts(List<Integer> highlightedPosts) {
            this.highlightedPosts = highlightedPosts;
            return this;
        }

        public Builder setContext(Context context) {
            this.context = context;
            return this;
        }

        public Builder setComment(CharSequence comment) {
            this.comment = comment;
            return this;
        }

        public Builder setBoardName(String boardName) {
            this.boardName = boardName;
            return this;
        }

        public Builder setOpTag(String opTag) {
            this.opTag = opTag;
            return this;
        }

        public Builder setYouTag(String youTag) {
            this.youTag = youTag;
            return this;
        }

        public Builder setThreadId(int threadId) {
            this.threadId = threadId;
            return this;
        }

        public Builder setReplyColor(int replyColor) {
            this.replyColor = replyColor;
            return this;
        }

        public Builder setHighlightColor(int highlightColor) {
            this.highlightColor = highlightColor;
            return this;
        }

        public Builder setQuoteColor(int quoteColor) {
            this.quoteColor = quoteColor;
            return this;
        }

        public Builder setLinkColor(int linkColor) {
            this.linkColor = linkColor;
            return this;
        }

        public Builder setDemoMode(boolean enabled) {
            this.demoMode = enabled;
            return this;
        }

        public abstract CommentParser build();

    }

    public abstract Spannable parse();
}
