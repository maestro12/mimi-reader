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

package com.emogoth.android.phone.mimi.fourchan.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mimireader.chanlib.interfaces.BoardConverter;
import com.mimireader.chanlib.models.ChanBoard;

/**
 * Generated from jsonschema2pojo.org
 */
public class FourChanBoard implements BoardConverter {
    @SerializedName("board")
    @Expose
    private String name;
    @SerializedName("title")
    @Expose
    private String title;
    @SerializedName("ws_board")
    @Expose
    private int wsBoard;
    @SerializedName("per_page")
    @Expose
    private int perPage;
    @SerializedName("pages")
    @Expose
    private int pages;
    @SerializedName("max_filesize")
    @Expose
    private int maxFilesize;
    @SerializedName("max_webm_filesize")
    @Expose
    private int maxWebmFilesize;
    @SerializedName("max_comment_chars")
    @Expose
    private int maxCommentChars;
    @SerializedName("bump_limit")
    @Expose
    private int bumpLimit;
    @SerializedName("image_limit")
    @Expose
    private int imageLimit;
    @SerializedName("meta_description")
    @Expose
    private String metaDescription;
    @SerializedName("is_archived")
    @Expose
    private int isArchived;
    @SerializedName("spoilers")
    @Expose
    private int spoilers;
    @SerializedName("custom_spoilers")
    @Expose
    private int customSpoilers;
    @SerializedName("user_ids")
    @Expose
    private int userIds;
    @SerializedName("code_tags")
    @Expose
    private int codeTags;
    @SerializedName("country_flags")
    @Expose
    private int countryFlags;
    @SerializedName("sjis_tags")
    @Expose
    private int sjisTags;
    @SerializedName("math_tags")
    @Expose
    private int mathTags;

    /**
     * @return The board
     */
    public String getName() {
        return name;
    }

    /**
     * @param board The board
     */
    public void setName(String board) {
        this.name = board;
    }

    /**
     * @return The title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title The title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return The wsBoard
     */
    public int getWsBoard() {
        return wsBoard;
    }

    /**
     * @param wsBoard The ws_board
     */
    public void setWsBoard(int wsBoard) {
        this.wsBoard = wsBoard;
    }

    /**
     * @return The perPage
     */
    public int getPerPage() {
        return perPage;
    }

    /**
     * @param perPage The per_page
     */
    public void setPerPage(int perPage) {
        this.perPage = perPage;
    }

    /**
     * @return The pages
     */
    public int getPages() {
        return pages;
    }

    /**
     * @param pages The pages
     */
    public void setPages(int pages) {
        this.pages = pages;
    }

    /**
     * @return The maxFilesize
     */
    public int getMaxFilesize() {
        return maxFilesize;
    }

    /**
     * @param maxFilesize The max_filesize
     */
    public void setMaxFilesize(int maxFilesize) {
        this.maxFilesize = maxFilesize;
    }

    /**
     * @return The maxWebmFilesize
     */
    public int getMaxWebmFilesize() {
        return maxWebmFilesize;
    }

    /**
     * @param maxWebmFilesize The max_webm_filesize
     */
    public void setMaxWebmFilesize(int maxWebmFilesize) {
        this.maxWebmFilesize = maxWebmFilesize;
    }

    /**
     * @return The maxCommentChars
     */
    public int getMaxCommentChars() {
        return maxCommentChars;
    }

    /**
     * @param maxCommentChars The max_comment_chars
     */
    public void setMaxCommentChars(int maxCommentChars) {
        this.maxCommentChars = maxCommentChars;
    }

    /**
     * @return The bumpLimit
     */
    public int getBumpLimit() {
        return bumpLimit;
    }

    /**
     * @param bumpLimit The bump_limit
     */
    public void setBumpLimit(int bumpLimit) {
        this.bumpLimit = bumpLimit;
    }

    /**
     * @return The imageLimit
     */
    public int getImageLimit() {
        return imageLimit;
    }

    /**
     * @param imageLimit The image_limit
     */
    public void setImageLimit(int imageLimit) {
        this.imageLimit = imageLimit;
    }

    /**
     * @return The metaDescription
     */
    public String getMetaDescription() {
        return metaDescription;
    }

    /**
     * @param metaDescription The meta_description
     */
    public void setMetaDescription(String metaDescription) {
        this.metaDescription = metaDescription;
    }

    /**
     * @return The isArchived
     */
    public int getIsArchived() {
        return isArchived;
    }

    /**
     * @param isArchived The is_archived
     */
    public void setIsArchived(int isArchived) {
        this.isArchived = isArchived;
    }

    /**
     * @return The spoilers
     */
    public int getSpoilers() {
        return spoilers;
    }

    /**
     * @param spoilers The spoilers
     */
    public void setSpoilers(int spoilers) {
        this.spoilers = spoilers;
    }

    /**
     * @return The customSpoilers
     */
    public int getCustomSpoilers() {
        return customSpoilers;
    }

    /**
     * @param customSpoilers The custom_spoilers
     */
    public void setCustomSpoilers(int customSpoilers) {
        this.customSpoilers = customSpoilers;
    }

    /**
     * @return The userIds
     */
    public int getUserIds() {
        return userIds;
    }

    /**
     * @param userIds The user_ids
     */
    public void setUserIds(int userIds) {
        this.userIds = userIds;
    }

    /**
     * @return The codeTags
     */
    public int getCodeTags() {
        return codeTags;
    }

    /**
     * @param codeTags The code_tags
     */
    public void setCodeTags(int codeTags) {
        this.codeTags = codeTags;
    }

    /**
     * @return The countryFlags
     */
    public int getCountryFlags() {
        return countryFlags;
    }

    /**
     * @param countryFlags The country_flags
     */
    public void setCountryFlags(int countryFlags) {
        this.countryFlags = countryFlags;
    }

    /**
     * @return The sjisTags
     */
    public int getSjisTags() {
        return sjisTags;
    }

    /**
     * @param sjisTags The sjis_tags
     */
    public void setSjisTags(int sjisTags) {
        this.sjisTags = sjisTags;
    }

    /**
     * @return The mathTags
     */
    public int getMathTags() {
        return mathTags;
    }

    /**
     * @param mathTags The math_tags
     */
    public void setMathTags(int mathTags) {
        this.mathTags = mathTags;
    }

    @Override
    public ChanBoard toBoard() {
        ChanBoard board = new ChanBoard();
        board.setName(name);
        board.setTitle(title);
        board.setBumpLimit(bumpLimit);
        board.setCodeTags(codeTags);
        board.setCountryFlags(countryFlags);
        board.setImageLimit(imageLimit);
        board.setIsArchived(isArchived);
        board.setMathTags(mathTags);
        board.setMaxFilesize(maxFilesize);
        board.setMaxCommentChars(maxCommentChars);
        board.setMetaDescription(metaDescription);
        board.setPages(pages);
        board.setPerPage(perPage);
        board.setSjisTags(sjisTags);
        board.setCodeTags(codeTags);
        board.setWsBoard(wsBoard);
        return board;
    }
}
