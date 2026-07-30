package com.example.minitomcat.http;

import lombok.Getter;

import java.util.Map;

@Getter
public class HttpRequest {

    private final HttpMethod method;

    private final String uri;

    private final String protocolVersion;

    private final Map<String, String> headers;

    private final char[] body;

    private HttpRequest(Builder builder) {
        this.method = builder.method;
        this.uri = builder.uri;
        this.protocolVersion = builder.protocolVersion;
        this.headers = builder.headers;
        this.body = builder.body;
    }

    public static class Builder {
        private HttpMethod method;
        private String uri;
        private String protocolVersion;
        private Map<String, String> headers;
        private char[] body;

        public Builder method(HttpMethod method) {
            this.method = method;
            return this;
        }

        public Builder uri(String uri) {
            this.uri = uri;
            return this;
        }

        public Builder protocolVersion(String protocolVersion) {
            this.protocolVersion = protocolVersion;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder body(char[] body) {
            this.body = body;
            return this;
        }

        public HttpRequest build() {
            return new HttpRequest(this);
        }
    }
}
