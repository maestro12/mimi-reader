package com.emogoth.android.phone.mimi.span

import android.text.TextPaint
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import com.emogoth.android.phone.mimi.activity.MimiActivity
import com.emogoth.android.phone.mimi.interfaces.ReplyClickListener
import com.emogoth.android.phone.mimi.util.MimiUtil

class ReplySpan(private val boardName: String, private val threadId: Long, private val replies: List<String>, private val textColor: Int) : ClickableSpan() {
    override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)
        ds.isUnderlineText = true
        ds.color = textColor
    }

    override fun onClick(widget: View) {
        Log.i(LOG_TAG, "Caught click on reply: view=" + widget.javaClass.simpleName)

        val activity = MimiUtil.scanForActivity(widget.context)
        if (activity is ReplyClickListener) {
            activity.onReplyClicked(boardName, threadId, -1, replies)
        }
    }

    companion object {
        private val LOG_TAG = ReplySpan::class.java.simpleName
    }

}