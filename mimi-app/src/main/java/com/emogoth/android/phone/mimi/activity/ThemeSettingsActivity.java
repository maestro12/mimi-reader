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


import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridLayout;
import android.widget.Spinner;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.adapter.ThreadListAdapter;
import com.emogoth.android.phone.mimi.fourchan.FourChanCommentParser;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.emogoth.android.phone.mimi.view.ColorImageView;
import com.rarepebble.colorpicker.ColorPickerView;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class ThemeSettingsActivity extends MimiActivity {
    private static final String LOG_TAG = ThemeSettingsActivity.class.getSimpleName();

    private static final String DEMO_COMMENT = "<a href=\"#p12345678\" class=\"quotelink\">&gt;&gt;12345678</a><br><a href=\"#p87654321\" class=\"quotelink\">&gt;&gt;87654321</a><br><a href=\"#p88765432\" class=\"quotelink\">&gt;&gt;88765432</a><br><span class=\"quote\">&gt;implying the above post is highlighted</span><br><span class=\"quote\">&gt;second line</span><br><br>This text color cannot be changed. It is set by the theme. I'm including it here so it looks more like a real post. This should be enough text to properly demonstrate how the colors will look in a post.<br>http://mimireader.com";

    private int selectedItem = 0;
    private int currentColor = 0;
//    private ColorPickerView colorPickerView;
    private GridLayout pickerContainer;
    private Subscription demoViewSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(MimiUtil.getInstance().getThemeResourceId());
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_theme_settings);

        Toolbar toolbar = (Toolbar) findViewById(R.id.mimi_toolbar);
        if (toolbar != null) {
            toolbar.setLogo(null);
            toolbar.setTitle(R.string.settings);
            toolbar.setNavigationIcon(R.drawable.ic_action_arrow_back);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        final ViewGroup container = (ViewGroup) findViewById(R.id.container);
        final View postView = LayoutInflater.from(this).inflate(R.layout.thread_post_item, container, false);

        final ColorImageView selectedColorView = (ColorImageView) findViewById(R.id.current_color);
        selectedColorView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ColorPickerView pickerView = new ColorPickerView(ThemeSettingsActivity.this);
                pickerView.setColor(currentColor);
                pickerView.showAlpha(false);

                AlertDialog.Builder builder = new AlertDialog.Builder(ThemeSettingsActivity.this);
                builder.setView(pickerView)
                        .setCancelable(true)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                currentColor = pickerView.getColor();
                                selectedColorView.setBackgroundColor(currentColor);
                                setSelectedColor(selectedItem, currentColor);
                                updateDemoView(postView);
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        });

        initColors(selectedColorView, postView);
        currentColor = getSelectedColor(0);

        final Spinner spinner = (Spinner) findViewById(R.id.color_type_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.color_types_list, android.R.layout.simple_spinner_item);

        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentColor = getSelectedColor(position);
                selectedItem = position;

                selectedColorView.setBackgroundColor(currentColor);

                Log.d(LOG_TAG, "position=" + position + ", id=" + id);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                spinner.setSelection(0);
            }
        });

        container.addView(postView);
        updateDemoView(postView);
    }

    private void initColors(ColorImageView selectedColorView, View postView) {
        final List<ColorImageView> views = new ArrayList<>(10);

        views.add((ColorImageView)findViewById( R.id.default_reply ));
        views.add((ColorImageView)findViewById( R.id.default_highlight ));
        views.add((ColorImageView)findViewById( R.id.default_quote ));
        views.add((ColorImageView)findViewById( R.id.default_link ));
        views.add((ColorImageView)findViewById( R.id.material_blue ));
        views.add((ColorImageView)findViewById( R.id.material_red ));
        views.add((ColorImageView)findViewById( R.id.material_green ));
        views.add((ColorImageView)findViewById( R.id.material_yellow ));
        views.add((ColorImageView)findViewById( R.id.material_light_grey ));
        views.add((ColorImageView)findViewById( R.id.material_dark_grey ));

        for (ColorImageView view : views) {
            view.setOnClickListener(setupClickListener(selectedColorView, postView));
        }

    }

    private View.OnClickListener setupClickListener(final ColorImageView selectedColor, final View postView) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(v instanceof ColorImageView) {
                    final ColorImageView view = (ColorImageView) v;
                    currentColor = view.getBackgroundColor();

                    selectedColor.setBackgroundColor(currentColor);
                    setSelectedColor(selectedItem, currentColor);
                    updateDemoView(postView);
                }
            }
        };
    }

    private void updateDemoView(View postView) {
        RxUtil.safeUnsubscribe(demoViewSubscription);
        demoViewSubscription = Observable.just(postView)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Func1<View, ThreadListAdapter.ViewHolder>() {
                    @Override
                    public ThreadListAdapter.ViewHolder call(View view) {
                        List<Integer> highlightedPostIDs = new ArrayList<>();
                        highlightedPostIDs.add(88765432);

                        FourChanCommentParser.Builder parserBuilder = new FourChanCommentParser.Builder();
                        parserBuilder.setContext(ThemeSettingsActivity.this)
                                .setComment(DEMO_COMMENT)
                                .setThreadId(12345678)
                                .setHighlightedPosts(highlightedPostIDs)
                                .setQuoteColor(MimiUtil.getInstance().getQuoteColor())
                                .setReplyColor(MimiUtil.getInstance().getReplyColor())
                                .setHighlightColor(MimiUtil.getInstance().getHighlightColor())
                                .setLinkColor(MimiUtil.getInstance().getLinkColor())
                                .setDemoMode(true);

                        CharSequence date = DateUtils.getRelativeTimeSpanString(
                                System.currentTimeMillis() - 60000,
                                System.currentTimeMillis(),
                                DateUtils.MINUTE_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_RELATIVE);

                        ThreadListAdapter.ViewHolder viewHolder = new ThreadListAdapter.ViewHolder(view);
                        viewHolder.userName.setText("Anonymous");
                        viewHolder.flagIcon.setVisibility(View.GONE);
                        viewHolder.thumbnailContainer.setVisibility(View.GONE);
                        viewHolder.postTime.setText(date);
                        viewHolder.threadId.setText("83736278");
                        viewHolder.comment.setText(parserBuilder.build().parse());
                        return viewHolder;
                    }
                })
                .subscribe(new Action1<ThreadListAdapter.ViewHolder>() {
                    @Override
                    public void call(ThreadListAdapter.ViewHolder viewHolder) {

                    }
                });

    }

    private int getSelectedColor(int selectedItem) {
        switch (selectedItem) {
            case 0:
                return MimiUtil.getInstance().getQuoteColor();
            case 1:
                return MimiUtil.getInstance().getReplyColor();
            case 2:
                return MimiUtil.getInstance().getHighlightColor();
            case 3:
                return MimiUtil.getInstance().getLinkColor();
        }

        return -1;
    }

    private void setSelectedColor(int selectedItem, int color) {
        switch (selectedItem) {
            case 0:
                MimiUtil.getInstance().setQuoteColor(color);
                break;
            case 1:
                MimiUtil.getInstance().setReplyColor(color);
            break;
            case 2:
                MimiUtil.getInstance().setHighlightColor(color);
            break;
            case 3:
                MimiUtil.getInstance().setLinkColor(color);
            break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        setSelectedColor(selectedItem, currentColor);
        RxUtil.safeUnsubscribe(demoViewSubscription);
    }

    @Override
    protected String getPageName() {
        return ThemeSettingsActivity.class.getSimpleName();
    }
}
