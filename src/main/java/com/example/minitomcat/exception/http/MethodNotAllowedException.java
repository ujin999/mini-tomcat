package com.example.minitomcat.exception.http;

import com.example.minitomcat.http.HttpStatus;

public class MethodNotAllowedException extends HttpException {

    public MethodNotAllowedException(HttpStatus status) {
        super(status);
    }

    public MethodNotAllowedException(HttpStatus status, String message) {
        super(status, message);
    }

    public MethodNotAllowedException(HttpStatus status, String message, Throwable e) {
        super(status, message, e);
    }
}
