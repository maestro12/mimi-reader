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
import java.util.Comparator;
import java.util.List;

import static com.mimireader.chanlib.util.ChanUtil.getVariableOrEmptyString;

public class ChanPost implements Parcelable, Externalizable {

    static final long serialVersionUID = -5847345613624646217L;

    private int no;
    private boolean closed;
    private boolean sticky;
    private String now;
    private String name;
    private String com;
    private transient CharSequence comment;
    private String sub;
    private transient CharSequence subject;
    private String filename;
    private String ext;
    private int w;
    private int h;
    private int tnW;
    private int tnH;
    private String tim;
    private int time;
    private String md5;
    private int fsize;
    private int resto;
    private int bumplimit;
    private int imagelimit;
    private String semanticUrl;
    private int replies;
    private int images;
    private int omittedPosts;
    private int omittedImages;
    private String email;
    private String trip;
    private String id;
    private String capcode;
    private String country;
    private String countryName;
    private boolean watched;
    private transient CharSequence displayedName;

    private ArrayList<String> repliesTo = new ArrayList<>();
    private ArrayList<ChanPost> repliesFrom = new ArrayList<>();

    private String humanReadableFileSize;

    public ChanPost(ChanPost other) {
        this.no = other.no;
        this.closed = other.closed;
        this.sticky = other.sticky;
        this.now = other.now;
        this.name = other.name;
        this.com = other.com;
        this.comment = other.comment;
        this.sub = other.sub;
        this.subject = other.subject;
        this.filename = other.filename;
        this.ext = other.ext;
        this.w = other.w;
        this.h = other.h;
        this.tnW = other.tnW;
        this.tnH = other.tnH;
        this.tim = other.tim;
        this.time = other.time;
        this.md5 = other.md5;
        this.fsize = other.fsize;
        this.resto = other.resto;
        this.bumplimit = other.bumplimit;
        this.imagelimit = other.imagelimit;
        this.semanticUrl = other.semanticUrl;
        this.replies = other.replies;
        this.images = other.images;
        this.omittedPosts = other.omittedPosts;
        this.omittedImages = other.omittedImages;
        this.email = other.email;
        this.trip = other.trip;
        this.id = other.id;
        this.capcode = other.capcode;
        this.country = other.country;
        this.countryName = other.countryName;
        this.displayedName = other.displayedName;
        this.repliesTo = other.repliesTo;
        this.repliesFrom = other.repliesFrom;
        this.watched = other.watched;
        this.humanReadableFileSize = other.humanReadableFileSize;
    }

    public int getNo() {
        return no;
    }

    public void setNo(int no) {
        this.no = no;
    }

    public String getNow() {
        return now;
    }

