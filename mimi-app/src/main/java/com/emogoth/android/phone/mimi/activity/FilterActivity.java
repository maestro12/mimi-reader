package com.emogoth.android.phone.mimi.activity;


import android.os.Bundle;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.util.MimiUtil;

public class FilterActivity extends MimiActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MimiUtil.getInstance().getThemeResourceId());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_filter);
    }

    @Override
    protected String getPageName() {
        return "post_filter";
    }
}
