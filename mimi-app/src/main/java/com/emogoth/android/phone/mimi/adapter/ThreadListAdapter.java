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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.collection.LongSparseArray;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.activity.GalleryActivity2;
import com.emogoth.android.phone.mimi.app.MimiApplication;
import com.emogoth.android.phone.mimi.async.ProcessThreadTask;
import com.emogoth.android.phone.mimi.db.UserPostTableConnection;
import com.emogoth.android.phone.mimi.dialog.RepliesDialog;
import com.emogoth.android.phone.mimi.interfaces.OnThumbnailClickListener;
import com.emogoth.android.phone.mimi.interfaces.ReplyMenuClickListener;
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

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class ThreadListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements Filterable {
    private static final String LOG_TAG = ThreadListAdapter.class.getSimpleName();

    public static final int VIEW_HEADER = 10;
    public static final int VIEW_TOP_LIST_ITEM = 11;
    public static final int VIEW_NORMAL_LIST_ITEM = 12;
    public static final int VIEW_FOOTER = 13;

    private static final int FIND_NO_RESULTS = -2;

    private final String flagUrl;
    private final String trollUrl;

    private final VectorDrawableCompat pinDrawable;
    private final VectorDrawableCompat lockDrawable;

    private List<ChanPost> items = new ArrayList<>();
    private List<Long> userPosts = new ArrayList<>();
    private String boardName;
    private long threadId;
    private CharSequence[] timeMap;
    private OnThumbnailClickListener thumbnailClickListener;
    private ReplyMenuClickListener replyMenuClickListener;

    private int defaultPostBackground;
    private int highlightPostBackground;
    private int highlightTextBackground;
    private int selectedTextBackground;

    private int lastPosition = 0;
//    private List<Long> userPostList;

    private List<String> repliesText = new ArrayList<>();
    private List<String> imagesText = new ArrayList<>();

    private int[] colorList;

    private final LongSparseArray<String> thumbUrlMap = new LongSparseArray<>();
    private final LongSparseArray<String> fullImageUrlMap = new LongSparseArray<>();
    private LinkedHashMap<Long, TextSearchResult> foundPosts = new LinkedHashMap<>();
    private PostFilter postFilter;
    private int foundPos = FIND_NO_RESULTS;
    private OnFilterUpdateCallback filterUpdateCallback;


    public ThreadListAdapter(@NonNull final ChanThread thread) {
        this.items.addAll(thread.getPosts());
        this.boardName = thread.getBoardName();
        this.threadId = thread.getThreadId();
        final Context context = MimiApplication.getInstance().getApplicationContext();

        this.flagUrl = MimiUtil.https() + context.getString(R.string.flag_int_link);
        this.trollUrl = MimiUtil.https() + context.getString(R.string.flag_pol_link);

        if (MimiUtil.getInstance().getTheme() == MimiUtil.THEME_LIGHT) {
            defaultPostBackground = R.color.row_item_background_light;
            highlightPostBackground = R.color.post_highlight_light;
            highlightTextBackground = ResourcesCompat.getColor(context.getResources(), R.color.text_highlight_background_light, context.getTheme());
            selectedTextBackground = ResourcesCompat.getColor(context.getResources(), R.color.text_select_background_light, context.getTheme());
        } else if (MimiUtil.getInstance().getTheme() == MimiUtil.THEME_DARK) {
            defaultPostBackground = R.color.row_item_background_dark;
            highlightPostBackground = R.color.post_highlight_dark;
            highlightTextBackground = ResourcesCompat.getColor(context.getResources(), R.color.text_highlight_background_dark, context.getTheme());
            selectedTextBackground = ResourcesCompat.getColor(context.getResources(), R.color.text_select_background_dark, context.getTheme());
        } else {
            defaultPostBackground = R.color.row_item_background_black;
            highlightPostBackground = R.color.post_highlight_black;
            highlightTextBackground = ResourcesCompat.getColor(context.getResources(), R.color.text_highlight_background_black, context.getTheme());
            selectedTextBackground = ResourcesCompat.getColor(context.getResources(), R.color.text_select_background_black, context.getTheme());
        }

        Pair<VectorDrawableCompat, VectorDrawableCompat> metadataDrawables = initMetadataDrawables();
        pinDrawable = metadataDrawables.first;
        lockDrawable = metadataDrawables.second;

        setupThread();

    }

    private void setupThread() {
        this.timeMap = new CharSequence[items.size()];
        this.colorList = new int[items.size()];
//        this.userPostList = ThreadRegistry.getInstance().getUserPosts(boardName, threadId);

        final Context context = MimiApplication.getInstance().getApplicationContext();

        for (int i = 0; i < items.size(); i++) {
            final ChanPost post = items.get(i);
            final CharSequence dateString = DateUtils.getRelativeTimeSpanString(
                    post.getTime() * 1000L,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE);

            if (post.getFilename() != null && !post.getFilename().equals("")) {
                thumbUrlMap.put(post.getNo(), MimiUtil.https() + context.getString(R.string.thumb_link) + context.getString(R.string.thumb_path, boardName, post.getTim()));
                fullImageUrlMap.put(post.getNo(), MimiUtil.https() + context.getString(R.string.image_link) + context.getString(R.string.full_image_path, boardName, post.getTim(), post.getExt()));
            }

            repliesText.add(context.getResources().getQuantityString(R.plurals.replies_plural, post.getRepliesFrom().size(), post.getRepliesFrom().size()));
            imagesText.add(context.getResources().getQuantityString(R.plurals.image_plural, post.getImages(), post.getImages()));

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

    public int getNextFoundStringPosition() {
        if (foundPosts.size() == 0) {
            return FIND_NO_RESULTS;
        }

        foundPos = foundPos == foundPosts.size() - 1 ? 0 : foundPos + 1;
        return getListPositionFromResultPosition(foundPos);
    }

    public int getPrevFoundStringPosition() {
        if (foundPosts.size() == 0) {
            return FIND_NO_RESULTS;
        }

        foundPos = foundPos == 0 ? foundPosts.size() - 1 : foundPos - 1;
        return getListPositionFromResultPosition(foundPos);
    }

    private int getListPositionFromResultPosition(int pos) {
        if (pos < 0) {
            return -1;
        }

        List<Long> resultList = new ArrayList<>(foundPosts.keySet());
        long foundPostId = resultList.get(pos);

        return MimiUtil.findPostPositionById(foundPostId, items);
    }

    public void setUserPosts(List<Long> posts) {
        this.userPosts.clear();
        this.userPosts.addAll(posts);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View v;
        switch (viewType) {
            case VIEW_TOP_LIST_ITEM:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.thread_first_post_item, parent, false);
                return new FirstPostViewHolder(v);
            case VIEW_NORMAL_LIST_ITEM:
                v = LayoutInflater.from(parent.getContext()).inflate(R.layout.thread_post_item, parent, false);
                return new ThreadPostViewHolder(v);
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
        return items.size();
    }

    private void reset(List<ChanPost> posts) {
        this.thumbUrlMap.clear();
        this.fullImageUrlMap.clear();

        this.repliesText.clear();
        this.imagesText.clear();

        items.clear();
        items.addAll(posts);

        setupThread();
    }

    public void setThread(final ChanThread thread) {

        if (TextUtils.isEmpty(boardName) || threadId <= 0) {
            boardName = thread.getBoardName();
            threadId = thread.getThreadId();

            reset(thread.getPosts());

            notifyDataSetChanged();
            return;
        }

        if (thread.getPosts().size() > this.items.size() && this.items.size() > 1) {

//            final DiffUtil.DiffResult result = DiffUtil.calculateDiff(new ThreadDiff(this.thread, thread), false);
//            final DiffUtil.DiffResult result = DiffUtil.calculateDiff(new ThreadDiff(this.thread, thread));
//            final DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
//                @Override
//                public int getOldListSize() {
//                    return items.size();
//                }
//
//                @Override
//                public int getNewListSize() {
//                    return thread.getPosts().size();
//                }
//
//                @Override
//                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
//                    return items.get(oldItemPosition).getNo() == thread.getPosts().get(newItemPosition).getNo();
//                }
//
//                @Override
//                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
//                    return items.get(oldItemPosition).getNo() == thread.getPosts().get(newItemPosition).getNo();
//                }
//            }, false);

            reset(thread.getPosts());
            notifyDataSetChanged();
//            result.dispatchUpdatesTo(this);

        } else if (items.size() <= 1) {
            reset(thread.getPosts());
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_TOP_LIST_ITEM;
        } else {
            return VIEW_NORMAL_LIST_ITEM;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder vh, final int position) {

        final long startTime = System.currentTimeMillis();

        final ViewHolder viewHolder = (ViewHolder) vh;
        final ChanPost postItem = items.get(position);

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
        final boolean usersPost = userPosts.contains(postItem.getNo());

        if (viewHolder.postContainer != null) {
            if (usersPost) {
                viewHolder.postContainer.setBackgroundResource(highlightPostBackground);
            } else {
                viewHolder.postContainer.setBackgroundResource(defaultPostBackground);
            }
        }

        viewHolder.thumbnailContainer.setOnClickListener(v -> {
            if (thumbnailClickListener != null) {
                final ArrayList<ChanPost> postsWithImages = GalleryPagerAdapter.getPostsWithImages(items);
                final int position1 = postsWithImages.indexOf(postItem);

                thumbnailClickListener.onThumbnailClick(postsWithImages, threadId, position1, boardName);
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
                viewHolder.flagIcon.setOnClickListener(v -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(viewHolder.flagIcon.getContext());
                    builder.setMessage(postItem.getCountryName())
                            .setCancelable(true)
                            .show()
                            .setCanceledOnTouchOutside(true);
                });

//                MimiUtil.loadImageWithFallback(viewHolder.flagIcon.getContext(), viewHolder.flagIcon, url, null, R.drawable.placeholder_image, null);
                Glide.with(viewHolder.flagIcon)
                        .load(url)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(viewHolder.flagIcon);

            } else {
                viewHolder.flagIcon.setVisibility(View.GONE);
                viewHolder.flagIcon.setOnClickListener(null);
                GlideApp.with(viewHolder.flagIcon).clear(viewHolder.flagIcon);
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

        viewHolder.menuButton.setOnClickListener(v -> {
            final PopupMenu menu = new PopupMenu(v.getContext(), v);
            if (position == 0) {
                menu.inflate(R.menu.thread_first_post_menu);
            } else {
                menu.inflate(R.menu.thread_menu);
            }

            final MenuItem claimMenuItem = menu.getMenu().findItem(R.id.claim_menu_item);
            final MenuItem unclaimMenuItem = menu.getMenu().findItem(R.id.unclaim_menu_item);

            claimMenuItem.setVisible(!usersPost);
            unclaimMenuItem.setVisible(usersPost);

            final MenuItem imageInfoItem = menu.getMenu().findItem(R.id.image_info_menu_item);
            if (imageUrl == null && imageInfoItem != null) {
                imageInfoItem.setVisible(false);
            }

            menu.setOnMenuItemClickListener(menuItem -> {
                final Context context = MimiApplication.getInstance().getApplicationContext();
                final String url = "https://" + context.getString(R.string.board_link);
                final String path = context.getString(R.string.raw_thread_path, boardName, threadId);
                final String link;
                if (postItem.getNo() == threadId) {
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
                    case R.id.claim_menu_item:
                        setPostOwner(postItem, true);
                        return true;
                    case R.id.unclaim_menu_item:
                        setPostOwner(postItem, false);
                        return true;
                    case R.id.copy_text:
                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("hread info", items.get(position).getComment());
                        clipboard.setPrimaryClip(clip);

                        Toast.makeText(context, R.string.text_copied_to_clipboard, Toast.LENGTH_SHORT).show();
                        return true;
                    case R.id.image_info_menu_item:
                        if (viewHolder.menuButton.getContext() instanceof Activity) {
                            final Activity activity = (Activity) viewHolder.menuButton.getContext();
                            createImageInfoDialog(activity, postItem, imageUrl);
                        }
                        return true;

                    case R.id.report_menu:
                        if (viewHolder.menuButton.getContext() instanceof Activity) {
                            final Activity activity = (Activity) viewHolder.menuButton.getContext();
                            final String reportUrl = "https://" + context.getString(R.string.sys_link);
                            final String reportPath = context.getString(R.string.report_path, boardName, items.get(position).getNo());
                            final Intent reportIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(reportUrl + reportPath));


                            activity.startActivity(reportIntent);
                        }
                        return true;

                    case R.id.copy_link_menu:
                        ClipboardManager linkClipboard = (ClipboardManager) MimiApplication.getInstance().getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData linkClip = ClipData.newPlainText("thread link", link);
                        linkClipboard.setPrimaryClip(linkClip);

                        Toast.makeText(context, R.string.text_copied_to_clipboard, Toast.LENGTH_SHORT).show();

                        return true;

                    case R.id.open_link_menu:
                        if (viewHolder.menuButton.getContext() instanceof Activity) {
                            final Activity activity = (Activity) viewHolder.menuButton.getContext();
                            final Intent openLinkIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                            activity.startActivity(openLinkIntent);
                        }

                        return true;

                    default:
                        return false;

                }
            });
            menu.show();
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
            viewHolder.userId.setOnClickListener(v -> {
                if (v.getContext() instanceof AppCompatActivity) {
                    final AppCompatActivity activity = (AppCompatActivity) viewHolder.menuButton.getContext();
                    RepliesDialog.newInstance(new ChanThread(boardName, threadId, items), postItem.getId()).show(activity.getSupportFragmentManager(), RepliesDialog.DIALOG_TAG);
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
                viewHolder.replyButton.setOnClickListener(v -> {
                    if (replyMenuClickListener != null) {
                        replyMenuClickListener.onReply(v, postItem.getNo());
                    }
                });
            }
        }

        if ((postItem.getRepliesFrom() != null && postItem.getRepliesFrom().size() > 0) || (viewHolder.galleryImageCount != null && postItem.getImages() > 0)) {

            if (postItem.getRepliesFrom() != null) {
                viewHolder.replyCount.setText(repliesText.get(position));
                viewHolder.replyCount.setOnClickListener(v -> {
                    if (v.getContext() instanceof AppCompatActivity) {
                        final AppCompatActivity activity = (AppCompatActivity) viewHolder.menuButton.getContext();
                        RepliesDialog.newInstance(new ChanThread(boardName, threadId, items), postItem).show(activity.getSupportFragmentManager(), RepliesDialog.DIALOG_TAG);
                    }
                });
            }

            if (viewHolder.galleryImageCount != null) {
                viewHolder.galleryImageCount.setText(imagesText.get(position));
                viewHolder.galleryImageCount.setOnClickListener(v -> {
                    ThreadRegistry.getInstance().setPosts(postItem.getNo(), GalleryPagerAdapter.getPostsWithImages(items));
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
            GlideApp.with(viewHolder.thumbnailContainer)
                    .load(thumbUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(viewHolder.thumbUrl);

        } else {
            viewHolder.thumbnailContainer.setVisibility(View.GONE);
            if (viewHolder.thumbnailInfoContainer != null) {
                viewHolder.thumbnailInfoContainer.setVisibility(View.GONE);
            }

            Glide.with(viewHolder.thumbUrl.getContext()).clear(viewHolder.thumbUrl);
        }

        long endTime = System.currentTimeMillis();
        long delta = endTime - startTime;

        Log.d(LOG_TAG, "onBindViewHolder took " + delta + "ms");
    }

    private void setPostOwner(final ChanPost post, boolean claim) {
        long id = post.getNo();
        if (claim) {
            UserPostTableConnection.addPost(boardName, threadId, id)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .single(false)
                    .subscribe(new SingleObserver<Boolean>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            // no op
                        }

                        @Override
                        public void onSuccess(Boolean success) {
                            if (!success) {
                                onError(new Exception("Error claiming thread. Please try again."));
                                return;
                            }

                            userPosts.add(id);
                            processPostReplies(post);
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.e(LOG_TAG, "Error claiming thread", e);
                            Toast.makeText(MimiApplication.getInstance().getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            UserPostTableConnection.removePost(boardName, threadId, id)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .single(false)
                    .subscribe(new SingleObserver<Boolean>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            // no op
                        }

                        @Override
                        public void onSuccess(Boolean success) {
                            if (!success) {
                                onError(new Exception("Error unclaiming thread. Please try again."));
                                return;
                            }

                            final int index = userPosts.indexOf(id);
                            if (index >= 0) {
                                userPosts.remove(index);
                                processPostReplies(post);
                            } else {
                                onError(new Exception("Error unclaiming thread. Please try again."));
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.e(LOG_TAG, "Error unclaiming thread", e);
                            Toast.makeText(MimiApplication.getInstance().getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void processPostReplies(ChanPost post) {
        final int postPosition = MimiUtil.findPostPositionById(post.getNo(), items);
        for (ChanPost reply : post.getRepliesFrom()) {
            final int i = MimiUtil.findPostPositionById(reply.getNo(), items);
            if (i >= 0) {
                ChanPost p = ProcessThreadTask.updatePost(items.get(i), userPosts, boardName, 0);
                items.set(i, p);

                notifyItemChanged(i);
            }
        }

        notifyItemChanged(postPosition);
    }

    private void createImageInfoDialog(final Activity activity, final ChanPost postItem, final String imageUrl) {
        final LayoutInflater inflater = LayoutInflater.from(activity);
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
            postFilter = new ThreadListAdapter.PostFilter(items);
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
                for (ChanPost chanPost : items) {
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

//    private class ThreadDiff extends DiffUtil.Callback {
//        private final ChanThread oldThread;
//        private final ChanThread newThread;
//
//        ThreadDiff(ChanThread oldThread, ChanThread newThread) {
//            this.oldThread = oldThread;
//            this.newThread = newThread;
//        }
//
//        @Override
//        public int getOldListSize() {
//            return oldThread == null ? 0 : olditems.size();
//        }
//
//        @Override
//        public int getNewListSize() {
//            return newThread == null ? 0 : newitems.size();
//        }
//
//        @Override
//        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
//            return olditems.get(oldItemPosition).getNo() == newitems.get(newItemPosition).getNo();
//        }
//
//        @Override
//        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
//            return olditems.get(oldItemPosition).getNo() == newitems.get(newItemPosition).getNo();
////            return olditems.get(oldItemPosition).equals(newitems.get(newItemPosition));
//        }
//
//        @Nullable
//        @Override
//        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
//            Log.d(LOG_TAG, "old number=" + olditems.get(oldItemPosition).getNo()
//                    + ", new number=" + newitems.get(newItemPosition).getNo()
//                    + ", equals=" + olditems.get(oldItemPosition).equals(newitems.get(newItemPosition)));
//            return super.getChangePayload(oldItemPosition, newItemPosition);
//        }
//    }

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
