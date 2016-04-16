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

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.view.GridItemImageView;
import com.mimireader.chanlib.ChanConnector;
import com.mimireader.chanlib.models.ChanPost;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class GalleryGridAdapter extends RecyclerView.Adapter<GalleryGridAdapter.GridViewHolder> {

    private final Context context;
    private final String boardName;
    private final List<ChanPost> posts;

    private final FragmentManager fm;
    private final Typeface typeface;
    private final ChanConnector chanConnector;
    private final boolean secureConnection;

    private boolean batchDownload = false;
    private boolean[] selectedItems;

    private AdapterView.OnItemLongClickListener itemLongClickListener;
    private AdapterView.OnItemClickListener itemClickListener;

    public GalleryGridAdapter(final Context context, final FragmentManager fm, final String boardName, final List<ChanPost> posts, final ChanConnector chanConnector) {
        this.context = context;
        this.fm = fm;
        this.boardName = boardName;
        this.posts = posts;
        this.chanConnector = chanConnector;

        final String fontPath = context.getString(R.string.font_path);
        typeface = Typeface.createFromAsset(context.getAssets(), fontPath);

        selectedItems = new boolean[posts.size()];

        secureConnection = MimiUtil.isSecureConnection(context);
    }

    @Override
    public GridViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.gallery_grid_item, parent, false);
        return new GridViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final GridViewHolder viewHolder, final int position) {
        final ChanPost threadItem = posts.get(position);
        final String url = chanConnector.getThumbUrl(boardName, threadItem.getTim(), secureConnection);
        viewHolder.galleryThumbnail.setAspectRatio(threadItem.getThumbnailWidth(), threadItem.getThumbnailHeight());
        Glide.with(context).load(url).crossFade().into(viewHolder.galleryThumbnail);

        viewHolder.fileSize.setText(MimiUtil.humanReadableByteCount(threadItem.getFsize(), true));

        if (threadItem.getExt() != null) {
            viewHolder.fileExt.setText(threadItem.getExt().substring(1).toUpperCase(Locale.getDefault()));
        }

        if (batchDownload) {
            if (selectedItems[position]) {
                viewHolder.selected.setVisibility(View.VISIBLE);
            } else {
                viewHolder.selected.setVisibility(View.GONE);
            }
        } else {
            viewHolder.selected.setVisibility(View.GONE);
        }

        viewHolder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemClickListener != null) {
                    itemClickListener.onItemClick(null, viewHolder.root, position, 0);
                }
            }
        });

        viewHolder.root.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (itemLongClickListener != null) {
                    itemLongClickListener.onItemLongClick(null, viewHolder.root, position, 0);
                }

                return true;
            }
        });
    }

    @Override
    public long getItemId(int position) {
        final ChanPost post = posts.get(position);
        return post.getNo();
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public void setPosts(List<ChanPost> postList) {
        posts.clear();
        posts.addAll(postList);

        selectedItems = new boolean[posts.size()];
        notifyDataSetChanged();
    }

    public ArrayList<ChanPost> getSelectedPosts() {
        final ArrayList<ChanPost> selectedPosts = new ArrayList<>(posts.size());

        for (int i = 0; i < posts.size(); i++) {
            if (selectedItems[i]) {
                selectedPosts.add(posts.get(i));
            }
        }

        return selectedPosts;
    }

    public boolean isPostSelected() {
        int i = 0;
        boolean done = false;
        boolean isSelected = false;
        while (!done) {
            if (i < posts.size()) {
                isSelected = selectedItems[i];

                if (isSelected) {
                    done = true;
                }
            } else {
                done = true;
            }

            i++;
        }

        return isSelected;
    }

    public void setBatchDownload(final boolean enabled) {
        batchDownload = enabled;
        notifyDataSetChanged();
    }

    public boolean isBatchDownload() {
        return batchDownload;
    }

    public void setSelectedItem(final int pos, final boolean selected) {
        if (pos < selectedItems.length) {
            selectedItems[pos] = true;
            notifyDataSetChanged();
        }
    }

    public void toggleSelectedItem(final int pos) {
        if (pos < selectedItems.length) {
            selectedItems[pos] = !selectedItems[pos];
            notifyDataSetChanged();
        }
    }

    public void invertSelection() {
        for (int i = 0; i < selectedItems.length; i++) {
            selectedItems[i] = !selectedItems[i];
        }

        notifyDataSetChanged();
    }

    public void selectAll() {
        for (int i = 0; i < selectedItems.length; i++) {
            selectedItems[i] = true;
        }

        notifyDataSetChanged();
    }

    public void selectNone() {
        for (int i = 0; i < selectedItems.length; i++) {
            selectedItems[i] = false;
        }

        notifyDataSetChanged();
    }

    public void setOnItemLongClickListener(AdapterView.OnItemLongClickListener listener) {
        itemLongClickListener = listener;
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener listener) {
        itemClickListener = listener;
    }


    public class GridViewHolder extends RecyclerView.ViewHolder {
        public final GridItemImageView galleryThumbnail;
        public final TextView fileSize;
        public final TextView fileExt;
        public final TextView selected;
        public final View root;

        public GridViewHolder(final View root) {
            super(root);

            galleryThumbnail = (GridItemImageView) root.findViewById(R.id.gallery_thumbnail);
            fileSize = (TextView) root.findViewById(R.id.file_size);
            fileExt = (TextView) root.findViewById(R.id.file_ext);
            selected = (TextView) root.findViewById(R.id.selected);
            selected.setTypeface(typeface);
            this.root = root;
        }
    }
}
