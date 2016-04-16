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

package com.emogoth.android.phone.mimi.dialog;


import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.interfaces.OnCaptchaViewReadyCallback;
import com.emogoth.android.phone.mimi.interfaces.OnRecaptchaResponseCallback;
import com.emogoth.android.phone.mimi.view.CaptchaWebView;


public class CaptchaDialog extends DialogFragment {
    private static final String LOG_TAG = CaptchaDialog.class.getSimpleName();

    private OnRecaptchaResponseCallback onRecaptcharesponseCallback;
    private CaptchaWebView webView;
    private TextView captchaLoading;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        final View v = inflater.inflate(R.layout.dialog_captcha, container, false);

        captchaLoading = (TextView) v.findViewById(R.id.loading_captcha);
        webView = (CaptchaWebView) v.findViewById(R.id.captch_webview);

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(webView != null) {
            webView.loadCaptchaPage();
            webView.setBackgroundColor(0x00000000);
            webView.setOnRecaptchaResponseCallback(new OnRecaptchaResponseCallback() {
                @Override
                public void onResponse(String response) {
                    if (onRecaptcharesponseCallback != null) {
                        onRecaptcharesponseCallback.onResponse(response);
                    }
                }
            });

            webView.setOnCaptchaViewReadyCallback(new OnCaptchaViewReadyCallback() {
                @Override
                public void onCaptchaViewReady() {
                    if(captchaLoading.getVisibility() == View.VISIBLE) {
                        captchaLoading.setVisibility(View.GONE);
                    }
                }
            });
        }
    }

    public void setOnRecaptchaResponseCallback(final OnRecaptchaResponseCallback callback) {
        this.onRecaptcharesponseCallback = callback;
    }
}
