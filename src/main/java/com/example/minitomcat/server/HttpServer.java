package com.example.minitomcat.server;

import com.example.minitomcat.dispatcher.HandlerAdapter;
import com.example.minitomcat.dispatcher.HandlerMapping;
import com.example.minitomcat.exception.http.HttpExceptionHandler;
import com.example.minitomcat.exception.http.HttpThreadPoolExceptionHandler;
import com.example.minitomcat.filter.*;
import com.example.minitomcat.handler.DefaultServlet;
import com.example.minitomcat.http.HttpParser;
import com.example.minitomcat.http.HttpSessionHandler;
import com.example.minitomcat.routing.Router;
import com.example.minitomcat.servlet.ServletContainer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
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
    private final List<Filter> filters;
    private final ExecutorService threadPool;

    private final HandlerMapping handlerMapping;
    private final HandlerAdapter handlerAdapter;

    public HttpServer(int port) {
        this.port = port;
        this.parser = new HttpParser();
        this.servletContainer = new ServletContainer();
        servletContainer.initialize();
        this.router = servletContainer.getRouter();
        this.httpExceptionHandler = new HttpExceptionHandler();
        this.httpSessionHandler = new HttpSessionHandler();
        this.defaultServlet = new DefaultServlet();
        this.filters = new ArrayList<>();
        this.threadPool = new ThreadPoolExecutor(
                10, 200, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100)
        );

        handlerMapping = new HandlerMapping();
        handlerAdapter = new HandlerAdapter();
    }

    public void initialize() {
        handlerMapping.initialize();

        filters.add(new SessionFilter(httpSessionHandler));
        filters.add(new DispatcherFilter(handlerMapping, handlerAdapter));
        filters.add(new RouterFilter(router));
        filters.add(new DefaultServletFilter(defaultServlet, httpExceptionHandler));

        logFilterRegistration();
    }

    public void start() {
        try (ServerSocketChannel serverChannel = ServerSocketChannel.open()){
            serverChannel.bind(new InetSocketAddress(port));
            log.info("HTTP Server started on port {}", port);

            while (true) {
                SocketChannel clientChannel = null;
                try {
                    clientChannel = serverChannel.accept();

                    Socket clientSocket = clientChannel.socket();
                    log.info("New client connected: {}", clientSocket.getRemoteSocketAddress());

                    threadPool.execute(new ClientHandler(clientSocket, parser, httpExceptionHandler, filters));
                } catch (RejectedExecutionException e) {
                    log.warn("Failed to accept socket because thread pool is full");

                    try {
                        clientChannel.configureBlocking(false);
                        new HttpThreadPoolExceptionHandler().handle(clientChannel);
                        clientChannel.close();
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

    public void logFilterRegistration() {
        log.info("Filter Chain Injected Successfully");

        if (filters == null || filters.isEmpty()) {
            log.warn("No filters have been registered in the chain.");
        } else {
            for (int i = 0; i < filters.size(); i++) {
                String filterName = filters.get(i).getClass().getSimpleName();
                log.info(" Order[{}] : {}", i + 1, filterName);
            }
        }
    }
}
