package com.example.minitomcat.http.session;

import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HttpSession {
    @Getter
    private final String id;
    @Getter
    private final long creationTime;
    @Getter
    private long lastAccessedTime;
    private final Map<String, Object> attributes;

    protected HttpSession(String id) {
        this.id = id;
        this.creationTime = System.currentTimeMillis();
        this.lastAccessedTime = this.creationTime;
        this.attributes = new ConcurrentHashMap<>();
    }

    public void setAttribute(String name, Object value) {
        this.lastAccessedTime = System.currentTimeMillis();
        this.attributes.put(name, value);
    }

    public Object getAttribute(String name) {
        this.lastAccessedTime = System.currentTimeMillis();
        return this.attributes.get(name);
    }

    public boolean isExpired(long maxInactiveIntervalSeconds) {
        long now = System.currentTimeMillis();
        return (now - lastAccessedTime) > maxInactiveIntervalSeconds * 1000;
    }
}
