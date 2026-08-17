package com.example.minitomcat.controller.bench.session_lock;

import com.example.minitomcat.annotation.Controller;
import com.example.minitomcat.annotation.RequestMapping;
import com.example.minitomcat.http.HttpRequest;
import com.example.minitomcat.http.session.HttpSession;

@Controller
public class PerSessionLockBench {
    private final PerSessionLockBenchAdapter adapter;
    public PerSessionLockBench() {
        adapter = new PerSessionLockBenchAdapter();

        for (int i = 0; i < 50; i++) {
            adapter.createNewSession(i);
        }
    }
    @RequestMapping("/bench/session/per-session")
    public String bench(HttpRequest request) {
        String sessionId = request.getHeaders().get("X-Session-Id");
        HttpSession session = adapter.getSession(sessionId);

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

        return "Success";
    }
}
