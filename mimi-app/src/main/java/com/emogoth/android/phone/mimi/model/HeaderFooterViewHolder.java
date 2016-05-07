
package com.emogoth.android.phone.mimi.model;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;

public class HeaderFooterViewHolder extends RecyclerView.ViewHolder{
    public FrameLayout base;
    public HeaderFooterViewHolder(View itemView) {
        super(itemView);
        base = (FrameLayout) itemView;
    }
}
