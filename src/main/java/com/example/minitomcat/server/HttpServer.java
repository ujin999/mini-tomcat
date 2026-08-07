package com.example.minitomcat.server;

import com.example.minitomcat.exception.http.HttpExceptionHandler;
import com.example.minitomcat.exception.http.HttpThreadPoolExceptionHandler;
import com.example.minitomcat.handler.DefaultServlet;
import com.example.minitomcat.http.HttpParser;
import com.example.minitomcat.http.HttpSessionHandler;
import com.example.minitomcat.routing.Router;
import com.example.minitomcat.servlet.ServletContainer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

@Slf4j
public class HttpServer {
    private final ServletContainer servletContainer;
    private final Router router;
    private final HttpParser parser;
    private final int port;
    private final HttpExceptionHandler httpExceptionHandler;
    private final HttpSessionHandler httpSessionHandler;
    private final DefaultServlet defaultServlet;
    private final ExecutorService threadPool;

    public HttpServer(int port) {
        this.port = port;
        this.parser = new HttpParser();
        this.servletContainer = new ServletContainer();
        servletContainer.initialize();
        this.router = servletContainer.getRouter();
        this.httpExceptionHandler = new HttpExceptionHandler();
        this.httpSessionHandler = new HttpSessionHandler();
        this.defaultServlet = new DefaultServlet();
        this.threadPool = new ThreadPoolExecutor(
                10, 200, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100)
        );
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)){
            log.info("HTTP Server started on port {}", port);
            while (true) {
                Socket clientSocket = null;
                try {
                    clientSocket = serverSocket.accept();
                    log.info("New client connected: {}", clientSocket.getRemoteSocketAddress());

                    threadPool.execute(new ClientHandler(clientSocket, parser, router, defaultServlet, httpExceptionHandler, httpSessionHandler));
                } catch (RejectedExecutionException e) {
                    log.warn("Failed to accept socket because thread pool is full");

                    try {
                        new HttpThreadPoolExceptionHandler().handle(clientSocket);
                        clientSocket.close();
                    } catch (IOException ie) {
                        log.error("Failed to process client socket connection", ie);
                    }
                } catch (IOException e) {
                    log.error("Failed to process client socket connection", e);
                }
            }
        } catch (IOException e) {
            log.error("Fatal: Failed to start HTTP server on port {}", port, e);
        }
    }
}
