package com.example.minitomcat.http;

import com.example.minitomcat.exception.HttpParseException;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class HttpParser {
    public HttpRequest parse(InputStream inputStream) throws HttpParseException {
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

        try {
            String requestLine = new String(br.readLine());
            Map<String, String> elements = new HashMap<>();

            String[] tokens = requestLine.split(" ");

            if (tokens.length == 3) {
                elements.put("Method", tokens[0]);
                elements.put("Uri", tokens[1]);
                elements.put("ProtocolVersion", tokens[2]);
            } else {
                throw new HttpParseException(
                        "Invalid Request Line : " + requestLine
                );
            }

            Map<String, String> headers = new HashMap<>();
            while ((requestLine = br.readLine()) != null && !requestLine.isEmpty()) {
                tokens = requestLine.split(":", 2);
                headers.put(tokens[0], tokens[1].trim());
            }

            int contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));

            if (contentLength > 0) {
                char[] body = new char[contentLength];
                br.read(body, 0, contentLength);
                elements.put("Body", new String(body));
            }

            return new HttpRequest.Builder()
                    .method(HttpMethod.fromString(elements.get("Method")))
                    .uri(elements.get("Uri"))
                    .protocolVersion(elements.get("ProtocolVersion"))
                    .headers(headers)
                    .body(elements.get("Body"))
                    .build();
        } catch (IOException e) {
            throw new HttpParseException("Failed to parse HTTP request due to I/O error", e);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new HttpParseException("Invalid HTTP method or protocol attribute", e);
        }
    }
}
