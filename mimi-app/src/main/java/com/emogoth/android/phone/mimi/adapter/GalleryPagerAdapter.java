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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.emogoth.android.phone.mimi.fragment.GalleryGifFragment;
import com.emogoth.android.phone.mimi.fragment.GalleryImageBase;
import com.emogoth.android.phone.mimi.fragment.GalleryImageFragment;
import com.emogoth.android.phone.mimi.fragment.GalleryWebmFragment;
import com.emogoth.android.phone.mimi.util.Extras;
import com.mimireader.chanlib.models.ChanPost;
import com.mimireader.chanlib.models.ChanThread;

import java.util.ArrayList;
import java.util.List;


public class GalleryPagerAdapter extends FragmentStatePagerAdapter {
    private static final String LOG_TAG = GalleryPagerAdapter.class.getSimpleName();
    private final List<ChanPost> posts;
    private GalleryImageBase.ImageDisplayedListener imageDisplayedListener;
    private String boardName;

    public GalleryPagerAdapter(final FragmentManager fm, List<ChanPost> posts, final String boardName, final GalleryImageBase.ImageDisplayedListener listener) {
        super(fm);

        this.boardName = boardName;
        this.imageDisplayedListener = listener;
        this.posts = getPostsWithImages(posts);
    }

    @Override
    public Fragment getItem(final int i) {
        if (i >= posts.size()) {
            return new GalleryImageFragment();
        }

        final ChanPost post = posts.get(i);
        if (post.getFilename() != null) {
            final GalleryImageBase fragment;

            if (post.getExt().equals(".webm")) {
                fragment = new GalleryWebmFragment();
            } else if (post.getExt().equals(".gif")) {
                fragment = new GalleryGifFragment();
            } else {
                fragment = new GalleryImageFragment();
            }

            fragment.setOnImageDisplayedListener(imageDisplayedListener);
            fragment.setArguments(createArgumentsBundle(post, boardName));

            return fragment;
        }

        return null;

    }

    @Override
    public int getCount() {
        return posts.size();
    }

    public static ArrayList<ChanPost> getPostsWithImages(final ChanThread thread) {
        return getPostsWithImages(thread.getPosts());
    }

    public static ArrayList<ChanPost> getPostsWithImages(final List<ChanPost> postList) {
        final ArrayList<ChanPost> posts = new ArrayList<>();
        for (final ChanPost post : postList) {
            if (post.getFilename() != null && !post.getFilename().equals("")) {
                posts.add(post);
            }
        }

        return posts;
    }

    public static int getIndexOfPost(final List<ChanPost> postList, final int postNumber) {
        final ChanPost post = new ChanPost();
        post.setNo(postNumber);
        return postList.indexOf(post);
    }

    private Bundle createArgumentsBundle(final ChanPost post, final String boardName) {
        final Bundle args = new Bundle();

        args.putString(Extras.EXTRAS_BOARD_NAME, boardName);
        args.putString(Extras.EXTRAS_POST_FILENAME_EXT, post.getExt());
        args.putString(Extras.EXTRAS_POST_TIM, post.getTim());
        args.putString(Extras.EXTRAS_POST_FILENAME, post.getFilename());
        args.putInt(Extras.EXTRAS_POST_SIZE, post.getFsize());

        return args;
    }
}
