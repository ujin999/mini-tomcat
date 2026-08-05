package com.example.minitomcat.exception.http;

import com.example.minitomcat.http.HttpStatus;
import lombok.Getter;

public class HttpException extends RuntimeException {

    @Getter
    private final HttpStatus status;

    public HttpException(HttpStatus status) {
        super();
        this.status = status;
    }

    public HttpException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
