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
import android.text.TextPaint;
import android.view.View;

public class SpoilerSpan extends LongClickableSpan {
    static final int SPOILER_COLOR = Color.parseColor("#505050");
    static final int PREVSPOILER_COLOR = Color.parseColor("#E1E1E1");

    boolean spoiled = true;

    @Override
    public void onClick(View widget) {
        spoiled = !spoiled;
        widget.invalidate();
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        if (spoiled) {
            ds.bgColor = SPOILER_COLOR;
            ds.setColor(SPOILER_COLOR);
        } else {
            ds.bgColor = SPOILER_COLOR;
            ds.setColor(PREVSPOILER_COLOR);
        }
    }
}
