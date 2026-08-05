package com.example.minitomcat.exception.http;

import com.example.minitomcat.http.HttpStatus;

public class RouteNotFoundException extends HttpException {

    public RouteNotFoundException(HttpStatus httpStatus, String message) {
        super(httpStatus, message);
    }

    public RouteNotFoundException(HttpStatus httpStatus, String message, Throwable cause) {
        super(httpStatus, message, cause);
    }
}
