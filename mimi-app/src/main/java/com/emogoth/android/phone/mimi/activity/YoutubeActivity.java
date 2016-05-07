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
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.util.Extras;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;


public class YoutubeActivity extends AppCompatActivity {
    public static final String LOG_TAG = YoutubeActivity.class.getSimpleName();
    public static final String FRAGMENT_TAG = "youtube_fragment";

    private YouTubePlayerSupportFragment youtubeFragment;
    private String youtubeId;
    private YouTubePlayer player;
    private int startTime = 0;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setContentView(R.layout.activity_youtube);

        if (bundle == null) {
            final Bundle extras = getIntent().getExtras();
            if (extras == null) {
                Toast.makeText(this, R.string.youtube_generic_error, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            if (extras.containsKey(Extras.EXTRAS_YOUTUBE_ID)) {
                youtubeId = extras.getString(Extras.EXTRAS_YOUTUBE_ID);
            }

            youtubeFragment = YouTubePlayerSupportFragment.newInstance();
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.youtube_container, youtubeFragment, FRAGMENT_TAG);
            ft.commit();

        } else {
            final FragmentManager fm = getSupportFragmentManager();
            youtubeId = bundle.getString(Extras.EXTRAS_YOUTUBE_ID);
            startTime = bundle.getInt(Extras.EXTRAS_YOUTUBE_START_TIME, 0);
            youtubeFragment = (YouTubePlayerSupportFragment) fm.findFragmentByTag(FRAGMENT_TAG);
        }

        getTheme().applyStyle(MimiUtil.getFontStyle(this), true);

        View background = findViewById(R.id.youtube_background);
        background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        String id = getString(R.string.google_youtube_id);
        if("stub".equals(id)) {
            Toast.makeText(this, R.string.youtube_id_not_defined, Toast.LENGTH_SHORT).show();
            finish();
        }

        youtubeFragment.initialize(id, new YouTubePlayer.OnInitializedListener() {
            @Override
            public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
                player = youTubePlayer;

                if (startTime == 0) {
                    player.loadVideo(youtubeId);
                } else {
                    player.loadVideo(youtubeId, startTime);
                }
            }

            @Override
            public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
                Log.e(LOG_TAG, "Error initializing YouTube API: result=" + youTubeInitializationResult.name() + ", msg=" + youTubeInitializationResult.toString());
                Toast.makeText(YoutubeActivity.this, R.string.youtube_generic_error, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(Extras.EXTRAS_YOUTUBE_ID, youtubeId);

        if (player != null) {
            outState.putInt(Extras.EXTRAS_YOUTUBE_START_TIME, player.getCurrentTimeMillis());
        }
    }
}
