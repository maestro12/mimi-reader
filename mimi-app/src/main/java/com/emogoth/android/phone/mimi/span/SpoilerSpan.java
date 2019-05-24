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

import android.graphics.Color;
import androidx.appcompat.app.AlertDialog;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

import com.emogoth.android.phone.mimi.R;

public class SpoilerSpan extends ClickableSpan {
    private static final int SPOILER_COLOR = Color.parseColor("#505050");
    private static final int PREVSPOILER_COLOR = Color.parseColor("#E1E1E1");

    private boolean hidden = true;

    @Override
    public void onClick(View widget) {
//        hidden = !hidden;
//        widget.invalidate();

        if (widget instanceof TextView) {
            TextView tv = (TextView) widget;
            CharSequence text = tv.getText();

            if (text instanceof Spanned) {
                Spanned s = (Spanned) text;
                int start = s.getSpanStart(this);
                int end = s.getSpanEnd(this);

                new AlertDialog.Builder(widget.getContext())
                        .setTitle(R.string.spoiler)
                        .setMessage(s.subSequence(start, end).toString())
                        .setCancelable(true)
                        .show();
            }
        }
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        if (hidden) {
            ds.bgColor = SPOILER_COLOR;
            ds.setColor(SPOILER_COLOR);
        } else {
            ds.bgColor = SPOILER_COLOR;
            ds.setColor(PREVSPOILER_COLOR);
        }
    }
}
