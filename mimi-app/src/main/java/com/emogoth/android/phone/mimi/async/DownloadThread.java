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

package com.emogoth.android.phone.mimi.async;


import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.emogoth.android.phone.mimi.util.HttpClientFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadThread extends Thread {
    private static final int IO_BUFFER_SIZE = 1024;
    private static final String LOG_TAG = DownloadThread.class.getSimpleName();

    private final File saveDir;
    private final String url;

    private ProgressListener progressListener;
    private ErrorListener errorListener;
    private boolean cancelled;

    public DownloadThread(final File saveDir, final String url) {
        this.saveDir = saveDir;
        this.url = url;
        this.cancelled = false;
    }

    @Override
    public void run() {
        super.run();

        try {
            if (saveDir.exists() && saveDir.length() > 0) {
                downloadComplete(saveDir.getAbsolutePath());
            } else {
                final boolean isError = !download(saveDir, url);
                if (!isError) {
                    downloadComplete(saveDir.getAbsolutePath());
                } else {
                    downloadError();
                }
            }
        } catch (final Exception e) {
            Log.e(LOG_TAG, "Error downloading: url=" + url, e);
        }

    }

    public void cancel() {
        cancelled = true;
    }

    private boolean download(final File saveDir, final String url) {
        OkHttpClient httpClient = HttpClientFactory.getInstance().getOkHttpClient();
        Request.Builder requestBuilder = new Request.Builder();
        Request request = requestBuilder.addHeader("Accept-Encoding", "gzip")
                .url(url)
                .tag(url)
                .get()
                .build();

        try {
            Response response = httpClient.newCall(request).execute();
            if (response.code() == 200) {
                final FileOutputStream fos = new FileOutputStream(saveDir);
                InputStream inputStream = null;
                try {
                    inputStream = response.body().byteStream();
                    byte[] buff = new byte[IO_BUFFER_SIZE * 4];
                    long downloaded = 0;
                    long target = response.body().contentLength();

                    downloadProgress(0);
                    while (true) {
                        int readed = inputStream.read(buff);
                        if (readed == -1) {
                            break;
                        }
                        //write buff
                        fos.write(buff, 0, readed);
                        downloaded += readed;
                        downloadProgress(Math.round(downloaded / (float) target * 100));

                        if (cancelled) {
                            if (saveDir.exists()) {
                                saveDir.delete();
                            }
                            return false;
                        }
                    }
                    return downloaded == target;
                } catch (IOException ignore) {
                    return false;
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void downloadProgress(final int progress) {
        if (progressListener != null) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    progressListener.onProgressUpdate(progress);
                }
            });
        }
    }

    private void downloadComplete(final String downloadFile) {
        if (progressListener != null) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    progressListener.onComplete(downloadFile);
                }
            });
        }
    }

    private void downloadError() {
        if (errorListener != null) {
            final Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    errorListener.onError();
                }
            });
        }
    }

    public void setProgressListener(final ProgressListener listener) {
        this.progressListener = listener;
    }

    public void setErrorListener(final ErrorListener listener) {
        this.errorListener = listener;
    }

    public interface ProgressListener {
        void onProgressUpdate(final int progress);

        void onComplete(final String filePath);
    }

    public interface ErrorListener {
        void onError();
    }
}
