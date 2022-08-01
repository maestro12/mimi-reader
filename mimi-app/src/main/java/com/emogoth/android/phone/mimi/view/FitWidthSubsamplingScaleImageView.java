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

import android.content.Context;
import android.util.AttributeSet;

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

public class FitWidthSubsamplingScaleImageView extends SubsamplingScaleImageView {

    private float aspectRatio = 1f;

    public FitWidthSubsamplingScaleImageView(Context context) {
        super(context);
    }

    public FitWidthSubsamplingScaleImageView(Context context, AttributeSet attr) {
        super(context, attr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = (int) (width * aspectRatio);
        setMeasuredDimension(width, height);
    }

    public void setAspectRatio(float aspectRatio) {
        boolean needsLayout = this.aspectRatio != aspectRatio;
        this.aspectRatio = aspectRatio;
        if (needsLayout) {
            requestLayout();
        }
    }
}
