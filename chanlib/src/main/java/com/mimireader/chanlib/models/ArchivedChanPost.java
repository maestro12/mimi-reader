package com.mimireader.chanlib.models;

import androidx.annotation.Nullable;

import com.google.gson.annotations.Expose;

public class ArchivedChanPost extends ChanPost {
    public ArchivedChanPost() {
        this.no = -1;
    }
    public ArchivedChanPost(ChanPost other, @Nullable String mediaLink, @Nullable String thumbLink) {
        super(other);
        this.mediaLink = mediaLink;
        this.thumbLink = thumbLink;
    }

    @Nullable
    @Expose
    public String mediaLink;

    @Nullable
    @Expose
    public String thumbLink;
}
