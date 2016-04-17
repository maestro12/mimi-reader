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

package com.emogoth.android.phone.mimi.view;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.emogoth.android.phone.mimi.R;


public class DrawerViewHolder {

    public final ImageView image;
    public final TextView boardName;
    public final TextView threadId;
    public final TextView lastviewed;
    public final TextView text;
    public final TextView unreadcount;
    public final ImageView deletehistory;
    public final View root;

    public DrawerViewHolder(View root) {
        image = (ImageView) root.findViewById(R.id.image);
        boardName = (TextView) root.findViewById(R.id.board_name);
        threadId = (TextView) root.findViewById(R.id.thread_id);
        lastviewed = (TextView) root.findViewById(R.id.last_viewed);
        text = (TextView) root.findViewById(R.id.text);
        unreadcount = (TextView) root.findViewById(R.id.unread_count);
        deletehistory = (ImageView) root.findViewById(R.id.delete_history);
        this.root = root;
    }

}
