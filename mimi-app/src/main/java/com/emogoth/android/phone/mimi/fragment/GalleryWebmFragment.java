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


import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.media.MediaCodec;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.ViewStubCompat;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;
import com.emogoth.android.phone.mimi.event.GalleryImageTouchEvent;
import com.emogoth.android.phone.mimi.interfaces.AudioSettingsHost;
import com.emogoth.android.phone.mimi.util.BusProvider;
import com.emogoth.android.phone.mimi.util.ExoPlayerHelper;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.emogoth.android.phone.mimi.util.Utils;
import com.emogoth.android.phone.mimi.util.WebmRendererBuilder;
import com.google.android.exoplayer.AspectRatioFrameLayout;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.audio.AudioTrack;
import com.google.android.exoplayer.util.PlayerControl;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.TimeInterval;


public class GalleryWebmFragment extends GalleryImageBase implements ExoPlayerHelper.Listener {
    private static final String LOG_TAG = GalleryWebmFragment.class.getSimpleName();

    private final Point mScreenSize = new Point();
    private final Point mVideoSize = new Point();
    private ExoPlayerHelper player;
    private PlayerControl playerControl;

    private AppCompatImageView previewImageView;
    private TextureView videoSurface;
    private AspectRatioFrameLayout videoContainer;

    private boolean muted;
    private View webmBar;
    private View openButton;
    private AppCompatImageView muteButton;
    private TextView webmTime;

    private Subscription videoTimer;

    public GalleryWebmFragment() {
    }

