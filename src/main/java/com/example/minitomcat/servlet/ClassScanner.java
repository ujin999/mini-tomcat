package com.example.minitomcat.servlet;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ClassScanner {

    public List<Class<?>> scan(String basePackage) {

        // package -> path
        String path = basePackage.replace('.', '/');

        // Class Object has ClassLoader ref
        ClassLoader classLoader = ClassScanner.class.getClassLoader();

        // Get URL
        URL url = classLoader.getResource(path);

        // NOTE: NullPointerException check is very important
        if (url == null) {
            throw new IllegalArgumentException(
                    "Package not found : " + basePackage
            );
        }

        // Directory
        File dir;

        try {
            // TODO: if url.toURI() is 'jar:...', File constructor gives IllegalArgumentException
            dir = new File(url.toURI());
        } catch (Exception e){
            log.error("Fail to find URI : ", e);
            throw new RuntimeException(e);
        }

        // .class file
        List<Class<?>> classes = new ArrayList<>();
        File[] files = dir.listFiles();

        // NOTE: NullPointerException check is very important
        if (files == null) {
            return null;
        }

        try {
            for (File f : files) {
                if (!f.getName().endsWith(".class")) {
                    continue;
                }

                // TODO: except inner class as 'User$Inner'
                String className = basePackage + "." + f.getName().replace(".class", "");
                // forName: load Class Object And return Class Object
                classes.add(Class.forName(className));
            }
        } catch (ClassNotFoundException e) {
            log.error("Fail to find Class : ", e);
            throw new RuntimeException(e);
        }

        return classes;
    }
}
