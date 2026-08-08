package com.example.minitomcat.dispatcher;

import lombok.Getter;

import java.lang.reflect.Method;

@Getter
public class HandlerMethod {
    private final Object controller;
    private final Method method;

    public HandlerMethod(Object controller, Method method) {
        this.controller = controller;
        this.method = method;
    }
}
