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

package com.emogoth.android.phone.mimi.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;


public class NavigationPagerAdapter extends FragmentStatePagerAdapter {
    public static final int LIST_PANE = 0;
    public static final int DETAIL_PANE = 1;

    private FragmentManager fragmentManager;
    private Fragment [] panes;

    public NavigationPagerAdapter(final FragmentManager fm) {
        super(fm);

        fragmentManager = fm;
        panes = new Fragment[2];
    }

    @Override
    public Fragment getItem(int i) {
        return panes[i];
    }

    @Override
    public int getItemPosition(Object object) {
        if(object instanceof Fragment) {
            final Fragment fragment = (Fragment) object;

            if(panes[LIST_PANE] == fragment) {
                return LIST_PANE;
            }

            if(panes[DETAIL_PANE] == fragment) {
                return DETAIL_PANE;
            }
        }

        return POSITION_NONE;
    }

    @Override
    public int getCount() {

        // There should always be at least 1 fragment in the viewpager
        if(panes[1] == null) {
            return 1;
        }

        return 2;
    }

    public void setPane(final Fragment fragment, final int paneIndex) {
//        if(panes[paneIndex] != null) {
//            fragmentManager.beginTransaction()
//                    .remove(panes[paneIndex])
//                    .add(fragment, fragment.getTag())
//                    .commit();
//        }

        if(panes[paneIndex] != fragment) {
            panes[paneIndex] = fragment;
            notifyDataSetChanged();
        }

    }
}
