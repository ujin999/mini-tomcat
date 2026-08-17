package com.example.minitomcat.controller.bench.session_lock;

import com.example.minitomcat.http.session.LockGuard;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GlobalLockBenchManager {
    private final Map<String, GlobalLockBenchSession> sessions;
    private final long maxInactiveIntervalSeconds = 30;
    private final Lock lock = new ReentrantLock();

    public GlobalLockBenchManager() {
        sessions = new HashMap<>();
    }

    public GlobalLockBenchSession getSession(String sessionId) {
        GlobalLockBenchSession session = sessions.get(sessionId);

        if (session == null) {
            return null;
        }

        if (session.isExpired(maxInactiveIntervalSeconds)) {
            return null;
        }
        session.updateExpirationTime();

        return session;
    }

    public GlobalLockBenchSession createNewSession(int idx) {
        String secureId = String.valueOf(idx);
        GlobalLockBenchSession newSession = new GlobalLockBenchSession(secureId);
        sessions.put(secureId, newSession);
        return newSession;
    }

    public LockGuard withLock() {
        lock.lock();
        return lock::unlock;
    }
}
