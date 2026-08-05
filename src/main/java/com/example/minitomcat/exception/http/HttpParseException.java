package com.example.minitomcat.exception.http;

import com.example.minitomcat.exception.http.HttpException;
import com.example.minitomcat.http.HttpStatus;

public class HttpParseException extends HttpException {

    public HttpParseException(HttpStatus httpStatus, String message) {
        super(httpStatus, message);
    }

    public HttpParseException(HttpStatus httpStatus, String message, Throwable cause) {
        super(httpStatus, message, cause);
    }
}
