package com.example.minitomcat.servlet;

import com.example.minitomcat.exception.ServletException;
import com.example.minitomcat.exception.http.MethodNotAllowedException;
import com.example.minitomcat.http.HttpMethod;
import com.example.minitomcat.http.HttpRequest;
import com.example.minitomcat.http.HttpResponse;
import com.example.minitomcat.http.HttpStatus;

public abstract class HttpServlet {
    public void service(HttpRequest httpRequest, HttpResponse httpResponse) {
        HttpMethod method = httpRequest.getMethod();
        switch (method) {
            case GET -> doGet(httpRequest, httpResponse);
            case POST -> doPost(httpRequest, httpResponse);
        }
    }

    public void init() throws ServletException {}

    protected void doGet(HttpRequest httpRequest, HttpResponse httpResponse) {
        throw new MethodNotAllowedException(HttpStatus.METHOD_NOT_ALLOWED);
    }

    protected void doPost(HttpRequest httpRequest, HttpResponse httpResponse) {
        throw new MethodNotAllowedException(HttpStatus.METHOD_NOT_ALLOWED);
    }
}
