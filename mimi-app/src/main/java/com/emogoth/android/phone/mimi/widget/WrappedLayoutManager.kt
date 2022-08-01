package com.emogoth.android.phone.mimi.widget

import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import java.lang.Exception

class WrappedLinearLayoutManager(context: Context?, orientation: Int, reverseLayout: Boolean) : LinearLayoutManager(context, orientation, reverseLayout) {

    companion object {
        var LOG_TAG = WrappedLinearLayoutManager::class.java.simpleName
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (e: IndexOutOfBoundsException) {
            Log.e(LOG_TAG, "Caught IndexOutOfBoundsException in LinearLayoutManager", e)
        }
    }
}

class WrappedGridLayoutManager(context: Context?, spanCount: Int) : GridLayoutManager(context, spanCount) {
    companion object {
        var LOG_TAG = WrappedGridLayoutManager::class.java.simpleName
    }
    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (e: IndexOutOfBoundsException) {
            Log.e(LOG_TAG, "Caught IndexOutOfBoundsException in GridLayoutManager", e)
        }
    }
}

class WrappedStaggeredGridLayoutManager(spanCount: Int, orientation: Int) : StaggeredGridLayoutManager(spanCount, orientation) {
    companion object {
        var LOG_TAG = WrappedStaggeredGridLayoutManager::class.java.simpleName
    }
    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (e: IndexOutOfBoundsException) {
            Log.e(LOG_TAG, "Caught IndexOutOfBoundsException in StaggeredGridLayoutManager", e)
        }
    }
}