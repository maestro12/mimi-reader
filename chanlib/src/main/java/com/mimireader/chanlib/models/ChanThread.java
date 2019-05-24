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

package com.mimireader.chanlib.models;


import android.text.TextUtils;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChanThread {
    @Expose protected List<ChanPost> posts = new ArrayList<>();
    @Expose protected String boardName;
    @Expose protected String boardTitle;
    @Expose protected long threadId;

    public ChanThread() {
        this.threadId = -1;
    }

    public ChanThread(String boardName, long threadId, List<ChanPost> posts) {
        this.boardName = boardName;
        this.threadId = threadId;
        this.posts.addAll(posts);
    }

    public ChanThread(ChanThread thread) {
        this.boardName = thread.getBoardName();
        this.boardTitle = thread.getBoardTitle();
        this.threadId = thread.getThreadId();
        this.posts.addAll(thread.getPosts());
    }

    public List<ChanPost> getPosts() {
        return posts;
    }

    public void setPosts(List<ChanPost> posts) {
        this.posts = posts;
    }

    public String getBoardName() {
        return boardName;
    }

    public void setBoardName(String boardName) {
        this.boardName = boardName;
    }

    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    public String getBoardTitle() {
        return boardTitle;
    }

    public void setBoardTitle(String boardTitle) {
        this.boardTitle = boardTitle;
    }

    public static ChanThread empty() {
        return new ChanThread("", -1, Collections.<ChanPost>emptyList());
    }

    public static boolean isEmpty(ChanThread thread) {
        return thread == null || (TextUtils.isEmpty(thread.boardName) && thread.threadId == -1 && thread.posts.size() == 0);
    }
}
