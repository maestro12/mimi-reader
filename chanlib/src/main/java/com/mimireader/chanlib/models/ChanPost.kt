package com.mimireader.chanlib.models

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import com.google.gson.annotations.Expose
import java.util.*
import kotlin.collections.ArrayList

open class ChanPost : Parcelable {

    @Expose
    var no: Long

    @Expose
    var isClosed = false

    @Expose
    var isSticky = false

    @Expose
    var now: String? = null

    @Expose
    var name: String? = null

    @Expose
    var com: String? = null

    @Transient
    var comment: CharSequence? = null

    @Expose
    var sub: String? = null

    @Transient
    var subject: CharSequence? = null

    @Expose
    var filename: String? = null

    @Expose
    var ext: String? = null

    @Expose
    var width = 0

    @Expose
    var height = 0

    @Expose
    var thumbnailWidth = 0

    @Expose
    var thumbnailHeight = 0

    @Expose
    var tim: String? = null

    @Expose
    var time: Long = 0

    @Expose
    var md5: String? = null

    @Expose
    var fsize = 0

    @Expose
    var resto = 0

    @Expose
    var bumplimit = 0

    @Expose
    var imagelimit = 0

    @Expose
    var semanticUrl: String? = null

    @Expose
    var replies = 0

    @Expose
    var images = 0

    @Expose
    var omittedPosts = 0

    @Expose
    var omittedImages = 0

    @Expose
    var email: String? = null

    @Expose
    var trip: String? = null

    @Expose
    var id: String? = null

    @Expose
    var capcode: String? = null

    @Expose
    var country: String? = null

    @Expose
    var countryName: String? = null

    @Expose
    var trollCountry: String? = null

    @Expose
    var isWatched = false

    @Transient
    var displayedName: CharSequence? = null

    @JvmField
    @Expose
    var spoiler = 0

    @Expose
    var customSpoiler = 0

    @Expose
    var repliesTo: ArrayList<String> = ArrayList()
    var repliesFrom: ArrayList<ChanPost> = ArrayList()
    var humanReadableFileSize: String? = null

    constructor(other: ChanPost) {
        no = other.no
        isClosed = other.isClosed
        isSticky = other.isSticky
        now = other.now
        name = other.name
        com = other.com
        comment = other.comment
        sub = other.sub
        subject = other.subject
        filename = other.filename
        ext = other.ext
        width = other.width
        height = other.height
        thumbnailWidth = other.thumbnailWidth
        thumbnailHeight = other.thumbnailHeight
        tim = other.tim
        time = other.time
        md5 = other.md5
        fsize = other.fsize
        resto = other.resto
        bumplimit = other.bumplimit
        imagelimit = other.imagelimit
        semanticUrl = other.semanticUrl
        replies = other.replies
        images = other.images
        omittedPosts = other.omittedPosts
        omittedImages = other.omittedImages
        email = other.email
        trip = other.trip
        id = other.id
        capcode = other.capcode
        country = other.country
        countryName = other.countryName
        trollCountry = other.trollCountry
        displayedName = other.displayedName
        repliesTo = other.repliesTo
        repliesFrom = other.repliesFrom
        isWatched = other.isWatched
        humanReadableFileSize = other.humanReadableFileSize
        spoiler = other.spoiler
        customSpoiler = other.customSpoiler
    }

//    fun getRepliesTo(): List<String>? {
//        return repliesTo
//    }
//
//    fun setRepliesTo(repliesTo: ArrayList<String>) {
//        this.repliesTo = repliesTo
//    }

    fun addReplyFrom(post: ChanPost) {
        repliesFrom.add(post)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val chanPost = o as ChanPost
        if (no != chanPost.no) {
            if (LOG_DEBUG) {
                Log.d("ChanPost", "chanPost.no is different: old=" + no + ", new=" + chanPost.no)
            }
            return false
        }
        if (isClosed != chanPost.isClosed) {
            if (LOG_DEBUG) {
                Log.d("ChanPost", "chanPost.closed is different: closed=" + isClosed + ", new=" + chanPost.isClosed)
            }
            return false
        }
        if (isSticky != chanPost.isSticky) {
            return false
        }
        if (width != chanPost.width) {
            return false
        }
        if (height != chanPost.height) {
            return false
        }
        if (thumbnailWidth != chanPost.thumbnailWidth) {
            return false
        }
        if (thumbnailHeight != chanPost.thumbnailHeight) {
            return false
        }
        if (time != chanPost.time) {
            return false
        }
        if (fsize != chanPost.fsize) {
            return false
        }
        if (resto != chanPost.resto) {
            return false
        }
        if (bumplimit != chanPost.bumplimit) {
            return false
        }
        if (imagelimit != chanPost.imagelimit) {
            return false
        }
        if (replies != chanPost.replies) {
            return false
        }
        if (images != chanPost.images) {
            return false
        }
        if (omittedPosts != chanPost.omittedPosts) {
            return false
        }
        if (omittedImages != chanPost.omittedImages) {
            return false
        }
        if (isWatched != chanPost.isWatched) {
            return false
        }
        if (now != chanPost.now) {
            return false
        }
        if (name != chanPost.name) {
            return false
        }
        if (com != chanPost.com) {
            return false
        }
        if (comment != chanPost.comment) {
            return false
        }
        if (sub != chanPost.sub) {
            return false
        }
        if (subject != chanPost.subject) {
            return false
        }
        if (filename != chanPost.filename) {
            return false
        }
        if (ext != chanPost.ext) {
            return false
        }
        if (tim != chanPost.tim) {
            return false
        }
        if (md5 != chanPost.md5) {
            if (LOG_DEBUG) {
                Log.d("ChanPost", "chanPost.md5 is different: old=" + md5 + ", new=" + chanPost.md5)
            }
            return false
        }
        if (semanticUrl != chanPost.semanticUrl) {
            return false
        }
        if (email != chanPost.email) {
            return false
        }
        if (trip != chanPost.trip) {
            if (LOG_DEBUG) {
                Log.d("ChanPost", "chanPost.trip is different: old=" + trip + ", new=" + chanPost.trip)
            }
            return false
        }
        if (id != chanPost.id) {
            if (LOG_DEBUG) {
                Log.d("ChanPost", "chanPost.id is different: old=" + id + ", new=" + chanPost.id)
            }
            return false
        }
        if (capcode != chanPost.capcode) {
            if (LOG_DEBUG) {
                Log.d("ChanPost", "chanPost.capcode is different: old=" + capcode + ", new=" + chanPost.capcode)
            }
            return false
        }
        if (country != chanPost.country) {
            if (LOG_DEBUG) {
                Log.d("ChanPost", "chanPost.country is different: old=" + country + ", new=" + chanPost.country)
            }
            return false
        }
        if (countryName != chanPost.countryName) {
            if (LOG_DEBUG) {
                Log.d("ChanPost", "chanPost.countryName is different: old=" + countryName + ", new=" + chanPost.countryName)
            }
            return false
        }
        if (trollCountry != chanPost.trollCountry) {
            if (LOG_DEBUG) {
                Log.d("ChanPost", "chanPost.trollCountry is different: old=" + trollCountry + ", new=" + chanPost.trollCountry)
            }
            return false
        }
        if (displayedName != chanPost.displayedName) {
            if (LOG_DEBUG) {
                Log.d("ChanPost", "chanPost.displayedName is different: old=" + displayedName + ", new=" + chanPost.displayedName)
            }
            return false
        }
        return humanReadableFileSize == chanPost.humanReadableFileSize
    }