    public void setNow(String now) {
        this.now = now;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCom() {
        return com;
    }

    public void setCom(String com) {
        this.com = com;
    }

    public CharSequence getComment() {
        return comment;
    }

    public void setComment(CharSequence comment) {
        this.comment = comment;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public int getWidth() {
        return w;
    }

    public void setWidth(int w) {
        this.w = w;
    }

    public int getHeight() {
        return h;
    }

    public void setHeight(int h) {
        this.h = h;
    }

    public int getThumbnailWidth() {
        return tnW;
    }

    public void setThumbnailWidth(int tnW) {
        this.tnW = tnW;
    }

    public int getThumbnailHeight() {
        return tnH;
    }

    public void setThumbnailHeight(int tnH) {
        this.tnH = tnH;
    }

    public String getTim() {
        return tim;
    }

    public void setTim(String tim) {
        this.tim = tim;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public int getFsize() {
        return fsize;
    }

    public void setFsize(int fsize) {
        this.fsize = fsize;
    }

    public int getResto() {
        return resto;
    }

    public void setResto(int resto) {
        this.resto = resto;
    }

    public int getBumplimit() {
        return bumplimit;
    }

    public void setBumplimit(int bumplimit) {
        this.bumplimit = bumplimit;
    }

    public int getImagelimit() {
        return imagelimit;
    }

    public void setImagelimit(int imagelimit) {
        this.imagelimit = imagelimit;
    }

    public String getSemanticUrl() {
        return semanticUrl;
    }

    public void setSemanticUrl(String semanticUrl) {
        this.semanticUrl = semanticUrl;
    }

    public int getReplies() {
        return replies;
    }

    public void setReplies(int replies) {
        this.replies = replies;
    }

    public int getImages() {
        return images;
    }

    public void setImages(int images) {
        this.images = images;
    }

    public int getOmittedPosts() {
        return omittedPosts;
    }

    public void setOmittedPosts(int omittedPosts) {
        this.omittedPosts = omittedPosts;
    }

    public int getOmittedImages() {
        return omittedImages;
    }

    public void setOmittedImages(int omittedImages) {
        this.omittedImages = omittedImages;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTrip() {
        return trip;
    }

    public void setTrip(String trip) {
        this.trip = trip;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCapcode() {
        return capcode;
    }

    public void setCapcode(String capcode) {
        this.capcode = capcode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public CharSequence getSubject() {
        return subject;
    }

    public void setSubject(CharSequence subject) {
        this.subject = subject;
    }

    public List<String> getRepliesTo() {
        return repliesTo;
    }

    public void setRepliesTo(ArrayList<String> repliesTo) {
        this.repliesTo = repliesTo;
    }

    public ArrayList<ChanPost> getRepliesFrom() {
        return repliesFrom;
    }

    public void addReplyFrom(ChanPost post) {
        repliesFrom.add(post);
    }

    public void setRepliesFrom(ArrayList<ChanPost> repliesFrom) {
        this.repliesFrom = repliesFrom;
    }

    public CharSequence getDisplayedName() {
        return displayedName;
    }

    public void setDisplayedName(CharSequence displayedName) {
        this.displayedName = displayedName;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public boolean isSticky() {
        return sticky;
    }

    public void setSticky(boolean sticky) {
        this.sticky = sticky;
    }

    public void setWatched(boolean watched) {
        this.watched = watched;
    }

    public boolean isWatched() {
        return watched;
    }

    public String getHumanReadableFileSize() {
        return humanReadableFileSize;
    }

    public void setHumanReadableFileSize(String humanReadableFileSize) {
        this.humanReadableFileSize = humanReadableFileSize;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ChanPost && getNo() == ((ChanPost) o).getNo();
    }

    @Override
    public int hashCode() {
        return no;
    }

    public ChanPost() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.no);
        dest.writeByte(closed ? (byte) 1 : (byte) 0);
        dest.writeByte(sticky ? (byte) 1 : (byte) 0);
        dest.writeByte(watched ? (byte) 1 : (byte) 0);
        dest.writeString(this.now);
        dest.writeString(this.name);
        dest.writeString(this.com);
        dest.writeString(this.sub);
        dest.writeString(this.filename);
        dest.writeString(this.ext);
        dest.writeInt(this.w);
        dest.writeInt(this.h);
        dest.writeInt(this.tnW);
        dest.writeInt(this.tnH);
        dest.writeString(this.tim);
        dest.writeInt(this.time);
        dest.writeString(this.md5);
        dest.writeInt(this.fsize);
        dest.writeInt(this.resto);
        dest.writeInt(this.bumplimit);
        dest.writeInt(this.imagelimit);
        dest.writeString(this.semanticUrl);
        dest.writeInt(this.replies);
        dest.writeInt(this.images);
        dest.writeInt(this.omittedPosts);
        dest.writeInt(this.omittedImages);
        dest.writeString(this.email);
        dest.writeString(this.trip);
        dest.writeString(this.id);
        dest.writeString(this.capcode);
        dest.writeString(this.country);
        dest.writeString(this.countryName);
        dest.writeStringList(this.repliesTo);
        dest.writeTypedList(repliesFrom);
        dest.writeString(this.humanReadableFileSize);
    }

    protected ChanPost(Parcel in) {
        this.no = in.readInt();
        this.closed = in.readByte() != 0;
        this.sticky = in.readByte() != 0;
        this.watched = in.readByte() != 0;
        this.now = in.readString();
        this.name = in.readString();
        this.com = in.readString();
        this.sub = in.readString();
        this.filename = in.readString();
        this.ext = in.readString();
        this.w = in.readInt();
        this.h = in.readInt();
        this.tnW = in.readInt();
        this.tnH = in.readInt();
        this.tim = in.readString();
        this.time = in.readInt();
        this.md5 = in.readString();
        this.fsize = in.readInt();
        this.resto = in.readInt();
        this.bumplimit = in.readInt();
        this.imagelimit = in.readInt();
        this.semanticUrl = in.readString();
        this.replies = in.readInt();
        this.images = in.readInt();
        this.omittedPosts = in.readInt();
        this.omittedImages = in.readInt();
        this.email = in.readString();
        this.trip = in.readString();
        this.id = in.readString();
        this.capcode = in.readString();
        this.country = in.readString();
        this.countryName = in.readString();
        this.repliesTo = in.createStringArrayList();
        this.repliesFrom = in.createTypedArrayList(ChanPost.CREATOR);
        this.humanReadableFileSize = in.readString();
    }

    public static final Creator<ChanPost> CREATOR = new Creator<ChanPost>() {
        public ChanPost createFromParcel(Parcel source) {
            return new ChanPost(source);
        }

        public ChanPost[] newArray(int size) {
            return new ChanPost[size];
        }
    };

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.no = in.readInt();
        this.closed = in.readByte() != 0;
        this.sticky = in.readByte() != 0;
        this.now = in.readUTF();
        this.name = in.readUTF();
        this.com = in.readUTF();
        this.sub = in.readUTF();
        this.filename = in.readUTF();
        this.ext = in.readUTF();
        this.w = in.readInt();
        this.h = in.readInt();
        this.tnW = in.readInt();
        this.tnH = in.readInt();
        this.tim = in.readUTF();
        this.time = in.readInt();
        this.md5 = in.readUTF();
        this.fsize = in.readInt();
        this.resto = in.readInt();
        this.bumplimit = in.readInt();
        this.imagelimit = in.readInt();
        this.semanticUrl = in.readUTF();
        this.replies = in.readInt();
        this.images = in.readInt();
        this.omittedPosts = in.readInt();
        this.omittedImages = in.readInt();
        this.email = in.readUTF();
        this.trip = in.readUTF();
        this.id = in.readUTF();
        this.capcode = in.readUTF();
        this.country = in.readUTF();
        this.countryName = in.readUTF();
        this.repliesTo = new ArrayList<>();
        this.repliesFrom = new ArrayList<>();
        this.humanReadableFileSize = in.readUTF();
    }

    @Override
    public void writeExternal(ObjectOutput dest) throws IOException {
        dest.writeInt(this.no);
        dest.writeByte(closed ? (byte) 1 : (byte) 0);
        dest.writeByte(sticky ? (byte) 1 : (byte) 0);
        dest.writeUTF(getVariableOrEmptyString(this.now));
        dest.writeUTF(getVariableOrEmptyString(this.name));
        dest.writeUTF(getVariableOrEmptyString(this.com));
        dest.writeUTF(getVariableOrEmptyString(this.sub));
        dest.writeUTF(getVariableOrEmptyString(this.filename));
        dest.writeUTF(getVariableOrEmptyString(this.ext));
        dest.writeInt(this.w);
        dest.writeInt(this.h);
        dest.writeInt(this.tnW);
        dest.writeInt(this.tnH);
        dest.writeUTF(getVariableOrEmptyString(this.tim));
        dest.writeInt(this.time);
        dest.writeUTF(getVariableOrEmptyString(this.md5));
        dest.writeInt(this.fsize);
        dest.writeInt(this.resto);
        dest.writeInt(this.bumplimit);
        dest.writeInt(this.imagelimit);
        dest.writeUTF(getVariableOrEmptyString(this.semanticUrl));
        dest.writeInt(this.replies);
        dest.writeInt(this.images);
        dest.writeInt(this.omittedPosts);
        dest.writeInt(this.omittedImages);
        dest.writeUTF(getVariableOrEmptyString(this.email));
        dest.writeUTF(getVariableOrEmptyString(this.trip));
        dest.writeUTF(getVariableOrEmptyString(this.id));
        dest.writeUTF(getVariableOrEmptyString(this.capcode));
        dest.writeUTF(getVariableOrEmptyString(this.country));
        dest.writeUTF(getVariableOrEmptyString(this.countryName));
//        dest.writeObject(repliesTo);
//        dest.writeObject(repliesFrom);
        dest.writeUTF(getVariableOrEmptyString(this.humanReadableFileSize));
    }

    public static class ThreadIdComparator implements Comparator<ChanPost> {
        @Override
        public int compare(ChanPost o1, ChanPost o2) {
            return o1.no < o2.no ? 1 : (o1.no > o2.no ? -1 : 0);
        }
    }

    public static class ImageCountComparator implements Comparator<ChanPost> {
        @Override
        public int compare(ChanPost o1, ChanPost o2) {
            return o1.images < o2.images ? 1 : (o1.images > o2.images ? -1 : 0);
        }
    }

    public static class ReplyCountComparator implements Comparator<ChanPost> {
        @Override
        public int compare(ChanPost o1, ChanPost o2) {
            return o1.replies < o2.replies ? 1 : (o1.replies > o2.replies ? -1 : 0);
        }
    }
}
