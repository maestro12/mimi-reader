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

import android.os.Parcel;
import android.os.Parcelable;

public class ThreadInfo implements Parcelable {
    public static final String BUNDLE_KEY = "thread_bundle";

    public int threadId;
    public String boardName;
    public String boardTitle;
    public boolean watched;
    public long refreshTimestamp;

    public ThreadInfo(final int threadId, final String boardName, final String boardTitle, final boolean watched) {
        this.threadId = threadId;
        this.boardName = boardName;
        this.boardTitle = boardTitle;
        this.watched = watched;
        this.refreshTimestamp = 0;
    }

    public ThreadInfo(final int threadId, final String boardName, final long lastRefreshTime, final boolean watched) {
        this.threadId = threadId;
        this.boardName = boardName;
        this.boardTitle = null;
        this.watched = watched;
        this.refreshTimestamp = lastRefreshTime;
    }

    public void setTimestamp(final long timestamp) {
        this.refreshTimestamp = timestamp;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || ((Object)this).getClass() != o.getClass()) return false;

        ThreadInfo that = (ThreadInfo) o;

        if(boardName == null || threadId == 0) return false;

        if (!boardName.equals(that.boardName)) return false;
        if (threadId != that.threadId) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = threadId;
        result = 31 * result + boardName.hashCode();
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.threadId);
        dest.writeString(this.boardName);
        dest.writeString(this.boardTitle);
        dest.writeByte(watched ? (byte) 1 : (byte) 0);
        dest.writeLong(this.refreshTimestamp);
    }

    protected ThreadInfo(Parcel in) {
        this.threadId = in.readInt();
        this.boardName = in.readString();
        this.boardTitle = in.readString();
        this.watched = in.readByte() != 0;
        this.refreshTimestamp = in.readLong();
    }

    public static final Creator<ThreadInfo> CREATOR = new Creator<ThreadInfo>() {
        public ThreadInfo createFromParcel(Parcel source) {
            return new ThreadInfo(source);
        }

        public ThreadInfo[] newArray(int size) {
            return new ThreadInfo[size];
        }
    };
}
