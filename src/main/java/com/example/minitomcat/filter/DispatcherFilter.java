package com.example.minitomcat.filter;

import com.example.minitomcat.dispatcher.HandlerAdapter;
import com.example.minitomcat.dispatcher.HandlerMapping;
import com.example.minitomcat.dispatcher.HandlerMethod;
import com.example.minitomcat.exception.http.HttpExceptionHandler;
import com.example.minitomcat.http.HttpRequest;
import com.example.minitomcat.http.HttpResponse;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;

public class DispatcherFilter implements Filter {
    @Nullable
    private final HandlerMapping handlerMapping;

    @Nullable
    private final HandlerAdapter handlerAdapter;

    public DispatcherFilter(HandlerMapping handlerMapping, HandlerAdapter handlerAdapter) {
        this.handlerMapping = handlerMapping;
        this.handlerAdapter = handlerAdapter;
    }

    @Override
    public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) {
        HandlerMethod handlerMethod = handlerMapping.getHandler(request);

        if (handlerMethod != null) {
            try {
                handlerAdapter.handle(request, response, handlerMethod);
            } catch (InvocationTargetException | IllegalAccessException e) {
                new HttpExceptionHandler().handleUnexpected(e, response);
            }

            return;
        }

        chain.doFilter(request, response);
    }
}
