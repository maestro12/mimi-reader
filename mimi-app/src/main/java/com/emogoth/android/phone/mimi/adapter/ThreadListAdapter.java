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

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.activity.GalleryActivity;
import com.emogoth.android.phone.mimi.dialog.RepliesDialog;
import com.emogoth.android.phone.mimi.interfaces.OnThumbnailClickListener;
import com.emogoth.android.phone.mimi.interfaces.ReplyMenuClickListener;
import com.emogoth.android.phone.mimi.model.HeaderFooterViewHolder;
import com.emogoth.android.phone.mimi.util.Extras;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.ThreadRegistry;
import com.emogoth.android.phone.mimi.view.LongClickLinkMovementMethod;
import com.mimireader.chanlib.models.ChanPost;
import com.mimireader.chanlib.models.ChanThread;
import com.mimireader.chanlib.util.ChanUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ThreadListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String LOG_TAG = ThreadListAdapter.class.getSimpleName();

    public static final int VIEW_HEADER = 10;
    public static final int VIEW_TOP_LIST_ITEM = 11;
    public static final int VIEW_NORMAL_LIST_ITEM = 12;
    public static final int VIEW_FOOTER = 13;

    private final LayoutInflater inflater;
    private final FragmentManager fm;
    private final String flagUrl;

    private FragmentActivity activity;
    private ChanThread thread;
    private String boardName;
    private CharSequence[] timeMap;
    private OnThumbnailClickListener thumbnailClickListener;
    private ReplyMenuClickListener replyMenuClickListener;

    private int defaultPostBackground;
    private int highlightPostBackground;

    private int lastPosition = 0;
    private List<Integer> userPostList;

    private List<View> headers = new ArrayList<>();
    private List<View> footers = new ArrayList<>();

    private List<String> repliesText = new ArrayList<>();
    private List<String> imagesText = new ArrayList<>();

    private int[] colorList;

    private final Map<Integer, String> thumbUrlMap = new HashMap<>();
    private final Map<Integer, String> fullImageUrlMap = new HashMap<>();


    public ThreadListAdapter(final FragmentActivity activity, final FragmentManager fm, final ChanThread thread, final String boardName) {
        this.activity = activity;
        this.fm = fm;
        this.thread = thread;
        this.boardName = boardName;
        this.inflater = LayoutInflater.from(activity);

        this.flagUrl = MimiUtil.httpOrHttps(activity) + activity.getString(R.string.flag_int_link);

        if (MimiUtil.getInstance().getTheme() == MimiUtil.THEME_LIGHT) {
            defaultPostBackground = R.color.row_item_background_light;
            highlightPostBackground = R.color.post_highlight_light;
        } else if (MimiUtil.getInstance().getTheme() == MimiUtil.THEME_DARK) {
            defaultPostBackground = R.color.row_item_background_dark;
            highlightPostBackground = R.color.post_highlight_dark;
        } else {
            defaultPostBackground = R.color.row_item_background_black;
            highlightPostBackground = R.color.post_highlight_black;
        }

        setupThread();

    }

    private void setupThread() {
        this.timeMap = new CharSequence[thread.getPosts().size()];
        this.colorList = new int[thread.getPosts().size()];
        this.userPostList = ThreadRegistry.getInstance().getUserPosts(boardName, thread.getThreadId());

        for (int i = 0; i < thread.getPosts().size(); i++) {
            final ChanPost post = thread.getPosts().get(i);
            final CharSequence dateString = DateUtils.getRelativeTimeSpanString(
                    post.getTime() * 1000L,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE);

            if (post.getFilename() != null && !post.getFilename().equals("")) {
                thumbUrlMap.put(post.getNo(), MimiUtil.httpOrHttps(activity) + activity.getString(R.string.thumb_link) + activity.getString(R.string.thumb_path, boardName, post.getTim()));
                fullImageUrlMap.put(post.getNo(), MimiUtil.httpOrHttps(activity) + activity.getString(R.string.image_link) + activity.getString(R.string.full_image_path, boardName, post.getTim(), post.getExt()));
            }

            repliesText.add(activity.getResources().getQuantityString(R.plurals.replies_plural, post.getRepliesFrom().size(), post.getRepliesFrom().size()));
            imagesText.add(activity.getResources().getQuantityString(R.plurals.image_plural, post.getImages(), post.getImages()));

            timeMap[i] = dateString;

            if (!TextUtils.isEmpty(post.getId())) {
                colorList[i] = ChanUtil.calculateColorBase(post.getId());
            }

            if (post.getFsize() > 0) {
                post.setHumanReadableFileSize(MimiUtil.humanReadableByteCount(post.getFsize(), true) + " " + post.getExt().substring(1).toUpperCase());
            }
        }

    }

    private boolean isHeader(int position) {
        return (position < headers.size());
    }

    private boolean isFooter(int position) {
        return (position >= headers.size() + thread.getPosts().size());
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
            notifyItemInserted(headers.size() + thread.getPosts().size() + footers.size() - 1);
        }
    }

    //remove a footer from the adapter
    public void removeFooter(View footer) {
        if (footers.contains(footer)) {
            //animate
            notifyItemRemoved(headers.size() + thread.getPosts().size() + footers.indexOf(footer));
            footers.remove(footer);
        }
    }

    public int getLastPosition() {
        return lastPosition;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View v;
        switch (viewType) {
            case VIEW_HEADER:
                FrameLayout headerFrameLayout = new FrameLayout(parent.getContext());
                //make sure it fills the space
                headerFrameLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, MimiUtil.dpToPx(10)));
                return new HeaderFooterViewHolder(headerFrameLayout);
            case VIEW_TOP_LIST_ITEM:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.thread_first_post_item, parent, false);
                return new ViewHolder(v);
            case VIEW_NORMAL_LIST_ITEM:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.thread_post_item, parent, false);
                return new ViewHolder(v);
            case VIEW_FOOTER:
                FrameLayout footerFrameLayout = new FrameLayout(parent.getContext());
                //make sure it fills the space
                footerFrameLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, MimiUtil.dpToPx(60)));
                return new HeaderFooterViewHolder(footerFrameLayout);
            default:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_layout, parent, false);
                return new ViewHolder(v);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        if (thread != null && thread.getPosts() != null) {
            return headers.size() + thread.getPosts().size() + footers.size();
        }

        return 0;
    }

    public void setThread(final ChanThread thread) {
        this.thread = thread;

        this.thumbUrlMap.clear();
        this.fullImageUrlMap.clear();

        this.repliesText.clear();
        this.imagesText.clear();

        setupThread();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (isHeader(position)) {
            return VIEW_HEADER;
        } else if (isFooter(position)) {
            return VIEW_FOOTER;
        } else if (position == headers.size()) {
            return VIEW_TOP_LIST_ITEM;
        } else {
            return VIEW_NORMAL_LIST_ITEM;
        }
    }

    private void prepareHeaderFooter(HeaderFooterViewHolder vh, View view) {

        //if the view already belongs to another layout, remove it
        if (view.getParent() != null) {
            ((ViewGroup) view.getParent()).removeView(view);
        }

        //empty out our FrameLayout and replace with our header/footer
        vh.base.removeAllViews();
        vh.base.addView(view);

    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder vh, final int pos) {

        if (isHeader(pos) || isFooter(pos)) {
            if (isHeader(pos)) {
                View v = headers.get(pos);
                //add our view to a header view and display it
                prepareHeaderFooter((HeaderFooterViewHolder) vh, v);
            } else {
                View v = footers.get(pos - thread.getPosts().size() - headers.size());
                //add our view to a footer view and display it
                prepareHeaderFooter((HeaderFooterViewHolder) vh, v);
            }

            return;
        }

        final ViewHolder viewHolder = (ViewHolder) vh;
        final int position = pos - headers.size();
        final ChanPost postItem = thread.getPosts().get(position);

        if (postItem == null) {
            return;
        }

        if (lastPosition < position) {
            lastPosition = position;
        }

        if (viewHolder.postContainer != null) {
            if (userPostList.contains(postItem.getNo())) {
                viewHolder.postContainer.setBackgroundResource(highlightPostBackground);
            } else {
                viewHolder.postContainer.setBackgroundResource(defaultPostBackground);
            }
        }

        viewHolder.thumbnailContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (thumbnailClickListener != null) {
                    final ArrayList<ChanPost> postsWithImages = GalleryPagerAdapter.getPostsWithImages(thread);
                    final int position = postsWithImages.indexOf(postItem);
                    final int threadId = thread.getPosts().get(0).getNo();

                    thumbnailClickListener.onThumbnailClick(postsWithImages, threadId, position, boardName);
                }
            }
        });

        final String thumbUrl = thumbUrlMap.get(postItem.getNo());
        final String imageUrl = fullImageUrlMap.get(postItem.getNo());
        final String country = postItem.getCountry();

        if (country != null && !"".equals(country)) {
            final String url = flagUrl + country.toLowerCase() + ".gif";

            viewHolder.flagIcon.setVisibility(View.VISIBLE);
            viewHolder.flagIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setMessage(postItem.getCountryName())
                            .setCancelable(true)
                            .show()
                            .setCanceledOnTouchOutside(true);
                }
            });
            Glide.with(activity)
                    .load(url)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .crossFade()
                    .into(viewHolder.flagIcon);
        } else {
            viewHolder.flagIcon.setVisibility(View.GONE);
            viewHolder.flagIcon.setOnClickListener(null);
            Glide.clear(viewHolder.flagIcon);
        }

        viewHolder.menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final PopupMenu menu = new PopupMenu(v.getContext(), v);
                if (position == 0) {
                    menu.inflate(R.menu.thread_first_post_menu);
                } else {
                    menu.inflate(R.menu.thread_menu);
                }

                final MenuItem imageInfoItem = menu.getMenu().findItem(R.id.image_info_menu_item);
                if (imageUrl == null && imageInfoItem != null) {
                    imageInfoItem.setVisible(false);
                }

                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(final MenuItem menuItem) {
                        int sdk = android.os.Build.VERSION.SDK_INT;
                        final String url = "https://" + activity.getString(R.string.board_link);
                        final String path = activity.getString(R.string.raw_thread_path, boardName, thread.getThreadId());
                        final String link;
                        if (postItem.getNo() == thread.getThreadId()) {
                            link = url + path;
                        } else {
                            link = url + path + "#q" + postItem.getNo();
                        }
                        switch (menuItem.getItemId()) {
                            case R.id.reply_menu_item:
                                if (replyMenuClickListener != null) {
                                    replyMenuClickListener.onReply(v, postItem.getNo());
                                }

                                return true;
                            case R.id.quote_menu_item:
                                if (replyMenuClickListener != null) {
                                    replyMenuClickListener.onQuote(v, postItem.getNo(), postItem);
                                }

                                return true;
                            case R.id.copy_text:
                                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                                android.content.ClipData clip = android.content.ClipData.newPlainText("thread info", thread.getPosts().get(position).getComment());
                                clipboard.setPrimaryClip(clip);

                                Toast.makeText(activity, R.string.text_copied_to_clipboard, Toast.LENGTH_SHORT).show();
                                return true;
                            case R.id.image_info_menu_item:
                                createImageInfoDialog(postItem, imageUrl);
                                return true;

                            case R.id.report_menu:
                                final String reportUrl = "https://" + activity.getString(R.string.sys_link);
                                final String reportPath = activity.getString(R.string.report_path, boardName, thread.getPosts().get(position).getNo());
                                final Intent reportIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(reportUrl + reportPath));

                                activity.startActivity(reportIntent);
                                return true;

                            case R.id.copy_link_menu:
                                ClipboardManager linkClipboard = (android.content.ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                                ClipData linkClip = android.content.ClipData.newPlainText("thread link", link);
                                linkClipboard.setPrimaryClip(linkClip);

                                Toast.makeText(activity, R.string.text_copied_to_clipboard, Toast.LENGTH_SHORT).show();

                                return true;

                            case R.id.open_link_menu:
                                final Intent openLinkIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                                activity.startActivity(openLinkIntent);

                                return true;

                            default:
                                return false;

                        }
                    }
                });
                menu.show();
            }
        });

        viewHolder.threadId.setText(String.valueOf(postItem.getNo()));

        viewHolder.userName.setText(postItem.getDisplayedName());
        viewHolder.postTime.setText(timeMap[position]);

        if (!TextUtils.isEmpty(postItem.getSubject())) {
            viewHolder.subject.setText(postItem.getSubject());
            viewHolder.subject.setVisibility(View.VISIBLE);
        } else {
            viewHolder.subject.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(postItem.getId())) {
            viewHolder.userId.setBackgroundColor(colorList[position]);
            viewHolder.userId.setText(postItem.getId());
            viewHolder.userId.setVisibility(View.VISIBLE);
            viewHolder.userId.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RepliesDialog.newInstance(thread, postItem.getId()).show(activity.getSupportFragmentManager(), RepliesDialog.DIALOG_TAG);
                }
            });
        } else {
            viewHolder.userId.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(postItem.getTrip())) {
            viewHolder.tripCode.setText(postItem.getTrip());
            viewHolder.tripCode.setVisibility(View.VISIBLE);
        } else {
            viewHolder.tripCode.setVisibility(View.GONE);
        }

        if (postItem.getComment() != null) {
            viewHolder.comment.setText(postItem.getComment());
        } else {
            viewHolder.comment.setText("");
        }

        if (viewHolder.replyButton != null) {
            if (postItem.isClosed()) {
                viewHolder.replyButton.setVisibility(View.GONE);
            } else {
                viewHolder.replyButton.setVisibility(View.VISIBLE);
                viewHolder.replyButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (replyMenuClickListener != null) {
                            replyMenuClickListener.onReply(v, postItem.getNo());
                        }
                    }
                });
            }
        }

        if ((postItem.getRepliesFrom() != null && postItem.getRepliesFrom().size() > 0) || (viewHolder.galleryImageCount != null && postItem.getImages() > 0)) {

            if (postItem.getRepliesFrom() != null) {
                viewHolder.replyCount.setText(repliesText.get(position));
                viewHolder.replyCount.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        RepliesDialog.newInstance(thread, postItem).show(activity.getSupportFragmentManager(), RepliesDialog.DIALOG_TAG);
                    }
                });
            }

            if (viewHolder.galleryImageCount != null) {
                viewHolder.galleryImageCount.setText(imagesText.get(position));
                viewHolder.galleryImageCount.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final Bundle args = new Bundle();
                        args.putInt(Extras.EXTRAS_GALLERY_TYPE, 0);
                        ThreadRegistry.getInstance().setPosts(postItem.getNo(), GalleryPagerAdapter.getPostsWithImages(thread.getPosts()));
//                        args.putParcelableArrayList(Extras.EXTRAS_POST_LIST, GalleryPagerAdapter.getPostsWithImages(thread));
                        args.putInt(Extras.EXTRAS_THREAD_ID, postItem.getNo());
                        args.putInt(Extras.EXTRAS_POSITION, position);
                        args.putString(Extras.EXTRAS_BOARD_NAME, boardName);

                        final Intent galleryIntent = new Intent(activity, GalleryActivity.class);
                        galleryIntent.putExtras(args);
                        galleryIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

                        activity.startActivity(galleryIntent);
                    }
                });

                viewHolder.galleryImageCount.setVisibility(View.VISIBLE);

            }

            viewHolder.replyCount.setVisibility(View.VISIBLE);

        } else {
            viewHolder.replyCount.setVisibility(View.INVISIBLE);

            if (viewHolder.galleryImageCount != null) {
                viewHolder.galleryImageCount.setVisibility(View.INVISIBLE);
            }
        }

        if (viewHolder != null && viewHolder.replyContainer != null) {
            if (viewHolder.replyCount.getVisibility() != View.VISIBLE && viewHolder.galleryImageCount == null) {
                viewHolder.replyContainer.setVisibility(View.GONE);
            } else {
                viewHolder.replyContainer.setVisibility(View.VISIBLE);
            }
        }

        viewHolder.thumbnailContainer.setVisibility(View.INVISIBLE);
        if (postItem.getFilename() != null && !postItem.getFilename().equals("")) {

            if (viewHolder.thumbnailInfoContainer != null) {
                viewHolder.thumbnailInfoContainer.setVisibility(View.VISIBLE);
            }

            viewHolder.thumbnailContainer.setVisibility(View.VISIBLE);
            Glide.with(activity)
                    .load(thumbUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .crossFade()
                    .into(viewHolder.thumbUrl);

        } else {
            viewHolder.thumbnailContainer.setVisibility(View.GONE);
            if (viewHolder.thumbnailInfoContainer != null) {
                viewHolder.thumbnailInfoContainer.setVisibility(View.GONE);
            }

            Glide.clear(viewHolder.thumbUrl);
        }
    }

    private void createImageInfoDialog(final ChanPost postItem, final String imageUrl) {
        final View root = inflater.inflate(R.layout.dialog_image_info_buttons, null, false);
        final TextView fileName = (TextView) root.findViewById(R.id.file_name);
        final TextView dimensions = (TextView) root.findViewById(R.id.dimensions);
        final View iqdb = root.findViewById(R.id.iqdb_button);
        final View google = root.findViewById(R.id.google_button);
        final View saucenao = root.findViewById(R.id.saucenao_button);
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final AlertDialog dialog;

        fileName.setText(postItem.getFilename() + postItem.getExt());
        dimensions.setText(postItem.getWidth() + " x " + postItem.getHeight());
        builder.setTitle(R.string.image_info)
                .setView(root)
                .setPositiveButton(R.string.exit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        dialog = builder.create();

        iqdb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity != null) {
                    final String url;
                    try {
                        url = activity.getString(R.string.iqdb_image_search_link, URLEncoder.encode(imageUrl, "UTF-8"));
                        final Intent searchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

                        activity.startActivity(searchIntent);
                        dialog.dismiss();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        google.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity != null) {
                    final String url;
                    try {
                        url = activity.getString(R.string.google_image_search_link, URLEncoder.encode(imageUrl, "UTF-8"));
                        final Intent searchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

                        activity.startActivity(searchIntent);
                        dialog.dismiss();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        saucenao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity != null) {
                    final String url;
                    try {
                        url = activity.getString(R.string.saucenao_image_search_link, URLEncoder.encode(imageUrl, "UTF-8"));
                        final Intent searchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

                        activity.startActivity(searchIntent);
                        dialog.dismiss();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        dialog.show();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewGroup postContainer;
        public TextView threadId;
        public ViewGroup thumbnailContainer;
        public ViewGroup thumbnailInfoContainer;
        public TextView userName;
        public TextView postTime;
        public TextView userId;
        public TextView tripCode;
        public TextView subject;
        public TextView comment;
        public TextView replyCount;
        public TextView replyButton;
        public TextView galleryImageCount;
        public ImageView thumbUrl;
        public View menuButton;
        public View replyContainer;
        public ImageView flagIcon;

        public ViewHolder(final View root) {
            super(root);

            postContainer = (ViewGroup) root.findViewById(R.id.post_container);
            threadId = (TextView) root.findViewById(R.id.thread_id);
            userName = (TextView) root.findViewById(R.id.user_name);
            postTime = (TextView) root.findViewById(R.id.timestamp);
            userId = (TextView) root.findViewById(R.id.user_id);
            tripCode = (TextView) root.findViewById(R.id.tripcode);
            subject = (TextView) root.findViewById(R.id.subject);
            comment = (TextView) root.findViewById(R.id.comment);
            replyCount = (TextView) root.findViewById(R.id.replies_number);
            replyButton = (TextView) root.findViewById(R.id.reply_button);
            replyContainer = root.findViewById(R.id.replies_row);
            galleryImageCount = (TextView) root.findViewById(R.id.image_count);
            thumbUrl = (ImageView) root.findViewById(R.id.thumbnail);
            thumbnailInfoContainer = (ViewGroup) root.findViewById(R.id.thumbnail_info_container);
            thumbnailContainer = (ViewGroup) root.findViewById(R.id.thumbnail_container);
            flagIcon = (ImageView) root.findViewById(R.id.flag_icon);
            menuButton = root.findViewById(R.id.menu_button);

            comment.setMovementMethod(LongClickLinkMovementMethod.getInstance());
        }
    }

    public void setOnThumbnailClickListener(final OnThumbnailClickListener listener) {
        thumbnailClickListener = listener;
    }

    public void setOnReplyMenuClickListener(final ReplyMenuClickListener listener) {
        replyMenuClickListener = listener;
    }
}
