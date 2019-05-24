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


import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.emogoth.android.phone.mimi.BuildConfig;
import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.app.MimiApplication;
import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.CookiePersistor;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;

//import okhttp3.JavaNetCookieJar;

public class HttpClientFactory {
    private static final String LOG_TAG = HttpClientFactory.class.getSimpleName();
    private static final int MAX_CACHE_SIZE = 100 * 1024 * 1024;

    private static HttpClientFactory ourInstance = new HttpClientFactory();
    private OkHttpClient client;
    private SharedPrefsCookiePersistor cookiePersistor;

    public enum ClientType {
        API, DOWNLOAD
    }

    public static HttpClientFactory getInstance() {
        return ourInstance;
    }

    private HttpClientFactory() {
    }

    private void init(ClientType type) {
        final Cache cache = new Cache(MimiUtil.getInstance().getCacheDir(), MAX_CACHE_SIZE);
        final OkHttpClient.Builder builder = new OkHttpClient.Builder();

        cookiePersistor = new SharedPrefsCookiePersistor(MimiApplication.getInstance());
        PersistentCookieJar jar = new PersistentCookieJar(new SetCookieCache(), cookiePersistor);

        builder.cache(cache)
                .cookieJar(jar)
                .followSslRedirects(true)
                .connectTimeout(90, TimeUnit.SECONDS)
                .readTimeout(90, TimeUnit.SECONDS)
                .writeTimeout(90, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true);

        if (BuildConfig.DEBUG) {
            builder.addNetworkInterceptor(loggingInterceptor());
            builder.addNetworkInterceptor(new StethoInterceptor());
        }

//        try {
//            SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
//            builder.sslSocketFactory(sslSocketFactory, new MyX509TrustManager());
//        } catch (NoSuchAlgorithmException | KeyStoreException e) {
//            Log.e(LOG_TAG, "Could not enable SSL", e);
//            Crashlytics.logException(e);
//        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MimiApplication.getInstance());
        String prefsKey = MimiApplication.getInstance().getString(R.string.http_buffer_size_pref);
        String defaultValue = MimiApplication.getInstance().getString(R.string.http_buffer_size_default);

        int bufferSize = Integer.valueOf(prefs.getString(prefsKey, defaultValue));
        if (bufferSize > 0) {
            builder.socketFactory(new DelegatingSocketFactory(SocketFactory.getDefault()) {
                @Override
                protected Socket configureSocket(Socket socket) throws IOException {
                    socket.setSendBufferSize(bufferSize);
                    socket.setReceiveBufferSize(bufferSize);

                    return socket;
                }
            });
        }
        client = builder.build();
    }

    private Interceptor loggingInterceptor() {
        return chain -> {
            Request request = chain.request();
            Log.d(LOG_TAG, "[Secure] " + String.valueOf(chain.connection().socket() instanceof javax.net.ssl.SSLSocket));
            Log.d(LOG_TAG, "[URL] " + request.url().toString());
            for (Map.Entry<String, List<String>> stringListEntry : request.headers().toMultimap().entrySet()) {
                for (String s : stringListEntry.getValue()) {
                    Log.d(LOG_TAG, "[Header] " + stringListEntry.getKey() + ": " + s);
                }
            }
            return chain.proceed(request);
        };
    }

    public CookiePersistor getCookieStore() {
        return cookiePersistor;
    }

    public OkHttpClient getClient() {
        if (client == null) {
            init(ClientType.API);
        }

        return client;
    }

    public void reset() {
        client = null;
    }
}
