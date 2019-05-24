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

import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;

import com.emogoth.android.phone.mimi.event.ShowRepliesEvent;
import com.emogoth.android.phone.mimi.util.BusProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;


public final class ReplySpan extends ClickableSpan {
    private static final String LOG_TAG = ReplySpan.class.getSimpleName();

    private final ConcurrentSkipListSet<String> replies;
    private final String boardName;
    private final long threadId;
    private final int textColor;

    public ReplySpan(final String boardName, final long threadId, final List<String> replies, final int textColor) {
        this.boardName = boardName;
        this.threadId = threadId;
        if (replies == null) {
            this.replies = new ConcurrentSkipListSet<>();
        } else {
            this.replies = new ConcurrentSkipListSet<>(replies);
        }
        this.textColor = textColor;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        super.updateDrawState(ds);
        ds.setUnderlineText(true);
        ds.setColor(textColor);
    }

    @Override
    public void onClick(final View widget) {
        Log.i(LOG_TAG, "Caught click on reply: view=" + widget.getClass().getSimpleName());

        final ArrayList<String> threads = new ArrayList<>(replies);
        final ShowRepliesEvent event = new ShowRepliesEvent();

        event.setBoardName(boardName);
        event.setThreadId(threadId);
        event.setReplies(threads);

        BusProvider.getInstance().post(event);
    }
}
