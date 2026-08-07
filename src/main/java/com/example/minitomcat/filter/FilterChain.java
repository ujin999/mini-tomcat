package com.example.minitomcat.filter;

import com.example.minitomcat.http.HttpRequest;
import com.example.minitomcat.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class FilterChain {
    private final List<Filter> filters;
    private int index = 0;

    public FilterChain(List<Filter> filters) {
        this.filters = filters;
    }

    public void doFilter(HttpRequest request, HttpResponse response) {

        if (index >= filters.size()) {
            return;
        }

        Filter next = filters.get(index++);
        next.doFilter(request, response, this);
    }
}
