package com.example.minitomcat.http;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public enum HttpMethod {
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    PATCH("PATCH"),
    DELETE("DELETE"),
    HEAD("HEAD"),
    OPTIONS("OPTIONS"),
    TRACE("TRACE"),
    CONNECT("CONNECT");

    private final String value;
    private static final Map<String, HttpMethod> METHODS = new HashMap<>();
    HttpMethod(String value) {
        this.value = value;
    }

    static {
        for (HttpMethod httpMethod : HttpMethod.values()) {
            METHODS.put(httpMethod.getValue(), httpMethod);
        }
    }

    public String getValue() {
        return value;
    }

    // text와 일치하는 value에 맞는 enum으로 바꾸기 위한 함수
    public static HttpMethod fromString(String text) {
        return METHODS.get(text);
    }
}
