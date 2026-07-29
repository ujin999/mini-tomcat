package com.example.minitomcat.exception;

public class HttpParseException extends RuntimeException {

    public HttpParseException(String message) {
        super(message);
    }

    public HttpParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
