package com.emogoth.android.phone.mimi.util;

import androidx.annotation.IdRes;
import android.view.View;

import com.mimireader.chanlib.models.ChanPost;

import java.util.List;

public class FindPostHelper {
    private final View root;

    private int currentIndex;
    private List<ChanPost> foundPosts;

    public FindPostHelper(View root) {
        this.root = root;
    }

    public void setFindNextListener(@IdRes int findNextRes, View.OnClickListener listener) {
        View v = root.findViewById(findNextRes);
        if (v != null) {
            v.setOnClickListener(listener);
        }
    }
    public void setFindPreviousListener(@IdRes int findPrevRes, View.OnClickListener listener) {
        View v = root.findViewById(findPrevRes);
        if (v != null) {
            v.setOnClickListener(listener);
        }
    }
    public void setCloseListener(@IdRes int closeRes, View.OnClickListener listener) {
        View v = root.findViewById(closeRes);
        if (v != null) {
            v.setOnClickListener(listener);
        }
    }

    public void find(String text, List<ChanPost> posts) {

    }
}
