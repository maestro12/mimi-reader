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

package com.emogoth.android.phone.mimi.widget;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;


public class MimiRecyclerView extends RecyclerView {
    public MimiRecyclerView(Context context) {
        super(context);
    }

    public MimiRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MimiRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

//    @Override
//    public int computeVerticalScrollRange() {
//        return super.computeVerticalScrollRange();
//    }

//    @Override
//    public int computeVerticalScrollExtent() {
//        return super.computeVerticalScrollExtent();
//    }

//    @Override
//    public int computeVerticalScrollOffset() {
//        return super.computeVerticalScrollOffset();
//    }

//    @Override
//    public void scrollTo(int x, int y) {
//        if(getLayoutManager() instanceof LinearLayoutManager) {
////            final LinearLayoutManager manager = (LinearLayoutManager) getLayoutManager();
////            manager.sc
//        }
//        else {
//            super.scrollTo(x, y);
//        }
//    }


}
