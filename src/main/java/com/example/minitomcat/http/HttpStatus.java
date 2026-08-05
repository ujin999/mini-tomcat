package com.example.minitomcat.http;

import lombok.Getter;

public enum HttpStatus {
    // 2xx - Success
    OK(200, "OK"),
    CREATED(201, "Created"),
    ACCEPTED(202, "Accepted"),

    // 3xx - Redirection
    MULTIPLE_CHOICES(300, "Multiple Choices"),
    MOVED_PERMANENTLY(301, "Moved Permanently"),
    FOUND(302, "Found"),


    // 4xx - Client Error
    BAD_REQUEST(400, "Bad Request"),
    // UNAUTHORIZED(401, "Unauthorized"),
    // PAYMENT_REQUIRED(402, "Payment Required"),
    // FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    NOT_ACCEPTABLE(406, "Not Acceptable"),
    REQUEST_TIMEOUT(408, "Request Timeout"),

    // 5xx
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    NOT_IMPLEMENTED(501, "Not Implemented"),
    BAD_GATEWAY(502, "Bad Gateway"),
    SERVICE_UNAVAILABLE(503, "Service Unavailable");

    @Getter
    private final int statusCode;

    @Getter
    private final String responsePhrase;

    HttpStatus(int statusCode, String responsePhrase) {
        this.statusCode = statusCode;
        this.responsePhrase = responsePhrase;
    }

}
