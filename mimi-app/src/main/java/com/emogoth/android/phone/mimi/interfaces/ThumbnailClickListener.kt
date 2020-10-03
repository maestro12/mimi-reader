package com.emogoth.android.phone.mimi.interfaces

import com.mimireader.chanlib.models.ChanPost

interface ThumbnailClickListener {
    fun onThumbnailClick(posts: List<ChanPost>, threadId: Long, position: Int, boardName: String)
}