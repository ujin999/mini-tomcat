package com.example.minitomcat.servlet;

import com.example.minitomcat.annotation.WebServlet;
import com.example.minitomcat.http.HttpMethod;
import com.example.minitomcat.routing.Router;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServletContainer {
    private final Router router = new Router();

    public ServletContainer() {
    }

    public void initialize() {
        register(HttpMethod.GET, "/hello", new HelloServlet());
    }

    private void register(
            HttpMethod method,
            String uri,
            HttpServlet servlet
    ) {
        servlet.init();

        Class<?> clazz = HelloServlet.class;

        if (clazz.isAnnotationPresent(WebServlet.class)) {

            WebServlet annotation = clazz.getAnnotation(WebServlet.class);

            uri = annotation.value();

            log.info("Servlet container found uri: {}", uri);
        }

        router.register(method, uri, servlet);
    }

    public Router getRouter() {
        return router;
    }
}
