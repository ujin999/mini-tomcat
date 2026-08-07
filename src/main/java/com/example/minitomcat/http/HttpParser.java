package com.example.minitomcat.http;

import com.example.minitomcat.exception.http.HttpException;
import com.example.minitomcat.exception.http.HttpParseException;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

@Slf4j
public class HttpParser {
    public HttpRequest parse(InputStream inputStream) throws HttpParseException, IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

        try {
            String requestLine = br.readLine();
            String[] tokens = requestLine.split(" ");

            String method, uri, protocolVersion;
            if (tokens.length == 3) {
                method = tokens[0];
                uri = tokens[1];
                protocolVersion = tokens[2];
            } else {
                throw new HttpParseException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid Request Line : " + requestLine
                );
            }

            Map<String, String> headers = new HashMap<>();
            Map<String, String> cookies = new HashMap<>();
            while ((requestLine = br.readLine()) != null && !requestLine.isEmpty()) {
                tokens = requestLine.split(":", 2);

                if (tokens[0].equals("Cookie")) {
                    parseCookie(requestLine, cookies);
                    continue;
                }

                headers.put(tokens[0], tokens[1].trim());
            }

            int contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));

            char[] body = new char[contentLength];
            if (contentLength > 0) {
                br.read(body, 0, contentLength);
            }

            return new HttpRequest.Builder()
                    .method(HttpMethod.fromString(method))
                    .uri(uri)
                    .protocolVersion(protocolVersion)
                    .headers(headers)
                    .cookies(cookies)
                    .body(body)
                    .build();
        } catch (IOException e) {
            throw new IOException("Failed to parse HTTP request due to I/O error", e);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new HttpParseException(HttpStatus.BAD_REQUEST, "Invalid HTTP method or protocol attribute", e);
        }
    }

    public void parseCookie(String cookieHeader, Map<String, String> cookies) {
        String cookiesString = cookieHeader.split(":", 2)[1];
        String[] pairs = cookiesString.split(";");

        for (String pair : pairs) {
            pair = pair.trim();

            String[] keyValue = pair.split("=", 2);

            cookies.put(keyValue[0], keyValue[1]);
        }
    }
}
