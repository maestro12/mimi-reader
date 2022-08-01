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

package com.emogoth.android.phone.mimi.fragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.PagerTitleStrip;
import androidx.viewpager.widget.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.activity.MimiActivity;
import com.emogoth.android.phone.mimi.adapter.BookmarksDrawerAdapter;
import com.emogoth.android.phone.mimi.util.Extras;
import com.emogoth.android.phone.mimi.util.MimiUtil;


public class BookmarkDrawerFragment extends Fragment {
    private static final String LOG_TAG = BookmarkDrawerFragment.class.getSimpleName();

    private ViewPager viewPager;
    private PagerTitleStrip tabs;
    private TextView bookmarkText;
    private TextView historyText;
    private BookmarksDrawerAdapter drawerAdapter;
    private int bookmarkOrHistory = MimiActivity.VIEWING_NONE;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getArguments() != null) {
            if (getArguments().containsKey(Extras.EXTRAS_VIEWING_HISTORY)) {
                bookmarkOrHistory = getArguments().getInt(Extras.EXTRAS_VIEWING_HISTORY);
            }
        }

        drawerAdapter = new BookmarksDrawerAdapter(getChildFragmentManager(), getActivity(), bookmarkOrHistory);
        final View v = inflater.inflate(R.layout.fragment_bookmark_drawer, container, false);
        return v;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewPager = (ViewPager) view.findViewById(R.id.pager);
        viewPager.setAdapter(drawerAdapter);

        bookmarkText = (TextView) view.findViewById(R.id.bookmark_tab);
        historyText = (TextView) view.findViewById(R.id.history_tab);

        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        bookmarkText.setTextColor(getResources().getColor(R.color.tab_highlight));
                        historyText.setTextColor(getResources().getColor(R.color.tab_unhighlight));
                        break;
                    case 1:
                        bookmarkText.setTextColor(getResources().getColor(R.color.tab_unhighlight));
                        historyText.setTextColor(getResources().getColor(R.color.tab_highlight));
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

    }

    private void createTabs() {
        if (historyText != null) {
            if (MimiUtil.historyEnabled(getActivity())) {
                bookmarkText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (viewPager.getCurrentItem() == 1) {
                            viewPager.setCurrentItem(0, true);
                            bookmarkText.setTextColor(getResources().getColor(R.color.tab_highlight));
                            historyText.setTextColor(getResources().getColor(R.color.tab_unhighlight));
                        }
                    }
                });

                historyText.setVisibility(View.VISIBLE);
                historyText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (viewPager.getCurrentItem() == 0) {
                            viewPager.setCurrentItem(1, true);
                            bookmarkText.setTextColor(getResources().getColor(R.color.tab_unhighlight));
                            historyText.setTextColor(getResources().getColor(R.color.tab_highlight));
                        }
                    }
                });
            } else {
                historyText.setVisibility(View.GONE);
            }

            viewPager.setCurrentItem(0);
//            viewPager.setAdapter(drawerAdapter);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        createTabs();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
