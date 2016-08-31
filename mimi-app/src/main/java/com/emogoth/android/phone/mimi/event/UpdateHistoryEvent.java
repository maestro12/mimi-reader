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

package com.emogoth.android.phone.mimi.event;

import com.mimireader.chanlib.models.ChanThread;

public class UpdateHistoryEvent {

    private ChanThread thread;
    private String boardName;
    private int newPostCount;

    private int threadId;
    private int threadSize;

    public UpdateHistoryEvent() {
    }

    public UpdateHistoryEvent(final String boardName, final ChanThread thread) {
        this.boardName = boardName;
        this.thread = thread;
    }

    public ChanThread getThread() {
        return thread;
    }

    public void setThread(ChanThread thread) {
        this.thread = thread;
        if (thread != null) {
            this.threadId = thread.getThreadId();
            this.threadSize = thread.getPosts().size();
        }
    }

    public String getBoardName() {
        return boardName;
    }

    public void setBoardName(String boardName) {
        this.boardName = boardName;
    }

    public int getNewPostCount() {
        return newPostCount;
    }

    public void setNewPostCount(int newPostCount) {
        this.newPostCount = newPostCount;
    }

    public int getThreadId() {
        if (thread != null) {
            threadId = thread.getThreadId();
        }
        return threadId;
    }

    public void setThreadId(int threadId) {
        this.threadId = threadId;
    }

    public int getThreadSize() {
        if (thread != null) {
            threadSize = thread.getPosts().size();
        }
        return threadSize;
    }

    public void setThreadSize(int threadSize) {
        this.threadSize = threadSize;
    }

}
