package com.mimireader.chanlib.models

import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import com.google.gson.annotations.Expose
import java.util.*

open class ChanThread : Parcelable {

    @Expose
    var posts: MutableList<ChanPost> = ArrayList()

    @Expose
    var boardName: String = ""

    @Expose
    var boardTitle: String = ""

    @Expose
    var threadId: Long = -1

    constructor(parcel: Parcel) : this() {
        boardName = parcel.readString() ?: ""
        boardTitle = parcel.readString() ?: ""
        threadId = parcel.readLong()
    }

    constructor() {
        threadId = -1
    }

    constructor(boardName: String, threadId: Long, posts: List<ChanPost>) : this() {
        this.boardName = boardName
        this.threadId = threadId
        this.posts.addAll(posts)
    }

    constructor(thread: ChanThread) : this() {
        boardName = thread.boardName
        boardTitle = thread.boardTitle
        threadId = thread.threadId
        posts.addAll(thread.posts)
    }

    override fun toString(): String {
        return "ChanThread{" +
                "board='" + boardName + '\'' +
                ", title='" + boardTitle + '\'' +
                ", thread id='" + threadId + '\'' +
                ", post count='" + posts.size + '\'' +
                '}'
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(boardName)
        parcel.writeString(boardTitle)
        parcel.writeLong(threadId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {

        @JvmField
        val CREATOR : Parcelable.Creator<ChanThread> = object : Parcelable.Creator<ChanThread> {
            override fun createFromParcel(parcel: Parcel): ChanThread {
                return ChanThread(parcel)
            }

            override fun newArray(size: Int): Array<ChanThread?> {
                return arrayOfNulls(size)
            }
        }

        @JvmStatic
        fun empty(): ChanThread {
            return ChanThread("", -1, emptyList<ChanPost>())
        }

        @JvmStatic
        fun isEmpty(thread: ChanThread): Boolean {
            return TextUtils.isEmpty(thread.boardName) && thread.threadId == -1L && thread.posts.size == 0
        }
    }
}