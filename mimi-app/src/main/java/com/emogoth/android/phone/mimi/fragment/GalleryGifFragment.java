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

package com.emogoth.android.phone.mimi.fragment;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.emogoth.android.phone.mimi.util.MimiUtil;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import pl.droidsonroids.gif.GifDrawable;


public class GalleryGifFragment extends GalleryImageBase {
    private static final String LOG_TAG = GalleryGifFragment.class.getSimpleName();

    public GalleryGifFragment() {
        doUpdateWhenVisible(true);
    }

    @Override
    public void startAnimation() {
        if (getImageViewTouch() != null && getGifImageView().getDrawable() instanceof GifDrawable) {
            ((GifDrawable) getGifImageView().getDrawable()).start();
        }
    }

    @Override
    public void stopAnimation() {
        if (getImageViewTouch() != null && getGifImageView().getDrawable() instanceof GifDrawable) {
            ((GifDrawable) getGifImageView().getDrawable()).stop();
        }
    }

    @Override
    public void scaleBitmap(final ImageDisplayedListener listener) {
        final BitmapFactory.Options options = new BitmapFactory.Options();

        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(getImageFile().getAbsolutePath(), options);
        options.inSampleSize = MimiUtil.calculateInSampleSize(options, getMaxWidth(), getMaxHeight());
        options.inJustDecodeBounds = false;

        if (listener != null) {
            final WeakReference<Bitmap> weakBitmap = new WeakReference<>(BitmapFactory.decodeFile(getImageFile().getAbsolutePath(), options));
            listener.onImageDisplayed(null, weakBitmap.get());
        }
    }

    @Override
    public void displayImage(final File imageFileName, final boolean isVisible) {
        WeakReference<Bitmap> weakBitmap = new WeakReference<>(null);
        showBasicImage();
        if (isVisible) {
            try {
                final GifDrawable gifDrawable = new GifDrawable(imageFileName);
                gifDrawable.setSpeed(1.25F);
                getGifImageView().setImageDrawable(gifDrawable);
                gifDrawable.setVisible(true, true);
                startAnimation();

                weakBitmap = new WeakReference<>(gifDrawable.seekToFrameAndGet(0));
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error setting up gif drawable: " + e.getLocalizedMessage());
            }
        } else {
            final BitmapFactory.Options options = new BitmapFactory.Options();

            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFileName.getAbsolutePath(), options);
            options.inSampleSize = MimiUtil.calculateInSampleSize(options, getMaxWidth(), getMaxHeight());
            options.inJustDecodeBounds = false;

            weakBitmap = new WeakReference<>(BitmapFactory.decodeFile(imageFileName.getAbsolutePath(), options));
            getGifImageView().setImageBitmap(weakBitmap.get());
        }

        if (getOnImageDisplayedListener() != null) {
            getOnImageDisplayedListener().onImageDisplayed(this, weakBitmap.get());
        }
    }

    @Override
    public String getPageName() {
        return "gallery_gif_image";
    }
}
