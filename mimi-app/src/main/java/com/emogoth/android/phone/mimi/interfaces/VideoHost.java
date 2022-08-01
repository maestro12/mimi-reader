package com.emogoth.android.phone.mimi.interfaces;

import com.emogoth.android.phone.mimi.util.ExoPlayer2Helper;

public interface VideoHost {
    ExoPlayer2Helper getExoPlayerHelper();
    void clearExoPlayerHelper();
}
