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


import com.emogoth.android.phone.mimi.db.model.History;
import com.emogoth.android.phone.mimi.db.model.UserPost;

import java.util.ArrayList;
import java.util.List;

public class ThreadRegistryModel {
    private final  long threadId;
    private final String boardName;
    private final int threadSize;
    private final int lastReadPosition;
    private final int unreadCount;
    private final boolean bookmarked;
    private final boolean active;
    private final List<Long> userPosts = new ArrayList<>();

    public ThreadRegistryModel(History history, List<UserPost> userPosts) {
        threadId = history.threadId;
        boardName = history.boardName;
        threadSize = history.threadSize;
        lastReadPosition = history.lastReadPosition;
        unreadCount = history.unreadCount;
        bookmarked = history.watched == 1;
        active = true;

        if (userPosts != null) {
            for (UserPost userPost : userPosts) {
                if (userPost.boardName.equals(history.boardName) && userPost.threadId == history.threadId) {
                    this.userPosts.add(userPost.postId);
                }
            }
        }

    }


    public long getThreadId() {
        return threadId;
    }

    public String getBoardName() {
        return boardName;
    }

    public int getThreadSize() {
        return threadSize;
    }

    public int getUnreadCount() {
        return threadSize - 1 - lastReadPosition;
    }

    public boolean isBookmarked() {
        return bookmarked;
    }

    public int getLastReadPosition() {
        return lastReadPosition;
    }

    public List<Long> getUserPosts() {
        return userPosts;
    }

    public boolean isActive() {
        return active;
    }
}
