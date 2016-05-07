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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.activity.GalleryActivity;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;
import com.emogoth.android.phone.mimi.db.HistoryTableConnection;
import com.emogoth.android.phone.mimi.interfaces.GalleryMenuItemClickListener;
import com.emogoth.android.phone.mimi.interfaces.OnPostItemClickListener;
import com.emogoth.android.phone.mimi.model.HeaderFooterViewHolder;
import com.emogoth.android.phone.mimi.util.Extras;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.RefreshScheduler;
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.emogoth.android.phone.mimi.util.ThreadRegistry;
import com.emogoth.android.phone.mimi.view.GridItemImageView;
import com.mimireader.chanlib.ChanConnector;
import com.mimireader.chanlib.models.ChanCatalog;
import com.mimireader.chanlib.models.ChanPost;

import java.util.ArrayList;
import java.util.List;

import rx.Subscription;
import rx.functions.Action1;


public class PostItemsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {

    private static final String LOG_TAG = PostItemsAdapter.class.getSimpleName();

    private static final int VIEW_HEADER = 0;
    private static final int VIEW_LIST_ITEM = 1;
    private static final int VIEW_GRID_ITEM = 2;
    private static final int VIEW_FOOTER = 3;

    private final OnPostItemClickListener clickListener;
    private final ChanConnector chanConnector;
    private final boolean secureConnection;

    protected List<ChanPost> postList;

    protected String boardName;
    protected String boardTitle;
    protected final FragmentActivity activity;
    protected int postCount = 0;
    protected int lastPosition = 0;
    protected String flagUrl;
    protected PostFilter postFilter;
    protected boolean searching;
    protected LayoutInflater inflater;
    private RecyclerView.LayoutManager layoutManager;

    private List<View> headers = new ArrayList<>();
    private List<View> footers = new ArrayList<>();

    private Subscription historyInfoSubscription;
    private Subscription fetchPostSubscription;


    public enum ManagerType {
        LIST(VIEW_LIST_ITEM), STAGGERED_GRID(VIEW_GRID_ITEM), GRID(VIEW_GRID_ITEM), OTHER(-1);

        final int type;

        ManagerType(int type) {
            this.type = type;
        }

        public int getItemViewType() {
            return type;
        }
    }

    protected ManagerType managerType = ManagerType.LIST;


    public PostItemsAdapter(final FragmentActivity activity, final String boardName, final String boardTitle, final ChanCatalog threads, final ChanConnector chanConnector, final OnPostItemClickListener clickListener) {
        this.activity = activity;
        this.boardName = boardName;
        this.boardTitle = boardTitle;
        this.postList = new ArrayList<>();
        this.clickListener = clickListener;
        this.chanConnector = chanConnector;

        for (int i = 0; i < threads.getPosts().size(); i++) {
            postList.addAll(threads.getPosts());
        }

        this.postCount = postList.size();

        if (activity != null) {
            this.secureConnection = MimiUtil.isSecureConnection(activity);
            setup();
        } else {
            this.secureConnection = false;
        }
    }

    public PostItemsAdapter(final FragmentActivity activity, final String boardName, final String boardTitle, final List<ChanPost> postList, final ChanConnector chanConnector, final OnPostItemClickListener clickListener) {
        this.activity = activity;
        this.boardName = boardName;
        this.boardTitle = boardTitle;
        this.postList = postList;
        this.postCount = postList.size();
        this.clickListener = clickListener;
        this.chanConnector = chanConnector;

        if (activity != null) {
            this.secureConnection = MimiUtil.isSecureConnection(activity);
            setup();
        } else {
            this.secureConnection = false;
        }
    }

    private void setup() {
        inflater = activity.getLayoutInflater();
        if (postList.size() == 0) {
            return;
        }

        this.flagUrl = MimiUtil.httpOrHttps(activity) + activity.getString(R.string.flag_int_link);
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View v;
        switch (viewType) {
            case VIEW_HEADER:
                FrameLayout headerFrameLayout = new FrameLayout(parent.getContext());
                //make sure it fills the space
                headerFrameLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                return new HeaderFooterViewHolder(headerFrameLayout);
            case VIEW_LIST_ITEM:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.catalog_post_list_item, parent, false);
                return new ViewHolderListItem(v);

            case VIEW_GRID_ITEM:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.catalog_post_grid_item, parent, false);
                return new ViewHolderGridItem(v);

