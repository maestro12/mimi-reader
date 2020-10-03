package com.emogoth.android.phone.mimi.event

data class ShowRepliesEvent(
    val boardName: String,
    var threadId: Long,
    var id: Long = -1,
    var replies: List<String> = emptyList()
)

