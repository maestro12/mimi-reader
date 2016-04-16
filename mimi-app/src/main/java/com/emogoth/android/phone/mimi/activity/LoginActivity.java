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

package com.emogoth.android.phone.mimi.activity;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.fourchan.FourChanConnector;
import com.emogoth.android.phone.mimi.util.HttpClientFactory;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.mimireader.chanlib.ChanConnector;

import java.io.IOException;

import okhttp3.ResponseBody;
import rx.Subscription;
import rx.functions.Action1;

public class LoginActivity extends MimiActivity {
    private static final String LOG_TAG = LoginActivity.class.getSimpleName();

    public static final String INCORRECT_TOKEN = "Incorrect Token or PIN";

    private Subscription loginSubscription;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.login_activity);

        final EditText tokenView = (EditText) findViewById(R.id.token_text);
        if (tokenView == null) {
            return;
        }

        final EditText pinView = (EditText) findViewById(R.id.pin_text);
        if (pinView == null) {
            return;
        }

        final TextView errorMessage = (TextView) findViewById(R.id.error_message);
        if (errorMessage == null) {
            return;
        }

        final View loginButton = findViewById(R.id.login_button);
        if (loginButton != null) {
            loginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String token = tokenView.getText().toString();
                    final String pin = pinView.getText().toString();

                    if (TextUtils.isEmpty(token) || TextUtils.isEmpty(pin)) {
                        Snackbar.make(errorMessage, R.string.login_error, Snackbar.LENGTH_SHORT).show();
                        return;
                    }

                    final ChanConnector chanConnector = new FourChanConnector.Builder()
                            .setClient(HttpClientFactory.getInstance().getOkHttpClient())
                            .setEndpoint(FourChanConnector.getDefaultEndpoint(MimiUtil.isSecureConnection(LoginActivity.this)))
                            .setPostEndpoint(FourChanConnector.getDefaultPostEndpoint())
                            .build();

                    RxUtil.safeUnsubscribe(loginSubscription);
                    loginSubscription = chanConnector.login(token, pin)
                            .subscribe(new Action1<retrofit2.Response<ResponseBody>>() {
                                @Override
                                public void call(retrofit2.Response<ResponseBody> responseBodyResponse) {
                                    try {
                                        String response;
                                        if (responseBodyResponse.errorBody() != null) {
                                            response = responseBodyResponse.errorBody().string();
                                        } else {
                                            response = responseBodyResponse.body().string();
                                        }

                                        Log.i(LOG_TAG, "response=" + response);

                                        if (response.contains(INCORRECT_TOKEN)) {
                                            errorMessage.setText(R.string.auth_failure);
                                        } else if (response.contains("Authentication Failed")) {
                                            errorMessage.setText(R.string.login_error);
                                        } else if (response.contains("Authorization Successful")) {
                                            Toast.makeText(LoginActivity.this, R.string.auth_success, Toast.LENGTH_SHORT).show();
                                            finish();
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }, new Action1<Throwable>() {
                                @Override
                                public void call(Throwable throwable) {
                                    Log.w(LOG_TAG, "Error logging in", throwable);
                                }
                            });

                    errorMessage.setText(null);
                }
            });
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        RxUtil.safeUnsubscribe(loginSubscription);
    }

    @Override
    protected String getPageName() {
        return "login_page";
    }
}
