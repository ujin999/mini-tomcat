package com.example.minitomcat.exception;

public class DispatcherException extends RuntimeException {
    public DispatcherException(String message) {
        super(message);
    }

    public DispatcherException(String message, Throwable cause) {
        super(message, cause);
    }
}
