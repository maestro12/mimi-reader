package com.emogoth.android.phone.mimi.span

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.TextPaint
import android.view.View
import android.widget.Toast
import com.emogoth.android.phone.mimi.R
import com.emogoth.android.phone.mimi.util.MimiUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class YoutubeLinkSpan(private val videoId: String, private val linkColor: Int) : LongClickableSpan() {
    override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)
        ds.isUnderlineText = true
        ds.color = linkColor
    }

    override fun onClick(widget: View) {
        openLink(widget.context)
    }

    private fun showChoiceDialog(context: Context) {
        val url = MimiUtil.https() + "youtube.com/watch?v=" + videoId
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.youtube_link)
                    .setItems(R.array.youtube_dialog_list) { dialog, which ->
                        if (which == 0) {
                            openLink(context)
                        } else {
                            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboardManager.setPrimaryClip(ClipData.newPlainText("youtube_link", url))
                            Toast.makeText(context, R.string.link_copied_to_clipboard, Toast.LENGTH_SHORT).show()
                        }
                    }
                    .setCancelable(true)
                    .show()
                    .setCanceledOnTouchOutside(true)
        }
    }

    private fun openLink(context: Context) {
        val url = MimiUtil.https() + "youtube.com/watch?v=" + videoId
        val openIntent = Intent(Intent.ACTION_VIEW)
        openIntent.data = Uri.parse(url)
        context.startActivity(openIntent)
    }

    override fun onLongClick(v: View): Boolean {
        showChoiceDialog(v.context)
        return true
    }

}