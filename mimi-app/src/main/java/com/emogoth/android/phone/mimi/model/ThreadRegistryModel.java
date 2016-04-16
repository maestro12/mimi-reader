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

package com.emogoth.android.phone.mimi.model;


import java.util.ArrayList;
import java.util.List;

public class ThreadRegistryModel {
    private int threadId;
    private String boardName;
    private int threadSize;
    private int unreadCount = 0;
    private int lastReadPosition = 0;
    private boolean bookmarked = false;
    private boolean active;
    private List<Integer> userPosts;


    public int getThreadId() {
        return threadId;
    }

    public void setThreadId(int threadId) {
        this.threadId = threadId;
    }

    public String getBoardName() {
        return boardName;
    }

    public void setBoardName(String boardName) {
        this.boardName = boardName;
    }

    public int getThreadSize() {
        return threadSize;
    }

    public void setThreadSize(int threadSize) {
        this.threadSize = threadSize;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public boolean isBookmarked() {
        return bookmarked;
    }

    public void setBookmarked(boolean bookmarked) {
        this.bookmarked = bookmarked;
    }

    public int getLastReadPosition() {
        return lastReadPosition;
    }

    public void setLastReadPosition(int lastReadPosition) {
        this.lastReadPosition = lastReadPosition;

        if (this.threadSize > this.lastReadPosition) {
            this.unreadCount = threadSize - this.lastReadPosition;
        } else {
            this.unreadCount = 0;
        }
    }

    public List<Integer> getUserPosts() {
        if (userPosts == null) {
            userPosts = new ArrayList<>();
        }

        return userPosts;
    }

    public void setUserPosts(List<Integer> userPosts) {
        this.userPosts = userPosts;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
