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
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.ViewStubCompat;
import android.view.View;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.emogoth.android.phone.mimi.BuildConfig;
import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.event.GalleryImageTouchEvent;
import com.emogoth.android.phone.mimi.util.BusProvider;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.RxUtil;

import java.io.File;
import java.lang.ref.WeakReference;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class GalleryImageFragment extends GalleryImageBase {
    private static final String LOG_TAG = GalleryImageFragment.class.getSimpleName();

    private Subscription resizeImageSubscription;
    private SubsamplingScaleImageView imageViewTouch;

    public GalleryImageFragment() {

    }

    @Override
    public void onViewCreated(View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        inflateLayout(R.layout.gallery_image_zoom, new ViewStubCompat.OnInflateListener() {
            @Override
            public void onInflate(ViewStubCompat stub, View view) {
                imageViewTouch = (SubsamplingScaleImageView) view.findViewById(R.id.full_image);
                if (imageViewTouch != null) {
                    imageViewTouch.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            BusProvider.getInstance().post(new GalleryImageTouchEvent());
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        RxUtil.safeUnsubscribe(resizeImageSubscription);
    }

    @Override
    public void startAnimation() {

    }

    @Override
    public void stopAnimation() {

    }

    @Override
    public void scaleBitmap(final ImageDisplayedListener listener) {

        if (getActivity() != null) {
            setOnImageDisplayedListener(listener);

            if (listener != null && imageViewTouch != null && getImageFile() != null && getImageFile().exists()) {

                RxUtil.safeUnsubscribe(resizeImageSubscription);
                resizeImageSubscription = Observable.just(getImageFile())
                        .subscribeOn(Schedulers.newThread())
                        .map(new Func1<File, Bitmap>() {
                            @Override
                            public Bitmap call(File file) {
                                final BitmapFactory.Options options = new BitmapFactory.Options();
                                final WeakReference<Bitmap> weakBitmap;

                                options.inJustDecodeBounds = true;
                                BitmapFactory.decodeFile(getImageFile().getAbsolutePath(), options);
                                options.inSampleSize = MimiUtil.calculateInSampleSize(options, getMaxWidth(), getMaxHeight());
                                options.inJustDecodeBounds = false;

                                weakBitmap = new WeakReference<>(BitmapFactory.decodeFile(getImageFile().getAbsolutePath(), options));
                                return weakBitmap.get();
                            }
                        })
                        .unsubscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .onErrorReturn(new Func1<Throwable, Bitmap>() {
                            @Override
                            public Bitmap call(Throwable throwable) {
                                return null;
                            }
                        })
                        .subscribe(new Action1<Bitmap>() {
                            @Override
                            public void call(Bitmap bitmap) {
                                listener.onImageDisplayed(GalleryImageFragment.this, bitmap);
                            }
                        });
            }
        }
    }

    @Override
    public void displayImage(final File imageFileName, final boolean isVisible) {

        if (getActivity() != null && imageViewTouch != null && imageFileName != null && imageFileName.exists()) {
            showContent();
            imageViewTouch.setOnImageEventListener(new SubsamplingScaleImageView.OnImageEventListener() {
                @Override
                public void onReady() {

                }

                @Override
                public void onImageLoaded() {
                    if (getOnImageDisplayedListener() != null) {
                        getOnImageDisplayedListener().onImageDisplayed(GalleryImageFragment.this, imageViewTouch.getDrawingCache(true));
                    }
                }

                @Override
                public void onPreviewLoadError(Exception e) {

                }

                @Override
                public void onImageLoadError(Exception e) {

                }

                @Override
                public void onTileLoadError(Exception e) {

                }
            });

            imageViewTouch.setDebug(BuildConfig.DEBUG);
            imageViewTouch.setImage(ImageSource.uri(imageFileName.getAbsolutePath()));
        }

    }

    @Override
    public String getPageName() {
        return "gallery_static_image";
    }
}
