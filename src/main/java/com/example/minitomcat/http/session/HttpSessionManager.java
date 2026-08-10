package com.example.minitomcat.http.session;

import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HttpSessionManager {
    private final Map<String, HttpSession> sessions;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    // TODO: this value will be changed after develop
    private final long maxInactiveIntervalSeconds = 30;

    public HttpSessionManager() {
        this.sessions = new ConcurrentHashMap<>();
        startSessionCleanupTask();
    }

    public HttpSession getSession(String sessionId) {
        HttpSession session = sessions.get(sessionId);

        if (session == null) {
            return null;
        }

        try (LockGuard ignored = session.withLock()) {
            if (session.isExpired(maxInactiveIntervalSeconds)) {
                return null;
            }
            session.updateExpirationTime();
        }

        return session;
    }

    public HttpSession createNewSession() {
        String secureId = UUID.randomUUID().toString();
        HttpSession newSession = new HttpSession(secureId);
        sessions.put(secureId, newSession);
        return newSession;
    }

    private void startSessionCleanupTask() {
        scheduler.scheduleWithFixedDelay(
                this::cleanUpExpiredSessions,
                10,
                30,     // TODO: this value will be changed.
                TimeUnit.SECONDS
        );
    }

    private void cleanUpExpiredSessions() {
        for (Iterator<Map.Entry<String, HttpSession>> it = sessions.entrySet().iterator(); it.hasNext();) {
            try {
                Map.Entry<String, HttpSession> entry = it.next();

                String key = entry.getKey();
                HttpSession session = entry.getValue();
                try (LockGuard ignored = session.withLock()){
                    if (session.isExpired(maxInactiveIntervalSeconds)) {
                        sessions.remove(key);
                    }
                }
            } catch (Exception e) {
                log.warn("error occurred while processing the session", e);
            }
        }
    }
}
