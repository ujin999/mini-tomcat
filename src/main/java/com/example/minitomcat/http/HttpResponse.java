package com.example.minitomcat.http;

import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
public class HttpResponse {

    private String protocolVersion = "HTTP/1.1";

    private int statusCode;

    private String reasonPhrase;

    private final Map<String, String> headers = new HashMap<>();

    private String body;

    public void write(String body) {
        this.body = body;
    }

    public byte[] toBytes() {
        StringBuilder sb = new StringBuilder();

        sb.append(protocolVersion).append(" ").append(statusCode).append(" ").append(reasonPhrase).append("\r\n");

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue());
        }
        sb.append("\r\n\r\n");

        byte[] headersBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        byte[] responseBytes = new byte[headersBytes.length + bodyBytes.length];
        System.arraycopy(headersBytes, 0, responseBytes, 0, headersBytes.length);
        System.arraycopy(bodyBytes, 0, responseBytes, headersBytes.length, bodyBytes.length);

        return responseBytes;
    }
}
