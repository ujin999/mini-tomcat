package com.example.minitomcat.server;

import com.example.minitomcat.exception.HttpParseException;
import com.example.minitomcat.http.HttpMethod;
import com.example.minitomcat.http.HttpParser;
import com.example.minitomcat.http.HttpRequest;
import com.example.minitomcat.http.HttpResponse;
import com.example.minitomcat.routing.Router;
import com.example.minitomcat.servlet.HelloServlet;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

@Slf4j
public class HttpServer {
    private final Router router;
    private final HttpParser parser;
    private final int port;

    public HttpServer(int port) {
        this.port = port;
        this.parser = new HttpParser();
        this.router = new Router();
        router.register(HttpMethod.GET, "/hello", new HelloServlet());
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
                    } catch (HttpParseException e) {
                        log.error("400 Bad Request: Invalid HTTP Format", e);

                        response.setStatusCode(400);
                        response.setReasonPhrase("Bad Request");
                        response.getHeaders().put("Content-Type", "text/plain; charset=utf-8");
                        response.write("400 Bad Request");
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
