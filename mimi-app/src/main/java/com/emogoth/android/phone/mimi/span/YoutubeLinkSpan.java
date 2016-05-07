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

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.view.View;
import android.widget.Toast;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.activity.YoutubeActivity;
import com.emogoth.android.phone.mimi.util.Extras;
import com.emogoth.android.phone.mimi.util.MimiUtil;


public class YoutubeLinkSpan extends LongClickableSpan {
    private final Context context;
    private final String videoId;
    private final int linkColor;

    public YoutubeLinkSpan(final Context context, final String videoId, int linkColor) {
        this.context = context;
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

        if (MimiUtil.handleYouTubeLinks(context)) {
            final Intent browselink = new Intent(context, YoutubeActivity.class);

            browselink.putExtra(Extras.EXTRAS_YOUTUBE_ID, videoId);
            browselink.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            context.startActivity(browselink);
        } else {
            openLink();
        }
    }

    private void showChoiceDialog() {
        final String url = MimiUtil.httpOrHttps(context) + "youtube.com/watch?v=" + videoId;
        final Handler handler = new Handler(Looper.getMainLooper());

        handler.post(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(context)
                        .setTitle(R.string.youtube_link)
                        .setItems(R.array.youtube_dialog_list, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == 0) {
                                    openLink();
                                } else {
                                    ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                    clipboardManager.setPrimaryClip(ClipData.newPlainText("youtube_link", url));
                                    Toast.makeText(context, R.string.link_copied_to_clipboard, Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setCancelable(true)
                        .show()
                        .setCanceledOnTouchOutside(true);
            }
        });
    }

    private void openLink() {
        final String url = MimiUtil.httpOrHttps(context) + "youtube.com/watch?v=" + videoId;
        final Intent openIntent = new Intent(Intent.ACTION_VIEW);

        openIntent.setData(Uri.parse(url));
        context.startActivity(openIntent);
    }

    @Override
    public boolean onLongClick(View v) {
        showChoiceDialog();
        return true;
    }
}
