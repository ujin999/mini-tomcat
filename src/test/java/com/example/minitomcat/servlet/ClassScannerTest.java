package com.example.minitomcat.servlet;

import com.example.minitomcat.annotation.Controller;
import com.example.minitomcat.annotation.WebServlet;
import com.example.minitomcat.servlet.fixture.Included;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class ClassScannerTest {
    private ClassScanner scanner;

    @BeforeEach
    void setUP() {
        scanner = new ClassScanner();
    }

    @Test
    void scan_skipsNestedClass_whenDollarSignExists() {
        // given
        String packageName = "com.example.minitomcat.servlet";

        // when
        List<Class<?>> classes = scanner.scan(packageName,
                clazz -> true);

        // then
        assertAll(
                classes.stream().map(clazz ->
                        () -> assertFalse(clazz.getName().contains("$"),
                                "Failed class name: " + clazz.getName()))
        );
    }

    @Test
    void scan_includesNormalClass_whenNoDollarSign() {
        // given
        String packageName = "com.example.minitomcat.servlet";

        // when
        List<Class<?>> classes = scanner.scan(packageName,
                clazz -> true);

        // then
        assertAll(
                classes.stream().map(clazz ->
                        () -> assertFalse(clazz.getName().contains("$"),
                                "Failed class name: " + clazz.getName()))
        );

        assertFalse(classes.isEmpty());
    }

    @Test
    void scan_scansRecursively_whenSubPackagesExist() {
        // given
        String packageName = "com.example.minitomcat.servlet";

        // when
        List<Class<?>> classes = scanner.scan(packageName,
                clazz -> true);

        // then
        boolean hasSubClass = classes.stream()
                .anyMatch(clazz -> clazz.getSimpleName().contains("SubPackageClass"));
        assertTrue(hasSubClass);
    }

    @Test
    void scan_excludesClass_whenFilterMismatch() {
        // given
        String packageName = "com.example.minitomcat.servlet";
        Predicate<Class<?>> filter =
                clazz -> clazz.isAnnotationPresent(Controller.class) ||
                        clazz.isAnnotationPresent(WebServlet.class);

        // when
        List<Class<?>> classes = scanner.scan(packageName, filter);

        // then
        assertAll(
                classes.stream().map(clazz ->
                        () -> assertFalse(clazz.isAnnotationPresent(Included.class)))
        );

        assertAll(
                classes.stream().map(clazz ->
                        () -> assertTrue(clazz.isAnnotationPresent(Controller.class) ||
                                clazz.isAnnotationPresent(WebServlet.class)))
        );
    }

    @Test
    void scan_returnsEmptyList_whenPackageDoesNotExist() {
        // given
        String packageName = "com.example.minitomcat.servlet.empty";

        // when
        List<Class<?>> classes = scanner.scan(packageName,
                clazz -> true);

        // then
        assertTrue(classes.isEmpty());
    }

    @Test
    void scan_populatesCache_whenExecuted() {
        // given
        String packageName = "com.example.minitomcat.servlet";

        // when
        scanner.scan(packageName, clazz -> true);

        // then
        assertTrue(scanner.classesByPackage.containsKey(packageName));
    }

    @Test
    void scan_returnsCorrectResult_whenSameBasePackageScannedWithDifferentFilters() {
        // given
        String packageName = "com.example.minitomcat.servlet";
        Predicate<Class<?>> filterA = clazz -> clazz.isAnnotationPresent(Controller.class);
        Predicate<Class<?>> filterB = clazz -> clazz.isAnnotationPresent(WebServlet.class);

        // when
        List<Class<?>> classesA = scanner.scan(packageName, filterA);
        List<Class<?>> classesB = scanner.scan(packageName, filterB);

        // then
        boolean hasAllControllers = !classesA.isEmpty() &&
                classesA.stream()
                .allMatch(clazz -> clazz.isAnnotationPresent(Controller.class));
        boolean hasAllWebServlets = !classesB.isEmpty() &&
                classesB.stream()
                .allMatch(clazz -> clazz.isAnnotationPresent(WebServlet.class));
        assertTrue(hasAllControllers);
        assertTrue(hasAllWebServlets);

    }
}