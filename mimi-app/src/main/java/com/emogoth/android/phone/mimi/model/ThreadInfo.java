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

import com.emogoth.android.phone.mimi.autorefresh.GsonConverter;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import java.io.IOException;
import java.io.OutputStream;

public class ThreadInfo implements Parcelable {
    public static final String BUNDLE_KEY = "thread_bundle";

    @Expose
    public long threadId;
    @Expose
    public String boardName;
    @Expose
    public String boardTitle;
    @Expose
    public boolean watched;
    @Expose
    public long refreshTimestamp;

    public ThreadInfo() {

    }

    public ThreadInfo(final long threadId, final String boardName, final String boardTitle, final boolean watched) {
        this.threadId = threadId;
        this.boardName = boardName;
        this.boardTitle = boardTitle;
        this.watched = watched;
        this.refreshTimestamp = 0;
    }

    public ThreadInfo(final long threadId, final String boardName, final long lastRefreshTime, final boolean watched) {
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
        if (o == null || ((Object) this).getClass() != o.getClass()) return false;

        ThreadInfo that = (ThreadInfo) o;

        if (boardName == null || threadId == 0) return false;

        if (!boardName.equals(that.boardName)) return false;
        if (threadId != that.threadId) return false;

        return true;
    }

    @Override
    public int hashCode() {
        long result = threadId;
        result = 31 * result + boardName.hashCode();
        return (int) result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.threadId);
        dest.writeString(this.boardName);
        dest.writeString(this.boardTitle);
        dest.writeByte(watched ? (byte) 1 : (byte) 0);
        dest.writeLong(this.refreshTimestamp);
    }

    protected ThreadInfo(Parcel in) {
        this.threadId = in.readLong();
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

//    @Override
//    public void writeExternal(ObjectOutput out) throws IOException {
//        out.writeInt(this.threadId);
//        out.writeUTF(this.boardName);
//        out.writeUTF(this.boardTitle == null ? "" : boardTitle);
//        out.writeByte(this.watched ? 1 : 0);
//        out.writeLong(this.refreshTimestamp);
//    }
//
//    @Override
//    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
//        this.threadId = in.readInt();
//        this.boardName = in.readUTF();
//        this.boardTitle = in.readUTF();
//        this.watched = in.readByte() != 0;
//        this.refreshTimestamp = in.readLong();
//    }

    public static ThreadInfo from(byte[] bytes) throws IOException {
        Gson gson = new Gson();
        GsonConverter<ThreadInfo> converter = new GsonConverter<>(gson, ThreadInfo.class);
        return converter.from(bytes);
    }

    public void toStream(OutputStream bytes) throws IOException {
        Gson gson = new Gson();
        GsonConverter<ThreadInfo> converter = new GsonConverter<>(gson, ThreadInfo.class);
        converter.toStream(this, bytes);
    }
}
