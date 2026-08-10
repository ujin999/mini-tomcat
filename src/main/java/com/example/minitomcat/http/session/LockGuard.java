package com.example.minitomcat.http.session;

@FunctionalInterface
public interface LockGuard extends AutoCloseable {
    @Override
    void close();
}
