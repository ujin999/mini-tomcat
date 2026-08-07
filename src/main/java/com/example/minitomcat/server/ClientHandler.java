package com.example.minitomcat.server;

import com.example.minitomcat.exception.http.HttpException;
import com.example.minitomcat.exception.http.HttpExceptionHandler;
import com.example.minitomcat.exception.http.RouteNotFoundException;
import com.example.minitomcat.filter.Filter;
import com.example.minitomcat.filter.FilterChain;
import com.example.minitomcat.handler.DefaultServlet;
import com.example.minitomcat.http.HttpParser;
import com.example.minitomcat.http.HttpRequest;
import com.example.minitomcat.http.HttpResponse;
import com.example.minitomcat.http.HttpSessionHandler;
import com.example.minitomcat.routing.Router;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

@Slf4j
public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final HttpParser parser;
    private final FilterChain filterChain;
    private final HttpExceptionHandler httpExceptionHandler;

    public ClientHandler(Socket socket, HttpParser parser, HttpExceptionHandler httpExceptionHandler,
                         List<Filter> filters) {
        this.clientSocket = socket;
        this.parser = parser;
        this.filterChain = new FilterChain(filters);
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

                filterChain.doFilter(request, response);
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
