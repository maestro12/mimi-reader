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

public class ChanBoard implements Parcelable {
    private String name;
    private String title;
    private int wsBoard;
    private int perPage;
    private int pages;
    private int maxFilesize;
    private int maxWebmFilesize;
    private int maxCommentChars;
    private int bumpLimit;
    private int imageLimit;
    private String metaDescription;
    private int isArchived;
    private int spoilers;
    private int customSpoilers;
    private int userIds;
    private int codeTags;
    private int countryFlags;
    private int sjisTags;
    private int mathTags;
    private boolean favorite;

    public String getName() {
        return name;
    }

    public void setName(String board) {
        this.name = board;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getWsBoard() {
        return wsBoard;
    }

    public void setWsBoard(int wsBoard) {
        this.wsBoard = wsBoard;
    }

    public int getPerPage() {
        return perPage;
    }

    public void setPerPage(int perPage) {
        this.perPage = perPage;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public int getMaxFilesize() {
        return maxFilesize;
    }

    public void setMaxFilesize(int maxFilesize) {
        this.maxFilesize = maxFilesize;
    }

    public int getMaxWebmFilesize() {
        return maxWebmFilesize;
    }

    public void setMaxWebmFilesize(int maxWebmFilesize) {
        this.maxWebmFilesize = maxWebmFilesize;
    }

    public int getMaxCommentChars() {
        return maxCommentChars;
    }

    public void setMaxCommentChars(int maxCommentChars) {
        this.maxCommentChars = maxCommentChars;
    }

    public int getBumpLimit() {
        return bumpLimit;
    }

    public void setBumpLimit(int bumpLimit) {
        this.bumpLimit = bumpLimit;
    }

    public int getImageLimit() {
        return imageLimit;
    }

    public void setImageLimit(int imageLimit) {
        this.imageLimit = imageLimit;
    }

    public String getMetaDescription() {
        return metaDescription;
    }

    public void setMetaDescription(String metaDescription) {
        this.metaDescription = metaDescription;
    }

    public int getIsArchived() {
        return isArchived;
    }

    public void setIsArchived(int isArchived) {
        this.isArchived = isArchived;
    }

    public int getSpoilers() {
        return spoilers;
    }

    public void setSpoilers(int spoilers) {
        this.spoilers = spoilers;
    }

    public int getCustomSpoilers() {
        return customSpoilers;
    }

    public void setCustomSpoilers(int customSpoilers) {
        this.customSpoilers = customSpoilers;
    }

    public int getUserIds() {
        return userIds;
    }

    public void setUserIds(int userIds) {
        this.userIds = userIds;
    }

    public int getCodeTags() {
        return codeTags;
    }

    public void setCodeTags(int codeTags) {
        this.codeTags = codeTags;
    }

    public int getCountryFlags() {
        return countryFlags;
    }

    public void setCountryFlags(int countryFlags) {
        this.countryFlags = countryFlags;
    }

    public int getSjisTags() {
        return sjisTags;
    }

    public void setSjisTags(int sjisTags) {
        this.sjisTags = sjisTags;
    }

    public int getMathTags() {
        return mathTags;
    }

    public void setMathTags(int mathTags) {
        this.mathTags = mathTags;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public boolean isFavorite() {
        return favorite;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeString(this.title);
        dest.writeValue(this.wsBoard);
        dest.writeValue(this.perPage);
        dest.writeValue(this.pages);
        dest.writeValue(this.maxFilesize);
        dest.writeValue(this.maxWebmFilesize);
        dest.writeValue(this.maxCommentChars);
        dest.writeValue(this.bumpLimit);
        dest.writeValue(this.imageLimit);
        dest.writeString(this.metaDescription);
        dest.writeValue(this.isArchived);
        dest.writeValue(this.spoilers);
        dest.writeValue(this.customSpoilers);
        dest.writeValue(this.userIds);
        dest.writeValue(this.codeTags);
        dest.writeValue(this.countryFlags);
        dest.writeValue(this.sjisTags);
        dest.writeValue(this.mathTags);
        dest.writeByte(favorite ? (byte) 1 : (byte) 0);
    }

    public ChanBoard() {
    }

    protected ChanBoard(Parcel in) {
        this.name = in.readString();
        this.title = in.readString();
        this.wsBoard = (int) in.readValue(int.class.getClassLoader());
        this.perPage = (int) in.readValue(int.class.getClassLoader());
        this.pages = (int) in.readValue(int.class.getClassLoader());
        this.maxFilesize = (int) in.readValue(int.class.getClassLoader());
        this.maxWebmFilesize = (int) in.readValue(int.class.getClassLoader());
        this.maxCommentChars = (int) in.readValue(int.class.getClassLoader());
        this.bumpLimit = (int) in.readValue(int.class.getClassLoader());
        this.imageLimit = (int) in.readValue(int.class.getClassLoader());
        this.metaDescription = in.readString();
        this.isArchived = (int) in.readValue(int.class.getClassLoader());
        this.spoilers = (int) in.readValue(int.class.getClassLoader());
        this.customSpoilers = (int) in.readValue(int.class.getClassLoader());
        this.userIds = (int) in.readValue(int.class.getClassLoader());
        this.codeTags = (int) in.readValue(int.class.getClassLoader());
        this.countryFlags = (int) in.readValue(int.class.getClassLoader());
        this.sjisTags = (int) in.readValue(int.class.getClassLoader());
        this.mathTags = (int) in.readValue(int.class.getClassLoader());
        this.favorite = in.readByte() != 0;
    }

    public static final Parcelable.Creator<ChanBoard> CREATOR = new Parcelable.Creator<ChanBoard>() {
        public ChanBoard createFromParcel(Parcel source) {
            return new ChanBoard(source);
        }

        public ChanBoard[] newArray(int size) {
            return new ChanBoard[size];
        }
    };
}
