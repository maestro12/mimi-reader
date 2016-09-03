package com.emogoth.android.phone.mimi.activity;


import android.os.Bundle;

import com.emogoth.android.phone.mimi.R;

public class FilterActivity extends MimiActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_filter);
    }

    @Override
    protected String getPageName() {
        return "post_filter";
    }
}
