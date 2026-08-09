package com.example.minitomcat.exception.http;

import com.example.minitomcat.http.HttpResponse;
import com.example.minitomcat.http.HttpStatus;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class HttpThreadPoolExceptionHandler {
    public void handle(SocketChannel socketChannel) throws IOException {
        HttpResponse response = new HttpResponse();

        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE.getStatusCode());
        response.setReasonPhrase(HttpStatus.SERVICE_UNAVAILABLE.getResponsePhrase());
        response.setContentType("text/plain; charset=utf-8");
        response.setHeader("Retry-After", "120"); // TODO: change this value

        String body = HttpStatus.SERVICE_UNAVAILABLE.getResponsePhrase();

        response.write(body);
        ByteBuffer buffer = ByteBuffer.wrap(response.toBytes());

        socketChannel.write(buffer);
    }
}
