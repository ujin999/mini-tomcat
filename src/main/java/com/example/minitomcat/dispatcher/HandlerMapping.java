package com.example.minitomcat.dispatcher;

import com.example.minitomcat.annotation.Controller;
import com.example.minitomcat.annotation.RequestMapping;
import com.example.minitomcat.exception.RouteException;
import com.example.minitomcat.http.HttpMethod;
import com.example.minitomcat.http.HttpRequest;
import com.example.minitomcat.servlet.ClassScanner;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class HandlerMapping {

    private final Map<String, HandlerMethod> handlers = new HashMap<>();

    public void initialize() {
        ClassScanner scanner = new ClassScanner();
        List<Class<?>> servletClasses = scanner.scan("com.example.minitomcat.controller");

        for (Class<?> clazz : servletClasses) {
            if (clazz.isAnnotationPresent(Controller.class)) {
                try {
                    // Read Annotation
                    Method[] methods = clazz.getDeclaredMethods();

                    for (Method method : methods) {
                        if (method.isAnnotationPresent(RequestMapping.class)) {
                            RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);

                            if (requestMapping != null) {
                                HttpMethod httpMethod = requestMapping.method();
                                String uri = requestMapping.value();
                                Object controller = clazz.getDeclaredConstructor().newInstance();
                                register(httpMethod, uri, controller, method);
                            }
                        }
                    }

                } catch (Exception e) {
                    log.error("Fail to create servlet instance", e);
                    throw new RuntimeException("Fatal error: Server failure", e);
                }
            }
        }
    }

    private void register(HttpMethod method, String uri, Object controller, Method handlerMethod) {
        String key = method.getValue() + ":" + uri;

        if (handlers.containsKey(key)) {
            throw new RouteException("This routing address is already registered: " + key);
        }

        HandlerMethod handler = new HandlerMethod(controller, handlerMethod);

        handlers.put(key, handler);
    }

    public HandlerMethod getHandler(HttpRequest request) {
        String key = request.getMethod() + ":" + request.getUri();

        // returns null if the handler is not found.
        return handlers.get(key);
    }
}
