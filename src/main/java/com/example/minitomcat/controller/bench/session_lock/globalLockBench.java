package com.example.minitomcat.controller.bench.session_lock;

import com.example.minitomcat.annotation.Controller;
import com.example.minitomcat.annotation.RequestMapping;
import com.example.minitomcat.http.HttpRequest;
import com.example.minitomcat.http.session.LockGuard;

@Controller
public class globalLockBench {
    private final GlobalLockBenchManager manager;

    public globalLockBench() {
        this.manager = new GlobalLockBenchManager();

        for (int i = 0; i < 50; i++) {
            manager.createNewSession(i);
        }
    }
    @RequestMapping(value = "/bench/session/global")
    public String bench(HttpRequest request) {
        String sessionId = request.getHeaders().get("X-Session-Id");

        try (LockGuard g = manager.withLock()) {
            GlobalLockBenchSession session = manager.getSession(sessionId);
            session.computeAttribute("test-value", (key, value) -> {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                if (value == null) {
                    return 1;
                }
                return (Integer)value + 1;
            });
        }

        return "Success";
    }
}
