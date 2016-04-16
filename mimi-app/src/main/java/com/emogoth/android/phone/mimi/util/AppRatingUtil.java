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

package com.emogoth.android.phone.mimi.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import com.emogoth.android.phone.mimi.R;


public class AppRatingUtil {
    public static void init(final ViewGroup container) {
        final View rateNow = container.findViewById(R.id.rate_now);
        final View rateLater = container.findViewById(R.id.rate_later);
        final View rateNever = container.findViewById(R.id.rate_never);

        final Context context = container.getContext();

        rateNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MimiUtil.getInstance().openMarketLink(context);
                AppRater.dontShowAgain(context);

                final AlphaAnimation animation = new AlphaAnimation(1.0F, 0.0F);
                animation.setDuration(200);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        container.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });

                container.startAnimation(animation);
            }
        });

        rateLater.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlphaAnimation animation = new AlphaAnimation(1.0F, 0.0F);
                final SharedPreferences prefs = context.getSharedPreferences("apprater", 0);
                final SharedPreferences.Editor editor = prefs.edit();

                editor.putLong("launch_count", 0);
                editor.putLong("date_firstlaunch", System.currentTimeMillis());
                editor.apply();

                animation.setDuration(200);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        container.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });

                container.startAnimation(animation);
            }
        });

        rateNever.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppRater.dontShowAgain(context);

                final AlphaAnimation animation = new AlphaAnimation(1.0F, 0.0F);
                animation.setDuration(200);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        container.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });

                container.startAnimation(animation);
            }
        });
    }
}
