package com.emogoth.android.phone.mimi.interfaces

interface ThreadSelectedListener {
    fun onThreadSelected(boardName: String, threadId: Long, position: Int)
}