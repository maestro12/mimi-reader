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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.widget.PopupMenu;
import androidx.collection.LongSparseArray;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.activity.GalleryActivity2;
import com.emogoth.android.phone.mimi.app.MimiApplication;
import com.emogoth.android.phone.mimi.dialog.RepliesDialog;
import com.emogoth.android.phone.mimi.interfaces.OnThumbnailClickListener;
import com.emogoth.android.phone.mimi.interfaces.ReplyMenuClickListener;
import com.emogoth.android.phone.mimi.model.HeaderFooterViewHolder;
import com.emogoth.android.phone.mimi.util.GlideApp;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.ThreadRegistry;
import com.emogoth.android.phone.mimi.view.LongClickLinkMovementMethod;
import com.emogoth.android.phone.mimi.view.gallery.GalleryPagerAdapter;
import com.mimireader.chanlib.models.ChanPost;
import com.mimireader.chanlib.models.ChanThread;
import com.mimireader.chanlib.util.ChanUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class ThreadListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {
    private static final String LOG_TAG = ThreadListAdapter.class.getSimpleName();

    public static final int VIEW_HEADER = 10;
    public static final int VIEW_TOP_LIST_ITEM = 11;
    public static final int VIEW_NORMAL_LIST_ITEM = 12;
    public static final int VIEW_FOOTER = 13;

    private static final int FIND_NO_RESULTS = -2;

    private final LayoutInflater inflater;
    private final FragmentManager fm;
    private final String flagUrl;
    private final String trollUrl;

    private final VectorDrawableCompat pinDrawable;
    private final VectorDrawableCompat lockDrawable;

    private FragmentActivity activity;
    private ChanThread thread;
    private String boardName;
    private CharSequence[] timeMap;
    private OnThumbnailClickListener thumbnailClickListener;
    private ReplyMenuClickListener replyMenuClickListener;

    private int defaultPostBackground;
    private int highlightPostBackground;
    private int highlightTextBackground;
    private int selectedTextBackground;

    private int lastPosition = 0;
    private List<Long> userPostList;

    private List<View> headers = new ArrayList<>();
    private List<View> footers = new ArrayList<>();

    private List<String> repliesText = new ArrayList<>();
    private List<String> imagesText = new ArrayList<>();

    private int[] colorList;

    private final LongSparseArray<String> thumbUrlMap = new LongSparseArray<>();
    private final LongSparseArray<String> fullImageUrlMap = new LongSparseArray<>();
    private LinkedHashMap<Long, TextSearchResult> foundPosts = new LinkedHashMap<>();
    private PostFilter postFilter;
    private int foundPos = FIND_NO_RESULTS;
    private OnFilterUpdateCallback filterUpdateCallback;


    public ThreadListAdapter(final FragmentActivity activity, final FragmentManager fm, final ChanThread thread, final String boardName) {
        this.activity = activity;
        this.fm = fm;
        this.thread = thread;
        this.boardName = boardName;
        this.inflater = LayoutInflater.from(activity);

        this.flagUrl = MimiUtil.https() + activity.getString(R.string.flag_int_link);
        this.trollUrl = MimiUtil.https() + activity.getString(R.string.flag_pol_link);

        if (MimiUtil.getInstance().getTheme() == MimiUtil.THEME_LIGHT) {
            defaultPostBackground = R.color.row_item_background_light;
            highlightPostBackground = R.color.post_highlight_light;
            highlightTextBackground = ResourcesCompat.getColor(activity.getResources(), R.color.text_highlight_background_light, activity.getTheme());
            selectedTextBackground = ResourcesCompat.getColor(activity.getResources(), R.color.text_select_background_light, activity.getTheme());
        } else if (MimiUtil.getInstance().getTheme() == MimiUtil.THEME_DARK) {
            defaultPostBackground = R.color.row_item_background_dark;
            highlightPostBackground = R.color.post_highlight_dark;
            highlightTextBackground = ResourcesCompat.getColor(activity.getResources(), R.color.text_highlight_background_dark, activity.getTheme());
            selectedTextBackground = ResourcesCompat.getColor(activity.getResources(), R.color.text_select_background_dark, activity.getTheme());
        } else {
            defaultPostBackground = R.color.row_item_background_black;
            highlightPostBackground = R.color.post_highlight_black;
            highlightTextBackground = ResourcesCompat.getColor(activity.getResources(), R.color.text_highlight_background_black, activity.getTheme());
            selectedTextBackground = ResourcesCompat.getColor(activity.getResources(), R.color.text_select_background_black, activity.getTheme());
        }

        Pair<VectorDrawableCompat, VectorDrawableCompat> metadataDrawables = initMetadataDrawables();
        pinDrawable = metadataDrawables.first;
        lockDrawable = metadataDrawables.second;

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
                thumbUrlMap.put(post.getNo(), MimiUtil.https() + activity.getString(R.string.thumb_link) + activity.getString(R.string.thumb_path, boardName, post.getTim()));
                fullImageUrlMap.put(post.getNo(), MimiUtil.https() + activity.getString(R.string.image_link) + activity.getString(R.string.full_image_path, boardName, post.getTim(), post.getExt()));
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

    private Pair<VectorDrawableCompat, VectorDrawableCompat> initMetadataDrawables() {
        final Resources.Theme theme = MimiApplication.getInstance().getTheme();
        final Resources res = MimiApplication.getInstance().getResources();
        final int drawableColor = MimiUtil.getInstance().getTheme() == MimiUtil.THEME_LIGHT ? R.color.md_grey_800 : R.color.md_green_50;
        final VectorDrawableCompat pin;
        final VectorDrawableCompat lock;

        pin = VectorDrawableCompat.create(res, R.drawable.ic_pin, theme);
        lock = VectorDrawableCompat.create(res, R.drawable.ic_lock, theme);

        if (pin != null) {
            pin.setTint(res.getColor(drawableColor));
        }

        if (lock != null) {
            lock.setTint(res.getColor(drawableColor));
        }

        return Pair.create(pin, lock);
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

    public int headerCount() {
        return headers == null ? 0 : headers.size();
    }

    public int getNextFoundStringPosition() {
        if (foundPosts.size() == 0) {
            return FIND_NO_RESULTS;
        }

        foundPos = foundPos == foundPosts.size() - 1 ? 0 : foundPos + 1;
        return getListPositionFromResultPosition(foundPos) + headerCount();
    }

    public int getPrevFoundStringPosition() {
        if (foundPosts.size() == 0) {
            return FIND_NO_RESULTS;
        }

        foundPos = foundPos == 0 ? foundPosts.size() - 1 : foundPos - 1;
        return getListPositionFromResultPosition(foundPos) + headerCount();
    }

    private int getListPositionFromResultPosition(int pos) {
        if (pos < 0) {
            return -1;
        }

        List<Long> resultList = new ArrayList<>(foundPosts.keySet());
        long foundPostId = resultList.get(pos);

        return MimiUtil.findPostPositionById(foundPostId, thread.getPosts());
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
                return new FirstPostViewHolder(v);
            case VIEW_NORMAL_LIST_ITEM:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.thread_post_item, parent, false);
                return new ThreadPostViewHolder(v);
            case VIEW_FOOTER:
                FrameLayout footerFrameLayout = new FrameLayout(parent.getContext());
                //make sure it fills the space
                footerFrameLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, MimiUtil.dpToPx(60)));
                return new HeaderFooterViewHolder(footerFrameLayout);
            default:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_layout, parent, false);
                return new ThreadPostViewHolder(v);
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
        this.thumbUrlMap.clear();
        this.fullImageUrlMap.clear();

        this.repliesText.clear();
        this.imagesText.clear();

