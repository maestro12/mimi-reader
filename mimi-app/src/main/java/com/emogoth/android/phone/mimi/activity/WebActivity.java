package com.emogoth.android.phone.mimi.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;

import com.emogoth.android.phone.mimi.R;

public class WebActivity extends MimiActivity {

    public static final String EXTRA_HTML = "html_extra";

    public static void start(Context context, String html) {
        Intent intent = new Intent(context, WebActivity.class);
        intent.putExtra(EXTRA_HTML, html);

        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        String html = getIntent().getStringExtra(EXTRA_HTML);

        WebView webView = findViewById(R.id.web_view);
        webView.loadData(html, "text/html; charset=UTF-8", null);
    }

    @Override
    protected String getPageName() {
        return "web_activity";
    }
}
