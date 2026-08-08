package com.example.minitomcat.servlet;

import com.example.minitomcat.annotation.WebServlet;
import com.example.minitomcat.http.HttpMethod;
import com.example.minitomcat.routing.Router;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ServletContainer {
    private final Router router = new Router();

    public void initialize() {
        ClassScanner scanner = new ClassScanner();
        List<Class<?>> servletClasses = scanner.scan("com.example.minitomcat.servlet");

        for (Class<?> clazz : servletClasses) {
            if (clazz.isAnnotationPresent(WebServlet.class)) {

                try {
                    // Read Annotation
                    WebServlet annotation = clazz.getAnnotation(WebServlet.class);
                    String uri = annotation.value();
                    HttpMethod method = annotation.method();

                    // Create Reflection
                    if (!HttpServlet.class.isAssignableFrom(clazz)) {
                        continue;
                    }

                    Object instance = clazz.getDeclaredConstructor().newInstance();
                    HttpServlet servlet = (HttpServlet) instance;

                    // init()
                    servlet.init();

                    // register()
                    register(method, uri, servlet);

                } catch (Exception e) {
                    log.error("Fail to create servlet instance", e);
                    throw new RuntimeException("Fatal error: Server failure", e);
                }
            }
        }
    }

    private void register(
            HttpMethod method,
            String uri,
            HttpServlet servlet
    ) {
        router.register(method, uri, servlet);
    }

    public Router getRouter() {
        return router;
    }
}
