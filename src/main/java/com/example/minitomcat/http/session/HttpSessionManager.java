package com.example.minitomcat.http.session;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HttpSessionManager {
    private final Map<String, HttpSession> sessions;

    public HttpSessionManager() {
        this.sessions = new ConcurrentHashMap<>();
    }

    public HttpSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public HttpSession createNewSession() {
        String secureId = UUID.randomUUID().toString();
        HttpSession newSession = new HttpSession(secureId);
        sessions.put(secureId, newSession);
        return newSession;
    }
}
