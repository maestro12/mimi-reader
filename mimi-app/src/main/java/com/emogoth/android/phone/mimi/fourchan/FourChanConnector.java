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

import android.util.Log;

import com.emogoth.android.phone.mimi.app.MimiApplication;
import com.emogoth.android.phone.mimi.fourchan.api.FourChanApi;
import com.emogoth.android.phone.mimi.fourchan.api.FourChanPostApi;
import com.emogoth.android.phone.mimi.fourchan.models.FourChanBoards;
import com.emogoth.android.phone.mimi.fourchan.models.FourChanPost;
import com.emogoth.android.phone.mimi.fourchan.models.FourChanThread;
import com.emogoth.android.phone.mimi.fourchan.models.FourChanThreadPage;
import com.mimireader.chanlib.ChanConnector;
import com.mimireader.chanlib.models.ChanBoard;
import com.mimireader.chanlib.models.ChanCatalog;
import com.mimireader.chanlib.models.ChanPost;
import com.mimireader.chanlib.models.ChanThread;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;

public class FourChanConnector extends ChanConnector {
    private static final String LOG_TAG = FourChanConnector.class.getSimpleName();

    private final FourChanApi api;
    private final FourChanPostApi postApi;

    private FourChanConnector(FourChanApi chanInterface, FourChanPostApi postInterface) {
        this.api = chanInterface;
        this.postApi = postInterface;
    }

    @Override
    public Flowable<List<ChanBoard>> fetchBoards() {
        return api.fetchBoards()
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .map(FourChanBoards::toBoardList)
                .onErrorReturn(throwable -> Collections.emptyList());
    }

    @Override
    public Flowable<ChanCatalog> fetchCatalog(final String boardName, final String boardTitle) {
        return api.fetchCatalog(boardName)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .map(fourChanThreadPages -> {
                    ChanCatalog cat = new ChanCatalog();
                    cat.setBoardName(boardName);
                    cat.setBoardTitle(boardTitle);
                    for (FourChanThreadPage fourChanThreadPage : fourChanThreadPages) {
                        List<ChanPost> posts = new ArrayList<>();
                        for (FourChanPost fourChanPost : fourChanThreadPage.getThreads()) {
                            fourChanPost.processComment(MimiApplication.getInstance().getApplicationContext(), boardName, 0);
                            posts.add(fourChanPost.toPost());
                        }
                        cat.addPosts(posts);
                    }

                    return cat;
                });
    }

    @Override
    public Flowable<ChanCatalog> fetchPage(final int page, final String boardName, final String boardTitle) {
        return api.fetchPage(page, boardName)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .map(fourChanThreads -> {
                    ChanCatalog cat = new ChanCatalog();
                    List<ChanPost> posts = new ArrayList<>();
                    for (FourChanThread fourChanThread : fourChanThreads.getThreads()) {
                        FourChanPost post = fourChanThread.getPosts().get(0);
                        post.processComment(MimiApplication.getInstance().getApplicationContext(), boardName, 0);
                        posts.add(post.toPost());
                    }
                    cat.setBoardName(boardName);
                    cat.setBoardTitle(boardTitle);
                    cat.addPosts(posts);
                    return cat;
                });
    }

    @Override
    public Flowable<ChanThread> fetchThread(final String boardName, final long threadId, final String cacheControl) {
        return api.fetchThread(boardName, threadId, cacheControl)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .map(fourChanThread -> {
                    List<ChanPost> posts = new ArrayList<>();
                    for (FourChanPost fourChanPost : fourChanThread.getPosts()) {
                        posts.add(fourChanPost.toPost());
                    }

                    return new ChanThread(boardName, threadId, posts);
                });
    }

    @Override
    public Single<Response<ResponseBody>> post(String boardName, Map<String, Object> params) {
        final Map<String, RequestBody> parts = getPartsFromMap(params);

        return postApi.post(boardName, parts)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorReturn(throwable -> {
                    Log.e(LOG_TAG, "Error posting comment", throwable);
                    return badResponse(throwable);
                });

    }

    @Override
    public Single<Response<ResponseBody>> login(String token, String pin) {
        MediaType type = MediaType.parse("text/plain");
        RequestBody tokenRequestBody = RequestBody.create(type, token);
        RequestBody pinRequestBody = RequestBody.create(type, pin);
        RequestBody longLoginRequestBody = RequestBody.create(type, "no");
        RequestBody actRequestBody = RequestBody.create(type, "do_login");
        return postApi.login(tokenRequestBody, pinRequestBody, longLoginRequestBody, actRequestBody)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorReturn(FourChanConnector::badResponse);
    }

    public String getThumbUrl(String boardName, String id) {
        return FourChanEndpoints.Image + "/" + boardName + "/" + id + "s.jpg";
    }

    public String getImageBaseUrl() {
        return FourChanEndpoints.Image;
    }

    @Override
    public String getImageCountText(int imageCount) {

        if (imageCount == 1) {
            return "1 image";
        }

        return imageCount + " images";
    }

    @Override
    public String getRepliesCountText(int repliesCount) {
        if (repliesCount == 1) {
            return "1 reply";
        }

        return repliesCount + " replies";
    }

    public static String getDefaultEndpoint() {
        return FourChanEndpoints.Default;
    }

    public static String getDefaultPostEndpoint() {
        return FourChanEndpoints.Post;
    }

    private static Response<ResponseBody> badResponse(Throwable throwable) {
        return Response.error(-1, ResponseBody.create(MediaType.parse("text/plain"), throwable.getLocalizedMessage()));
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