//        final DiffUtil.DiffResult result = DiffUtil.calculateDiff(new ThreadDiff(this.thread, thread));
        this.thread = new ChanThread(thread);

        setupThread();
//        result.dispatchUpdatesTo(ThreadListAdapter.this);
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

        final long startTime = System.currentTimeMillis();

        final ViewHolder viewHolder = (ViewHolder) vh;
        final int position = pos - headers.size();
        final ChanPost postItem = thread.getPosts().get(position);

        if (postItem == null) {
            return;
        }

        if (lastPosition < position) {
            lastPosition = position;
        }

        final TextSearchResult searchResult = getSearchResultByPostId(postItem.getNo());
//        final int postPosition = getListPositionFromResultPosition(foundPos);
//        final boolean selected = foundPos >= 0 && position == postPosition;
        // setting this dynamically causes the app to crash
        final boolean selected = false;

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
                    final long threadId = thread.getPosts().get(0).getNo();

                    thumbnailClickListener.onThumbnailClick(postsWithImages, threadId, position, boardName);
                }
            }
        });

        final String thumbUrl = thumbUrlMap.get(postItem.getNo());
        final String imageUrl = fullImageUrlMap.get(postItem.getNo());

        if (viewHolder.flagIcon != null) {
            final String country;
            final String url;
            if (postItem.getCountry() == null) {
                country = postItem.getTrollCountry();
                if (country != null) {
                    url = trollUrl + country.toLowerCase() + ".gif";
                } else {
                    url = null;
                }
            } else {
                country = postItem.getCountry();
                if (country != null) {
                    url = flagUrl + country.toLowerCase() + ".gif";
                } else {
                    url = null;
                }
            }
            if (country != null) {

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

                MimiUtil.loadImageWithFallback(activity, viewHolder.flagIcon, url, null, R.drawable.placeholder_image, null);
//                    Glide.with(activity)
//                            .load(url)
//                            .placeholder(R.drawable.ic_placeholder_image)
//                            .diskCacheStrategy(DiskCacheStrategy.ALL)
//                            .crossFade()
//                            .into(viewHolder.flagIcon);

            } else {
                viewHolder.flagIcon.setVisibility(View.GONE);
                viewHolder.flagIcon.setOnClickListener(null);
                GlideApp.with(activity).clear(viewHolder.flagIcon);
            }
        }

        if (viewHolder.lockIcon != null) {
            if (postItem.isClosed()) {
                viewHolder.lockIcon.setImageDrawable(lockDrawable);
                viewHolder.lockIcon.setVisibility(View.VISIBLE);
            } else {
                viewHolder.lockIcon.setVisibility(View.GONE);
            }
        }

        if (viewHolder.pinIcon != null) {
            if (postItem.isSticky()) {
                viewHolder.pinIcon.setImageDrawable(pinDrawable);
                viewHolder.pinIcon.setVisibility(View.VISIBLE);
            } else {
                viewHolder.pinIcon.setVisibility(View.GONE);
            }
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
                                    replyMenuClickListener.onQuote(v, postItem);
                                }

                                return true;
                            case R.id.copy_text:
                                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                                android.content.ClipData clip = android.content.ClipData.newPlainText("hread info", thread.getPosts().get(position).getComment());
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

            List<Pair<Integer, Integer>> data = searchResult.textLocation.get(TextLocation.LOCATION_SUBJECT);
            if (data != null) {
                SpannableStringBuilder span = new SpannableStringBuilder(postItem.getSubject());
                for (Pair<Integer, Integer> integerIntegerPair : data) {
                    span.setSpan(new BackgroundColorSpan(selected ? selectedTextBackground : highlightTextBackground), integerIntegerPair.first, integerIntegerPair.second, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                viewHolder.subject.setText(span);
            } else {
                viewHolder.subject.setText(postItem.getSubject());
            }
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
            List<Pair<Integer, Integer>> data = searchResult.textLocation.get(TextLocation.LOCATION_COMMENT);
            if (data != null) {
                SpannableStringBuilder span = new SpannableStringBuilder(postItem.getComment());
                for (Pair<Integer, Integer> integerIntegerPair : data) {
                    span.setSpan(new BackgroundColorSpan(selected ? selectedTextBackground : highlightTextBackground), integerIntegerPair.first, integerIntegerPair.second, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                viewHolder.comment.setText(span);
            } else {
                viewHolder.comment.setText(postItem.getComment());
            }
            viewHolder.comment.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
                public static final int QUOTE_MENU_ID = 10109;

                @Override
                public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                    menu.add(1, QUOTE_MENU_ID, Menu.NONE, R.string.quote_menu_item).setIcon(R.drawable.ic_content_new);
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                    return false;
                }

                @Override
                public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                    if (menuItem.getItemId() == QUOTE_MENU_ID) {
                        int start = viewHolder.comment.getSelectionStart();
                        int end = viewHolder.comment.getSelectionEnd();

                        replyMenuClickListener.onQuoteSelection(viewHolder.comment, postItem, start, end);
                    }
                    actionMode.finish();
                    return true;
                }

                @Override
                public void onDestroyActionMode(ActionMode actionMode) {

                }
            });
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
                        if (activity != null && v != null) {
                            RepliesDialog.newInstance(thread, postItem).show(activity.getSupportFragmentManager(), RepliesDialog.DIALOG_TAG);
                        }
                    }
                });
            }

            if (viewHolder.galleryImageCount != null) {
                viewHolder.galleryImageCount.setText(imagesText.get(position));
                viewHolder.galleryImageCount.setOnClickListener(v -> {
                    ThreadRegistry.getInstance().setPosts(postItem.getNo(), GalleryPagerAdapter.getPostsWithImages(thread.getPosts()));
                    GalleryActivity2.start(v.getContext(), 0, postItem.getNo(), boardName, postItem.getNo(), new long[0]);
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

                final String info;
                if (postItem.getExt() != null) {
                    info = MimiUtil.humanReadableByteCount(postItem.getFsize(), true)
                            + " "
                            + postItem.getExt().toUpperCase().substring(1);

                } else {
                    info = MimiUtil.humanReadableByteCount(postItem.getFsize(), true);
                }

                viewHolder.fileExt.setText(info);
            }

            viewHolder.thumbnailContainer.setVisibility(View.VISIBLE);
            GlideApp.with(activity)
                    .load(thumbUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(viewHolder.thumbUrl);

        } else {
            viewHolder.thumbnailContainer.setVisibility(View.GONE);
            if (viewHolder.thumbnailInfoContainer != null) {
                viewHolder.thumbnailInfoContainer.setVisibility(View.GONE);
            }

            Glide.with(activity).clear(viewHolder.thumbUrl);
        }

        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        Log.d(LOG_TAG, "onBindViewHolder took " + String.valueOf(delta) + "ms");
    }

    private void createImageInfoDialog(final ChanPost postItem, final String imageUrl) {
        final View root = inflater.inflate(R.layout.dialog_image_info_buttons, null, false);
        final TextView fileName = (TextView) root.findViewById(R.id.file_name);
        final TextView dimensions = (TextView) root.findViewById(R.id.dimensions);
        final View iqdb = root.findViewById(R.id.iqdb_button);
        final View google = root.findViewById(R.id.google_button);
        final View saucenao = root.findViewById(R.id.saucenao_button);
        final View yandex = root.findViewById(R.id.yandex_button);
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final AlertDialog dialog;

        fileName.setText(postItem.getFilename() + postItem.getExt());
        dimensions.setText(postItem.getWidth() + " x " + postItem.getHeight());
        builder.setTitle(R.string.image_info)
                .setView(root)
                .setPositiveButton(R.string.exit, (dialog1, which) -> dialog1.dismiss());

        dialog = builder.create();

        iqdb.setOnClickListener(v -> openImageSearch(activity, R.string.iqdb_image_search_link, imageUrl, dialog));
        google.setOnClickListener(v -> openImageSearch(activity, R.string.google_image_search_link, imageUrl, dialog));
        saucenao.setOnClickListener(v -> openImageSearch(activity, R.string.saucenao_image_search_link, imageUrl, dialog));
        yandex.setOnClickListener(v -> openImageSearch(activity, R.string.yandex_image_search_link, imageUrl, dialog));

        dialog.show();
    }

    private void openImageSearch(Activity act, @StringRes int baseSearchUrl, String imageUrl, DialogInterface dialog) {
        if (act != null) {
            final String url;
            try {
                url = act.getString(baseSearchUrl, URLEncoder.encode(imageUrl, "UTF-8"));
                final Intent searchIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

                act.startActivity(searchIntent);
                dialog.dismiss();
            } catch (UnsupportedEncodingException e) {
                Toast.makeText(act, R.string.error_opening_search_link, Toast.LENGTH_SHORT).show();
                Log.e(LOG_TAG, "Error opening search link", e);
            }
        }
    }

    public void clearFilter() {
        if (foundPosts != null) {
            foundPosts.clear();
            foundPos = FIND_NO_RESULTS;
            notifyDataSetChanged();
        }

        if (filterUpdateCallback != null) {
            filterUpdateCallback.onFilterUpdated("", 0);
        }
    }

    public int getFilterCount() {
        return foundPosts != null ? foundPosts.size() : 0;
    }

    @Override
    public Filter getFilter() {
        if (postFilter == null) {
            postFilter = new ThreadListAdapter.PostFilter(thread.getPosts());
        }

        return postFilter;
    }

    private class PostFilter extends Filter {
        private List<ChanPost> posts;

        public PostFilter(List<ChanPost> posts) {
            this.posts = posts;

            if (this.posts == null) {
                this.posts = new ArrayList<>();
            }
        }

        @Override
        protected FilterResults performFiltering(CharSequence searchStr) {
            Log.d(LOG_TAG, "Filtering on: " + searchStr);
            long startTime = System.currentTimeMillis();
            FilterResults results = new FilterResults();
            if (searchStr == null || searchStr.length() == 0) {
                results.count = posts.size();
                results.values = new LinkedHashMap<Integer, TextSearchResult>();
            } else {
                String constraint = searchStr.toString().toLowerCase();
                LinkedHashMap<Long, TextSearchResult> resultMap = new LinkedHashMap<>();
                for (ChanPost chanPost : thread.getPosts()) {
                    final TextSearchResult result = new TextSearchResult();
                    result.searchStr = constraint;
                    result.postId = chanPost.getNo();

                    TextLocation textLocation;

                    int start = 0;
                    int end = 0;
                    while (start > -1) {
                        if (chanPost.getName() != null && chanPost.getName().toLowerCase().substring(end).contains(constraint)) {
                            start = chanPost.getName().toLowerCase().indexOf(result.searchStr, end);
                            textLocation = TextLocation.LOCATION_NAME;

                            end = start + constraint.length();

                            List<Pair<Integer, Integer>> data = result.textLocation.get(textLocation);
                            if (data == null) {
                                data = new ArrayList<>();
                            }

                            data.add(new Pair<>(start, end));
                            result.textLocation.put(textLocation, data);
                        } else {
                            start = -1;
                        }
                    }

                    start = 0;
                    end = 0;
                    while (start > -1) {
                        if (chanPost.getSubject() != null && chanPost.getSubject().toString().toLowerCase().substring(end).contains(constraint)) {
                            start = chanPost.getSubject().toString().toLowerCase().indexOf(result.searchStr, end);
                            textLocation = TextLocation.LOCATION_SUBJECT;

                            end = start + constraint.length();

                            List<Pair<Integer, Integer>> data = result.textLocation.get(textLocation);
                            if (data == null) {
                                data = new ArrayList<>();
                            }

                            data.add(new Pair<>(start, end));
                            result.textLocation.put(textLocation, data);
                        } else {
                            start = -1;
                        }
                    }

                    start = 0;
                    end = 0;
                    while (start > -1) {
                        if (chanPost.getComment() != null && chanPost.getComment().toString().toLowerCase().substring(end).contains(constraint)) {
                            start = chanPost.getComment().toString().toLowerCase().indexOf(result.searchStr, end);
                            textLocation = TextLocation.LOCATION_COMMENT;

                            end = start + constraint.length();

                            List<Pair<Integer, Integer>> data = result.textLocation.get(textLocation);
                            if (data == null) {
                                data = new ArrayList<>();
                            }

                            data.add(new Pair<>(start, end));
                            result.textLocation.put(textLocation, data);
                        } else {
                            start = -1;
                        }
                    }

                    if (result.textLocation.size() > 0) {
                        resultMap.put(chanPost.getNo(), result);
                    }
                }

                results.count = resultMap.size();
                results.values = resultMap;
            }
            long endTime = System.currentTimeMillis();
            long delta = endTime - startTime;

            Log.d(LOG_TAG, "Filtering took " + delta + "ms");

            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            long startTime = System.currentTimeMillis();
            LinkedHashMap<Long, TextSearchResult> textSearchResults = (LinkedHashMap<Long, TextSearchResult>) results.values;
            if (textSearchResults.size() > 0) {
                foundPos = -1;
            } else {
                foundPos = FIND_NO_RESULTS;
            }
            foundPosts = new LinkedHashMap<>(textSearchResults);

            long endTime = System.currentTimeMillis();
            long delta = endTime - startTime;

            Log.d(LOG_TAG, "Publishing results took " + delta + "ms");

            notifyDataSetChanged();

            if (filterUpdateCallback != null) {
                filterUpdateCallback.onFilterUpdated(constraint.toString(), foundPosts.size());
            }
        }
    }

    public TextSearchResult getSearchResultByPostId(long postId) {
        if (foundPosts.size() == 0) {
            return new TextSearchResult();
        }

        TextSearchResult result = foundPosts.get(postId);
        if (result == null) {
            return new TextSearchResult();
        }

        return result;
    }

    private enum TextLocation {
        LOCATION_NAME, LOCATION_SUBJECT, LOCATION_COMMENT, NONE
    }

    private class TextSearchResult {
        public String searchStr = "";
        public long postId = -1L;
        public Map<TextLocation, List<Pair<Integer, Integer>>> textLocation = new HashMap<>();

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TextSearchResult result = (TextSearchResult) o;

            return postId == result.postId;

        }

        @Override
        public int hashCode() {
            return (int) (postId ^ (postId >>> 32));
        }
    }

    public static abstract class ViewHolder extends RecyclerView.ViewHolder {
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
        public TextView fileExt;
        public View menuButton;
        public View replyContainer;
        public ImageView flagIcon;
        public ImageView lockIcon;
        public ImageView pinIcon;

        public ViewHolder(final View root) {
            super(root);

            postContainer = root.findViewById(R.id.post_container);
            threadId = root.findViewById(R.id.thread_id);
            userName = root.findViewById(R.id.user_name);
            postTime = root.findViewById(R.id.timestamp);
            userId = root.findViewById(R.id.user_id);
            tripCode = root.findViewById(R.id.tripcode);
            subject = root.findViewById(R.id.subject);
            comment = root.findViewById(R.id.comment);
            replyCount = root.findViewById(R.id.replies_number);
            replyButton = root.findViewById(R.id.reply_button);
            replyContainer = root.findViewById(R.id.replies_row);
            galleryImageCount = root.findViewById(R.id.image_count);
            thumbUrl = root.findViewById(R.id.thumbnail);
            fileExt = root.findViewById(R.id.file_ext);
            thumbnailInfoContainer = root.findViewById(R.id.thumbnail_info_container);
            thumbnailContainer = root.findViewById(R.id.thumbnail_container);
            flagIcon = root.findViewById(R.id.flag_icon);
            lockIcon = root.findViewById(R.id.lock_icon);
            pinIcon = root.findViewById(R.id.pin_icon);
            menuButton = root.findViewById(R.id.menu_button);

            comment.setMovementMethod(LongClickLinkMovementMethod.getInstance());
        }
    }

    private static class FirstPostViewHolder extends ViewHolder {

        public FirstPostViewHolder(View root) {
            super(root);
        }
    }

    public static class ThreadPostViewHolder extends ViewHolder {

        public ThreadPostViewHolder(View root) {
            super(root);
        }
    }

    private class ThreadDiff extends DiffUtil.Callback {
        private ChanThread oldThread;
        private ChanThread newThread;

        public ThreadDiff(ChanThread oldThread, ChanThread newThread) {
            this.oldThread = oldThread;
            this.newThread = newThread;
        }

        @Override
        public int getOldListSize() {
            return oldThread == null ? 0 : oldThread.getPosts().size();
        }

        @Override
        public int getNewListSize() {
            return newThread == null ? 0 : newThread.getPosts().size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldThread.getPosts().get(oldItemPosition).getNo() == newThread.getPosts().get(newItemPosition).getNo();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldThread.getPosts().get(oldItemPosition).equals(newThread.getPosts().get(newItemPosition));
        }
    }

    public void setOnThumbnailClickListener(final OnThumbnailClickListener listener) {
        thumbnailClickListener = listener;
    }

    public void setOnReplyMenuClickListener(final ReplyMenuClickListener listener) {
        replyMenuClickListener = listener;
    }

    public void setOnFilterUpdateCallback(OnFilterUpdateCallback callback) {
        filterUpdateCallback = callback;
    }

    public interface OnFilterUpdateCallback {
        public void onFilterUpdated(String filteredString, int count);
    }
}
