package com.example.minitomcat.http;

import com.example.minitomcat.http.HttpRequest;
import com.example.minitomcat.http.HttpResponse;
import com.example.minitomcat.http.session.HttpSession;
import com.example.minitomcat.http.session.HttpSessionManager;

import java.util.Map;

public class HttpSessionHandler {
    private final HttpSessionManager sessionManager;

    public HttpSessionHandler() {
        this.sessionManager = new HttpSessionManager();
    }

    public void handle(HttpRequest request, HttpResponse response) {
        Map<String, String> cookies = request.getCookies();
        String sessionId = cookies.getOrDefault("JSESSIONID", "");

        HttpSession session = sessionManager.getSession(sessionId);
        if (session == null) {
            session = sessionManager.createNewSession();
            response.setCookie("JSESSIONID", session.getId());
        }

        request.setSession(session);
    }
}
