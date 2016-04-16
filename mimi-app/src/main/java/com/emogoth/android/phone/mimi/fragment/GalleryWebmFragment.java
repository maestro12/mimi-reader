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


import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.Animatable;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.dialog.VideoDialog;
import com.emogoth.android.phone.mimi.util.Extras;
import com.emogoth.android.phone.mimi.util.MimiUtil;

import java.io.File;
import java.util.concurrent.ExecutionException;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


public class GalleryWebmFragment extends GalleryImageBase {
    private static final String LOG_TAG = GalleryWebmFragment.class.getSimpleName();
    public static final String DIALOG_TAG = "video_dialog";

    private ImageDisplayedListener imageListener;

    public GalleryWebmFragment() {
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        enableOpenButton(true);
    }

    @Override
    public String getPageName() {
        return "gallery_webm_image";
    }

    @Override
    public void scaleBitmap(final ImageDisplayedListener listener) {
        imageListener = listener;

        if (getActivity() != null) {
            loadBitmapFromVideo(getImageFile().getAbsolutePath(), getMaxWidth(), getMaxHeight());
        }
    }

    @Override
    public void displayImage(final File imageFileName, final boolean isVisible) {
        if (getImageViewTouch() != null && getActivity() != null) {
            showBasicImage();
            loadBitmapFromVideo(getImageFile().getAbsolutePath(), getMaxWidth(), getMaxHeight());
        }
    }

    public void startAnimation() {
        if (getImageViewTouch() != null && getGifImageView().getDrawable() instanceof Animatable) {
            ((Animatable) getGifImageView().getDrawable()).start();
        }
    }

    public void stopAnimation() {
        if (getImageViewTouch() != null && getGifImageView().getDrawable() instanceof Animatable) {
            ((Animatable) getGifImageView().getDrawable()).stop();
        }
    }

//    @Override
//    public void showHighQualityImage() {
//        // ignore
//    }

    private void setupPlayButton(final Bitmap bmp, final int width, final int height) {
//        width = bmp.getWidth();
//        height = bmp.getHeight();

        setPlayButtonVisibility(View.VISIBLE);
        setOnPlayButtonClicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final Fragment frag = getChildFragmentManager().findFragmentByTag(DIALOG_TAG);

                if (frag == null) {
                    final VideoDialog dialog = new VideoDialog();
                    final Bundle args = new Bundle();

                    args.putString(Extras.EXTRAS_VIDEO_FILENAME, getImageFile().getAbsolutePath());
                    args.putInt(Extras.EXTRAS_WIDTH, width);
                    args.putInt(Extras.EXTRAS_HEIGHT, height);

                    Log.i(LOG_TAG, "video width=" + width);
                    Log.i(LOG_TAG, "video height=" + height);
                    dialog.setArguments(args);
                    dialog.show(getChildFragmentManager(), DIALOG_TAG);
                }

            }
        });

        if (getGifImageView() != null) {
            getGifImageView().setImageBitmap(bmp);
        }

        if (imageListener != null) {
            imageListener.onImageDisplayed(GalleryWebmFragment.this, bmp);
        }
    }

    private Subscription loadBitmapFromVideo(String videoPath, final int width, final int height) {
        return Observable.just(videoPath)
                .subscribeOn(Schedulers.newThread())
                .map(new Func1<String, Bitmap>() {
                    @Override
                    public Bitmap call(String s) {
                        Bitmap bmp = getBitmapFromVideoFile(s, width, height);
                        if (bmp == null) {
                            try {
                                if (getActivity() != null) {
                                    final String url = MimiUtil.httpOrHttps(getActivity())
                                            + getActivity().getString(R.string.thumb_link)
                                            + getActivity().getString(R.string.thumb_path, getBoardName(), String.valueOf(getTim()));
                                    bmp = Glide.with(getActivity())
                                            .load(url)
                                            .asBitmap()
                                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                                            .into(width, height)
                                            .get();

                                    if(bmp != null) {
                                        Point p = getBitmapDimensions(bmp, width, height);
                                        int w = p.x;
                                        int h = p.y;

                                        bmp = Bitmap.createScaledBitmap(bmp, w, h, true);
                                    }
                                }
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            }
                        }

                        return bmp;
                    }
                })
                .unsubscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorReturn(new Func1<Throwable, Bitmap>() {
                    @Override
                    public Bitmap call(Throwable throwable) {
                        throwable.printStackTrace();
                        return null;
                    }
                })
                .subscribe(new Action1<Bitmap>() {
                    @Override
                    public void call(Bitmap bitmap) {
                        if (bitmap == null) {
                            setupPlayButton(null, 0, 0);
                        } else {
                            setupPlayButton(bitmap, bitmap.getWidth(), bitmap.getHeight());
                        }
                    }
                });
    }

    private Bitmap getBitmapFromVideoFile(String path, int width, int height) {
        try {
            if (path == null) {
                return null;
            }

            MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
            metadataRetriever.setDataSource(path);

            final Bitmap bmp = metadataRetriever.getFrameAtTime();

            if (bmp == null) {
                Log.e(LOG_TAG, "Could not retrieve bitmap from MediaMetadataRetriever");
                return null;
            }

            Point p = getBitmapDimensions(bmp, width, height);
            int w = p.x;
            int h = p.y;

            metadataRetriever.release();

            return Bitmap.createScaledBitmap(bmp, w, h, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private Point getBitmapDimensions(Bitmap bmp, int width, int height) {
        Log.i(LOG_TAG, "Normal dimensions: width=" + width + ", height=" + height);
        final float scale;
        int w = width;
        int h = height;
        if (w < h) {
            Log.i(LOG_TAG, "width is less than height");
            scale = (float) width / (float) bmp.getWidth();
            h = Math.round((float) bmp.getHeight() * scale);
        } else {
            Log.i(LOG_TAG, "height is less than width");
            scale = (float) height / (float) bmp.getHeight();
            w = Math.round((float) bmp.getWidth() * scale);
        }

        Log.i(LOG_TAG, "Scaled dimensions: scale=" + scale + ",width=" + width + ", height=" + height);

        return new Point(w, h);
    }
}
