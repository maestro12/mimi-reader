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


public final class CloseTabEvent {
    private final int id;
    private final String boardName;
    private final String boardTitle;
    private final boolean closeOthers;

    public CloseTabEvent(int id, String boardName, String boardTitle, boolean closeOthers) {
        this.id = id;
        this.boardName = boardName;
        this.boardTitle = boardTitle;
        this.closeOthers = closeOthers;
    }

    public CloseTabEvent(boolean closeOthers) {
        this.id = -1;
        this.boardName = null;
        this.boardTitle = null;
        this.closeOthers = closeOthers;
    }

    public int getId() {
        return id;
    }

    public String getBoardName() {
        return boardName;
    }

    public String getBoardTitle() {
        return boardTitle;
    }

    public boolean isCloseOthers() {
        return closeOthers;
    }
}
