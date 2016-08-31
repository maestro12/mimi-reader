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

package com.emogoth.android.phone.mimi.interfaces;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;

public abstract class EndlessRecyclerOnScrollListener extends RecyclerView.OnScrollListener {
    public static String TAG = EndlessRecyclerOnScrollListener.class.getSimpleName();

    private int previousTotal = 0; // The total number of items in the dataset after the last load
    private boolean loading = true; // True if we are still waiting for the last set of data to load.
    private int visibleThreshold = 5; // The minimum amount of items to have below your current scroll position before loading more.
    int firstVisibleItem, visibleItemCount, totalItemCount;

    private int currentPage = 1;

    private RecyclerView.LayoutManager layoutManager;
    private boolean locked = false;

    public EndlessRecyclerOnScrollListener(RecyclerView.LayoutManager layoutManager) {
        this.layoutManager = layoutManager;
    }

    private int getFirstVisibleItemPosition(RecyclerView.LayoutManager layoutManager) {
        if (layoutManager instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
        }

        if (layoutManager instanceof StaggeredGridLayoutManager) {
            int[] firstVisibleItems = null;
            firstVisibleItems = ((StaggeredGridLayoutManager) layoutManager).findFirstVisibleItemPositions(firstVisibleItems);

            if (firstVisibleItems != null && firstVisibleItems.length > 0) {
                return firstVisibleItems[0];
            }
        }

        return 0;
    }


    @Override
    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        visibleItemCount = recyclerView.getChildCount();
        totalItemCount = layoutManager.getItemCount();
        firstVisibleItem = getFirstVisibleItemPosition(layoutManager);

        /*
        if(firstVisibleItems != null && firstVisibleItems.length > 0) {
            firstVisibleItem = firstVisibleItems[0];
        }

        if (loading) {
            if ((visibleItemCount + firstVisibleItem) >= totalItemCount) {
                loading = false;
                Log.d("tag", "LOAD NEXT ITEM");
            }
        }
    }
         */

        if (loading) {
            if (totalItemCount > previousTotal) {
                loading = false;
                previousTotal = totalItemCount;
            }
        }
        if (!loading && !locked && (totalItemCount - visibleItemCount)
                <= (firstVisibleItem + visibleThreshold)) {
            // End has been reached

            // Do something
            currentPage++;

            onLoadMore(currentPage);

            loading = true;
            locked = true;
        }
    }

    public void reset() {
        loading = false;
        currentPage = 1;
        previousTotal = 0;

        firstVisibleItem = 0;
        visibleItemCount = 0;
        totalItemCount = 0;
    }

    public void lock() {
        locked = true;
        Log.d("EndlessScrollListener", "locking loadmore functionality");
    }

    public void unlock() {
        locked = false;
        Log.d("EndlessScrollListener", "unlocking loadmore functionality");
    }

    public abstract void onLoadMore(int currentPage);
}
