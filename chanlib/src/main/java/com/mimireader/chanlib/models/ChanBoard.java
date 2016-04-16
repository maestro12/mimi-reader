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
    private Integer wsBoard;
    private Integer perPage;
    private Integer pages;
    private Integer maxFilesize;
    private Integer maxWebmFilesize;
    private Integer maxCommentChars;
    private Integer bumpLimit;
    private Integer imageLimit;
    private String metaDescription;
    private Integer isArchived;
    private Integer spoilers;
    private Integer customSpoilers;
    private Integer userIds;
    private Integer codeTags;
    private Integer countryFlags;
    private Integer sjisTags;
    private Integer mathTags;
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

    public Integer getWsBoard() {
        return wsBoard;
    }

    public void setWsBoard(Integer wsBoard) {
        this.wsBoard = wsBoard;
    }

    public Integer getPerPage() {
        return perPage;
    }

    public void setPerPage(Integer perPage) {
        this.perPage = perPage;
    }

    public Integer getPages() {
        return pages;
    }

    public void setPages(Integer pages) {
        this.pages = pages;
    }

    public Integer getMaxFilesize() {
        return maxFilesize;
    }

    public void setMaxFilesize(Integer maxFilesize) {
        this.maxFilesize = maxFilesize;
    }

    public Integer getMaxWebmFilesize() {
        return maxWebmFilesize;
    }

    public void setMaxWebmFilesize(Integer maxWebmFilesize) {
        this.maxWebmFilesize = maxWebmFilesize;
    }

    public Integer getMaxCommentChars() {
        return maxCommentChars;
    }

    public void setMaxCommentChars(Integer maxCommentChars) {
        this.maxCommentChars = maxCommentChars;
    }

    public Integer getBumpLimit() {
        return bumpLimit;
    }

    public void setBumpLimit(Integer bumpLimit) {
        this.bumpLimit = bumpLimit;
    }

    public Integer getImageLimit() {
        return imageLimit;
    }

    public void setImageLimit(Integer imageLimit) {
        this.imageLimit = imageLimit;
    }

    public String getMetaDescription() {
        return metaDescription;
    }

    public void setMetaDescription(String metaDescription) {
        this.metaDescription = metaDescription;
    }

    public Integer getIsArchived() {
        return isArchived;
    }

    public void setIsArchived(Integer isArchived) {
        this.isArchived = isArchived;
    }

    public Integer getSpoilers() {
        return spoilers;
    }

    public void setSpoilers(Integer spoilers) {
        this.spoilers = spoilers;
    }

    public Integer getCustomSpoilers() {
        return customSpoilers;
    }

    public void setCustomSpoilers(Integer customSpoilers) {
        this.customSpoilers = customSpoilers;
    }

    public Integer getUserIds() {
        return userIds;
    }

    public void setUserIds(Integer userIds) {
        this.userIds = userIds;
    }

    public Integer getCodeTags() {
        return codeTags;
    }

    public void setCodeTags(Integer codeTags) {
        this.codeTags = codeTags;
    }

    public Integer getCountryFlags() {
        return countryFlags;
    }

    public void setCountryFlags(Integer countryFlags) {
        this.countryFlags = countryFlags;
    }

    public Integer getSjisTags() {
        return sjisTags;
    }

    public void setSjisTags(Integer sjisTags) {
        this.sjisTags = sjisTags;
    }

    public Integer getMathTags() {
        return mathTags;
    }

    public void setMathTags(Integer mathTags) {
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
        this.wsBoard = (Integer) in.readValue(Integer.class.getClassLoader());
        this.perPage = (Integer) in.readValue(Integer.class.getClassLoader());
        this.pages = (Integer) in.readValue(Integer.class.getClassLoader());
        this.maxFilesize = (Integer) in.readValue(Integer.class.getClassLoader());
        this.maxWebmFilesize = (Integer) in.readValue(Integer.class.getClassLoader());
        this.maxCommentChars = (Integer) in.readValue(Integer.class.getClassLoader());
        this.bumpLimit = (Integer) in.readValue(Integer.class.getClassLoader());
        this.imageLimit = (Integer) in.readValue(Integer.class.getClassLoader());
        this.metaDescription = in.readString();
        this.isArchived = (Integer) in.readValue(Integer.class.getClassLoader());
        this.spoilers = (Integer) in.readValue(Integer.class.getClassLoader());
        this.customSpoilers = (Integer) in.readValue(Integer.class.getClassLoader());
        this.userIds = (Integer) in.readValue(Integer.class.getClassLoader());
        this.codeTags = (Integer) in.readValue(Integer.class.getClassLoader());
        this.countryFlags = (Integer) in.readValue(Integer.class.getClassLoader());
        this.sjisTags = (Integer) in.readValue(Integer.class.getClassLoader());
        this.mathTags = (Integer) in.readValue(Integer.class.getClassLoader());
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
