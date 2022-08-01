package com.emogoth.android.phone.mimi.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import io.reactivex.disposables.Disposable

class GalleryScrollReceiver(val boardName: String, val threadId: Long, val positionChangedListener: ((Long) -> (Unit))) : BroadcastReceiver() {
    var scrollPositionEmitter: FlowableEmitter<Long>? = null
    var scrollPositionObserver: Disposable? = null

    val intentFilter: String
        get() = createIntentFilter(boardName, threadId)

    var id: Long = 0

    init {
        val flowable: Flowable<Long> = Flowable.create({
            scrollPositionEmitter = it

        }, BackpressureStrategy.DROP)

        scrollPositionObserver = flowable.subscribe({
            positionChangedListener.invoke(it)
        }, {
            Log.e("GalleryScrollReceiver", "Error running GalleryScrollReceiver", it)
        })
    }

    public fun destroy() {
        RxUtil.safeUnsubscribe(scrollPositionObserver)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.extras?.containsKey(SCROLL_ID_FLAG) == true) {
            id = intent.extras?.getLong(SCROLL_ID_FLAG) ?: -1
            scrollPositionEmitter?.onNext(id)
        }
    }

    companion object {
        const val SCROLL_ID_FLAG = "gallery_scroll_position"
        private const val SCROLL_INTENT_FILTER = "gallery_scrolled_event"

        fun createIntentFilter(boardName: String, threadId: Long): String {
            return "${boardName}_${threadId}_${SCROLL_INTENT_FILTER}"
        }
    }
}