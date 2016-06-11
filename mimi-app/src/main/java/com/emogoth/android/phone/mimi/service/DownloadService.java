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

package com.emogoth.android.phone.mimi.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.app.MimiApplication;
import com.emogoth.android.phone.mimi.util.HttpClientFactory;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.ThreadRegistry;
import com.mimireader.chanlib.models.ChanPost;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class DownloadService extends IntentService {
    public static final String LOG_TAG = DownloadService.class.getSimpleName();
    public static final int IO_BUFFER_SIZE = 1024;

    public static final int REGISTRY_ID = 1001;

    // keys
    public static final String CONTEXT_KEY = "context"; // The context we will be running in. Usually the application context.
    public static final String RECEIVER_KEY = "receiver";
    public static final String POSITION_KEY = "position";
    public static final String FILENAME_KEY = "filename";
    public static final String FILEURL_KEY = "fileurl";
    public static final String THREADID_KEY = "threadid";
    public static final String THUMBNAIL_KEY = "thumbnailname";
    public static final String THUMBURL_KEY = "thumburl";
    public static final String FILESIZE_KEY = "filesize";
    public static final String STATUS_KEY = "status";
    public static final String ERROR_KEY = "error";
    public static final String WIDTH_KEY = "display_width";
    public static final String PROGRESS_KEY = "progress";
    public static final String FILTER_KEY = "filter";
    public static final String THREAD_KEY = "threadid";
    public static final String BOARD_KEY = "board";
    public static final String DATA_KEY = "data";
    public static final String ITEMS_KEY = "items";
    public static final String DOWNLOAD_TYPE_KEY = "downloadtype";

    // responses
    public static String COMMAND_RECEIVED = "command_received";
    public static String COMMAND_SAVE = "command_save";
    public static String COMMAND_SAVE_DEVICE = "command_save_device";
    public static String PERCENT_DOWNLOADED = "percent_downloaded";

    // Status Constants
    public static final int STATUS_STARTED = 0x1;
    public static final int STATUS_RUNNING = 0x2;
    public static final int STATUS_FINISHED = 0x3;
    public static final int STATUS_SUCCESS = 0x4;
    public static final int STATUS_ERROR = 0x5;

    // Download Types
    public static final int DOWNLOAD_BATCH = 0x1;
    public static final int DOWNLOAD_SINGLE = 0x2;
    public static final int DOWNLOAD_CANCEL = 0x3;

    // Storage Constants
    public static final int SAVE_CACHE = 0x1;
    public static final int SAVE_SDCARD = 0x2;

    private static final int KB = 1024;
    private static final int MB = KB * 1024;

    private ArrayList<ChanPost> posts;

    private int position;
    private int fileSize;
    private String intentFilter;
    private MimiApplication app;

    private int NOTIFICATION_ID = 1;
    private NotificationCompat.Builder notificationCompat;
    private Notification notification;
    private NotificationManager notificationManager;

    private ResultReceiver receiver;

    private ThreadPoolExecutor executor;
    private HashMap<String, Runnable> runnableMap;

    public DownloadService() {
        super("MimiDownloadService");

        executor = new ThreadPoolExecutor(2, 4, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(2));
        runnableMap = new HashMap<>();
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        boolean is_err = false;
        final Bundle serviceData = intent.getExtras();

        if (serviceData == null) {
            return;
        }

        final int downloadType = serviceData.getInt(DownloadService.DOWNLOAD_TYPE_KEY);

        app = (MimiApplication) getApplication();
        intentFilter = serviceData.getString(FILTER_KEY);

        switch (downloadType) {
            case DownloadService.DOWNLOAD_SINGLE:

                final String fileUrl = serviceData.getString(FILEURL_KEY);

                final Runnable downloadRunnable = new Runnable() {
                    @Override
                    public void run() {
                        position = serviceData.getInt(POSITION_KEY);
                        String downloadFileName = serviceData.getString(FILENAME_KEY);
                        fileSize = serviceData.getInt(FILESIZE_KEY);


                        String downloadDir = serviceData.getString(COMMAND_SAVE);

                        final boolean isError = __download(downloadDir, downloadFileName, fileUrl);

                        final Intent returnIntent = new Intent();

                        returnIntent.setAction(intentFilter);
                        returnIntent.addCategory(Intent.CATEGORY_DEFAULT);
                        returnIntent.putExtra(POSITION_KEY, position);
                        returnIntent.putExtra(ERROR_KEY, isError);
                        returnIntent.putExtra(STATUS_KEY, STATUS_FINISHED); // complete
                        LocalBroadcastManager.getInstance(app).sendBroadcast(returnIntent);

                        runnableMap.remove(fileUrl);
                    }
                };
                runnableMap.put(fileUrl, downloadRunnable);
                executor.execute(downloadRunnable);

//            sendBroadcast(returnIntent);
                break;
            case DownloadService.DOWNLOAD_BATCH:
                serviceData.setClassLoader(ChanPost.class.getClassLoader());
                final List<ChanPost> batchData;
                final ArrayList<String> files = new ArrayList<>();

                if(serviceData.containsKey(DATA_KEY)) {
                    batchData = serviceData.getParcelableArrayList(DATA_KEY);
                } else {
                    batchData = ThreadRegistry.getInstance().getPosts(REGISTRY_ID);
                    ThreadRegistry.getInstance().clearPosts(REGISTRY_ID);
                }

//			final int [] items = serviceData.getIntArray(ITEMS_KEY);
                final String board = serviceData.getString(BOARD_KEY);
                final String thread = String.valueOf(serviceData.getInt(THREAD_KEY));
                final String save_dir = serviceData.getString(COMMAND_SAVE);

                final File mimi_dir = new File(save_dir);
                final File board_dir = new File(mimi_dir, board);
                final File thread_path = new File(board_dir, thread);
                if (!thread_path.exists()) {
                    thread_path.mkdirs();
                }

                final String protocol = MimiUtil.httpOrHttps(app);
                final String baseUrl = getString(R.string.image_link);

                final String dir = thread_path.getAbsolutePath();
                final String[] location = new String[batchData.size()];

                final boolean useOriginalFilename = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.use_original_filename_pref), false);

                createNotification();

                for (int i = 0; i < batchData.size(); i++) {

                    final ChanPost post = batchData.get(i);
                    final int index = i;
                    final String basePath = getString(R.string.full_image_path, board, post.getTim(), post.getExt());
                    final String url = protocol + baseUrl + basePath;

                    Log.i(LOG_TAG, "Downloading image: url=" + url);

                    final Intent startedIntent = new Intent();
                    String fileName;
                    if (useOriginalFilename) {
                        fileName = post.getFilename() + post.getExt();
                    } else {
                        fileName = post.getTim() + post.getExt();
                    }

                    final File downloadFile = new File(dir, fileName);

                    final float fProgress = (float) index / (float) batchData.size();
                    final int iProgress = (int) (fProgress * 100.0);

                    boolean isError = false;

                    notificationProgressUpdate(iProgress);

                    if (!TextUtils.isEmpty(intentFilter)) {
                        startedIntent.setAction(intentFilter); // com.emogoth.android.phone.mimi.donate.intent.action.BatchDownloadResponse
                        startedIntent.addCategory(Intent.CATEGORY_DEFAULT);
                        startedIntent.putExtra(THREADID_KEY, batchData.get(index).getNo());
                        startedIntent.putExtra(POSITION_KEY, index);
                        startedIntent.putExtra(ERROR_KEY, false);
                        startedIntent.putExtra(STATUS_KEY, STATUS_STARTED); // complete

                        Log.i(LOG_TAG, "Sent: STATUS_STARTED [" + index + "]");
                        LocalBroadcastManager.getInstance(app.getApplicationContext()).sendBroadcast(startedIntent);
                    }

                    if (!downloadFile.exists()) {
//                    int fileCount = 1;
//                    File saveFile = downloadFile;
//                    while (saveFile.exists()) {
//                        fileName = post.getFilename() + "(" + fileCount + ")" + post.getExt();
//                        saveFile = new File(dir, fileName);
//                        fileCount++;
//                    }

                        fileSize = batchData.get(index).getFsize();
                        isError = __download(dir, fileName, url);

                        if (!isError) {
                            files.add(dir + "/" + fileName);
                        }

                        if (!TextUtils.isEmpty(intentFilter)) {
                            final Intent finishedIntent = new Intent();

                            finishedIntent.setAction(intentFilter);
                            finishedIntent.addCategory(Intent.CATEGORY_DEFAULT);
                            finishedIntent.putExtra(THREADID_KEY, batchData.get(index).getNo());
                            finishedIntent.putExtra(POSITION_KEY, index);
                            finishedIntent.putExtra(ERROR_KEY, isError);
                            finishedIntent.putExtra(STATUS_KEY, STATUS_FINISHED); // complete

                            Log.i(LOG_TAG, "Sent: STATUS_FINISHED[" + index + "]");
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(finishedIntent);
                        }
                    }

                    location[i] = downloadFile.getAbsolutePath();

                }

                notificationCompleted();

                try {
                    final File noMediaFile = new File(MimiUtil.getSaveDir(this), ".nomedia");
                    if (!noMediaFile.exists()) {
                        final ContentValues values = new ContentValues();

                        values.put(MediaStore.Images.Media.DATA, mimi_dir.getAbsolutePath());
                        values.put(MediaStore.Images.Media.DATE_MODIFIED, mimi_dir.lastModified());
                        getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                        MediaScannerConnection.scanFile(app, location, null, new MediaScannerConnection.OnScanCompletedListener() {
                            @Override
                            public void onScanCompleted(final String path, final Uri uri) {
                                Log.i(LOG_TAG, "SCAN COMPLETED: " + path);
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Failed to scan media -- Ignoring");
                }

                break;
            case DOWNLOAD_CANCEL:

            default:
                break;
        }

    }

    private boolean __download(String dir, String filename, String urlStr) {

        boolean isError = false;

        File f = null;

//		URL url;
        InputStream is;
//        BufferedInputStream bis;

//        try {
        if (filename != null) {
            f = new File(dir, filename);

        } else {
            return true;
        }

        try {

            OkHttpClient httpClient = HttpClientFactory.getInstance().getOkHttpClient();
            Request.Builder requestBuilder = new Request.Builder();
            Request request = requestBuilder.addHeader("Accept-Encoding", "gzip")
                    .url(urlStr)
                    .tag(urlStr)
                    .get()
                    .build();

            Response response = httpClient.newCall(request).execute();

            if (response.code() != 200) {
                //mError = true;
                Log.e(LOG_TAG, "Http status code: " + response.code());

                final Intent returnIntent = new Intent();
                returnIntent.setAction(intentFilter);
                returnIntent.addCategory(Intent.CATEGORY_DEFAULT);
                returnIntent.putExtra(POSITION_KEY, position);
                returnIntent.putExtra(ERROR_KEY, true);
                returnIntent.putExtra(STATUS_KEY, STATUS_ERROR); // complete
                returnIntent.putExtra(PROGRESS_KEY, 0);
                LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(returnIntent);

                return false;
            }

            is = response.body().byteStream();

            Log.i(LOG_TAG, "Image size: " + fileSize + " bytes");

            final FileOutputStream fos = new FileOutputStream(f);

            @SuppressWarnings("unused")
            byte[] data;

            int i = -1;
            boolean sentBroadcast = false;
            int count = 1;

            byte[] bytes = new byte[IO_BUFFER_SIZE];
            while (((i = is.read(bytes)) != -1)) {
                fos.write(bytes, 0, i);

                bytes = new byte[IO_BUFFER_SIZE];

                final int percent = (int) (((float) (count * IO_BUFFER_SIZE) / (float) fileSize) * 100.0F);

                if (!TextUtils.isEmpty(intentFilter) && fileSize > 0) {
                    if (percent % 5 == 0) {

                        if (!sentBroadcast) {
                            final Intent returnIntent = new Intent();

                            returnIntent.setAction(intentFilter);
                            returnIntent.addCategory(Intent.CATEGORY_DEFAULT);
                            returnIntent.putExtra(POSITION_KEY, position);
                            returnIntent.putExtra(ERROR_KEY, false);
                            returnIntent.putExtra(STATUS_KEY, STATUS_RUNNING); // complete
                            returnIntent.putExtra(PROGRESS_KEY, percent);
                            LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(returnIntent);
                        }

                        sentBroadcast = true;
                    } else {
                        sentBroadcast = false;
                    }
                }

                count++;

            }

            try {
                fos.close();
                is.close();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error closing handle: " + e.getMessage());
                e.printStackTrace();
            }

            return false;

        } catch (OutOfMemoryError e) {
//        	data = null;
            Log.e(LOG_TAG, "Error Downloading Image: " + e.getMessage());
            return true;

        } catch (Exception e) {
//        	data = null;
            Log.e(LOG_TAG, "Error Downloading Image: " + e.getMessage());
            e.printStackTrace();
            return true;

        }
    }

    public void createNotification() {

//    	Context appContext = app.getApplicationContext();

        //get the notification manager
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //create the notification
        int icon = android.R.drawable.stat_sys_download;
        CharSequence tickerText = getString(R.string.download_ticker); //Initial text that appears in the status bar
        long when = System.currentTimeMillis();

        notificationCompat = new NotificationCompat.Builder(app);

        //create the content which is shown in the notification pulldown
        final CharSequence contentTitle = getString(R.string.content_title);
        final CharSequence contentText = getString(R.string.percent_complete, 0); //Text of the notification in the pull down

        //you have to set a PendingIntent on a notification to tell the system what you want it to do when the notification is selected
        //I don't want to use this here so I'm just creating a blank one
        final Intent notificationIntent = new Intent();
        final PendingIntent contentIntent = PendingIntent.getActivity(app, 0, notificationIntent, 0);

        notificationCompat.setContentIntent(contentIntent)
                .setSmallIcon(icon)
                .setTicker(tickerText)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setAutoCancel(true)
                .setWhen(when);

        notification = notificationCompat.build();
        notification.flags = Notification.FLAG_ONGOING_EVENT;

        //show the notification
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    public void notificationProgressUpdate(int percentageComplete) {
        //build up the new status message
        final CharSequence contentText = getString(R.string.percent_complete, percentageComplete);
//        CharSequence contentText = fileName;

        // set up the notification
        notificationCompat.setContentText(contentText);

        //publish it to the status bar
        notification = notificationCompat.build();
        notificationManager.notify(NOTIFICATION_ID, notification);

        Log.i(LOG_TAG, "Notification update: " + contentText);
    }

    public void notificationCompleted() {

        Log.i(LOG_TAG, "Notification completed");

        CharSequence tickerText = getString(R.string.download_ticker); //Initial text that appears in the status bar
        long when = System.currentTimeMillis();

        notificationCompat = new NotificationCompat.Builder(app);

        //create the content which is shown in the notification pulldown
        final CharSequence contentTitle = getString(R.string.content_title);
        final CharSequence contentText = getString(R.string.download_complete); //Text of the notification in the pull down

        //you have to set a PendingIntent on a notification to tell the system what you want it to do when the notification is selected
        //I don't want to use this here so I'm just creating a blank one
        final Intent notificationIntent = new Intent();
        final PendingIntent contentIntent = PendingIntent.getActivity(app, 0, notificationIntent, 0);

        notificationCompat.setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_notification_photo)
                .setTicker(tickerText)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setAutoCancel(true)
                .setWhen(when);

        notification = notificationCompat.build();
//        notification.flags = Notification.FLAG_ONGOING_EVENT;

        //show the notification
        notificationManager.notify(NOTIFICATION_ID, notification);

//        notification.flags = Notification.DEFAULT_ALL;
//        notificationCompat.setContentText(getString(R.string.download_complete));
//        notificationManager.notify(NOTIFICATION_ID, notification);
//        notificationManager.cancel(NOTIFICATION_ID);
    }

}
