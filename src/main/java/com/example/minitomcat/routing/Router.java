package com.example.minitomcat.routing;

import com.example.minitomcat.exception.RouteException;
import com.example.minitomcat.http.HttpRequest;
import com.example.minitomcat.http.HttpResponse;
import com.example.minitomcat.servlet.HttpServlet;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class Router {
    private final Map<String, HttpServlet> routingTable = new HashMap<>();

    public void register(String method, String uri, HttpServlet servlet) {
        String key = method + ":" + uri;

        if (routingTable.containsKey(key)) {
            throw new RouteException("This routing address is already registered: " + key);
        }

        routingTable.put(key, servlet);
        log.info("Route Registered: [" + method + "]" + uri);
    }

    // servlet으로 넘겨줘야 하기 때문에 Method method, String uri 로 넘기는 것이 불가능하다.
    public void route(HttpRequest httpRequest, HttpResponse httpResponse) {
        String key = httpRequest.getMethod() + ":" + httpRequest.getUri();
        HttpServlet httpServlet = routingTable.get(key);

        if (httpServlet != null) {
            httpServlet.service(httpRequest, httpResponse);
        } else {
            httpResponse.setStatusCode(404);
            httpResponse.setReasonPhrase("Not Found");
            httpResponse.write("Page Not Found");
        }
    }
}
