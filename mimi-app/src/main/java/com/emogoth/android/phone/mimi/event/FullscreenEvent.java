package com.emogoth.android.phone.mimi.event;

public class FullscreenEvent {
    final private boolean forceVisibility;
    final private boolean visible;

    public FullscreenEvent(boolean forceVisibility, boolean visible) {
        this.forceVisibility = forceVisibility;
        this.visible = visible;
    }

    public boolean isForceVisibility() {
        return forceVisibility;
    }

    public boolean isVisbile() {
        return visible;
    }
}
