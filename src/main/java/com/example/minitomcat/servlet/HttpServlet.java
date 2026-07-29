package com.example.minitomcat.servlet;

import com.example.minitomcat.http.HttpMethod;
import com.example.minitomcat.http.HttpRequest;
import com.example.minitomcat.http.HttpResponse;

public abstract class HttpServlet {
    public void service(HttpRequest httpRequest, HttpResponse httpResponse) {
        HttpMethod method = httpRequest.getMethod();
        switch (method) {
            case GET -> doGet(httpRequest, httpResponse);
            case POST -> doPost(httpRequest, httpResponse);
        }
    }

    protected void doGet(HttpRequest httpRequest, HttpResponse httpResponse) {
        httpResponse.setStatusCode(405);
        httpResponse.write("405 Method Not Allowed");
    }

    protected void doPost(HttpRequest httpRequest, HttpResponse httpResponse) {
        httpResponse.setStatusCode(405);
        httpResponse.write("405 Method Not Allowed");
    }
}
