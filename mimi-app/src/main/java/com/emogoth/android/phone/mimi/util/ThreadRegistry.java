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

import androidx.collection.LongSparseArray;

import com.emogoth.android.phone.mimi.db.DatabaseUtils;
import com.emogoth.android.phone.mimi.db.HistoryTableConnection;
import com.emogoth.android.phone.mimi.db.models.History;
import com.emogoth.android.phone.mimi.model.ThreadRegistryModel;
import com.mimireader.chanlib.models.ChanPost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.disposables.Disposable;


public class ThreadRegistry {
    private static final String LOG_TAG = ThreadRegistry.class.getSimpleName();
    private static ThreadRegistry instance = new ThreadRegistry();

    private LongSparseArray<ThreadRegistryModel> threadArray;
    private List<ChanPost> chanPosts;
    private long threadId;

    private ThreadRegistry() {
    }

    public static ThreadRegistry getInstance() {
        return instance;
    }

//    public void init() {
//        if (threadArray != null) {
//            threadArray.clear();
//        } else {
//            threadArray = new LongSparseArray<>();
//        }
//
//        Disposable sub = HistoryTableConnection.fetchActiveBookmarks(0)
//                .compose(DatabaseUtils.applySingleSchedulers())
//                .subscribe(historyList -> {
//                    for (History historyDbModel : historyList) {
//                        final ThreadRegistryModel t = new ThreadRegistryModel(historyDbModel, Collections.emptyList());
//                        final long i = historyDbModel.getThreadId();
//
//                        Log.d(LOG_TAG, "[refresh] Adding bookmark to registry: size=" + historyDbModel.getThreadSize());
//
//                        threadArray.append(i, t);
//                    }
//                });
//    }

//    public void setPosts(long id, List<ChanPost> posts) {
//        if (this.threadId == 0) {
//            Log.d(LOG_TAG, "[refresh] Setting a post list: id=" + id + ", size=" + posts.size());
//            this.threadId = id;
//            this.chanPosts = posts;
//        }
//    }
//
//    public List<ChanPost> getPosts(long id) {
//        if (this.threadId == id) {
//            Log.d(LOG_TAG, "[refresh] Getting post list: id=" + id);
//            return this.chanPosts;
//        }
//
//        return new ArrayList<>();
//    }
//
//    public void clearPosts(long id) {
//        if (threadId > 0 && threadId == id) {
//            Log.d(LOG_TAG, "[refresh] clearing post list: id=" + id);
//            this.threadId = 0;
//            this.chanPosts = null;
//        }
//    }

//    public void add(final String boardName, final long threadId, final long postId, final int postCount, final boolean bookmarked) {
//        Log.d(LOG_TAG, "[refresh] Adding thread information: thread id=" + threadId + ", size=" + postCount);
//        final ThreadRegistryModel thread;
//        if (threadExists(threadId)) {
//            thread = threadArray.get(threadId);
//            threadArray.remove(threadId);
//
//            thread.setBookmarked(bookmarked);
//
//            if (bookmarked) {
//                updateUnreadCount(thread, postCount);
//            } else {
//                thread.setUnreadCount(0);
//            }
//
//            if (postId > 0 && !thread.getUserPosts().contains(postId)) {
//                thread.getUserPosts().add(postId);
//            }
//
//            thread.setThreadSize(postCount);
//
//            threadArray.put(threadId, thread);
//            Log.i(LOG_TAG, "[refresh] Modifying thread: id=" + threadId + ", board=" + boardName + ", size=" + postCount);
//        } else {
//
//            thread = new ThreadRegistryModel();
//            thread.setThreadId(threadId);
//            thread.setBoardName(boardName);
//            thread.setThreadSize(postCount);
//            thread.setUnreadCount(0);
//            thread.setBookmarked(bookmarked);
//
//            Log.i(LOG_TAG, "[refresh] Adding thread: id=" + threadId + ", board=" + boardName + ", size=" + postCount);
//
//            if (postId > 0 && !thread.getUserPosts().contains(postId)) {
//                thread.getUserPosts().add(postId);
//            }
//
//            threadArray.append(threadId, thread);
//        }
//
//    }
//
//    public void add(final String boardName, final long threadId, final int postCount, final boolean bookmarked) {
//        add(boardName, threadId, 0, postCount, bookmarked);
//    }
//
//    public void remove(final long threadId) {
//        if (threadArray != null && threadArray.get(threadId) != null) {
//            final ThreadRegistryModel threadRegistryModel = threadArray.get(threadId);
//            threadArray.remove(threadId);
//
//            RefreshScheduler.getInstance().removeThread(threadRegistryModel.getBoardName(), threadRegistryModel.getThreadId());
//        } else {
//            Log.e(LOG_TAG, "[refresh] Could not remove thread from registry: id=" + threadId);
//        }
//    }
//
//    public void deactivate(final long threadId) {
//        if (threadExists(threadId)) {
//            threadArray.get(threadId).setActive(false);
//        }
//    }
//
//    public void setLastReadPosition(final long threadId, final int position) {
//        if (threadExists(threadId)) {
//            final ThreadRegistryModel thread = threadArray.get(threadId);
//            final int pos;
//            if (position > thread.getThreadSize()) {
//                pos = thread.getThreadSize();
//            } else {
//                pos = position >= 0 ? position : 0;
//            }
//
//            if (pos > thread.getLastReadPosition()) {
//                thread.setLastReadPosition(pos);
//            }
//        } else {
//            Log.e(LOG_TAG, "[refresh] Trying to update the last read position, but thread doesn't exist: id=" + threadId);
//        }
//    }
//
//    public int getLastReadPosition(final long threadId) {
//        if (threadExists(threadId)) {
//            return threadArray.get(threadId).getLastReadPosition();
//        } else {
//            return -1;
//        }
//    }
//
//    public void addUserPost(final String boardname, final long threadId, final long postId) {
//        final ThreadRegistryModel threadRegistryModel = threadArray.get(threadId);
//
//        if (threadRegistryModel != null) {
//            if (!threadRegistryModel.getUserPosts().contains(postId)) {
//                threadRegistryModel.getUserPosts().add(postId);
//            }
//
//            threadArray.put(threadId, threadRegistryModel);
//
//            Log.d(LOG_TAG, "[refresh] Added post id " + postId + " to /" + boardname + "/" + threadId);
//        } else {
//            Log.e(LOG_TAG, "[refresh] Error trying to add post id " + postId + " to /" + boardname + "/" + threadId);
//        }
//
//    }
//
//    public void populateUserPosts(List<UserPost> userPosts) {
//        for (UserPost userPostDbModel : userPosts) {
//            addUserPost(userPostDbModel.boardName, userPostDbModel.threadId, userPostDbModel.postId);
//        }
//    }
//
//    public List<Long> getUserPosts(final String boardName, final long threadId) {
//        final ThreadRegistryModel threadRegistryModel = threadArray.get(threadId);
//
//        if (threadRegistryModel != null) {
//            return threadRegistryModel.getUserPosts();
//        }
//
//        return new ArrayList<>();
//    }
//
//    public boolean threadExists(final long threadId) {
//        if (threadArray != null && threadArray.get(threadId) != null) {
//            return true;
//        }
//
//        return false;
//    }
//
//    public void setUnreadCount(final long threadId, final int count) {
//        if (threadExists(threadId)) {
//            threadArray.get(threadId).setUnreadCount(count);
//        }
//    }
//
//    public void setThreadSize(final long threadId, final int size) {
//        if (threadExists(threadId)) {
//            final ThreadRegistryModel thread = threadArray.get(threadId);
//            thread.setThreadSize(size);
//            if (thread.isBookmarked()) {
//                updateUnreadCount(thread, size);
//            }
//        }
//    }
//
//    private void updateUnreadCount(final ThreadRegistryModel thread, final int size) {
//        if (thread.getLastReadPosition() >= 0) {
//            thread.setUnreadCount(
//                    size - thread.getLastReadPosition()
//            );
//        } else {
//            thread.setUnreadCount(
//                    size - thread.getThreadSize()
//            );
//        }
//    }
//
//    public int getThreadSize(final long threadId) {
//        if (threadExists(threadId)) {
//            return threadArray.get(threadId).getThreadSize();
//        }
//
//        return -1;
//    }
//
//    public List<ThreadRegistryModel> getUpdatedThreads() {
//        if (threadArray != null && threadArray.size() > 0) {
//            final List<ThreadRegistryModel> updatedThreadList = new ArrayList<>(threadArray.size());
//            for (int i = 0; i < threadArray.size(); i++) {
//                final long key = threadArray.keyAt(i);
//                final ThreadRegistryModel model = threadArray.get(key);
//
//                if (model.getUnreadCount() > 0) {
//                    updatedThreadList.add(model);
//                }
//            }
//
//            return updatedThreadList;
//        }
//
//        return Collections.emptyList();
//    }
//
//    public void update(final String boardName, final long threadId, final int size, final boolean reset, final boolean bookmarked) {
//        Log.d(LOG_TAG, "[refresh] update called: id=" + threadId + ", size=" + size + ", reset=" + reset + ", bookmarked=" + bookmarked);
//
//        if (threadExists(threadId)) {
//            HistoryTableConnection.fetchPost(boardName, threadId)
//                    .compose(DatabaseUtils.applySchedulers())
//                    .doOnNext(history -> {
//                        final ThreadRegistryModel threadRegistryModel = threadArray.get(threadId);
//                        if (threadRegistryModel != null) {
//
//                            if (threadRegistryModel.getLastReadPosition() == 0) {
//                                threadRegistryModel.setLastReadPosition(history.lastReadPosition);
//                            }
//                            Log.d(LOG_TAG, "[refresh] Updating thread information: thread id=" + threadId + ", size=" + size + ", old size=" + threadRegistryModel.getThreadSize() + ", last read pos=" + threadRegistryModel.getLastReadPosition());
//                            if (reset) {
//                                threadRegistryModel.setThreadSize(size);
//                            }
//
//                            if (bookmarked) {
//                                updateUnreadCount(threadRegistryModel, size);
//                            } else {
//                                threadRegistryModel.setUnreadCount(0);
//                            }
//
//                            threadRegistryModel.setBookmarked(bookmarked);
//                            Log.d(LOG_TAG, "[refresh] Setting unread count: count=" + threadRegistryModel.getUnreadCount() + ", thread id=" + threadId);
//                            threadArray.put(threadId, threadRegistryModel);
//                        }
//                    })
//                    .doOnError(throwable -> Log.e(LOG_TAG, "Error updating thread registry from database", throwable))
//                    .subscribe();
//
//        }
////        }
//
//    }
//
//    public void update(final String boardName, final long threadId, final int size, final boolean bookmarked) {
//        Log.d(LOG_TAG, "[refresh] Calling update");
//        update(boardName, threadId, size, false, bookmarked);
//    }

//    public void clear() {
//        if (threadArray != null) {
//            threadArray.clear();
//        }
//    }

//    public int getUnreadCount(final long threadId, final int postCount) {
//        threadArray.get(threadId).setUnreadCount(postCount - threadArray.get(threadId).getThreadSize());
//        return threadArray.get(threadId).getUnreadCount();
//    }
//
//    public int getUnreadCount(final long threadId) {
//        if (threadArray.get(threadId) == null) {
//            return -1;
//        }
//        final ThreadRegistryModel threadRegistryModel = threadArray.get(threadId);
//        return threadRegistryModel.getUnreadCount();
//    }
//
//    public int getUnreadCount() {
//        int count = 0;
//        if (threadArray != null) {
//            for (int i = 0; i < threadArray.size(); i++) {
//                final long index = threadArray.keyAt(i);
//                if (threadArray.get(index).isBookmarked()) {
//                    final int unreadCount = threadArray.get(index).getUnreadCount();
//                    Log.d(LOG_TAG, "[refresh] Thread " + threadArray.get(index).getThreadId() + ": unread count=" + threadArray.get(index).getUnreadCount());
//                    if (unreadCount > 0)
//                        count += unreadCount;
//                }
//            }
//        }
//        return count;
//    }
//
//    public ThreadRegistryModel getThread(Long threadId) {
//        return threadArray.get(threadId);
//    }
}
