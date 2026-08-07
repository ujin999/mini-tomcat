package com.example.minitomcat.filter;

import com.example.minitomcat.http.HttpRequest;
import com.example.minitomcat.http.HttpResponse;
import com.example.minitomcat.http.HttpSessionHandler;

public class SessionFilter implements Filter {
    private final HttpSessionHandler sessionHandler;

    public SessionFilter(HttpSessionHandler sessionHandler) {
        this.sessionHandler = sessionHandler;
    }

    @Override
    public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) {
        sessionHandler.handle(request, response);

        chain.doFilter(request, response);
    }
}