    @Override
    public void onViewCreated(View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        doUpdateWhenVisible(true);

        inflateLayout(R.layout.gallery_image_webm, new ViewStubCompat.OnInflateListener() {
            @Override
            public void onInflate(ViewStubCompat stub, View inflated) {
                if (getActivity() == null) {
                    return;
                }

                if (getActivity() instanceof AudioSettingsHost) {
                    muted = !((AudioSettingsHost) getActivity()).isAudioLocked();
                }

                webmBar = inflated.findViewById(R.id.webm_bar);
                openButton = inflated.findViewById(R.id.open_button);
                webmTime = (TextView) inflated.findViewById(R.id.webm_time);
                muteButton = (AppCompatImageView) inflated.findViewById(R.id.mute_button);
                if (muted) {
                    muteButton.setImageResource(R.drawable.ic_audio_off);
                } else {
                    muteButton.setImageResource(R.drawable.ic_audio_on);
                    if (getActivity() instanceof AudioSettingsHost && ((AudioSettingsHost) getActivity()).isAudioLocked()) {
                        lockAudio();
                    }
                }

                previewImageView = (AppCompatImageView) inflated.findViewById(R.id.preview_image);
                videoSurface = (TextureView) inflated.findViewById(R.id.video_surface);
                videoContainer = (AspectRatioFrameLayout) inflated.findViewById(R.id.video_container);
                if (previewImageView != null) {
                    previewImageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            BusProvider.getInstance().post(new GalleryImageTouchEvent());
                        }
                    });
                }

                stopTimer();

            }
        });

        getActivity().getWindowManager().getDefaultDisplay().getSize(mScreenSize);
    }

    @Override
    public String getPageName() {
        return "gallery_webm_image";
    }

    @Override
    public void scaleBitmap(final ImageDisplayedListener listener) {
        setOnImageDisplayedListener(listener);

        if (getActivity() != null) {
            loadBitmapFromVideo(getImageFile().getAbsolutePath(), getMaxWidth(), getMaxHeight());
        }
    }

    @Override
    public void displayImage(final File imageFileName, final boolean isVisible) {
        if (videoSurface != null && getActivity() != null) {
            if (videoSurface.getVisibility() != View.VISIBLE) {
                videoSurface.setVisibility(View.VISIBLE);
            }

            if (isVisible) {

                if (player == null) {
                    setupVideo();
                }

                player.mute(muted);
                startAnimation();
            } else {

                if (player != null) {
                    player.release();
                    player = null;
                }

                if (previewImageView != null) {
                    videoSurface.setVisibility(View.INVISIBLE);
                    previewImageView.setVisibility(View.VISIBLE);
                }
                if (previewImageView != null && previewImageView.getDrawable() == null) {
                    loadBitmapFromVideo(getImageFile().getAbsolutePath(), getMaxWidth(), getMaxHeight());
                }
            }

            initWebmBar();
        }
    }

    private void initWebmBar() {
        webmBar.setVisibility(View.VISIBLE);
        openButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final String filePath = getImageFile().getAbsolutePath();
                        final String fileExt = filePath.substring(filePath.lastIndexOf(".") + 1);
                        final String tmpFileName = MimiUtil.getTempPath(getActivity(), fileExt);
                        final File tmpFile = new File(tmpFileName);
                        final String fileUri = "file://" + tmpFileName;
                        if (copyFile(getImageFile(), tmpFile)) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(fileUri));
                            intent.setDataAndType(Uri.parse(fileUri), Utils.getMimeType(fileExt));
                            startActivity(intent);
                        }


                    }
                });
                thread.start();
            }
        });

        muteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (muted) {
                    updateMuteButton(R.color.md_white_1000, R.drawable.ic_audio_on);
                    muted = false;
                } else {
                    muteButton.setImageResource(R.drawable.ic_audio_off);
                    muted = true;

                    if (getActivity() instanceof AudioSettingsHost) {
                        ((AudioSettingsHost) getActivity()).setAudioLock(false);
                    }
                }

                if (player != null) {
                    player.mute(muted);
                    player.seekTo(0);
                }
            }
        });

        muteButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (player != null && muted) {
                    player.mute(false);
                    player.seekTo(0);
                }

                return lockAudio();
            }
        });

    }

    private boolean lockAudio() {
        if (getActivity() == null) {
            return false;
        }

        ((AudioSettingsHost) getActivity()).setAudioLock(true);

        muted = false;
        return updateMuteButton(R.color.md_green_400, R.drawable.ic_audio_on);
    }

    private boolean updateMuteButton(@ColorRes int colorRes, @DrawableRes int iconRes) {
        int color = ResourcesCompat.getColor(getResources(), colorRes, getActivity().getTheme());
        Drawable normalDrawable = VectorDrawableCompat.create(getResources(), iconRes, getActivity().getTheme());

        if (normalDrawable != null) {
            Drawable wrapDrawable = DrawableCompat.wrap(normalDrawable);
            DrawableCompat.setTint(wrapDrawable, color);

//                                    DrawableCompat.setTint(normalDrawable, color);
            muteButton.setImageDrawable(normalDrawable);
            return true;
        }

        return false;
    }

    @Override
    public void startAnimation() {
        showVideoSurface(true);
        if (videoSurface.getSurfaceTexture() == null) {
            videoSurface.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                    startVideoPlayback(videoSurface.getSurfaceTexture());
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {

                }
            });
        } else {
            startVideoPlayback(videoSurface.getSurfaceTexture());
        }
    }

    private void updateDisplayTime() {
        if (player != null && webmTime != null) {
            long position = player.getCurrentPosition();
            long duration = player.getDuration();
            String display = String.format(Locale.getDefault(), "%02d:%02d / %02d:%02d",
                    TimeUnit.MINUTES.convert(position, TimeUnit.MILLISECONDS),
                    TimeUnit.MILLISECONDS.toSeconds(position) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(position)),
                    TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS),
                    TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));
            webmTime.setText(display);
        }
    }

    private void startVideoPlayback(SurfaceTexture surfaceTexture) {
        final Surface surface = new Surface(surfaceTexture);
        player.setSurface(surface);
        player.setPlayWhenReady(true);

        startTimer();
    }

    @Override
    public void stopAnimation() {
        if (player != null && playerControl != null) {
            playerControl.pause();
            player.seekTo(0);
            player.removeListener(this);

            showPreviewImage();
        }

        stopTimer();
    }

    private void showPreviewImage() {
        previewImageView.setVisibility(View.VISIBLE);
        videoSurface.setVisibility(View.GONE);
    }

    protected void showVideoSurface(boolean onlyShowSurface) {

        videoContainer.setVisibility(View.VISIBLE);

        if (!onlyShowSurface) {
            if (getMediaWidth() > 0 && getMediaHeight() > 0) {
                float aspectRatio = getMediaWidth() / (float) getMediaHeight();
                videoContainer.setAspectRatio(aspectRatio);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        getActivity().getWindowManager().getDefaultDisplay().getSize(mScreenSize);
        if (mVideoSize.x > 0 && mVideoSize.y > 0) {
            // trigger resize
            onVideoSizeChanged(mVideoSize.x, mVideoSize.y, 0, 1.0f);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (player != null) {
            player.release();
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (!isVisibleToUser) {
            stopAnimation();
        }

        if (getActivity() instanceof AudioSettingsHost) {
            muted = !((AudioSettingsHost) getActivity()).isAudioLocked();
        }

        if (muteButton != null) {
            if (muted) {
                muteButton.setImageResource(R.drawable.ic_audio_off);
            } else {
                muteButton.setImageResource(R.drawable.ic_audio_on);
                if (getActivity() instanceof AudioSettingsHost && ((AudioSettingsHost) getActivity()).isAudioLocked()) {
                    lockAudio();
                }
            }

            if(player != null && player.isMuted() != muted) {
                player.mute(muted);
            }
        }
    }

    private Subscription loadBitmapFromVideo(String videoPath, final int width, final int height) {
        return Observable.just(videoPath)
                .map(new Func1<String, Bitmap>() {
                    @Override
                    public Bitmap call(String s) {
                        Bitmap bmp = getBitmapFromVideoFile(s);
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

                                    if (bmp != null) {
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
                .compose(RxUtil.<Bitmap>applyBackgroundSchedulers())
                .subscribe(new Action1<Bitmap>() {
                    @Override
                    public void call(Bitmap bitmap) {
                        if (previewImageView != null) {
                            previewImageView.setImageBitmap(bitmap);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {

                    }
                });
    }

    private Bitmap getBitmapFromVideoFile(String path) {
        try {
            if (path == null) {
                return null;
            }

            MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
            metadataRetriever.setDataSource(path);
            final WeakReference<Bitmap> weakBmp = new WeakReference<>(metadataRetriever.getFrameAtTime());
            return weakBmp.get();
        } catch (Exception e) {
            Log.w(LOG_TAG, "Error getting bitmap from the video", e);
        }

        return null;
    }

    private void startTimer() {
        updateDisplayTime();
        videoTimer = Observable.interval(1, TimeUnit.SECONDS).timeInterval()
                .compose(DatabaseUtils.<TimeInterval<Long>>applySchedulers())
                .subscribe(new Action1<TimeInterval<Long>>() {
                    @Override
                    public void call(TimeInterval<Long> longTimeInterval) {
                        updateDisplayTime();
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e(LOG_TAG, "Timer error", throwable);
                    }
                });
    }

    private void stopTimer() {
        if (webmTime != null) {
            webmTime.setText("00:00 / 00:00");
        }

        RxUtil.safeUnsubscribe(videoTimer);
    }

    private void setupVideo() {
        Uri uri = Uri.fromFile(getImageFileLocation());
        ExoPlayerHelper.RendererBuilder rendererBuilder = new WebmRendererBuilder(getActivity(), uri);
        player = new ExoPlayerHelper(rendererBuilder);
        player.setLooping(true);
        player.prepare();
        player.setPlayWhenReady(false);
        player.addListener(this);

        player.setInternalErrorListener(new ExoPlayerHelper.InternalErrorListener() {
            @Override
            public void onRendererInitializationError(Exception e) {
                Log.e(LOG_TAG, "onRendererInitializationError", e);
            }

            @Override
            public void onAudioTrackInitializationError(AudioTrack.InitializationException e) {
                Log.e(LOG_TAG, "onAudioTrackInitializationError", e);
            }

            @Override
            public void onAudioTrackWriteError(AudioTrack.WriteException e) {
                Log.e(LOG_TAG, "onAudioTrackWriteError", e);
            }

            @Override
            public void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
                Log.e(LOG_TAG, "onAudioTrackUnderrun: buffersize=" + bufferSize + ", bufferSizeMs=" + bufferSize + ", elapsedSinceLastFeedMs=" + elapsedSinceLastFeedMs);
            }

            @Override
            public void onDecoderInitializationError(MediaCodecTrackRenderer.DecoderInitializationException e) {
                Log.e(LOG_TAG, "onRendererInitializationError", e);
            }

            @Override
            public void onCryptoError(MediaCodec.CryptoException e) {
                Log.e(LOG_TAG, "onRendererInitializationError", e);
            }

            @Override
            public void onLoadError(int sourceId, IOException e) {
                Log.e(LOG_TAG, "onRendererInitializationError", e);
            }

            @Override
            public void onDrmSessionManagerError(Exception e) {
                Log.e(LOG_TAG, "onRendererInitializationError", e);
            }
        });

        playerControl = player.getPlayerControl();
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

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        // no op
    }

    @Override
    public void onError(Exception e) {
        Log.w(LOG_TAG, "ExoPlayer Error", e);
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        if (videoSurface != null) {
            float widthRatio = (float) mScreenSize.x / (float) width;
            float heightRatio = (float) mScreenSize.y / (float) height;
            float ratio = widthRatio < heightRatio ? widthRatio : heightRatio;

            ViewGroup.LayoutParams layoutParams = videoSurface.getLayoutParams();
            layoutParams.width = (int) (width * ratio);
            layoutParams.height = (int) (height * ratio);
            videoSurface.setLayoutParams(layoutParams);
        }

        mVideoSize.set(width, height);
    }

    @Override
    public void onDrawnToSurface(Surface surface) {
        if (videoSurface != null && videoSurface.getVisibility() != View.VISIBLE) {
            showVideoSurface(false);
        }
    }
}
