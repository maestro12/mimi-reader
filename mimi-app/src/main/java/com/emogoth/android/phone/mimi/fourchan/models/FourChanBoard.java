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
public class FourChanBoard implements BoardConverter{
    @SerializedName("board")
    @Expose
    private String name;
    @SerializedName("title")
    @Expose
    private String title;
    @SerializedName("ws_board")
    @Expose
    private Integer wsBoard;
    @SerializedName("per_page")
    @Expose
    private Integer perPage;
    @SerializedName("pages")
    @Expose
    private Integer pages;
    @SerializedName("max_filesize")
    @Expose
    private Integer maxFilesize;
    @SerializedName("max_webm_filesize")
    @Expose
    private Integer maxWebmFilesize;
    @SerializedName("max_comment_chars")
    @Expose
    private Integer maxCommentChars;
    @SerializedName("bump_limit")
    @Expose
    private Integer bumpLimit;
    @SerializedName("image_limit")
    @Expose
    private Integer imageLimit;
    @SerializedName("meta_description")
    @Expose
    private String metaDescription;
    @SerializedName("is_archived")
    @Expose
    private Integer isArchived;
    @SerializedName("spoilers")
    @Expose
    private Integer spoilers;
    @SerializedName("custom_spoilers")
    @Expose
    private Integer customSpoilers;
    @SerializedName("user_ids")
    @Expose
    private Integer userIds;
    @SerializedName("code_tags")
    @Expose
    private Integer codeTags;
    @SerializedName("country_flags")
    @Expose
    private Integer countryFlags;
    @SerializedName("sjis_tags")
    @Expose
    private Integer sjisTags;
    @SerializedName("math_tags")
    @Expose
    private Integer mathTags;

    /**
     *
     * @return
     * The board
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @param board
     * The board
     */
    public void setName(String board) {
        this.name = board;
    }

    /**
     *
     * @return
     * The title
     */
    public String getTitle() {
        return title;
    }

    /**
     *
     * @param title
     * The title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     *
     * @return
     * The wsBoard
     */
    public Integer getWsBoard() {
        return wsBoard;
    }

    /**
     *
     * @param wsBoard
     * The ws_board
     */
    public void setWsBoard(Integer wsBoard) {
        this.wsBoard = wsBoard;
    }

    /**
     *
     * @return
     * The perPage
     */
    public Integer getPerPage() {
        return perPage;
    }

    /**
     *
     * @param perPage
     * The per_page
     */
    public void setPerPage(Integer perPage) {
        this.perPage = perPage;
    }

    /**
     *
     * @return
     * The pages
     */
    public Integer getPages() {
        return pages;
    }

    /**
     *
     * @param pages
     * The pages
     */
    public void setPages(Integer pages) {
        this.pages = pages;
    }

    /**
     *
     * @return
     * The maxFilesize
     */
    public Integer getMaxFilesize() {
        return maxFilesize;
    }

    /**
     *
     * @param maxFilesize
     * The max_filesize
     */
    public void setMaxFilesize(Integer maxFilesize) {
        this.maxFilesize = maxFilesize;
    }

    /**
     *
     * @return
     * The maxWebmFilesize
     */
    public Integer getMaxWebmFilesize() {
        return maxWebmFilesize;
    }

    /**
     *
     * @param maxWebmFilesize
     * The max_webm_filesize
     */
    public void setMaxWebmFilesize(Integer maxWebmFilesize) {
        this.maxWebmFilesize = maxWebmFilesize;
    }

    /**
     *
     * @return
     * The maxCommentChars
     */
    public Integer getMaxCommentChars() {
        return maxCommentChars;
    }

    /**
     *
     * @param maxCommentChars
     * The max_comment_chars
     */
    public void setMaxCommentChars(Integer maxCommentChars) {
        this.maxCommentChars = maxCommentChars;
    }

    /**
     *
     * @return
     * The bumpLimit
     */
    public Integer getBumpLimit() {
        return bumpLimit;
    }

    /**
     *
     * @param bumpLimit
     * The bump_limit
     */
    public void setBumpLimit(Integer bumpLimit) {
        this.bumpLimit = bumpLimit;
    }

    /**
     *
     * @return
     * The imageLimit
     */
    public Integer getImageLimit() {
        return imageLimit;
    }

    /**
     *
     * @param imageLimit
     * The image_limit
     */
    public void setImageLimit(Integer imageLimit) {
        this.imageLimit = imageLimit;
    }

    /**
     *
     * @return
     * The metaDescription
     */
    public String getMetaDescription() {
        return metaDescription;
    }

    /**
     *
     * @param metaDescription
     * The meta_description
     */
    public void setMetaDescription(String metaDescription) {
        this.metaDescription = metaDescription;
    }

    /**
     *
     * @return
     * The isArchived
     */
    public Integer getIsArchived() {
        return isArchived;
    }

    /**
     *
     * @param isArchived
     * The is_archived
     */
    public void setIsArchived(Integer isArchived) {
        this.isArchived = isArchived;
    }

    /**
     *
     * @return
     * The spoilers
     */
    public Integer getSpoilers() {
        return spoilers;
    }

    /**
     *
     * @param spoilers
     * The spoilers
     */
    public void setSpoilers(Integer spoilers) {
        this.spoilers = spoilers;
    }

    /**
     *
     * @return
     * The customSpoilers
     */
    public Integer getCustomSpoilers() {
        return customSpoilers;
    }

    /**
     *
     * @param customSpoilers
     * The custom_spoilers
     */
    public void setCustomSpoilers(Integer customSpoilers) {
        this.customSpoilers = customSpoilers;
    }

    /**
     *
     * @return
     * The userIds
     */
    public Integer getUserIds() {
        return userIds;
    }

    /**
     *
     * @param userIds
     * The user_ids
     */
    public void setUserIds(Integer userIds) {
        this.userIds = userIds;
    }

    /**
     *
     * @return
     * The codeTags
     */
    public Integer getCodeTags() {
        return codeTags;
    }

    /**
     *
     * @param codeTags
     * The code_tags
     */
    public void setCodeTags(Integer codeTags) {
        this.codeTags = codeTags;
    }

    /**
     *
     * @return
     * The countryFlags
     */
    public Integer getCountryFlags() {
        return countryFlags;
    }

    /**
     *
     * @param countryFlags
     * The country_flags
     */
    public void setCountryFlags(Integer countryFlags) {
        this.countryFlags = countryFlags;
    }

    /**
     *
     * @return
     * The sjisTags
     */
    public Integer getSjisTags() {
        return sjisTags;
    }

    /**
     *
     * @param sjisTags
     * The sjis_tags
     */
    public void setSjisTags(Integer sjisTags) {
        this.sjisTags = sjisTags;
    }

    /**
     *
     * @return
     * The mathTags
     */
    public Integer getMathTags() {
        return mathTags;
    }

    /**
     *
     * @param mathTags
     * The math_tags
     */
    public void setMathTags(Integer mathTags) {
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
