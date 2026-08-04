package com.example.minitomcat.servlet;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Slf4j
public class ClassScanner {

    public List<Class<?>> scan(String basePackage) {

        // basePackage ex. com.example.minitomcat.servlet

        List<Class<?>> classes = new ArrayList<>();

        // package -> path
        String path = basePackage.replace('.', '/');

        // Class Object has ClassLoader ref
        ClassLoader classLoader = ClassScanner.class.getClassLoader();


        try {
            // get URL
            //
            /*
            * NOTE: Since multi module or library(JAR File) have same path, it needs to check all of path
            *
            * For example
            * core module: com.example.minitomcat.servlet
            * web module: com.example.minitomcat.servlet
            * */
            Enumeration<URL> resources = classLoader.getResources(path);

            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                String protocol = url.getProtocol();

                if ("file".equals(protocol)) {
                    File dir = new File(url.toURI());
                    findClassesInDirectory(dir, basePackage, classes);
                } else if ("jar".equals(protocol)) {
                    findClassesInJar(url, path, classes);
                }
            }
        } catch (Exception e) {
            log.error("Error is occurred during auto scan: ", e);
            throw new RuntimeException(e);
        }

        return classes;
    }

    private void findClassesInDirectory(File directory, String basePackage, List<Class<?>> classes) throws ClassNotFoundException {
        if (!directory.exists()) return;

        File[] files = directory.listFiles();

        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                findClassesInDirectory(file, basePackage + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
                String className = basePackage + "." + file.getName().substring(0, file.getName().length() - 6);

                Class<?> clazz = Class.forName(className);

                // NOTE: Interface and Abstract class is not added in classes.
                if (Modifier.isAbstract(clazz.getModifiers())) {
                    continue;
                }

                classes.add(clazz);
            }
        }
    }

    private void findClassesInJar(URL url, String path, List<Class<?>> classes) throws IOException, ClassNotFoundException {
        JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
        try (JarFile jarFile = jarURLConnection.getJarFile()) {
            Enumeration<JarEntry> entries = jarFile.entries();

            // TODO: Scanning all elements is not effective. It needs refactoring.
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                // ex: com/example/servlet/HelloServlet.class
                String entryName = entry.getName();

                if (entryName.startsWith(path) && entryName.endsWith(".class") && !entryName.contains("$")) {
                    String className = entryName.replace('/', '.').substring(0, entryName.length() - 6);
                    classes.add(Class.forName(className));
                }
            }
        }
    }
}
