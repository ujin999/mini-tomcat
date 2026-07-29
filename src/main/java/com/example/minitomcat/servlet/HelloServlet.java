package com.example.minitomcat.servlet;

import com.example.minitomcat.http.HttpRequest;
import com.example.minitomcat.http.HttpResponse;

import java.util.Map;

public class HelloServlet extends HttpServlet {
    @Override
    public void doGet(HttpRequest httpRequest, HttpResponse httpResponse) {
        httpResponse.setStatusCode(200);
        httpResponse.setReasonPhrase("ok");

        String body = "Hello World";
        httpResponse.write(body);

        Map<String, String> headers = httpResponse.getHeaders();
        headers.put("Content-Type", "text/plain; charset=utf-8");
        headers.put("Content-Length", String.valueOf(body.getBytes().length));
    }
}
