package com.emogoth.android.phone.mimi.interfaces

interface ReplyClickListener {
    fun onReplyClicked(boardName: String, threadId: Long, id: Long, replies: List<String>)
}