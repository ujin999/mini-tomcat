package com.example.minitomcat.controller.bench.session_lock;

import com.example.minitomcat.http.session.HttpSession;
import com.example.minitomcat.http.session.HttpSessionManager;

import java.util.HashMap;
import java.util.Map;

public class PerSessionLockBenchAdapter {
    Map<String, String> sessionIds;
    HttpSessionManager manager;

    public PerSessionLockBenchAdapter() {
        sessionIds = new HashMap<>();
        manager = new HttpSessionManager();
    }

    public void createNewSession(int idx) {
        String sessionId = String.valueOf(idx);
        HttpSession session = manager.createNewSession();
        sessionIds.put(sessionId, session.getId());
    }

    public HttpSession getSession(String idx) {
        String sessionId = sessionIds.get(idx);
        return manager.getSession(sessionId);
    }
}
