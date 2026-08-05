package com.example.minitomcat.exception.http;

import com.example.minitomcat.http.HttpResponse;
import com.example.minitomcat.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpExceptionHandler {

    public void handle(HttpException e, HttpResponse response) {
        HttpStatus status = e.getStatus();

        logError(status, e);

        response.setStatusCode(status.getStatusCode());
        response.setReasonPhrase(status.getResponsePhrase());
        response.setContentType("text/plain; charset=utf-8");

        String body = status.getStatusCode() + " ";
        if (e.getMessage() == null) {
            body += status.getResponsePhrase();
        } else {
            body += e.getMessage();
        }

        response.write(body);
    }

    public void handleUnexpected(Exception e, HttpResponse response) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        logError(status, e);

        response.setStatusCode(status.getStatusCode());
        response.setReasonPhrase(status.getResponsePhrase());
        response.setContentType("text/plain; charset=utf-8");
        response.write(status.getStatusCode() + " " + status.getResponsePhrase());
    }

    public void logError(HttpStatus status, Exception e) {
        if (e.getMessage() == null) {
            log.error("[HTTP_ERROR] status={}, phrase={}",
                    status.getStatusCode(),
                    status.getResponsePhrase(),
                    e);
        } else {
            log.error("[HTTP_ERROR] status={}, phrase={}, message=\"{}\"",
                    status.getStatusCode(),
                    status.getResponsePhrase(),
                    e.getMessage(),
                    e);
        }
    }
}
