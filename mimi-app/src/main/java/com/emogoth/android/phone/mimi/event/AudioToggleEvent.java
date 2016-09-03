package com.emogoth.android.phone.mimi.event;

public class AudioToggleEvent {
    private final boolean muted;

    public AudioToggleEvent(boolean muted) {
        this.muted = muted;
    }
}
