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

package com.emogoth.android.phone.mimi.event;

public class GalleryImageTouchEvent {
    private boolean touched;
    private boolean forceVisibility = false;
    private boolean visible;

    public GalleryImageTouchEvent() {
    }

    public boolean isTouched() {
        return touched;
    }

    public void setTouched(boolean touched) {
        this.touched = touched;
    }

    public boolean isForceVisibility() {
        return forceVisibility;
    }

    public void setForceVisibility(boolean forceVisibility) {
        this.forceVisibility = forceVisibility;
    }

    public boolean isVisbile() {
        return visible;
    }

    public void setVisbility(boolean visbility) {
        this.visible = visbility;
    }
}
