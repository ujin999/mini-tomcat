package com.example.minitomcat.server;

import com.example.minitomcat.exception.HttpParseException;
import com.example.minitomcat.http.HttpParser;
import com.example.minitomcat.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

import java.net.ServerSocket;
import java.net.Socket;

@Slf4j
public class HttpServer {
    HttpParser parser;
    private final int port;

    public HttpServer(int port) {
        this.port = port;
        this.parser = new HttpParser();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)){
            log.info("HTTP Server started on port {}", port);
            while (true) {
                try (Socket socket = serverSocket.accept()) {
                    HttpRequest request = parser.parse(socket.getInputStream());

                    log.info("Method: {}", request.getMethod());
                    log.info("Uri: {}", request.getUri());
                    log.info("ProtocolVersion: {}", request.getProtocolVersion());

                    log.info("Client connected");
                } catch (IOException e) {
                    log.error("Failed to connect with client", e);
                }
            }
        } catch (IOException e) {
            log.error("Failed to start HTTP server on port {}", port, e);
        } catch(HttpParseException e) {

            // 400 Bad Request 응답
        }


    }

}
