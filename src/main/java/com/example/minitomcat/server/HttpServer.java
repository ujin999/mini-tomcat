package com.example.minitomcat.server;

import com.example.minitomcat.exception.http.HttpException;
import com.example.minitomcat.exception.http.HttpExceptionHandler;
import com.example.minitomcat.http.HttpParser;
import com.example.minitomcat.http.HttpRequest;
import com.example.minitomcat.http.HttpResponse;
import com.example.minitomcat.routing.Router;
import com.example.minitomcat.servlet.ServletContainer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

@Slf4j
public class HttpServer {
    private final ServletContainer servletContainer;
    private final Router router;
    private final HttpParser parser;
    private final int port;
    private final HttpExceptionHandler httpExceptionHandler;

    public HttpServer(int port) {
        this.port = port;
        this.parser = new HttpParser();
        this.servletContainer = new ServletContainer();
        servletContainer.initialize();
        this.router = servletContainer.getRouter();
        this.httpExceptionHandler = new HttpExceptionHandler();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)){
            log.info("HTTP Server started on port {}", port);
            while (true) {
                try (Socket socket = serverSocket.accept();
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream()) {
                    HttpRequest request;
                    HttpResponse response = new HttpResponse();

                    try {
                        request = parser.parse(in);
                        log.info("Client connected to: {}", request.getUri());

                        router.route(request, response);
                    } catch (HttpException e) {
                        httpExceptionHandler.handle(e, response);
                    } catch (Exception e) {
                        httpExceptionHandler.handleUnexpected(e, response);
                    }
                    out.write(response.toBytes());
                    out.flush();
                } catch (IOException e) {
                    log.error("Failed to process client socket connection", e);
                }
            }
        } catch (IOException e) {
            log.error("Fatal: Failed to start HTTP server on port {}", port, e);
        }
    }
}
