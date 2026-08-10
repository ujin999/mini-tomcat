package com.example.minitomcat.exception.http;

import com.example.minitomcat.http.HttpStatus;

public class RequestTimeoutException extends HttpException {
    public RequestTimeoutException(HttpStatus httpStatus) {
        super(httpStatus);
    }
    public RequestTimeoutException(HttpStatus httpStatus, String message) {
        super(httpStatus, message);
    }

    public RequestTimeoutException(HttpStatus httpStatus, String message, Throwable cause) {
        super(httpStatus, message, cause);
    }
}
