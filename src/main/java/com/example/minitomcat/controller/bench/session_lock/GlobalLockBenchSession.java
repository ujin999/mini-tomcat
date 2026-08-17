package com.example.minitomcat.controller.bench.session_lock;

import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public class GlobalLockBenchSession {
    @Getter
    private final String id;
    @Getter
    private final long creationTime;
    private long lastAccessedTime;
    private final Map<String, Object> attributes;

    protected GlobalLockBenchSession(String id) {
        this.id = id;
        this.creationTime = System.currentTimeMillis();
        this.lastAccessedTime = this.creationTime;
        this.attributes = new ConcurrentHashMap<>();
    }

    public void computeAttribute(String name, BiFunction<String, Object, Object> remappingFunction) {
        lastAccessedTime = System.currentTimeMillis();
        Object current = attributes.get(name);
        Object updated = remappingFunction.apply(name, current);
        attributes.put(name, updated);
    }

    public Object getAttribute(String name) {
        this.lastAccessedTime = System.currentTimeMillis();
        return this.attributes.get(name);
    }

    public void updateExpirationTime() {
        this.lastAccessedTime = System.currentTimeMillis();
    }

    public boolean isExpired(long maxInactiveIntervalSeconds) {
        long now = System.currentTimeMillis();
        return (now - lastAccessedTime) > maxInactiveIntervalSeconds * 1000;
    }
}
