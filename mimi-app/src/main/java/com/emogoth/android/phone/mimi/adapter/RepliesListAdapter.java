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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.activity.GalleryActivity;
import com.emogoth.android.phone.mimi.activity.PostItemDetailActivity;
import com.emogoth.android.phone.mimi.activity.PostItemListActivity;
import com.emogoth.android.phone.mimi.dialog.RepliesDialog;
import com.emogoth.android.phone.mimi.event.ReplyClickEvent;
import com.emogoth.android.phone.mimi.model.OutsideLink;
import com.emogoth.android.phone.mimi.util.BusProvider;
import com.emogoth.android.phone.mimi.util.Extras;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.view.LongClickLinkMovementMethod;
import com.mimireader.chanlib.models.ChanPost;
import com.mimireader.chanlib.models.ChanThread;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RepliesListAdapter extends BaseAdapter {
    private static final String LOG_TAG = RepliesListAdapter.class.getSimpleName();
    private final List<ChanPost> replies;
    private final LayoutInflater inflater;
    private final FragmentActivity activity;
    private final String boardName;
    private final String flagUrl;
    private final List<OutsideLink> links;
    private final Map<Integer, String> thumbUrlMap;
    private final ChanThread thread;
    private CharSequence[] timeMap;


    public RepliesListAdapter(final FragmentActivity activity, final String boardName, final List<ChanPost> replies, final List<OutsideLink> links, final ChanThread thread) {
        this.activity = activity;
        this.boardName = boardName;
        this.replies = replies;
        this.links = (links == null) ? new ArrayList<OutsideLink>() : links;
        this.thread = thread;
        this.inflater = LayoutInflater.from(activity);

        this.thumbUrlMap = new HashMap<>();

        if(boardName.equals("pol")) {
            this.flagUrl = MimiUtil.httpOrHttps(activity) + activity.getString(R.string.flag_pol_link);
        }
        else {
            this.flagUrl = MimiUtil.httpOrHttps(activity) + activity.getString(R.string.flag_int_link);
        }

        setupReplies();
    }

    private void setupReplies() {
        this.timeMap = new CharSequence[replies.size() + 1];
        for(int i = 0; i < replies.size(); i++) {
            final ChanPost post = replies.get(i);

            final CharSequence dateString = DateUtils.getRelativeTimeSpanString(
                    post.getTime() * 1000L,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE);

            if(post.getFilename() != null && !"".equals(post.getFilename())) {
                thumbUrlMap.put(post.getNo(), MimiUtil.httpOrHttps(activity) + activity.getString(R.string.thumb_link) + activity.getString(R.string.thumb_path, boardName, post.getTim()));
            }

            timeMap[i] = dateString;
        }

    }

    @Override
    public int getCount() {
        return replies.size() + links.size();
    }

    @Override
    public ChanPost getItem(final int position) {
        return replies.get(position);
    }

    @Override
    public long getItemId(final int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        if(replies.size() > position) {
            final ViewHolderItem viewHolder;
            final ChanPost postItem = replies.get(position);

            if (postItem == null) {
                return null;
            }

            if(convertView == null) {
                convertView = inflater.inflate(R.layout.reply_post_item, parent, false);

                viewHolder = new ViewHolderItem(convertView);
                convertView.setTag(viewHolder);
            }
            else {
                if(convertView.getTag() == null) {
                    convertView = inflater.inflate(R.layout.reply_post_item, parent, false);

                    viewHolder = new ViewHolderItem(convertView);
                    convertView.setTag(viewHolder);
                }
                else {
                    viewHolder = (ViewHolderItem) convertView.getTag();
                }
            }

            if(viewHolder.flagIcon != null) {
                if (postItem.getCountry() != null && !"".equals(postItem.getCountry())) {
                    final String url = flagUrl + postItem.getCountry().toLowerCase() + ".gif";
                    Log.i(LOG_TAG, "flag url=" + url);
                    viewHolder.flagIcon.setVisibility(View.VISIBLE);
                    Glide.with(activity)
                            .load(url)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.placeholder_image)
                            .crossFade()
                            .into(viewHolder.flagIcon);
                } else {
                    viewHolder.flagIcon.setVisibility(View.GONE);
                }
            }

            viewHolder.gotoPost.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final ReplyClickEvent event = new ReplyClickEvent(replies.get(position), position);
                    BusProvider.getInstance().post(event);
                }
            });

            viewHolder.threadId.setText(String.valueOf(postItem.getNo()));

            if(viewHolder.userName != null) {
                viewHolder.userName.setText(postItem.getDisplayedName());
            }

            if(viewHolder.postTime != null && timeMap.length > position) {
                viewHolder.postTime.setText(timeMap[position]);
            }

            viewHolder.repliesText.setText(activity.getResources().getQuantityString(R.plurals.replies_plural, postItem.getRepliesFrom().size(), postItem.getRepliesFrom().size()));
            viewHolder.repliesText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if(postItem.getRepliesFrom().size() > 0) {
                        if (thread != null) {
                            RepliesDialog.newInstance(thread, postItem).show(activity.getSupportFragmentManager(), RepliesDialog.DIALOG_TAG);
                        } else {
                            Toast.makeText(activity, R.string.error_opening_replies, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

            if (postItem.getComment() != null) {
                viewHolder.comment.setText(postItem.getComment());
            } else {
                viewHolder.comment.setText("");
            }

            final String url = thumbUrlMap.get(postItem.getNo());
            if(url != null) {
                viewHolder.thumbnailContainer.setVisibility(View.VISIBLE);
                Glide.with(activity)
                        .load(url)
                        .placeholder(R.drawable.placeholder_image)
                        .crossFade()
                        .into(viewHolder.thumbUrl);

                viewHolder.thumbnailContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final ArrayList<ChanPost> postsWithImages = GalleryPagerAdapter.getPostsWithImages(replies);
                        final int index = GalleryPagerAdapter.getIndexOfPost(postsWithImages, postItem.getNo());
                        final Bundle args = new Bundle();
                        args.putInt(Extras.EXTRAS_GALLERY_TYPE, GalleryActivity.GALLERY_TYPE_PAGER);
                        args.putInt(Extras.EXTRAS_THREAD_ID, postItem.getNo());
                        args.putParcelableArrayList(Extras.EXTRAS_POST_LIST, postsWithImages);
                        args.putInt(Extras.EXTRAS_POSITION, index);
                        args.putString(Extras.EXTRAS_BOARD_NAME, boardName);

                        final Intent galleryIntent = new Intent(activity, GalleryActivity.class);
                        galleryIntent.putExtras(args);
                        galleryIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

                        activity.startActivity(galleryIntent);
                    }
                });
            } else {
                viewHolder.thumbnailContainer.setVisibility(View.GONE);
                Glide.clear(viewHolder.thumbUrl);
            }

            return convertView;
        }
        else {
            final int pos = position - replies.size();
            final OutsideLink link = links.get(pos);
            final View v = inflater.inflate(R.layout.reply_link_item, parent, false);

            final TextView linkText = (TextView) v.findViewById(R.id.link_text);

            if(!TextUtils.isEmpty(link.getThreadId())) {
                linkText.setText("/" + link.getBoardName() + "/" + link.getThreadId());
            }
            else {
                linkText.setText("/" + link.getBoardName() + "/");
            }
            linkText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Intent intent;

                    final String id = link.getThreadId();

                    if (id != null && TextUtils.isDigitsOnly(id)) {
                        intent = new Intent(activity, PostItemDetailActivity.class);
                        intent.putExtra(Extras.EXTRAS_THREAD_ID, Integer.valueOf(id));
                        intent.putExtra(Extras.EXTRAS_SINGLE_THREAD, true);
                    } else {
                        intent = new Intent(activity, PostItemListActivity.class);
                    }

                    intent.putExtra(Extras.EXTRAS_BOARD_NAME, link.getBoardName());

                    activity.startActivity(intent);
                }
            });

            return v;
        }

    }

    private static class ViewHolderItem {
        public TextView threadId;
        public ViewGroup thumbnailContainer;
        public TextView userName;
        public TextView postTime;
        public TextView userId;
        public TextView tripCode;
        public TextView subject;
        public TextView comment;
        public ImageView thumbUrl;
        public ImageView menuButton;
        public ViewGroup postContainer;
        public TextView gotoPost;
        public ImageView flagIcon;
        public TextView repliesText;

        public ViewHolderItem(final View v) {
            threadId = (TextView) v.findViewById(R.id.thread_id);
            userName = (TextView) v.findViewById(R.id.user_name);
            postTime = (TextView) v.findViewById(R.id.timestamp);
            userId = (TextView) v.findViewById(R.id.user_id);
            tripCode = (TextView) v.findViewById(R.id.tripcode);
            subject = (TextView) v.findViewById(R.id.subject);
            comment = (TextView) v.findViewById(R.id.comment);
            thumbUrl = (ImageView) v.findViewById(R.id.thumbnail);
            gotoPost = (TextView) v.findViewById(R.id.goto_post);
            repliesText = (TextView) v.findViewById(R.id.replies_number);
            thumbnailContainer = (ViewGroup) v.findViewById(R.id.thumbnail_container);
            flagIcon = (ImageView) v.findViewById(R.id.flag_icon);

            comment.setMovementMethod(LongClickLinkMovementMethod.getInstance());
        }
    }
}
