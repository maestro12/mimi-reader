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
