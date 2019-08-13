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

public class UpdateHistoryEvent {

    private final String boardName;
    private final long threadId;
    private final int threadSize;
    private final int lastReadPosition;
    private final boolean closed;
    private final boolean watched;

    public UpdateHistoryEvent(final long threadId, final String boardName, final int size, final int lastRead, final boolean closed, final boolean watched) {
        this.boardName = boardName;
        this.threadId = threadId;
        this.threadSize = size;
        this.lastReadPosition = lastRead;
        this.closed = closed;
        this.watched = watched;
    }

    public boolean isClosed() {
        return closed;
    }

    public String getBoardName() {
        return boardName;
    }

    public long getThreadId() {
        return threadId;
    }

    public int getThreadSize() {
        return threadSize;
    }

    public int getLastReadPosition() {
        return lastReadPosition;
    }

    public boolean isWatched() {
        return watched;
    }
}
