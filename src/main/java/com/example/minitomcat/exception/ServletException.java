package com.example.minitomcat.exception;

public class ServletException extends RuntimeException {

    public ServletException(String message) {
        super(message);
    }

    public ServletException(String message, Throwable cause) {
        super(message, cause);
    }
}
