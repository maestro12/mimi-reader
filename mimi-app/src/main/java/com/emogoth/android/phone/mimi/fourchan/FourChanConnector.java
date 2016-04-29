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

package com.emogoth.android.phone.mimi.fourchan;

import android.content.Context;

import com.emogoth.android.phone.mimi.fourchan.api.FourChanApi;
import com.emogoth.android.phone.mimi.fourchan.api.FourChanPostApi;
import com.emogoth.android.phone.mimi.fourchan.models.FourChanBoards;
import com.emogoth.android.phone.mimi.fourchan.models.FourChanPost;
import com.emogoth.android.phone.mimi.fourchan.models.FourChanThread;
import com.emogoth.android.phone.mimi.fourchan.models.FourChanThreadPage;
import com.emogoth.android.phone.mimi.fourchan.models.FourChanThreads;
import com.mimireader.chanlib.ChanConnector;
import com.mimireader.chanlib.models.ChanBoard;
import com.mimireader.chanlib.models.ChanCatalog;
import com.mimireader.chanlib.models.ChanPost;
import com.mimireader.chanlib.models.ChanThread;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class FourChanConnector extends ChanConnector {

    private static String DEFAULT_ENDPOINT = "a.4cdn.org";
    private static String DEFAULT_POST_ENDPOINT = "https://sys.4chan.org";

    private static String THUMB_ENDPOINT = "t.4cdn.org";

    private final FourChanApi api;
    private final FourChanPostApi postApi;

    private FourChanConnector(FourChanApi chanInterface, FourChanPostApi postInterface) {
        this.api = chanInterface;
        this.postApi = postInterface;
    }

    @Override
    public Observable<List<ChanBoard>> fetchBoards() {
        return api.fetchBoards()
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Func1<FourChanBoards, List<ChanBoard>>() {
                    @Override
                    public List<ChanBoard> call(FourChanBoards fourChanBoards) {
                        return fourChanBoards.toBoardList();
                    }
                })
                .onErrorReturn(new Func1<Throwable, List<ChanBoard>>() {
                    @Override
                    public List<ChanBoard> call(Throwable throwable) {
                        return null;
                    }
                });
    }

    @Override
    public Observable<ChanCatalog> fetchCatalog(final Context context, final String boardName, final String boardTitle) {
        return api.fetchCatalog(boardName)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Func1<List<FourChanThreadPage>, ChanCatalog>() {
                    @Override
                    public ChanCatalog call(List<FourChanThreadPage> fourChanThreadPages) {
                        ChanCatalog cat = new ChanCatalog();
                        cat.setBoardName(boardName);
                        cat.setBoardTitle(boardTitle);
                        for (FourChanThreadPage fourChanThreadPage : fourChanThreadPages) {
                            List<ChanPost> posts = new ArrayList<>();
                            for (FourChanPost fourChanPost : fourChanThreadPage.getThreads()) {
                                fourChanPost.processComment(context, boardName, 0);
                                posts.add(fourChanPost.toPost());
                            }
                            cat.addPosts(posts);
                        }

                        return cat;
                    }
                });
    }

    @Override
    public Observable<ChanCatalog> fetchPage(final Context context, final int page, final String boardName, final String boardTitle) {
        return api.fetchPage(page, boardName)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Func1<FourChanThreads, ChanCatalog>() {
                    @Override
                    public ChanCatalog call(FourChanThreads fourChanThreads) {
                        ChanCatalog cat = new ChanCatalog();
                        List<ChanPost> posts = new ArrayList<>();
                        for (FourChanThread fourChanThread : fourChanThreads.getThreads()) {
                            FourChanPost post = fourChanThread.getPosts().get(0);
                            post.processComment(context, boardName, 0);
                            posts.add(post.toPost());
                        }
                        cat.setBoardName(boardName);
                        cat.setBoardTitle(boardTitle);
                        cat.addPosts(posts);
                        return cat;
                    }
                });
    }

    @Override
    public Observable<ChanThread> fetchThread(final Context context, final String boardName, final int threadId, final String cacheControl) {
        return api.fetchThread(boardName, threadId, cacheControl)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Func1<FourChanThread, ChanThread>() {
                    @Override
                    public ChanThread call(FourChanThread fourChanThread) {
                        ChanThread thread = new ChanThread();

                        List<ChanPost> posts = new ArrayList<>();
                        for (FourChanPost fourChanPost : fourChanThread.getPosts()) {
                            posts.add(fourChanPost.toPost());
                        }

                        thread.setPosts(posts);
                        thread.setBoardName(boardName);
                        thread.setThreadId(threadId);

                        return thread;
                    }

                });
    }

    @Override
    public Observable<Response<ResponseBody>> post(String boardName, Map<String, Object> params) {
        final Map<String, RequestBody> parts = getPartsFromMap(params);

        return postApi.post(boardName, parts)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorReturn(new Func1<Throwable, Response<ResponseBody>>() {
                    @Override
                    public Response<ResponseBody> call(Throwable throwable) {
                        return null;
                    }
                });

    }

    @Override
    public Observable<Response<ResponseBody>> login(String token, String pin) {
        MediaType type = MediaType.parse("text/plain");
        RequestBody tokenRequestBody = RequestBody.create(type, token);
        RequestBody pinRequestBody = RequestBody.create(type, pin);
        RequestBody longLoginRequestBody = RequestBody.create(type, "no");
        RequestBody actRequestBody = RequestBody.create(type, "do_login");
        return postApi.login(tokenRequestBody, pinRequestBody, longLoginRequestBody, actRequestBody)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorReturn(new Func1<Throwable, Response<ResponseBody>>() {
                    @Override
                    public Response<ResponseBody> call(Throwable throwable) {
                        return null;
                    }
                });
    }

    public String getThumbUrl(String boardName, String id, boolean secureConnection) {
        final String http;
        if(secureConnection) {
            http = "https://";
        } else {
            http = "http://";
        }

        return http + THUMB_ENDPOINT + "/" + boardName + "/" + id + "s.jpg";
    }

    @Override
    public String getImageCountText(int imageCount) {

        if(imageCount == 1) {
            return "1 image";
        }

        return imageCount + " images";
    }

    @Override
    public String getRepliesCountText(int repliesCount) {
        if(repliesCount == 1) {
            return "1 reply";
        }

        return repliesCount + " replies";
    }

    public static String getDefaultEndpoint(boolean secureConnection) {
        return (secureConnection ? "https://" : "http://") + DEFAULT_ENDPOINT;
    }

    public static String getDefaultPostEndpoint() {
        return DEFAULT_POST_ENDPOINT;
    }

    public static class Builder extends ChanConnectorBuilder {

        @Override
        @SuppressWarnings("unchecked")
        public FourChanConnector build() {
            FourChanApi api = initRetrofit(false).create(FourChanApi.class);
            FourChanPostApi postApi = initRetrofit(true).create(FourChanPostApi.class);
            return new FourChanConnector(api, postApi);
        }
    }

}
