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

import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.util.Extras;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


public class VideoDialog extends DialogFragment implements TextureView.SurfaceTextureListener {
    private static final String LOG_TAG = VideoDialog.class.getSimpleName();

    public MediaPlayer mediaPlayer = null;
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
        if (args == null) {
            throw new IllegalArgumentException("Arguments bundle cannot be null");
        }

        final String videoFileName;
        if (args.containsKey(Extras.EXTRAS_VIDEO_FILENAME)) {
            try {
                videoFileName = args.getString(Extras.EXTRAS_VIDEO_FILENAME);
                final File videoFile = new File(videoFileName);

                if (videoFile.exists()) {
                    final FileInputStream fis = new FileInputStream(videoFile);
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setDataSource(fis.getFD());
                    mediaPlayer.setLooping(true);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
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
        textureView.setSurfaceTextureListener(this);

    }

    private void adjustAspectRatio(int videoWidth, int videoHeight) {
        int viewWidth = textureView.getWidth();
        int viewHeight = textureView.getHeight();
        double aspectRatio = (double) videoHeight / videoWidth;

        int newWidth, newHeight;
        if (viewHeight > (int) (viewWidth * aspectRatio)) {
            // limited by narrow width; restrict height
            newWidth = viewWidth;
            newHeight = (int) (viewWidth * aspectRatio);
        } else {
            // limited by short height; restrict width
            newWidth = (int) (viewHeight / aspectRatio);
            newHeight = viewHeight;
        }
        int xoff = (viewWidth - newWidth) / 2;
        int yoff = (viewHeight - newHeight) / 2;
        Log.v(LOG_TAG, "video=" + videoWidth + "x" + videoHeight +
                " view=" + viewWidth + "x" + viewHeight +
                " newView=" + newWidth + "x" + newHeight +
                " off=" + xoff + "," + yoff);

        Matrix txform = new Matrix();
        textureView.getTransform(txform);
        txform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
        //txform.postRotate(10);          // just for fun
        txform.postTranslate(xoff, yoff);
        textureView.setTransform(txform);
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
        if (mediaPlayer != null) {
            try {
                final Surface videoSurface = new Surface(surface);
                mediaPlayer.setSurface(videoSurface);
                mediaPlayer.prepareAsync();

                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        adjustAspectRatio(mediaPlayer.getVideoWidth(), mediaPlayer.getVideoHeight());

                        mediaPlayer.start();
                        Log.i(LOG_TAG, "Video dimension: width=" + mediaPlayer.getVideoWidth() + ", height=" + mediaPlayer.getVideoHeight());
                    }
                });
            } catch(final IllegalStateException e) {
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