            case VIEW_FOOTER:
                FrameLayout footerFrameLayout = new FrameLayout(parent.getContext());
                //make sure it fills the space
                footerFrameLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, MimiUtil.dpToPx(60)));
                return new HeaderFooterViewHolder(footerFrameLayout);

            default:
                FrameLayout defaultFrameLayout = new FrameLayout(parent.getContext());
                //make sure it fills the space
                defaultFrameLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                return new HeaderFooterViewHolder(defaultFrameLayout);
        }

    }

    public void setLayoutManager(RecyclerView.LayoutManager manager) {
        layoutManager = manager;
        if (layoutManager instanceof GridLayoutManager) {
            managerType = ManagerType.GRID;
            ((GridLayoutManager) layoutManager).setSpanSizeLookup(gridSpanSizeLookup);
        } else if (layoutManager instanceof LinearLayoutManager) {
            managerType = ManagerType.LIST;
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            managerType = ManagerType.STAGGERED_GRID;
            ((StaggeredGridLayoutManager) layoutManager).setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        } else {
            managerType = ManagerType.OTHER;
        }

        Log.d(LOG_TAG, "Set manager type: value=" + managerType.name());
    }

    public ManagerType getLayoutManagerType() {
        return managerType;
    }

    public int getGridSpan(int position) {
        if (isHeader(position) || isFooter(position)) {
            return getSpan();
        }
        position -= headers.size();
        if (layoutManager instanceof GridLayoutManager) {
            return ((GridLayoutManager) layoutManager).getSpanSizeLookup().getSpanSize(position);
        }

        return 1;
    }

    private int getSpan() {
        if (layoutManager instanceof GridLayoutManager) {
            return ((GridLayoutManager) layoutManager).getSpanCount();
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            return ((StaggeredGridLayoutManager) layoutManager).getSpanCount();
        }
        return 1;
    }

    private GridLayoutManager.SpanSizeLookup gridSpanSizeLookup = new GridLayoutManager.SpanSizeLookup() {
        @Override
        public int getSpanSize(int position) {
            return getGridSpan(position);
        }
    };

    @Override
    public long getItemId(final int position) {
        return position - 1;
    }

    @Override
    public int getItemCount() {
        if (postList != null) {
            return headers.size() + postList.size() + footers.size();
        }

        return 0;
    }

    private boolean isHeader(int position) {
        return (position < headers.size());
    }

    private boolean isFooter(int position) {
        return (position >= headers.size() + postList.size());
    }

    @Override
    public int getItemViewType(int position) {
        if (isHeader(position)) {
            return VIEW_HEADER;
        } else if (isFooter(position)) {
            return VIEW_FOOTER;
        } else {
            return managerType.getItemViewType();
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int pos) {

        if (isHeader(pos)) {
            View v = headers.get(pos);
            //add our view to a header view and display it
            prepareHeaderFooter((HeaderFooterViewHolder) holder, v);
        } else if (isFooter(pos)) {
            View v = footers.get(pos - postList.size() - headers.size());
            //add our view to a footer view and display it
            prepareHeaderFooter((HeaderFooterViewHolder) holder, v);
        } else {

            final int position = pos - headers.size();
            final ChanPost threadItem = postList.get(position);
            final int threadId = threadItem.getNo();
            final HolderAbstractor viewHolder = new HolderAbstractor(holder);

            if (clickListener != null) {
                viewHolder.cardRoot.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clickListener.onPostItemClick(v, postList, position, boardTitle, boardName, threadId);
                    }
                });
            }

            if (viewHolder.flagIcon != null) {
                final String country = threadItem.getCountry();
                if (country != null && !"".equals(country)) {
                    final String url = flagUrl + country.toLowerCase() + ".gif";
                    viewHolder.flagIcon.setVisibility(View.VISIBLE);

                    Glide.with(activity)
                            .load(url)
                            .crossFade()
                            .into(viewHolder.flagIcon);
                } else {
                    viewHolder.flagIcon.setVisibility(View.GONE);
                }

            }

            viewHolder.threadNo.setText("" + threadId);

            if (threadItem.getFilename() != null && !threadItem.getFilename().equals("")) {
                if (managerType == ManagerType.GRID || managerType == ManagerType.STAGGERED_GRID) {
                    viewHolder.thumbUrl.setAspectRatio(threadItem.getThumbnailWidth(), threadItem.getThumbnailHeight());
                } else {
                    viewHolder.thumbUrl.setAspectRatioEnabled(false);
                }
                viewHolder.thumbUrl.setVisibility(View.VISIBLE);
                viewHolder.thumbUrl.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final Bundle args = new Bundle();
                        args.putInt(Extras.EXTRAS_GALLERY_TYPE, 1);
                        args.putInt(Extras.EXTRAS_THREAD_ID, threadId);
                        args.putInt(Extras.EXTRAS_POSITION, 0);
                        args.putString(Extras.EXTRAS_BOARD_NAME, boardName);

                        final Intent galleryIntent = new Intent(activity, GalleryActivity.class);
                        galleryIntent.putExtras(args);
                        galleryIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

                        activity.startActivity(galleryIntent);

                    }
                });

                final String url = chanConnector.getThumbUrl(boardName, threadItem.getTim(), secureConnection);
                Glide.with(activity)
                        .load(url)
                        .crossFade()
                        .into(viewHolder.thumbUrl);

            } else {
                viewHolder.thumbUrl.setVisibility(View.GONE);
                Glide.clear(viewHolder.thumbUrl);
            }

            if (threadItem.getSubject() != null) {
                viewHolder.subject.setText(threadItem.getSubject());
                viewHolder.subject.setVisibility(View.VISIBLE);
            } else {
                viewHolder.subject.setVisibility(View.GONE);
            }

            if (viewHolder.name != null) {
                if (threadItem.getName() != null) {
                    viewHolder.name.setText(threadItem.getName());
                    viewHolder.name.setVisibility(View.VISIBLE);
                } else {
                    viewHolder.name.setVisibility(View.GONE);
                }
            }

            if (viewHolder.userId != null) {
                if (threadItem.getId() != null) {
                    viewHolder.userId.setText(threadItem.getId());
                    viewHolder.userId.setVisibility(View.VISIBLE);
                } else {
                    viewHolder.userId.setVisibility(View.GONE);
                }
            }

            viewHolder.comment.setText(threadItem.getComment());

            if (managerType == ManagerType.LIST) {
                viewHolder.replyCount.setText(chanConnector.getRepliesCountText(threadItem.getReplies()));
                viewHolder.imageCount.setText(chanConnector.getImageCountText(threadItem.getImages()));
            } else {
                viewHolder.replyCount.setText(String.valueOf(threadItem.getReplies()));
                viewHolder.imageCount.setText(String.valueOf(threadItem.getImages()));
            }

            if (viewHolder.timestamp != null) {
                viewHolder.timestamp.setText(threadItem.getNow());
            }

            viewHolder.menuIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    final PopupMenu menu = new PopupMenu(v.getContext(), v);
                    menu.inflate(R.menu.catalog_item_menu);
                    menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(final MenuItem menuItem) {
                            if (menuItem.getItemId() == R.id.gallery_menu && activity instanceof GalleryMenuItemClickListener) {
                                ((GalleryMenuItemClickListener) activity).onGalleryMenuItemClick(boardName, threadId);
                                return true;
                            } else if (menuItem.getItemId() == R.id.report_menu) {
                                final String url = "https://" + activity.getString(R.string.sys_link);
                                final String path = activity.getString(R.string.report_path, boardName, threadId);
                                final Intent reportIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url + path));

                                activity.startActivity(reportIntent);

                                return true;

                            } else if (menuItem.getItemId() == R.id.copy_link_menu) {
                                final String url = "https://" + activity.getString(R.string.board_link);
                                final String path = activity.getString(R.string.raw_thread_path, boardName, threadId);

                                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                                android.content.ClipData clip = android.content.ClipData.newPlainText("thread link", url + path);
                                clipboard.setPrimaryClip(clip);

                                Toast.makeText(activity, R.string.text_copied_to_clipboard, Toast.LENGTH_SHORT).show();

                                return true;

                            } else if (menuItem.getItemId() == R.id.open_link_menu) {
                                final String url = "https://" + activity.getString(R.string.board_link);
                                final String path = activity.getString(R.string.raw_thread_path, boardName, threadId);
                                final Intent openLinkIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url + path));

                                activity.startActivity(openLinkIntent);

                                return true;

                            }

                            return false;
                        }
                    });
                    menu.show();
                }
            });

            if (viewHolder.bookmarkButton != null) {
                if (threadItem.isWatched()) {
                    viewHolder.bookmarkButton.setText(R.string.ic_bookmark_set);
                } else {
                    viewHolder.bookmarkButton.setText(R.string.ic_bookmark_unset);
                }

                viewHolder.bookmarkButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final boolean watched = threadItem.isWatched();
                        threadItem.setWatched(!watched);

                        if (watched) {
                            viewHolder.bookmarkButton.setText(R.string.ic_bookmark_unset);
                            HistoryTableConnection.removeHistory(boardName, threadId)
                                    .doOnNext(new Action1<Boolean>() {
                                        @Override
                                        public void call(Boolean aBoolean) {
                                            MimiUtil.getInstance().removeBookmark(boardName, threadId);
                                        }
                                    })
                                    .compose(DatabaseUtils.<Boolean>applySchedulers())
                                    .subscribe();
                            RefreshScheduler.getInstance().removeThread(boardName, threadId);
                            ThreadRegistry.getInstance().remove(threadId);
                        } else {
                            viewHolder.bookmarkButton.setText(R.string.ic_bookmark_set);
                            RefreshScheduler.getInstance().addThread(boardName, threadId, true);
                            ThreadRegistry.getInstance().add(boardName, threadId, postList.size(), true);
                        }

                        RxUtil.safeUnsubscribe(historyInfoSubscription);
                        historyInfoSubscription = HistoryTableConnection.putHistory(boardName, threadItem, postCount, !watched).subscribe();
                    }
                });
            }
        }

    }

    public void addPosts(final List<ChanPost> posts) {
        if (postList != null) {
            for (final ChanPost post : posts) {
                if (postList.indexOf(post) < 0) {
                    postList.add(post);
                }
            }

            postCount = postList.size();

            notifyDataSetChanged();
        }
    }

    public void clear() {
        if (postList == null) {
            postList = new ArrayList<>();
        } else {
            postList.clear();
        }

        notifyDataSetChanged();
    }

    //add a header to the adapter
    public void addHeader(View header) {
        if (!headers.contains(header)) {
            headers.add(header);
            //animate
            notifyItemInserted(headers.size() - 1);
        }
    }

    //remove a header from the adapter
    public void removeHeader(View header) {
        if (headers.contains(header)) {
            //animate
            notifyItemRemoved(headers.indexOf(header));
            headers.remove(header);
        }
    }

    //add a footer to the adapter
    public void addFooter(View footer) {
        if (!footers.contains(footer)) {
            footers.add(footer);
            //animate
            notifyItemInserted(headers.size() + postList.size() + footers.size() - 1);
        }
    }

    //remove a footer from the adapter
    public void removeFooter(View footer) {
        if (footers.contains(footer)) {
            //animate
            notifyItemRemoved(headers.size() + postList.size() + footers.indexOf(footer));
            footers.remove(footer);
        }
    }

    private void prepareHeaderFooter(HeaderFooterViewHolder vh, View view) {

        //if it's a staggered grid, span the whole layout
        if (managerType == ManagerType.STAGGERED_GRID) {
            StaggeredGridLayoutManager.LayoutParams layoutParams = new StaggeredGridLayoutManager.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.setFullSpan(true);
            vh.itemView.setLayoutParams(layoutParams);
        }

        //if the view already belongs to another layout, remove it
        if (view.getParent() != null) {
            ((ViewGroup) view.getParent()).removeView(view);
        }

        //empty out our FrameLayout and replace with our header/footer
        vh.base.removeAllViews();
        vh.base.addView(view);

    }

    public void setPosts(List<ChanPost> posts, String boardName, String boardTitle) {
        if (posts == null || posts.size() == 0) {
            return;
        }

        this.boardName = boardName;
        this.boardTitle = boardTitle;
        postList = posts;
        postCount = posts.size();
        setup();

        notifyDataSetChanged();
    }

    public List<ChanPost> getPosts() {
        return postList;
    }

    protected class ViewHolderListItem extends RecyclerView.ViewHolder {
        TextView threadNo;
        TextView name;
        TextView userId;
        TextView subject;
        TextView comment;
        GridItemImageView thumbUrl;
        TextView replyCount;
        TextView imageCount;
        TextView timestamp;
        View menuIcon;
        ViewGroup postHeader;
        ImageView flagIcon;
        TextView bookmarkButton;
        CardView cardRoot;

        private ViewHolderListItem(final View root) {
            super(root);

            name = (TextView) root.findViewById(R.id.user_name);
            threadNo = (TextView) root.findViewById(R.id.thread_id);
            userId = (TextView) root.findViewById(R.id.user_id);
            subject = (TextView) root.findViewById(R.id.subject);
            comment = (TextView) root.findViewById(R.id.comment);
            thumbUrl = (GridItemImageView) root.findViewById(R.id.header_thumbnail);
            replyCount = (TextView) root.findViewById(R.id.reply_count);
            imageCount = (TextView) root.findViewById(R.id.image_count);
            timestamp = (TextView) root.findViewById(R.id.timestamp);
            menuIcon = root.findViewById(R.id.post_item_menu);
            postHeader = (ViewGroup) root.findViewById(R.id.post_header);
            flagIcon = (ImageView) root.findViewById(R.id.flag_icon);
            bookmarkButton = (TextView) root.findViewById(R.id.bookmark_button);
            cardRoot = (CardView) root.findViewById(R.id.post_card);
        }
    }

    protected class ViewHolderGridItem extends RecyclerView.ViewHolder {
        TextView threadNo;
        TextView name;
        TextView userId;
        TextView subject;
        TextView comment;
        GridItemImageView thumbUrl;
        TextView replyCount;
        TextView imageCount;
        TextView timestamp;
        View menuIcon;
        ViewGroup postHeader;
        ImageView flagIcon;
        TextView bookmarkButton;
        CardView cardRoot;

        private ViewHolderGridItem(final View root) {
            super(root);

            name = (TextView) root.findViewById(R.id.user_name);
            threadNo = (TextView) root.findViewById(R.id.thread_id);
            userId = (TextView) root.findViewById(R.id.user_id);
            subject = (TextView) root.findViewById(R.id.subject);
            comment = (TextView) root.findViewById(R.id.comment);
            thumbUrl = (GridItemImageView) root.findViewById(R.id.header_thumbnail);
            replyCount = (TextView) root.findViewById(R.id.reply_count);
            imageCount = (TextView) root.findViewById(R.id.image_count);
            timestamp = (TextView) root.findViewById(R.id.timestamp);
            menuIcon = root.findViewById(R.id.post_item_menu);
            postHeader = (ViewGroup) root.findViewById(R.id.post_header);
            flagIcon = (ImageView) root.findViewById(R.id.flag_icon);
            bookmarkButton = (TextView) root.findViewById(R.id.bookmark_button);
            cardRoot = (CardView) root.findViewById(R.id.post_card);
        }
    }

    private static class HolderAbstractor {
        public final TextView threadNo;
        public final TextView name;
        public final TextView userId;
        public final TextView subject;
        public final TextView comment;
        public final GridItemImageView thumbUrl;
        public final TextView replyCount;
        public final TextView imageCount;
        public final TextView timestamp;
        public final View menuIcon;
        public final ViewGroup postHeader;
        public final ImageView flagIcon;
        public final TextView bookmarkButton;
        public final CardView cardRoot;

        public HolderAbstractor(RecyclerView.ViewHolder item) {
            if (item instanceof ViewHolderGridItem) {
                threadNo = ((ViewHolderGridItem) item).threadNo;
                name = ((ViewHolderGridItem) item).name;
                userId = ((ViewHolderGridItem) item).userId;
                subject = ((ViewHolderGridItem) item).subject;
                comment = ((ViewHolderGridItem) item).comment;
                thumbUrl = ((ViewHolderGridItem) item).thumbUrl;
                replyCount = ((ViewHolderGridItem) item).replyCount;
                imageCount = ((ViewHolderGridItem) item).imageCount;
                timestamp = ((ViewHolderGridItem) item).timestamp;
                menuIcon = ((ViewHolderGridItem) item).menuIcon;
                postHeader = ((ViewHolderGridItem) item).postHeader;
                flagIcon = ((ViewHolderGridItem) item).flagIcon;
                bookmarkButton = ((ViewHolderGridItem) item).bookmarkButton;
                cardRoot = ((ViewHolderGridItem) item).cardRoot;
            } else if (item instanceof ViewHolderListItem) {
                threadNo = ((ViewHolderListItem) item).threadNo;
                name = ((ViewHolderListItem) item).name;
                userId = ((ViewHolderListItem) item).userId;
                subject = ((ViewHolderListItem) item).subject;
                comment = ((ViewHolderListItem) item).comment;
                thumbUrl = ((ViewHolderListItem) item).thumbUrl;
                replyCount = ((ViewHolderListItem) item).replyCount;
                imageCount = ((ViewHolderListItem) item).imageCount;
                timestamp = ((ViewHolderListItem) item).timestamp;
                menuIcon = ((ViewHolderListItem) item).menuIcon;
                postHeader = ((ViewHolderListItem) item).postHeader;
                flagIcon = ((ViewHolderListItem) item).flagIcon;
                bookmarkButton = ((ViewHolderListItem) item).bookmarkButton;
                cardRoot = ((ViewHolderListItem) item).cardRoot;
            } else {
                threadNo = null;
                name = null;
                userId = null;
                subject = null;
                comment = null;
                thumbUrl = null;
                replyCount = null;
                imageCount = null;
                timestamp = null;
                menuIcon = null;
                postHeader = null;
                flagIcon = null;
                bookmarkButton = null;
                cardRoot = null;
            }
        }
    }

    @Override
    public Filter getFilter() {
        if (postFilter == null) {
            postFilter = new PostFilter(postList);
        }

        return postFilter;
    }

    private class PostFilter extends Filter {
        private List<ChanPost> posts;

        public PostFilter(List<ChanPost> posts) {
            if (posts == null) {
                throw new NullPointerException("Cannot pass null post list into constructor");
            }
            this.posts = posts;
        }

        @Override
        protected FilterResults performFiltering(final CharSequence constraint) {
            final FilterResults results = new FilterResults();

            if (constraint == null || constraint.length() == 0) {
                searching = false;
                results.count = posts.size();
                results.values = posts;
            } else {
                final ArrayList<ChanPost> filteredList = new ArrayList<ChanPost>(posts.size());
                for (ChanPost post : posts) {
                    if (((post.getComment() != null) && (post.getComment().toString().toLowerCase().contains(constraint.toString().toLowerCase()))) ||
                            ((post.getSubject() != null) && post.getSubject().toString().toLowerCase().contains(constraint.toString().toLowerCase()))) {
                        filteredList.add(post);
                    }
                }

                searching = true;
                results.count = filteredList.size();
                results.values = filteredList;
            }

            if (results.values == null) {
                results.count = 0;
                results.values = new ArrayList<ChanPost>();
            }

            return results;
        }

        @Override
        protected void publishResults(final CharSequence constraint, final FilterResults results) {
            postList = (ArrayList<ChanPost>) results.values;
            notifyDataSetChanged();
        }
    }

}
