package com.emogoth.android.phone.mimi.interfaces

import android.view.View
import com.mimireader.chanlib.models.ChanPost

interface PostItemClickListener {
    fun onPostItemClick(v: View?, posts: List<ChanPost>, position: Int, boardTitle: String, boardName: String, threadId: Long)
}