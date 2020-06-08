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
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.emogoth.android.phone.mimi.interfaces.OnCaptchaViewReadyCallback;
import com.emogoth.android.phone.mimi.interfaces.OnRecaptchaResponseCallback;
import com.emogoth.android.phone.mimi.util.MimiJavaScriptInterface;
import com.emogoth.android.phone.mimi.util.MimiUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CaptchaWebView extends MimiWebView {
    private final static String LOG_TAG = CaptchaWebView.class.getSimpleName();

    private final Context context;
    private MimiWebClient mimiWebClient;
    private boolean expanded;
    private OnRecaptchaResponseCallback onRecaptchaResponseCallback;
    private OnCaptchaViewReadyCallback onCaptchaViewReadyCallback;


    public CaptchaWebView(Context context) {
        super(context);

        this.context = context;
        setup();
    }

    public CaptchaWebView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;
        setup();
    }

    public CaptchaWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        this.context = context;
        setup();
    }

//    @Override
//    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
//    {
//        if (isExpanded())
//        {
//
//            int expandSpec = MeasureSpec.makeMeasureSpec(MEASURED_SIZE_MASK,
//                    MeasureSpec.AT_MOST);
//            super.onMeasure(widthMeasureSpec, expandSpec);
//
//            ViewGroup.LayoutParams params = getLayoutParams();
//            params.height = getMeasuredHeight();
//        }
//        else
//        {
//            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        }
//    }

    private void setup() {
        getSettings().setJavaScriptEnabled(true);
        getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        mimiWebClient = new MimiWebClient();
        setWebViewClient(mimiWebClient);

        clearCache(true);
        clearHistory();

        addJavascriptInterface(new MimiJavaScriptInterface(context), "Android");

    }

    public void loadCaptchaPage() {
        BufferedReader reader = null;
        final int theme = MimiUtil.getInstance().getTheme();
        final String captchaHtml;
        final StringBuilder sb = new StringBuilder();

        if (theme == MimiUtil.THEME_LIGHT) {
            captchaHtml = "captcha/captcha_light.html";
        } else {
            captchaHtml = "captcha/captcha_dark.html";
        }

        try {
            reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open(captchaHtml)));

            // do reading, usually loop until end of file reading
            String line = reader.readLine();
            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = reader.readLine();
            }

            loadDataWithBaseURL("http://boards.4chan.org", sb.toString(), null, null, null);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error loading captcha html asset", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                }
            }
        }
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    @Override
    public void loadUrl(String url) {
        super.loadUrl(url);
    }

    private class MimiWebClient extends WebViewClient {
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {

            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);

            final Uri link = Uri.parse(url);
            final String response = link.getQueryParameter("g-recaptcha-response");

            Log.d(LOG_TAG, "Page load started: url=" + url);

            if (!TextUtils.isEmpty(response)) {
                Log.i(LOG_TAG, "Found recaptcha response: " + response);

                if (onRecaptchaResponseCallback != null) {
                    onRecaptchaResponseCallback.onResponse(response);
                }
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);

            if (onCaptchaViewReadyCallback != null) {
                onCaptchaViewReadyCallback.onCaptchaViewReady();
            }

            Log.d(LOG_TAG, "Page load finished: url=" + url);
        }


    }

    public void setOnRecaptchaResponseCallback(final OnRecaptchaResponseCallback callback) {
        this.onRecaptchaResponseCallback = callback;
    }

    public void setOnCaptchaViewReadyCallback(final OnCaptchaViewReadyCallback callback) {
        this.onCaptchaViewReadyCallback = callback;
    }

}
