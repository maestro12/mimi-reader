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

package com.emogoth.android.phone.mimi.dialog;

import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.util.Extras;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


public class VideoDialog extends DialogFragment implements TextureView.SurfaceTextureListener {
    private static final String LOG_TAG = VideoDialog.class.getSimpleName();

    public MediaPlayer mediaPlayer = null;
    private int videoWidth;
    private int videoHeight;
    private float videoScale;
    private TextureView textureView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.Dialog_Mimi_VideoPlayer);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.dialog_video_player, container, false);

        final Bundle args = getArguments();
        if(args == null) {
            throw new IllegalArgumentException("Arguments bundle cannot be null");
        }

        videoWidth = args.getInt(Extras.EXTRAS_WIDTH);
        videoHeight = args.getInt(Extras.EXTRAS_HEIGHT);
        videoScale = args.getFloat(Extras.EXTRAS_SCALE);

        final String videoFileName;
        if(args.containsKey(Extras.EXTRAS_VIDEO_FILENAME)) {
            try {
                videoFileName = args.getString(Extras.EXTRAS_VIDEO_FILENAME);
                final File videoFile = new File(videoFileName);

                if(videoFile.exists()) {
                    final FileInputStream fis = new FileInputStream(videoFile);
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setDataSource(fis.getFD());
                    mediaPlayer.setLooping(true);
                }

//                MediaController mediaController = new MediaController(getActivity());
//                mediaController.setMediaPlayer(mediaPlayer);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            videoFileName = null;
        }

        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ImageView closeButton = (ImageView) view.findViewById(R.id.close_vide_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        textureView = (TextureView) view.findViewById(R.id.video_surface);
        final ViewGroup.LayoutParams params = textureView.getLayoutParams();

        params.width = videoWidth;
        params.height = videoHeight;

        textureView.setLayoutParams(params);
        textureView.setSurfaceTextureListener(this);

    }

    @Override
    public void onPause() {
        super.onPause();
        try {
            if (mediaPlayer != null) {
                mediaPlayer.pause();
            }
        }
        catch (Exception e) {
            Log.e(LOG_TAG, "Could not pause video", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

//        if(mediaPlayer != null) {
//            mediaPlayer.start();
//        }
//
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            mediaPlayer.release();
        }
        catch(Exception e) {
            Log.e(LOG_TAG, "Could not release video", e);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if(mediaPlayer != null) {
            try {
                final Surface videoSurface = new Surface(surface);
                mediaPlayer.setSurface(videoSurface);
                mediaPlayer.prepareAsync();

                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        if(videoWidth <= 0 || videoHeight <= 0) {
                            final int viewWidth = textureView.getWidth();
                            final int viewHeight = textureView.getHeight();
                            final ViewGroup.LayoutParams params = textureView.getLayoutParams();
                            final float scale;

                            if(mediaPlayer.getVideoWidth() >= mediaPlayer.getVideoHeight()) {
                                scale = (float)videoWidth / (float)viewWidth;
                            }
                            else {
                                scale = (float)videoHeight / (float)viewHeight;
                            }

                            videoWidth = Math.round(mediaPlayer.getVideoWidth() * scale);
                            videoHeight = Math.round(mediaPlayer.getVideoHeight() * scale);

                            params.width = videoWidth;
                            params.height = videoHeight;

                            textureView.setLayoutParams(params);
                        }

                        mediaPlayer.start();

                        Log.i(LOG_TAG, "Video dimension: width=" + mediaPlayer.getVideoWidth() + ", height=" + mediaPlayer.getVideoHeight());
                    }
                });
            }
            catch(final IllegalStateException e) {
                Log.e(LOG_TAG, "Could not play video", e);
                if(getActivity() != null) {
                    Toast.makeText(getActivity(), R.string.could_not_play_video, Toast.LENGTH_SHORT).show();
                }

                dismiss();
            }
        }

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
}
