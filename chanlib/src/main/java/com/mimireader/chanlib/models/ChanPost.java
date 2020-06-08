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
import android.util.Log;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ChanPost implements Parcelable {
    private static final Boolean LOG_DEBUG = false;
    @Expose
    public long no;
    @Expose
    public boolean closed;
    @Expose
    public boolean sticky;
    @Expose
    public String now;
    @Expose
    public String name;
    @Expose
    public String com;
    public transient CharSequence comment;
    @Expose
    public String sub;
    public transient CharSequence subject;
    @Expose
    public String filename;
    @Expose
    public String ext;
    @Expose
    public int w;
    @Expose
    public int h;
    @Expose
    public int tnW;
    @Expose
    public int tnH;
    @Expose
    public String tim;
    @Expose
    public long time;
    @Expose
    public String md5;
    @Expose
    public int fsize;
    @Expose
    public int resto;
    @Expose
    public int bumplimit;
    @Expose
    public int imagelimit;
    @Expose
    public String semanticUrl;
    @Expose
    public int replies;
    @Expose
    public int images;
    @Expose
    public int omittedPosts;
    @Expose
    public int omittedImages;
    @Expose
    public String email;
    @Expose
    public String trip;
    @Expose
    public String id;
    @Expose
    public String capcode;
    @Expose
    public String country;
    @Expose
    public String countryName;
    @Expose
    public String trollCountry;
    @Expose
    public boolean watched;
    public transient CharSequence displayedName;
    @Expose
    public int spoiler;
    @Expose
    public int custom_spoiler;

    @Expose
    public ArrayList<String> repliesTo = new ArrayList<>();
    public ArrayList<ChanPost> repliesFrom = new ArrayList<>();

    public String humanReadableFileSize;

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
        this.trollCountry = other.trollCountry;
        this.displayedName = other.displayedName;
        this.repliesTo = other.repliesTo;
        this.repliesFrom = other.repliesFrom;
        this.watched = other.watched;
        this.humanReadableFileSize = other.humanReadableFileSize;
        this.spoiler = other.spoiler;
        this.custom_spoiler = other.custom_spoiler;
    }

    public long getNo() {
        return no;
    }

    public void setNo(long no) {
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

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
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

    public void setTrollCountry(String trollCountry) {
        this.trollCountry = trollCountry;
    }

    public String getTrollCountry() {
        return this.trollCountry;
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

    public int getSpoiler() {
        return spoiler;
    }

    public void setSpoiler(int spoiler){
        this.spoiler = spoiler;
    }

    public int getCustomSpoiler() {
        return custom_spoiler;
    }

    public void setCustomSpoiler(int customSpoiler) {
        this.custom_spoiler = customSpoiler;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChanPost chanPost = (ChanPost) o;

        if (no != chanPost.no) {
            if (LOG_DEBUG) {
                Log.d("ChanPost", "chanPost.no is different: old=" + no + ", new=" + chanPost.no);
            }
            return false;
        }
        if (closed != chanPost.closed) {
            if (LOG_DEBUG) {
                Log.d("ChanPost", "chanPost.closed is different: closed=" + closed + ", new=" + chanPost.closed);
            }
            return false;
        }
        if (sticky != chanPost.sticky) {
            return false;
        }
        if (w != chanPost.w) {
            return false;
        }
        if (h != chanPost.h) {
            return false;
        }
        if (tnW != chanPost.tnW) {
            return false;
        }
        if (tnH != chanPost.tnH) {
            return false;
        }
        if (time != chanPost.time) {
            return false;
        }
        if (fsize != chanPost.fsize) {
            return false;
        }
        if (resto != chanPost.resto) {
            return false;
        }
        if (bumplimit != chanPost.bumplimit) {
            return false;
        }
        if (imagelimit != chanPost.imagelimit) {
            return false;
        }
        if (replies != chanPost.replies) {
            return false;
        }
        if (images != chanPost.images) {
            return false;
        }
        if (omittedPosts != chanPost.omittedPosts) {
            return false;
        }
        if (omittedImages != chanPost.omittedImages) {
            return false;
        }
        if (watched != chanPost.watched) {
            return false;
        }
        if (!Objects.equals(now, chanPost.now)) {
            return false;
        }
        if (!Objects.equals(name, chanPost.name)) {
            return false;
        }
        if (!Objects.equals(com, chanPost.com)) {
            return false;
        }
        if (!Objects.equals(comment, chanPost.comment)) {
            return false;
        }
        if (!Objects.equals(sub, chanPost.sub)) {
            return false;
        }
        if (!Objects.equals(subject, chanPost.subject)) {
            return false;
        }
        if (!Objects.equals(filename, chanPost.filename)) {
            return false;
        }
        if (!Objects.equals(ext, chanPost.ext)) {
            return false;
        }
        if (!Objects.equals(tim, chanPost.tim)) {
            return false;
        }
        if (!Objects.equals(md5, chanPost.md5)) {
            if (LOG_DEBUG) {
                Log.d("ChanPost", "chanPost.md5 is different: old=" + md5 + ", new=" + chanPost.md5);
            }
            return false;
        }
        if (!Objects.equals(semanticUrl, chanPost.semanticUrl)) {
            return false;
        }
        if (!Objects.equals(email, chanPost.email)) {
            return false;
        }
        if (!Objects.equals(trip, chanPost.trip)) {
            if (LOG_DEBUG) {
                Log.d("ChanPost", "chanPost.trip is different: old=" + trip + ", new=" + chanPost.trip);
            }
            return false;
        }
        if (!Objects.equals(id, chanPost.id)) {
            if (LOG_DEBUG) {
                Log.d("ChanPost", "chanPost.id is different: old=" + id + ", new=" + chanPost.id);
            }
            return false;
        }
        if (!Objects.equals(capcode, chanPost.capcode)) {
            if (LOG_DEBUG) {
                Log.d("ChanPost", "chanPost.capcode is different: old=" + capcode + ", new=" + chanPost.capcode);
            }
            return false;
        }
        if (!Objects.equals(country, chanPost.country)) {
            if (LOG_DEBUG) {
                Log.d("ChanPost", "chanPost.country is different: old=" + country + ", new=" + chanPost.country);
            }
            return false;
        }
        if (!Objects.equals(countryName, chanPost.countryName)) {
            if (LOG_DEBUG) {
                Log.d("ChanPost", "chanPost.countryName is different: old=" + countryName + ", new=" + chanPost.countryName);
            }
            return false;
        }
        if (!Objects.equals(trollCountry, chanPost.trollCountry)) {
            if (LOG_DEBUG) {
                Log.d("ChanPost", "chanPost.trollCountry is different: old=" + trollCountry + ", new=" + chanPost.trollCountry);
            }
            return false;
        }
        if (!Objects.equals(displayedName, chanPost.displayedName)) {
            if (LOG_DEBUG) {
                Log.d("ChanPost", "chanPost.displayedName is different: old=" + displayedName + ", new=" + chanPost.displayedName);
            }
            return false;
        }
        return Objects.equals(humanReadableFileSize, chanPost.humanReadableFileSize);
    }

    @Override
    public int hashCode() {
        return (int) no;
    }

    public ChanPost() {
        no = -1;
    }

    public boolean empty() {
        return no == -1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.no);
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
        dest.writeLong(this.time);
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
        dest.writeInt(this.spoiler);
        dest.writeInt(this.custom_spoiler);
    }

    public ChanPost(Parcel in) {
        this.no = in.readLong();
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
        this.time = in.readLong();
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
        this.spoiler = in.readInt();
        this.custom_spoiler = in.readInt();
    }

    public static final Creator<ChanPost> CREATOR = new Creator<ChanPost>() {
        public ChanPost createFromParcel(Parcel source) {
            return new ChanPost(source);
        }

        public ChanPost[] newArray(int size) {
            return new ChanPost[size];
        }
    };

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
