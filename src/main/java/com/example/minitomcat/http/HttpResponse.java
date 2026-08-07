package com.example.minitomcat.http;

import com.example.minitomcat.http.session.HttpSession;
import lombok.AccessLevel;
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

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private final Map<String, String> headers = new HashMap<>();

    private Map<String, String> cookies = new HashMap<>();

    private byte[] body;

    public void write(String body) {
        this.body = body.getBytes(StandardCharsets.UTF_8);
    }

    public void write(byte[] body) {
        this.body = body;
    }

    public int getContentLength() {
        return body == null ? 0 : body.length;
    }

    public void setHeader(String key, String value) {
        headers.put(key, value);
    }

    public void setContentType(String contentType) {
        headers.put("Content-Type", contentType);
    }

    public void setCookie(String name, String value) {
        cookies.put(name, value);
    }

    public byte[] toBytes() {
        StringBuilder sb = new StringBuilder();

        sb.append(protocolVersion).append(" ").append(statusCode).append(" ").append(reasonPhrase).append("\r\n");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
        sb.append("Content-Length: ").append(getContentLength()).append("\r\n");
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            sb.append("Set-Cookie: ").append(entry.getKey()).append("=").append(entry.getValue()).append("\r\n");
        }
        sb.append("\r\n");

        byte[] headersBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] bodyBytes = body == null ? new byte[0] : body;

        byte[] responseBytes = new byte[headersBytes.length + bodyBytes.length];
        System.arraycopy(headersBytes, 0, responseBytes, 0, headersBytes.length);
        System.arraycopy(bodyBytes, 0, responseBytes, headersBytes.length, bodyBytes.length);

        return responseBytes;
    }
}
