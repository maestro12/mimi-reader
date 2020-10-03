package com.mimireader.chanlib.models

import com.google.gson.annotations.Expose

class ArchivedChanPost : ChanPost {
    constructor() {
        no = -1
    }

    constructor(other: ChanPost, mediaLink: String?, thumbLink: String?) : super(other) {
        this.mediaLink = mediaLink
        this.thumbLink = thumbLink
    }

    @JvmField
    @Expose
    var mediaLink: String? = null

    @JvmField
    @Expose
    var thumbLink: String? = null
}