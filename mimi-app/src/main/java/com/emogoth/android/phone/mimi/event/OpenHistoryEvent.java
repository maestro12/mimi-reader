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

public class OpenHistoryEvent {
    public final boolean watched;
    public final String boardName;
    public final long threadId;
    public final int position;
    public final int unreadCount;

    public OpenHistoryEvent(String boardName, long threadId, boolean watched, int position, int unreadCount) {
        this.watched = watched;
        this.boardName = boardName;
        this.threadId = threadId;
        this.position = position;
        this.unreadCount = unreadCount;
    }

    public OpenHistoryEvent(boolean watched) {
        this.watched = watched;
        this.boardName = null;
        this.threadId = 0L;
        this.position = 0;
        this.unreadCount = 0;
    }
}
