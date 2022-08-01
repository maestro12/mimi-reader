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

package com.mimireader.chanlib.rx;

import okhttp3.ResponseBody;
import retrofit2.Response;

public class ResponseAndPojo<T> {

    private final T value;
    private final Response<ResponseBody> response;

    public ResponseAndPojo(T value, Response<ResponseBody> response) {
        this.value = value;
        this.response = response;
    }

    public T getValue() {
        return value;
    }

    public Response<ResponseBody> getResponse() {
        return response;
    }
}
