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

package com.emogoth.android.phone.mimi.fourchan.api;


import com.emogoth.android.phone.mimi.fourchan.models.FourChanBoards;
import com.emogoth.android.phone.mimi.fourchan.models.FourChanThread;
import com.emogoth.android.phone.mimi.fourchan.models.FourChanThreadPage;
import com.emogoth.android.phone.mimi.fourchan.models.FourChanThreads;

import java.util.List;

import io.reactivex.Flowable;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import io.reactivex.Observable;


public interface FourChanApi {
    @GET("boards.json")
    Flowable<FourChanBoards> fetchBoards();

    @GET("{boardName}/{pageNumber}.json")
    Flowable<FourChanThreads> fetchPage(@Path("pageNumber") int page, @Path("boardName") String boardName);

    @GET("{boardName}/catalog.json")
    Flowable<List<FourChanThreadPage>> fetchCatalog(@Path("boardName") String boardName);

    @GET("{boardName}/thread/{threadId}.json")
    Flowable<FourChanThread> fetchThread(@Path("boardName") String boardName, @Path("threadId") long threadId, @Header("Cache-Control") String cacheControl);
}
