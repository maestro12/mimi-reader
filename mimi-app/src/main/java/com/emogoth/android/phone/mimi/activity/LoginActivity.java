package com.emogoth.android.phone.mimi.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.fourchan.FourChanConnector;
import com.emogoth.android.phone.mimi.util.HttpClientFactory;
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.google.android.material.snackbar.Snackbar;
import com.mimireader.chanlib.ChanConnector;

import java.io.IOException;

import io.reactivex.disposables.Disposable;

public class LoginActivity extends MimiActivity {
    private static final String LOG_TAG = LoginActivity.class.getSimpleName();

    public static final String INCORRECT_TOKEN = "Incorrect Token or PIN";

    private Disposable loginSubscription;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.login_activity);

        final EditText tokenView = findViewById(R.id.token_text);
        if (tokenView == null) {
            return;
        }

        final EditText pinView = findViewById(R.id.pin_text);
        if (pinView == null) {
            return;
        }

        final TextView errorMessage = findViewById(R.id.error_message);
        if (errorMessage == null) {
            return;
        }

        final View loginButton = findViewById(R.id.login_button);
        if (loginButton != null) {
            loginButton.setOnClickListener(v -> {
                final String token = tokenView.getText().toString();
                final String pin = pinView.getText().toString();

                if (TextUtils.isEmpty(token) || TextUtils.isEmpty(pin)) {
                    Snackbar.make(errorMessage, R.string.login_error, Snackbar.LENGTH_SHORT).show();
                    return;
                }

                final ChanConnector chanConnector = new FourChanConnector.Builder()
                        .setClient(HttpClientFactory.getInstance().getClient())
                        .setEndpoint(FourChanConnector.getDefaultEndpoint())
                        .setPostEndpoint(FourChanConnector.getDefaultPostEndpoint())
                        .build();

                RxUtil.safeUnsubscribe(loginSubscription);
                loginSubscription = chanConnector.login(token, pin)
                        .subscribe(responseBodyResponse -> {
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
                        }, throwable -> {
                            Log.w(LOG_TAG, "Error logging in", throwable);
                        });

                errorMessage.setText(null);
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
