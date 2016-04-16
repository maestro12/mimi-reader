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

package com.emogoth.android.phone.mimi.util;


import com.emogoth.android.phone.mimi.app.MimiApplication;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;

public class HttpClientFactory {
    public static final int MAX_CACHE_SIZE = 100 * 1024 * 1024;
    private static HttpClientFactory ourInstance = new HttpClientFactory();
    private OkHttpClient okHttpClient;
    private CookieHandler cookieHandler;
    private PersistentCookieStore cookieStore;

    public static HttpClientFactory getInstance() {
        return ourInstance;
    }

    private HttpClientFactory() {
    }

    public void init() {
        final Cache cache = new Cache(MimiUtil.getInstance().getCacheDir(), MAX_CACHE_SIZE);
        final OkHttpClient.Builder builder = new OkHttpClient.Builder();

        cookieStore = new PersistentCookieStore(MimiApplication.getInstance());
        cookieHandler = new CookieManager(cookieStore, CookiePolicy.ACCEPT_ALL);

        builder.cache(cache)
                .cookieJar(new JavaNetCookieJar(cookieHandler))
                .connectTimeout(30, TimeUnit.SECONDS);

        okHttpClient = builder.build();
    }

    public CookieStore getCookieStore() {
        return cookieStore;
    }

    public OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            init();
        }

        return okHttpClient;
    }
}
