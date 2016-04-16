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

package com.mimireader.chanlib;


import android.content.Context;
import android.webkit.MimeTypeMap;

import com.mimireader.chanlib.models.ChanBoard;
import com.mimireader.chanlib.models.ChanCatalog;
import com.mimireader.chanlib.models.ChanThread;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Cache;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;

public abstract class ChanConnector {

    public abstract Observable<List<ChanBoard>> fetchBoards();

    public abstract Observable<ChanCatalog> fetchCatalog(Context context, String boardName, String boardTitle);

    public abstract Observable<ChanCatalog> fetchPage(Context context, int page, String boardName, String boardTitle);

    public abstract Observable<ChanThread> fetchThread(Context context, String boardName, int threadId);

    public abstract Observable<Response<ResponseBody>> post(String boardName, Map<String, Object> params);

    public abstract Observable<Response<ResponseBody>> login(String token, String pin);

    public abstract String getThumbUrl(String boardName, String id, boolean secureConnection);

    public abstract String getImageCountText(int imageCount);

    public abstract String getRepliesCountText(int repliesCount);


    protected Map<String, RequestBody> getPartsFromMap(Map<String, Object> map) {
        Map<String, RequestBody> parts = new HashMap<>();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            RequestBody rb;
            try {
                if (entry.getValue() instanceof String) {
                    rb = RequestBody.create(MediaType.parse("text/plain"), (String) entry.getValue());
                } else if (entry.getValue() instanceof Integer) {
                    rb = RequestBody.create(MediaType.parse("text/plain"), String.valueOf((Integer) entry.getValue()));
                } else if (entry.getValue() instanceof Long) {
                    rb = RequestBody.create(MediaType.parse("text/plain"), String.valueOf((Long) entry.getValue()));
                } else if (entry.getValue() instanceof File) {
                    if (entry.getValue() != null) {
                        File f = (File) entry.getValue();
                        if (f.exists()) {
                            String extension = null;

                            int i = f.getAbsolutePath().lastIndexOf('.');
                            if (i > 0) {
                                extension = f.getAbsolutePath().substring(i + 1);
                            }

                            if (extension != null) {
                                String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                                rb = RequestBody.create(MediaType.parse("application/octet-stream"), f);
                                key = entry.getKey() + "\"; filename=\"" + f.getName();
                            } else {
                                rb = null;
                            }
                        } else {
                            rb = null;
                        }
                    } else {
                        rb = null;
                    }
                } else {
                    rb = null;
                }

                if (rb != null) {
                    parts.put(key, rb);
                }
            } catch (Exception e) {

            }


        }

        return parts;
    }

    public abstract static class ChanConnectorBuilder {
        private String endpoint;
        private String postEndpoint;
        private OkHttpClient client;
        private File cacheDir;
        private int cacheSize = 75 * 1024 * 1024;

        public ChanConnectorBuilder setCacheDirectory(File dir) {
            this.cacheDir = dir;
            return this;
        }

        public ChanConnectorBuilder setEndpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public ChanConnectorBuilder setPostEndpoint(String endpoint) {
            this.postEndpoint = endpoint;
            return this;
        }

        public ChanConnectorBuilder setClient(OkHttpClient client) {
            this.client = client;
            return this;
        }

        public ChanConnectorBuilder setMaxCacheSize(int size) {
            this.cacheSize = size;
            return this;
        }

        protected Retrofit initRetrofit(boolean isPost) {
            if (client == null) {
                OkHttpClient.Builder builder = new OkHttpClient.Builder();
                if(cacheDir != null && cacheDir.exists()) {
                    builder.cache(new Cache(cacheDir, cacheSize));
                }
                client = builder.build();
            }

            return new Retrofit.Builder()
                    .baseUrl(isPost ? postEndpoint : endpoint)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                    .build();
        }

        public abstract <T extends ChanConnector> T build();
    }
}
