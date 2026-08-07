package com.example.minitomcat.filter;

import com.example.minitomcat.exception.http.RouteNotFoundException;
import com.example.minitomcat.http.HttpRequest;
import com.example.minitomcat.http.HttpResponse;
import com.example.minitomcat.routing.Router;

public class RouterFilter implements Filter {
    private final Router router;

    public RouterFilter(Router router) {
        this.router = router;
    }

    @Override
    public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) {
        try {
            router.route(request, response);
        } catch (RouteNotFoundException e) {
            chain.doFilter(request, response);
        }
    }
}
