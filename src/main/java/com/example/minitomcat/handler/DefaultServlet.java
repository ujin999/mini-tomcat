package com.example.minitomcat.handler;

import com.example.minitomcat.exception.http.RouteNotFoundException;
import com.example.minitomcat.http.HttpRequest;
import com.example.minitomcat.http.HttpResponse;
import com.example.minitomcat.http.HttpStatus;
import com.example.minitomcat.http.MimeType;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
public class DefaultServlet {
    Path staticRoot;

    public DefaultServlet() {
        this("src/main/resources");
    }

    public DefaultServlet(String uri) {
        staticRoot = Path.of(uri).toAbsolutePath().normalize();
    }

    public void service(HttpRequest request, HttpResponse response) throws IOException {
        String method = request.getMethod().getValue();

        if (!"GET".equals(method) && !"HEAD".equals(method)) {
            throw new RouteNotFoundException(HttpStatus.NOT_FOUND);
        }

        String uri = request.getUri();
        Path requested = staticRoot.resolve(uri.substring(1)).normalize();

        if (!requested.startsWith(staticRoot)) {
            throw new RouteNotFoundException(HttpStatus.NOT_FOUND);
        }

        if (requested.toFile().exists() && !requested.toFile().isDirectory()) {
            response.setStatusCode(200);

            String extension = extractExtension(requested.getFileName().toString());
            response.setContentType(MimeType.fromExtension("." + extension).getMimeType());

            if ("GET".equals(method)) {
                // "Files.readAllBytes" has limitation
                // That could end up consuming all the memory
                // It will need be switched to streaming later on.
                response.write(Files.readAllBytes(requested));
            }
        } else {
            throw new RouteNotFoundException(HttpStatus.NOT_FOUND, "Resource Not Found");
        }
    }

    private static String extractExtension(String fileName) {
        int lastIndexOf = fileName.lastIndexOf('.');

        String extension = "";
        if (lastIndexOf > 0) {
            extension = fileName.substring(lastIndexOf + 1);
        }

        return extension;
    }
}
