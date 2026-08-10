package com.example.minitomcat.http.session;

import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;

public class HttpSession {
    @Getter
    private final String id;
    @Getter
    private final long creationTime;
    private long lastAccessedTime;
    private final Map<String, Object> attributes;
    private final Lock lock = new ReentrantLock();

    protected HttpSession(String id) {
        this.id = id;
        this.creationTime = System.currentTimeMillis();
        this.lastAccessedTime = this.creationTime;
        this.attributes = new ConcurrentHashMap<>();
    }

    public void setAttribute(String name, Object value) {
        try (LockGuard ignored = withLock()) {
            this.lastAccessedTime = System.currentTimeMillis();
            this.attributes.put(name, value);
        }
    }

    public void computeAttribute(String name, BiFunction<String, Object, Object> remappingFunction) {
        try (LockGuard ignored = withLock()) {
            lastAccessedTime = System.currentTimeMillis();
            Object current = attributes.get(name);
            Object updated = remappingFunction.apply(name, current);
            attributes.put(name, updated);
        }
    }

    public Object getAttribute(String name) {
        try (LockGuard ignored = withLock()) {
            this.lastAccessedTime = System.currentTimeMillis();
            return this.attributes.get(name);
        }
    }

    public long getLastAccessedTime() {
        try (LockGuard ignored = withLock()) {
            return this.lastAccessedTime;
        }
    }

    public void updateExpirationTime() {
        try (LockGuard ignored = withLock()) {
            this.lastAccessedTime = System.currentTimeMillis();
        }
    }

    public boolean isExpired(long maxInactiveIntervalSeconds) {
        try (LockGuard ignored = withLock()) {
            long now = System.currentTimeMillis();
            return (now - lastAccessedTime) > maxInactiveIntervalSeconds * 1000;
        }
    }

    public LockGuard withLock() {
        lock.lock();
        return lock::unlock;
    }
}
