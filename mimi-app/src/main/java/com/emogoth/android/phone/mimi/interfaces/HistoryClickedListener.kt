package com.emogoth.android.phone.mimi.interfaces

interface HistoryClickedListener {
    fun onHistoryItemClicked(boardName: String, threadId: Long, boardTitle: String, position: Int, watched: Boolean)
    fun openHistoryPage(watched: Boolean)
}