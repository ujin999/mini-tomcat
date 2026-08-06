package com.example.minitomcat.server;

import com.example.minitomcat.exception.http.HttpException;
import com.example.minitomcat.exception.http.HttpExceptionHandler;
import com.example.minitomcat.exception.http.RouteNotFoundException;
import com.example.minitomcat.handler.DefaultServlet;
import com.example.minitomcat.http.HttpParser;
import com.example.minitomcat.http.HttpRequest;
import com.example.minitomcat.http.HttpResponse;
import com.example.minitomcat.routing.Router;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

@Slf4j
public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final HttpParser parser;
    private final Router router;
    private final DefaultServlet defaultServlet;
    private final HttpExceptionHandler httpExceptionHandler;

    public ClientHandler(Socket socket, HttpParser parser, Router router,
                         DefaultServlet defaultServlet, HttpExceptionHandler httpExceptionHandler) {
        this.clientSocket = socket;
        this.parser = parser;
        this.router = router;
        this.defaultServlet = defaultServlet;
        this.httpExceptionHandler = httpExceptionHandler;
    }

    @Override
    public void run() {

        try (
            Socket socket = this.clientSocket;
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
        ) {
            HttpRequest request;
            HttpResponse response = new HttpResponse();

            try {
                request = parser.parse(in);
                log.info("Client connected to: {}", request.getUri());

                try {
                    router.route(request, response);
                } catch (RouteNotFoundException e) {
                    defaultServlet.service(request, response);
                }
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
}
