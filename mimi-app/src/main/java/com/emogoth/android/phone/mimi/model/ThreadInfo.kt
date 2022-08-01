package com.emogoth.android.phone.mimi.model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose

class ThreadInfo : Parcelable {

    @Expose
    var threadId: Long = 0

    @Expose
    var boardName: String = ""

    @Expose
    var boardTitle: String = ""

    @Expose
    var watched = false

    @Expose
    var refreshTimestamp: Long = 0

    constructor() {}
    constructor(threadId: Long, boardName: String, boardTitle: String, watched: Boolean) {
        this.threadId = threadId
        this.boardName = boardName
        this.boardTitle = boardTitle
        this.watched = watched
        refreshTimestamp = 0
    }

    constructor(threadId: Long, boardName: String, boardTitle: String, watched: Boolean, lastRefreshTime: Long) {
        this.threadId = threadId
        this.boardName = boardName
        this.boardTitle = boardTitle
        this.watched = watched
        this.refreshTimestamp = lastRefreshTime
    }

    constructor(threadId: Long, boardName: String, lastRefreshTime: Long, watched: Boolean) {
        this.threadId = threadId
        this.boardName = boardName
        boardTitle = ""
        this.watched = watched
        refreshTimestamp = lastRefreshTime
    }

    fun setTimestamp(timestamp: Long) {
        refreshTimestamp = timestamp
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || (this as Any).javaClass != o.javaClass) return false
        val that = o as ThreadInfo
        if (threadId == 0L) return false
        if (boardName != that.boardName) return false
        return if (threadId != that.threadId) false else true
    }

    override fun hashCode(): Int {
        var result = threadId
        result = 31 * result + boardName.hashCode()
        return result.toInt()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(threadId)
        dest.writeString(boardName)
        dest.writeString(boardTitle)
        dest.writeByte(if (watched) 1.toByte() else 0.toByte())
        dest.writeLong(refreshTimestamp)
    }

    constructor(input: Parcel) : this(
            threadId = input.readLong(),
            boardName = input.readString() ?: "",
            boardTitle = input.readString() ?: "",
            watched = input.readByte().toInt() != 0,
            lastRefreshTime = input.readLong())

    companion object {
        const val BUNDLE_KEY = "thread_bundle"

        @JvmField
        val CREATOR: Parcelable.Creator<ThreadInfo> = object : Parcelable.Creator<ThreadInfo> {
            override fun createFromParcel(source: Parcel): ThreadInfo? {
                return ThreadInfo(source)
            }

            override fun newArray(size: Int): Array<ThreadInfo?> {
                return arrayOfNulls(size)
            }
        }
    }
}