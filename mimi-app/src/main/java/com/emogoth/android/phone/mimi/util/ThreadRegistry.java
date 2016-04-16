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

package com.emogoth.android.phone.mimi.util;

import android.util.Log;
import android.util.SparseArray;

import com.emogoth.android.phone.mimi.db.HistoryTableConnection;
import com.emogoth.android.phone.mimi.db.model.History;
import com.emogoth.android.phone.mimi.model.ThreadRegistryModel;
import com.mimireader.chanlib.models.ChanPost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.functions.Action1;


public class ThreadRegistry {
    private static final String LOG_TAG = ThreadRegistry.class.getSimpleName();
    private static ThreadRegistry instance = new ThreadRegistry();

    private SparseArray<ThreadRegistryModel> threadArray;
    private List<ChanPost> chanPosts;
    private int threadId;

    private ThreadRegistry() {
    }

    public static ThreadRegistry getInstance() {
        return instance;
    }

    public void init() {
        if(threadArray != null) {
            threadArray.clear();
        }
        else {
            threadArray = new SparseArray<>();
        }

        HistoryTableConnection.fetchHistory(true, 0, false)
                .subscribe(new Action1<List<History>>() {
                    @Override
                    public void call(List<History> historyList) {
                        for (History historyDbModel : historyList) {
                            final ThreadRegistryModel t = new ThreadRegistryModel();
                            final int i = historyDbModel.threadId;
                            t.setThreadId(i);
                            t.setBoardName(historyDbModel.boardName);
                            t.setLastReadPosition(historyDbModel.threadSize);
                            t.setBookmarked(historyDbModel.watched);
                            t.setThreadSize(historyDbModel.threadSize);
                            t.setActive(true);

                            Log.d(LOG_TAG, "Adding bookmark to registry: size=" + historyDbModel.threadSize);

                            threadArray.append(i, t);
                        }
                    }
                });
    }

    public void setPosts(int id, List<ChanPost> posts) {
        if(this.threadId == 0) {
            Log.d(LOG_TAG, "Setting a post list: id=" + id + ", size=" + posts.size());
            this.threadId = id;
            this.chanPosts = posts;
        }
    }

    public List<ChanPost> getPosts(int id) {
        if(this.threadId == id) {
            Log.d(LOG_TAG, "Getting post list: id=" + id);
            return this.chanPosts;
        }

        return new ArrayList<>();
    }

    public void clearPosts(int id) {
        if(threadId > 0 && threadId == id) {
            Log.d(LOG_TAG, "clearing post list: id=" + id);
            this.threadId = 0;
            this.chanPosts = null;
        }
    }

    public void add(final String boardName, final int threadId, final int postId, final int postCount, final boolean bookmarked) {
        Log.d(LOG_TAG, "Adding thread information: thread id=" + threadId + ", size=" + postCount);
        final ThreadRegistryModel thread;
        if(threadExists(threadId)) {
            thread = threadArray.get(threadId);
            threadArray.remove(threadId);

            thread.setBookmarked(bookmarked);

            if(bookmarked) {
                updateUnreadCount(thread, postCount);
            }
            else {
                thread.setUnreadCount(0);
            }

            if(postId > 0 && !thread.getUserPosts().contains(postId)) {
                thread.getUserPosts().add(postId);
            }

            thread.setThreadSize(postCount);

            threadArray.put(threadId, thread);
            Log.i(LOG_TAG, "Modifying thread: id=" + threadId + ", board=" + boardName + ", size=" + postCount);
        }
        else {

            thread = new ThreadRegistryModel();
            thread.setThreadId(threadId);
            thread.setBoardName(boardName);
            thread.setThreadSize(postCount);
            thread.setUnreadCount(0);
            thread.setBookmarked(bookmarked);

            Log.i(LOG_TAG, "Adding thread: id=" + threadId + ", board=" + boardName + ", size=" + postCount);

            if(postId > 0 && !thread.getUserPosts().contains(postId)) {
                thread.getUserPosts().add(postId);
            }

            threadArray.append(threadId, thread);
        }

    }

    public void add(final String boardName, final int threadId, final int postCount, final boolean bookmarked) {
        add(boardName, threadId, 0, postCount, bookmarked);
    }

    public void remove(final int threadId) {
        if(threadArray != null && threadArray.get(threadId) != null) {
            final ThreadRegistryModel threadRegistryModel = threadArray.get(threadId);
            threadArray.remove(threadId);

            RefreshScheduler.getInstance().removeThread(threadRegistryModel.getBoardName(), threadRegistryModel.getThreadId());
        }
        else {
            Log.e(LOG_TAG, "Could not remove thread from registry: id=" + threadId);
        }
    }

    public void deactivate(final int threadId) {
        if(threadExists(threadId)) {
            threadArray.get(threadId).setActive(false);
        }
    }

    public void setLastReadPosition(final int threadId, final int position) {
        if(threadExists(threadId)) {
            final ThreadRegistryModel thread = threadArray.get(threadId);
            final int pos;
            if(position > thread.getThreadSize()) {
                pos = thread.getThreadSize();
            }
            else {
                pos = position;
            }

            if (pos > thread.getLastReadPosition()) {
                thread.setLastReadPosition(pos);
            }
        }
        else {
            Log.e(LOG_TAG, "Trying to update the last read position, but thread doesn't exist: id=" + threadId);
        }
    }

