/*
 * Copyright (c) 2016. Eli Connelly
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.emogoth.android.phone.mimi.span;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextPaint;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;


public class YoutubeLinkSpan extends LongClickableSpan {
    private final String videoId;
    private final int linkColor;

    public YoutubeLinkSpan(final String videoId, int linkColor) {
        this.videoId = videoId;
        this.linkColor = linkColor;
    }

    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        super.updateDrawState(ds);
        ds.setUnderlineText(true);
        ds.setColor(linkColor);
    }

    @Override
    public void onClick(View widget) {
        openLink(widget.getContext());
    }

    private void showChoiceDialog(final Context context) {
        final String url = MimiUtil.https() + "youtube.com/watch?v=" + videoId;
        final Handler handler = new Handler(Looper.getMainLooper());

        handler.post(() -> new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.youtube_link)
                .setItems(R.array.youtube_dialog_list, (dialog, which) -> {
                    if (which == 0) {
                        openLink(context);
                    } else {
                        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboardManager.setPrimaryClip(ClipData.newPlainText("youtube_link", url));
                        Toast.makeText(context, R.string.link_copied_to_clipboard, Toast.LENGTH_SHORT).show();
                    }
                })
                .setCancelable(true)
                .show()
                .setCanceledOnTouchOutside(true));
    }

    private void openLink(Context context) {
        final String url = MimiUtil.https() + "youtube.com/watch?v=" + videoId;
        final Intent openIntent = new Intent(Intent.ACTION_VIEW);

        openIntent.setData(Uri.parse(url));
        context.startActivity(openIntent);
    }

    @Override
    public boolean onLongClick(View v) {
        showChoiceDialog(v.getContext());
        return true;
    }
}
