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

import android.content.Context;
import android.text.Spannable;

import com.emogoth.android.phone.mimi.fourchan.FourChanCommentParser;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.mimireader.chanlib.interfaces.PostConverter;
import com.mimireader.chanlib.models.ChanPost;

import java.util.ArrayList;
import java.util.List;


public class FourChanPost implements PostConverter {
    @SerializedName("no")
    @Expose
    private int no;
    @SerializedName("closed")
    @Expose
    private int closed;
    @SerializedName("sticky")
    @Expose
    private int sticky;
    @SerializedName("now")
    @Expose
    private String now;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("com")
    @Expose
    private String com;
    @SerializedName("sub")
    @Expose
    private String sub;
    //    private Spanned subject;
    @SerializedName("filename")
    @Expose
    private String filename;
    @SerializedName("ext")
    @Expose
    private String ext;
    @SerializedName("w")
    @Expose
    private int w;
    @SerializedName("h")
    @Expose
    private int h;
    @SerializedName("tn_w")
    @Expose
    private int tnW;
    @SerializedName("tn_h")
    @Expose
    private int tnH;
    @SerializedName("tim")
    @Expose
    private String tim;
    @SerializedName("time")
    @Expose
    private int time;
    @SerializedName("md5")
    @Expose
    private String md5;
    @SerializedName("fsize")
    @Expose
    private int fsize;
    @SerializedName("resto")
    @Expose
    private int resto;
    @SerializedName("bumplimit")
    @Expose
    private int bumplimit;
    @SerializedName("imagelimit")
    @Expose
    private int imagelimit;
    @SerializedName("semantic_url")
    @Expose
    private String semanticUrl;
    @SerializedName("replies")
    @Expose
    private int replies;
    @SerializedName("images")
    @Expose
    private int images;
    @SerializedName("omitted_posts")
    @Expose
    private int omittedPosts;
    @SerializedName("omitted_images")
    @Expose
    private int omittedImages;
    @SerializedName("last_replies")
    @Expose
    private List<FourChanPost> lastReplies = new ArrayList<>();
    @Expose
    private String email;
    @Expose
    private String trip;
    @Expose
    private String id;
    @Expose
    private String capcode;
    @SerializedName("country")
    @Expose
    private String country;
    @SerializedName("country_name")
    @Expose
    private String countryName;

    private Spannable comment;

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

    public int getClosed() {
        return closed;
    }

    public void setClosed(int closed) {
        this.closed = closed;
    }

    public int getSticky() {
        return sticky;
    }

    public void setSticky(int sticky) {
        this.sticky = sticky;
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

    public int getW() {
        return w;
    }

    public void setW(int w) {
        this.w = w;
    }

    public int getH() {
        return h;
    }

    public void setH(int h) {
        this.h = h;
    }

    public int getTnW() {
        return tnW;
    }

    public void setTnW(int tnW) {
        this.tnW = tnW;
    }

    public int getTnH() {
        return tnH;
    }

    public void setTnH(int tnH) {
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

    public int getFileSize() {
        return fsize;
    }

    public void setFileSize(int fsize) {
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

    public List<FourChanPost> getLastReplies() {
        return lastReplies;
    }

    public void setLastReplies(List<FourChanPost> lastReplies) {
        this.lastReplies = lastReplies;
    }

    public Spannable getComment() {
        return comment;
    }

    public void setComment(Spannable comment) {
        this.comment = comment;
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

//    public Spanned getSubject() {
//        return subject;
//    }
//
//    public void setSubject(Spanned subject) {
//        this.subject = subject;
//    }

    @Override
    public ChanPost toPost() {
        ChanPost post = new ChanPost();
        post.setNo(no);
        post.setClosed(closed == 1);
        post.setSticky(sticky == 1);
        post.setBumplimit(bumplimit);
        post.setCom(com);
        post.setSub(sub);
        post.setName(name);
        post.setExt(ext);
        post.setFilename(filename);
        post.setFsize(fsize);
        post.setHeight(h);
        post.setWidth(w);
        post.setThumbnailHeight(tnH);
        post.setThumbnailWidth(tnW);
        post.setImagelimit(imagelimit);
        post.setImages(images);
        post.setReplies(replies);
        post.setResto(resto);
        post.setOmittedImages(omittedImages);
        post.setOmittedPosts(omittedPosts);
        post.setSemanticUrl(semanticUrl);
        post.setMd5(md5);
        post.setTim(tim);
        post.setTime(time);

        post.setEmail(email);
        post.setTrip(trip);
        post.setId(id);
        post.setCapcode(capcode);
        post.setCountry(country);
        post.setCountryName(countryName);

        return post;
    }

    public void processComment(Context context, String boardName, int threadId) {
        if (com != null) {
            FourChanCommentParser.Builder parserBuilder = new FourChanCommentParser.Builder();
            parserBuilder.setContext(context)
                    .setBoardName(boardName)
                    .setThreadId(threadId)
                    .setComment(com)
                    .setQuoteColor(MimiUtil.getInstance().getQuoteColor())
                    .setReplyColor(MimiUtil.getInstance().getReplyColor())
                    .setHighlightColor(MimiUtil.getInstance().getHighlightColor())
                    .setLinkColor(MimiUtil.getInstance().getLinkColor());
            ;

            comment = parserBuilder.build().parse();
        }
    }
}

