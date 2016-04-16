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

package com.mimireader.chanlib.models;


import android.os.Parcel;
import android.os.Parcelable;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import static com.mimireader.chanlib.util.ChanUtil.getVariableOrEmptyString;

public class ChanThread implements Parcelable, Externalizable {

    static final long serialVersionUID = -5847345613624646217L;

    private List<ChanPost> posts = new ArrayList<>();

    private String boardName;
    private String boardTitle;
    private int threadId;

    public List<ChanPost> getPosts() {
        return posts;
    }

    public void setPosts(List<ChanPost> posts) {
        this.posts = posts;
    }

    public String getBoardName() {
        return boardName;
    }

    public void setBoardName(String boardName) {
        this.boardName = boardName;
    }

    public int getThreadId() {
        return threadId;
    }

    public void setThreadId(int threadId) {
        this.threadId = threadId;
    }

    public String getBoardTitle() {
        return boardTitle;
    }

    public void setBoardTitle(String boardTitle) {
        this.boardTitle = boardTitle;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(posts);
        dest.writeString(this.boardName);
        dest.writeString(this.boardTitle);
        dest.writeInt(this.threadId);
    }

    public ChanThread() {
    }

    protected ChanThread(Parcel in) {
        this.posts = in.createTypedArrayList(ChanPost.CREATOR);
        this.boardName = in.readString();
        this.boardTitle = in.readString();
        this.threadId = in.readInt();
    }

    public static final Parcelable.Creator<ChanThread> CREATOR = new Parcelable.Creator<ChanThread>() {
        public ChanThread createFromParcel(Parcel source) {
            return new ChanThread(source);
        }

        public ChanThread[] newArray(int size) {
            return new ChanThread[size];
        }
    };

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.posts = (List<ChanPost>) in.readObject();
        this.boardName = in.readUTF();
        this.boardTitle = in.readUTF();
        this.threadId = in.readInt();
    }

    @Override
    public void writeExternal(ObjectOutput dest) throws IOException {
        dest.writeObject(posts);
        dest.writeUTF(getVariableOrEmptyString(this.boardName));
        dest.writeUTF(getVariableOrEmptyString(this.boardTitle));
        dest.writeInt(this.threadId);
    }
}
