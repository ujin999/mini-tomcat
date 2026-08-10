package com.example.minitomcat.filter;

import com.example.minitomcat.exception.http.HttpExceptionHandler;
import com.example.minitomcat.handler.DefaultServlet;
import com.example.minitomcat.http.HttpRequest;
import com.example.minitomcat.http.HttpResponse;

import java.io.IOException;

public class DefaultServletFilter implements Filter {
    private final DefaultServlet defaultServlet;
    private final HttpExceptionHandler httpExceptionHandler;

    public DefaultServletFilter(DefaultServlet defaultServlet, HttpExceptionHandler httpExceptionHandler) {
        this.defaultServlet = defaultServlet;
        this.httpExceptionHandler = httpExceptionHandler;
    }

    @Override
    public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) {
        try {
            defaultServlet.service(request, response);
        } catch (IOException e) {
            // TODO: It is recommended to throw an exception in the client handler
            httpExceptionHandler.handleUnexpected(e, response);
        }
    }
}