    public int getLastReadPosition(final int threadId) {
        if(threadExists(threadId)) {
            return threadArray.get(threadId).getLastReadPosition();
        }
        else {
            return -1;
        }
    }

    public void addUserPost(final String boardname, final int threadId, final int postId) {
        final ThreadRegistryModel threadRegistryModel = threadArray.get(threadId);

        if (threadRegistryModel != null) {
            if(!threadRegistryModel.getUserPosts().contains(postId)) {
                threadRegistryModel.getUserPosts().add(postId);
            }

            threadArray.put(threadId, threadRegistryModel);

            Log.d(LOG_TAG, "Added post id " + postId + " to /" + boardname + "/" + threadId);
        }
        else{
            Log.e(LOG_TAG, "Error trying to add post id " + postId + " to /" + boardname + "/" + threadId);
        }

    }

    public List<Integer> getUserPosts(final String boardName, final int threadId) {
        final ThreadRegistryModel threadRegistryModel = threadArray.get(threadId);

        if(threadRegistryModel != null) {
            return threadRegistryModel.getUserPosts();
        }

        return new ArrayList<>();
    }

    public boolean threadExists(final int threadId) {
        if(threadArray != null && threadArray.get(threadId) != null) {
            return true;
        }

        return false;
    }

    public void setUnreadCount(final int threadId, final int count) {
        if(threadExists(threadId)) {
            threadArray.get(threadId).setUnreadCount(count);
        }
    }

    public void setThreadSize(final int threadId, final int size) {
        if(threadExists(threadId)) {
            final ThreadRegistryModel thread = threadArray.get(threadId);
            if(thread.isBookmarked()) {
                updateUnreadCount(thread, size);
            }

            thread.setThreadSize(size);
        }
    }

    private void updateUnreadCount(final ThreadRegistryModel thread, final int size) {
        if (thread.getLastReadPosition() >= 0) {
            thread.setUnreadCount(
                    size - thread.getLastReadPosition()
            );
        } else {
            thread.setUnreadCount(
                    size - thread.getThreadSize()
            );
        }
    }

    public int getThreadSize(final int threadId) {
        if(threadExists(threadId)) {
            return threadArray.get(threadId).getThreadSize();
        }

        return -1;
    }

    public List<ThreadRegistryModel> getUpdatedThreads() {
        if(threadArray != null && threadArray.size() > 0) {
            final List<ThreadRegistryModel> updatedThreadList = new ArrayList<>(threadArray.size());
            for (int i = 0; i < threadArray.size(); i++) {
                final int key = threadArray.keyAt(i);
                final ThreadRegistryModel model = threadArray.get(key);

                if(model.getUnreadCount() > 0) {
                    updatedThreadList.add(model);
                }
            }

            return updatedThreadList;
        }

        return Collections.EMPTY_LIST;
    }

    public void update(final int threadId, final int size, final boolean reset, final boolean bookmarked) {
        Log.d(LOG_TAG, "[refresh] update called: id=" + threadId + ", size=" + size + ", reset=" + reset + ", bookmarked=" + bookmarked);

        if(threadExists(threadId)) {
            final ThreadRegistryModel threadRegistryModel = threadArray.get(threadId);
            Log.d(LOG_TAG, "[refresh] Updating thread information: thread id=" + threadId + ", size=" + size + ", old size=" + threadRegistryModel.getThreadSize() + ", last read pos=" + threadRegistryModel.getLastReadPosition());
            if (reset) {
                threadRegistryModel.setThreadSize(size);
            }

            if(bookmarked) {
                updateUnreadCount(threadRegistryModel, size);
            }
            else {
                threadRegistryModel.setUnreadCount(0);
            }

            threadRegistryModel.setBookmarked(bookmarked);
            Log.d(LOG_TAG, "[refresh] Setting unread count: count=" + threadRegistryModel.getUnreadCount() + ", thread id=" + threadId);
            threadArray.put(threadId, threadRegistryModel);
        }
//        }

    }

    public void update(final int threadId, final int size, final boolean bookmarked) {
        Log.d(LOG_TAG, "Calling update");
        update(threadId, size, false, bookmarked);
    }

    public void clear() {
        if(threadArray != null) {
            threadArray.clear();
        }
    }

    public int getUnreadCount(final int threadId, final int postCount) {
        threadArray.get(threadId).setUnreadCount(postCount - threadArray.get(threadId).getThreadSize());
        return threadArray.get(threadId).getUnreadCount();
    }

    public int getUnreadCount(final int threadId) {
        if(threadArray.get(threadId) == null) {
            return -1;
        }
        final ThreadRegistryModel threadRegistryModel = threadArray.get(threadId);
        return threadRegistryModel.getUnreadCount();
    }

    public int getUnreadCount() {
        int count = 0;
        if(threadArray != null) {
            for (int i = 0; i < threadArray.size(); i++) {
                final int index = threadArray.keyAt(i);
                if (threadArray.get(index).isBookmarked()) {
                    final int unreadCount = threadArray.get(index).getUnreadCount();
                    Log.d(LOG_TAG, "Thread " + threadArray.get(index).getThreadId() + ": unread count=" + threadArray.get(index).getUnreadCount());
                    if (unreadCount > 0)
                        count += unreadCount;
                }
            }
        }
        return count;
    }

    public ThreadRegistryModel getThread(Integer threadId) {
        return threadArray.get(threadId);
    }
}
