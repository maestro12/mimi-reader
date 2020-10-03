package com.mimireader.chanlib.models

import java.io.PrintWriter
import java.io.StringWriter

class ErrorChanThread(chanThread: ChanThread, val error: Throwable) : ChanThread() {

    init {
        boardName = chanThread.boardName
        threadId = chanThread.threadId
        posts.addAll(chanThread.posts)
    }

    override fun toString(): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        error.printStackTrace(pw)
        val trace: String = sw.toString()

        return "ErrorChanThread{board='$boardName',\ntitle='$boardTitle',\nthread id='$threadId',\npost count='${posts.size}',\nexception=${trace}}"
    }
}