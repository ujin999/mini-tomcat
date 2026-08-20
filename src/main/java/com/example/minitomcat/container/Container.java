package com.example.minitomcat.container;

import com.example.minitomcat.annotation.Component;
import com.example.minitomcat.annotation.Controller;
import com.example.minitomcat.exception.container.ContainerException;
import com.example.minitomcat.servlet.ClassScanner;

import java.lang.reflect.Constructor;
import java.util.*;

public class Container {
    private final Map<Class<?>, ComponentStatus> states;
    private final Map<Class<?>, Object> instances;
    private final Deque<Class<?>> resolutionStack;

    private final ClassScanner scanner;

    public Container(ClassScanner scanner) {
        this.states = new HashMap<>();
        this.instances = new HashMap<>();
        this.resolutionStack = new ArrayDeque<>();

        this.scanner = scanner;
    }

    public <T> T getBean(Class<T> clazz) {
        ComponentStatus state = states.getOrDefault(clazz, null);
        if (state == ComponentStatus.CREATED) {
            return clazz.cast(instances.get(clazz));
        }

        if (state == ComponentStatus.CREATING) {
            throw new ContainerException(
                    "Circular dependency detected for component: " + circularDependencyLog()
            );
        }

        states.put(clazz, ComponentStatus.CREATING);
        resolutionStack.push(clazz);

        Constructor<?> constructor = clazz.getConstructors()[0];
        constructor.setAccessible(true);

        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Object[] args = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            args[i] = getBean(parameterTypes[i]);
        }

        try {
            Object component = constructor.newInstance(args);
            states.put(clazz, ComponentStatus.CREATED);
            resolutionStack.pop();
            instances.put(clazz, component);

            return clazz.cast(component);
        } catch (Exception e) {
            states.put(clazz, ComponentStatus.FAILED);
            resolutionStack.pop();
            throw new ContainerException(
                    "Fail to create component instance: " + clazz.getName(),
                    e
            );
        }
    }

    public void populate(String basePackage) {
        List<Class<?>> classes = scanner.scan(
                basePackage,
                clazz -> clazz.isAnnotationPresent(Component.class) || clazz.isAnnotationPresent(Controller.class));

        for (Class<?> clazz : classes) {
            getBean(clazz);
        }
    }

    public String circularDependencyLog() {
        StringBuilder sb = new StringBuilder();
        resolutionStack.descendingIterator().forEachRemaining(
                clazz -> sb.append(clazz.getName()).append(", ")
        );

        sb.delete(sb.length() - 2, sb.length());

        return "[" + sb.toString() + "]";
    }
}
