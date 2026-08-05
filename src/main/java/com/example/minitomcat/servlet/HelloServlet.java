package com.example.minitomcat.servlet;

import com.example.minitomcat.annotation.WebServlet;
import com.example.minitomcat.http.HttpMethod;
import com.example.minitomcat.http.HttpRequest;
import com.example.minitomcat.http.HttpResponse;

import java.util.Map;

@WebServlet(
        value = "/hello",
        method = HttpMethod.GET
)
public class HelloServlet extends HttpServlet {

    @Override
    public void doGet(HttpRequest httpRequest, HttpResponse httpResponse) {
        httpResponse.setStatusCode(200);
        httpResponse.setReasonPhrase("ok");

        String body = "Hello World";
        httpResponse.write(body);

        httpResponse.setContentType("text/plain; charset=utf-8");
    }
}