    override fun hashCode(): Int {
        return no.toInt()
    }

    constructor() {
        no = -1
    }

    fun empty(): Boolean {
        return no == -1L
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(no)
        dest.writeByte(if (isClosed) 1 else 0)
        dest.writeByte(if (isSticky) 1 else 0)
        dest.writeByte(if (isWatched) 1 else 0)
        dest.writeString(now)
        dest.writeString(name)
        dest.writeString(com)
        dest.writeString(sub)
        dest.writeString(filename)
        dest.writeString(ext)
        dest.writeInt(width)
        dest.writeInt(height)
        dest.writeInt(thumbnailWidth)
        dest.writeInt(thumbnailHeight)
        dest.writeString(tim)
        dest.writeLong(time)
        dest.writeString(md5)
        dest.writeInt(fsize)
        dest.writeInt(resto)
        dest.writeInt(bumplimit)
        dest.writeInt(imagelimit)
        dest.writeString(semanticUrl)
        dest.writeInt(replies)
        dest.writeInt(images)
        dest.writeInt(omittedPosts)
        dest.writeInt(omittedImages)
        dest.writeString(email)
        dest.writeString(trip)
        dest.writeString(id)
        dest.writeString(capcode)
        dest.writeString(country)
        dest.writeString(countryName)
        dest.writeStringList(repliesTo)
        dest.writeTypedList(repliesFrom)
        dest.writeString(humanReadableFileSize)
        dest.writeInt(spoiler)
        dest.writeInt(customSpoiler)
    }

    constructor(input: Parcel) {
        no = input.readLong()
        isClosed = input.readByte().toInt() != 0
        isSticky = input.readByte().toInt() != 0
        isWatched = input.readByte().toInt() != 0
        now = input.readString()
        name = input.readString()
        com = input.readString()
        sub = input.readString()
        filename = input.readString()
        ext = input.readString()
        width = input.readInt()
        height = input.readInt()
        thumbnailWidth = input.readInt()
        thumbnailHeight = input.readInt()
        tim = input.readString()
        time = input.readLong()
        md5 = input.readString()
        fsize = input.readInt()
        resto = input.readInt()
        bumplimit = input.readInt()
        imagelimit = input.readInt()
        semanticUrl = input.readString()
        replies = input.readInt()
        images = input.readInt()
        omittedPosts = input.readInt()
        omittedImages = input.readInt()
        email = input.readString()
        trip = input.readString()
        id = input.readString()
        capcode = input.readString()
        country = input.readString()
        countryName = input.readString()
        repliesTo = input.createStringArrayList() ?: ArrayList()
        repliesFrom = input.createTypedArrayList(CREATOR) ?: ArrayList()
        humanReadableFileSize = input.readString()
        spoiler = input.readInt()
        customSpoiler = input.readInt()
    }

    class ThreadIdComparator : Comparator<ChanPost> {
        override fun compare(o1: ChanPost, o2: ChanPost): Int {
            return if (o1.no < o2.no) 1 else if (o1.no > o2.no) -1 else 0
        }
    }

    class ImageCountComparator : Comparator<ChanPost> {
        override fun compare(o1: ChanPost, o2: ChanPost): Int {
            return if (o1.images < o2.images) 1 else if (o1.images > o2.images) -1 else 0
        }
    }

    class ReplyCountComparator : Comparator<ChanPost> {
        override fun compare(o1: ChanPost, o2: ChanPost): Int {
            return if (o1.replies < o2.replies) 1 else if (o1.replies > o2.replies) -1 else 0
        }
    }

    companion object {
        private const val LOG_DEBUG = false

        @JvmField
        val CREATOR: Parcelable.Creator<ChanPost> = object : Parcelable.Creator<ChanPost> {
            override fun createFromParcel(source: Parcel): ChanPost? {
                return ChanPost(source)
            }

            override fun newArray(size: Int): Array<ChanPost?> {
                return arrayOfNulls(size)
            }
        }
    }
}