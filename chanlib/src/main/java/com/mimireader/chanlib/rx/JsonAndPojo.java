package com.mimireader.chanlib.rx;

public class JsonAndPojo<T> {

    private final T value;
    private final String json;

    public JsonAndPojo(T value, String json) {
        this.value = value;
        this.json = json;
    }

    public T getValue() {
        return value;
    }

    public String getJsonString() {
        return json;
    }
}
