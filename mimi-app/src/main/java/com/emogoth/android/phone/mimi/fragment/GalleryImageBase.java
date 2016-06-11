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


import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.ViewStubCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.activity.MimiActivity;
import com.emogoth.android.phone.mimi.async.DownloadThread;
import com.emogoth.android.phone.mimi.event.GalleryGridButtonEvent;
import com.emogoth.android.phone.mimi.util.BusProvider;
import com.emogoth.android.phone.mimi.util.Extras;
import com.emogoth.android.phone.mimi.util.IOUtils;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.Utils;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class GalleryImageBase extends MimiFragmentBase {
    private static final String LOG_TAG = GalleryImageBase.class.getSimpleName();
    private static final int NOTIFICATION_ID = 87;

    private static final int ACTION_CANCEL = 1;
    private static final int ACTION_OVERWITE = 2;
    private static final int ACTION_RENAME = 3;

    private int imageSampleSize = 1;
    //    private ChanPost post;
    private String originalFilename;
    private String fileExt;
    private String boardName;
    private String tim;

    private String imageUrl;

    private int maxWidth;
    private int maxHeight;
    private File imageFile;
    private ProgressBar progressBar;
    //    private int fileSize;
    private DownloadThread downloadTask;
    private ImageDisplayedListener imageDisplayedListener;

    private boolean defaultLayout = true;
    private boolean updateWhenVisible = false;
    private boolean useOriginalFilename;

    private int mediaWidth;
    private int mediaHeight;

    private boolean muted = true;
    private ViewStubCompat viewStub;

    public abstract void scaleBitmap(ImageDisplayedListener listener);

    public abstract void displayImage(final File imageFileName, final boolean isVisible);

    public abstract void startAnimation();

    public abstract void stopAnimation();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(false);
        setRetainInstance(false);

        final Bundle args = getArguments();
        if (args != null) {
            boardName = args.getString(Extras.EXTRAS_BOARD_NAME);
            originalFilename = args.getString(Extras.EXTRAS_POST_FILENAME);
            tim = args.getString(Extras.EXTRAS_POST_TIM);
            fileExt = args.getString(Extras.EXTRAS_POST_FILENAME_EXT);
            mediaWidth = args.getInt(Extras.EXTRAS_WIDTH, 0);
            mediaHeight = args.getInt(Extras.EXTRAS_HEIGHT, 0);
//            fileSize = args.getInt(Extras.EXTRAS_POST_SIZE);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        useOriginalFilename = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean(getString(R.string.use_original_filename_pref), false);

        View v = null;
        try {
            v = inflater.inflate(R.layout.fragment_gallery_image, container, false);
        } catch (final Exception e) {
            Log.e(LOG_TAG, "Error inflating view", e);
        }
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (view == null || !defaultLayout || getArguments() == null) {
            return;
        }

        viewStub = (ViewStubCompat) view.findViewById(R.id.view_stub);
        progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);

        final String protocol = MimiUtil.httpOrHttps(getActivity());
        final String baseUrl = getString(R.string.image_link);
        final String basePath = getString(R.string.full_image_path, boardName, tim, fileExt);
        imageUrl = protocol + baseUrl + basePath;

        downloadTask = new DownloadThread(getImageFileLocation(), imageUrl);
        downloadTask.setProgressListener(new DownloadThread.ProgressListener() {
            @Override
            public void onProgressUpdate(int progress) {
                progressBar.setProgress(progress);
            }

            @Override
            public void onComplete(final String filePath) {
                Log.i(LOG_TAG, "Finished downloading image: location=" + filePath);
                if (!TextUtils.isEmpty(filePath) && getActivity() != null) {

                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }

                    imageFile = new File(filePath);
                    displayImage(imageFile, getUserVisibleHint());


                }
            }
        });
        downloadTask.setErrorListener(new DownloadThread.ErrorListener() {
            @Override
            public void onError() {
                Log.e(LOG_TAG, "Error downloading file");
            }
        });

        downloadTask.start();
    }

    public void inflateLayout(@LayoutRes int res, ViewStubCompat.OnInflateListener listener) {
        if (viewStub != null) {
            if (listener != null) {
                viewStub.setOnInflateListener(listener);
            }

            viewStub.setLayoutResource(res);
            viewStub.inflate();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (downloadTask != null) {
            Log.d(LOG_TAG, "Stopping download task if active");
            downloadTask.cancel();
        }

        if (imageFile != null && imageFile.exists()) {
            Log.d(LOG_TAG, "Deleting image file: name=" + imageFile.getAbsolutePath());
            imageFile.delete();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(Extras.EXTRAS_BOARD_NAME, boardName);
        outState.putString(Extras.EXTRAS_POST_FILENAME, originalFilename);
        outState.putString(Extras.EXTRAS_POST_TIM, tim);
        outState.putString(Extras.EXTRAS_POST_FILENAME_EXT, fileExt);
        outState.putInt(Extras.EXTRAS_WIDTH, mediaWidth);
        outState.putInt(Extras.EXTRAS_HEIGHT, mediaHeight);

        if (imageFile != null) {
            outState.putString(Extras.EXTRAS_FILE_PATH, imageFile.getAbsolutePath());
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            boardName = savedInstanceState.getString(Extras.EXTRAS_BOARD_NAME);
            originalFilename = savedInstanceState.getString(Extras.EXTRAS_POST_FILENAME);
            tim = savedInstanceState.getString(Extras.EXTRAS_POST_TIM);
            fileExt = savedInstanceState.getString(Extras.EXTRAS_POST_FILENAME_EXT);
            mediaWidth = savedInstanceState.getInt(Extras.EXTRAS_WIDTH, 0);
            mediaHeight = savedInstanceState.getInt(Extras.EXTRAS_HEIGHT, 0);

            if (savedInstanceState.containsKey(Extras.EXTRAS_FILE_PATH)) {
                imageFile = new File(savedInstanceState.getString(Extras.EXTRAS_FILE_PATH));
            }
        }

    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {

        if (imageFile != null && imageFile.exists() && updateWhenVisible) {
            displayImage(imageFile, isVisibleToUser);
        }

        super.setUserVisibleHint(isVisibleToUser);
    }

    @Override
    public void initMenu() {
        super.initMenu();
        if (getActivity() != null && getActivity() instanceof MimiActivity) {
            final Toolbar toolbar = ((MimiActivity) getActivity()).getToolbar();
            if (toolbar != null) {
                toolbar.getMenu().clear();
                toolbar.inflateMenu(R.menu.image_menu);
            }
        }
    }

    protected void useDefaultLayout(final boolean defaultLayout) {
        this.defaultLayout = defaultLayout;
    }

    public File getImageFileLocation() {
        final File fullImagePath = new File(MimiUtil.getInstance().getCacheDir().getAbsolutePath(), "full_images/" + boardName + "/");
        final File imageFileName = new File(fullImagePath, tim + fileExt);
        try {
            fullImagePath.mkdirs();
            imageFileName.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return imageFileName;
    }

    protected int getMediaWidth() {
        return this.mediaWidth;
    }

    protected int getMediaHeight() {
        return this.mediaHeight;
    }

    protected void showContent() {
        progressBar.setVisibility(View.GONE);
        viewStub.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPause() {
        super.onPause();

        if (downloadTask != null) {
            downloadTask.cancel();
        }

        BusProvider.getInstance().unregister(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (imageFile != null) {
            final String filePath = imageFile.getAbsolutePath();
            final String ext = filePath.substring(filePath.lastIndexOf(".") + 1);
            final String tmpFileName = MimiUtil.getTempPath(getActivity(), ext);
            final File tmpFile = new File(tmpFileName);

            if (tmpFile.exists()) {
                tmpFile.delete();
            }

        }

        BusProvider.getInstance().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (downloadTask != null) {
            downloadTask.cancel();
        }
    }

    public void doUpdateWhenVisible(final boolean shouldUpdate) {
        updateWhenVisible = shouldUpdate;
    }

    public void safeSaveFile(final boolean showNotification) {

        if (imageFile != null) {
            final String fileName;
            if (useOriginalFilename) {
                fileName = originalFilename;
            } else {
                fileName = tim;
            }
            final File saveDir = MimiUtil.getSaveDir(getActivity());
            final File writeFile = new File(saveDir, fileName + fileExt);

            final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
            dialogBuilder.setTitle(R.string.copy_file);
            dialogBuilder.setMessage(R.string.file_name_is_taken);
            dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            dialogBuilder.setNeutralButton(R.string.overwrite, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    saveFile(showNotification, ACTION_OVERWITE);
                }
            });
            dialogBuilder.setPositiveButton(R.string.rename, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    saveFile(showNotification, ACTION_RENAME);
                }
            });

            if (writeFile.exists()) {
                dialogBuilder.show();
            } else {
                saveFile(showNotification, ACTION_OVERWITE);
            }

        }
    }

    public boolean saveFile(final boolean showNotification, final int action) {
        if (getActivity() == null) {
            return false;
        }

        try {
            final File saveDir = MimiUtil.getSaveDir(getActivity());
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }

            if (imageFile != null) {

                final String fileName;
                if (useOriginalFilename) {
                    fileName = originalFilename;
                } else {
                    fileName = tim;
                }

                File writeFile = new File(saveDir, fileName + fileExt);

                if (writeFile.exists() && action == ACTION_CANCEL) {
                    return false;
                }

                if (action == ACTION_RENAME) {
                    int i = 1;
                    while (writeFile.exists()) {
                        Log.d(LOG_TAG, "File " + writeFile.getAbsolutePath() + " exists");
                        writeFile = new File(saveDir, fileName + "(" + i + ")" + fileExt);
                        i++;
                    }
                }

                final File destPath = writeFile;
                if (copyFile(imageFile, destPath)) {
                    try {
                        invokeMediaScanner(destPath);

                        if (showNotification) {
                            scaleBitmap(new ImageDisplayedListener() {
                                @Override
                                public void onImageDisplayed(GalleryImageBase imageFragment, Bitmap bmp) {
                                    showSaveNotification(bmp, destPath);
                                }
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                Toast.makeText(getActivity(), R.string.failed_to_save_file, Toast.LENGTH_LONG).show();
            }

        } catch (final Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    private void invokeMediaScanner(File savedFile) {
        if (getActivity() != null) {

            Toast.makeText(getActivity(), R.string.file_saved, Toast.LENGTH_LONG).show();

            final File noMediaFile = new File(MimiUtil.getSaveDir(getActivity()), ".nomedia");
            if (!noMediaFile.exists()) {

                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.MIME_TYPE, Utils.getMimeType(fileExt));
                values.put(MediaStore.Images.Media.DATA, savedFile.getAbsolutePath());
                values.put(MediaStore.Images.Media.DATE_MODIFIED, savedFile.lastModified());

                getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                final String[] dir = {savedFile.getAbsolutePath()};
                MediaScannerConnection.scanFile(getActivity(), dir, null, new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i(LOG_TAG, "SCAN COMPLETED: path=" + path + ", uri=" + uri);
                    }
                });
            }

        }
    }

    private void showSaveNotification(Bitmap bmp, File destPath) {
        if (getActivity() == null) {
            return;
        }

        final String type;
        if (fileExt != null && fileExt.equalsIgnoreCase(".webm")) {
            type = "video/*";
        } else {
            type = "image/*";
        }
        final Uri uriToImage = Uri.parse("file://" + destPath.getAbsolutePath());
        final Intent contentIntent = new Intent();
        contentIntent.setAction(Intent.ACTION_VIEW);
        contentIntent.setDataAndType(uriToImage, type);

        final PendingIntent pendingContentIntent = PendingIntent.getActivity(getActivity(), 0, contentIntent, 0);

        final Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setDataAndType(uriToImage, type);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uriToImage);

        final PendingIntent pendingShareIntent = PendingIntent.getActivity(getActivity(), 0, shareIntent, 0);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity());
        builder.setContentTitle(getString(R.string.file_saved));
        builder.setContentText(destPath.getName());
        builder.setSubText(MimiUtil.humanReadableByteCount(destPath.length(), true));
        builder.setSmallIcon(R.drawable.ic_notification_photo);
        builder.setLargeIcon(bmp);
        builder.setStyle(new NotificationCompat.BigPictureStyle()
                .bigPicture(bmp));
        builder.setContentIntent(pendingContentIntent);
        builder.addAction(R.drawable.ic_notification_share, getString(R.string.share), pendingShareIntent);

        final Notification saveFileNotification = builder.build();
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getActivity());
        notificationManager.notify(NOTIFICATION_ID, saveFileNotification);
    }

    protected boolean copyFile(final File copyFrom, final File copyTo) {

        final int IO_BUFFER_SIZE = 1024;
        int i;

        InputStream fis = null;
        OutputStream fos = null;

        try {

            if (copyFrom.exists()) {

                final File destDir = new File(copyTo.getParent());
                if (!destDir.exists()) {
                    if (!destDir.mkdir()) {
                        Toast.makeText(getActivity(), R.string.error_copying_file, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }

                fis = new FileInputStream(copyFrom);
                fos = new FileOutputStream(copyTo);
                IOUtils.copy(fis, fos);

                return true;
            }

        } catch (OutOfMemoryError e) {
            Log.e(LOG_TAG, "Out of memory: " + e.getMessage());
            getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    Toast.makeText(getActivity(), "Out of memory", Toast.LENGTH_SHORT).show();
                }

            });

        } catch (Exception e) {
            Log.e(LOG_TAG, "Error copying file", e);

            getActivity().runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    Toast.makeText(getActivity(), R.string.error_copying_file, Toast.LENGTH_SHORT).show();
                }

            });
        } finally {
            IOUtils.closeQuietly(fos);
            IOUtils.closeQuietly(fis);
        }

        return false;
    }

    @Subscribe
    public void onGalleryGridButtonEvent(final GalleryGridButtonEvent event) {
        Log.i(LOG_TAG, "Detected grid button press");
    }

    @Override
    public String getTitle() {
        return originalFilename;
    }

    @Override
    public String getSubtitle() {
        return null;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public File getImageFile() {
        return imageFile;
    }

    public void setImageFile(final File imageFile) {
        this.imageFile = imageFile;
    }

    public String getTim() {
        return tim;
    }

    public void setTim(String tim) {
        this.tim = tim;
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public void setMaxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public void setMaxHeight(int maxHeight) {
        this.maxHeight = maxHeight;
    }

    public String getFileExt() {
        return fileExt;
    }

    public void setFileExt(String fileExt) {
        this.fileExt = fileExt;
    }

    public String getBoardName() {
        return boardName;
    }

    public void setBoardName(String boardName) {
        this.boardName = boardName;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public void setOnImageDisplayedListener(final ImageDisplayedListener listener) {
        this.imageDisplayedListener = listener;
    }

    public ImageDisplayedListener getOnImageDisplayedListener() {
        if (imageDisplayedListener == null) {
            Log.i(LOG_TAG, "Image displayed listener is null");
        } else {
            Log.i(LOG_TAG, "Image displayed listener is not null");
        }
        return imageDisplayedListener;
    }

    public interface ImageDisplayedListener {
        void onImageDisplayed(final GalleryImageBase imageFragment, final Bitmap bmp);
    }

}
