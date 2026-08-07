package com.example.minitomcat.filter;

import com.example.minitomcat.http.HttpRequest;
import com.example.minitomcat.http.HttpResponse;

public interface Filter {
    void doFilter(HttpRequest request, HttpResponse response, FilterChain chain);
}
