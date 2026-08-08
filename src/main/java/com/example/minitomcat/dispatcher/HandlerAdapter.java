package com.example.minitomcat.dispatcher;

import com.example.minitomcat.http.HttpRequest;
import com.example.minitomcat.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Slf4j
public class HandlerAdapter {
    public void handle(HttpRequest request, HttpResponse response, HandlerMethod handlerMethod)
        throws InvocationTargetException, IllegalArgumentException, IllegalAccessException {

        Method method = handlerMethod.getMethod();
        Class<?>[] parameterTypes = method.getParameterTypes();

        Object[] args = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> type = parameterTypes[i];

            if (type == HttpRequest.class) {
                args[i] = request;
            } else if (type == HttpResponse.class) {
                args[i] = response;
            } else {
                throw new RuntimeException(type.getCanonicalName() + " is not supported");
            }
        }

        Object controllerInstance = handlerMethod.getController();

        String body = (String) method.invoke(controllerInstance, args);

        response.setStatusCode(200);
        response.setReasonPhrase("ok");
        response.write(body);
        response.setContentType("text/plain; charset=utf-8");
    }
}
