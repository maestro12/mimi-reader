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

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.util.Extras;
import com.emogoth.android.phone.mimi.util.MimiUtil;

import java.util.List;


public class UrlRouterActivity extends AppCompatActivity {
    private static final String LOG_TAG = UrlRouterActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MimiUtil.getInstance().getThemeResourceId());
        super.onCreate(savedInstanceState);

//        Crashlytics.start(this);
        try {
            if (getIntent() != null && getIntent().getAction() != null) {
                Log.i(LOG_TAG, "Host=" + getIntent().getData().getHost() + ", Intent Action=" + getIntent().getAction());

                for (String s : getIntent().getData().getPathSegments()) {
                    Log.i(LOG_TAG, "segment=" + s);
                }

                if (getIntent().getData().getHost().equals(getString(R.string.board_link))) {
                    final List<String> args = getIntent().getData().getPathSegments();
                    for (int i = 0; i < args.size(); i++) {
                        Log.i(LOG_TAG, "arg " + i + ": " + args.get(i));
                    }

                    final Intent intent;
                    String threadId;
                    switch (args.size()) {
                        case 1: // link to a board
                            intent = new Intent(this, PostItemListActivity.class);
                            intent.putExtra(Extras.EXTRAS_BOARD_NAME, args.get(0).toLowerCase());
                            break;
                        case 2: // probably a link to the catalog
                            intent = new Intent(this, PostItemListActivity.class);
                            intent.putExtra(Extras.EXTRAS_BOARD_NAME, args.get(0).toLowerCase());
                            intent.putExtra(Extras.EXTRAS_SINGLE_THREAD, true);
                            break;
                        case 3: // probably a link to a specific thread
                        case 4:
                            intent = new Intent(this, PostItemDetailActivity.class);
                            intent.putExtra(Extras.EXTRAS_BOARD_NAME, args.get(0).toLowerCase());
                            threadId = args.get(2);
                            if (threadId != null) {
                                if (threadId.contains("#")) {
                                    final String highlightedPost = threadId.substring(threadId.indexOf("#") + 1);
                                    intent.putExtra(Extras.EXTRAS_POST_ID, highlightedPost);
                                    threadId = threadId.substring(1, threadId.indexOf("#") - 1);
                                }

                                intent.putExtra(Extras.EXTRAS_THREAD_ID, Long.valueOf(threadId));
                            }

                            break;

                        default:
                            intent = new Intent(this, PostItemListActivity.class);
                            Log.e(LOG_TAG, "Error opening URL: " + getIntent().getData().toString());
                    }

                    intent.putExtra(Extras.EXTRAS_FROM_URL, true);
                    startActivity(intent);

                }

            }
        }
        catch (final Exception e) {
            Log.e(LOG_TAG, "Error", e);
        }

        finish();

    }
}
