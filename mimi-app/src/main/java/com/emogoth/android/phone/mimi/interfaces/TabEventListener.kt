package com.emogoth.android.phone.mimi.interfaces

interface TabEventListener {
    fun onTabClosed(id: Long, boardName: String, boardTitle: String, closeOthers: Boolean)
}