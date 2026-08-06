package com.example.minitomcat.http;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public enum MimeType {

    // Text document
    TEXT_HTML(".html", "text/html"),
    TEXT_CSS(".css", "text/css"),
    TEXT_PLAIN(".txt", "text/plain"),
    TEXT_JAVASCRIPT(".js", "text/javascript"),

    // Image
    IMAGE_JPEG(".jpeg", "image/jpeg"),
    IMAGE_JPG(".jpg", "image/jpg"),
    IMAGE_PNG(".png", "image/png"),
    IMAGE_SVG(".svg", "image/svg"),

    // Video
    VIDEO_MP4(".mp4", "video/mp4"),

    // Application
    APPLICATION_JSON(".json", "application/json"),
    APPLICATION_XML(".xml", "application/xml"),
    APPLICATION_PDF(".pdf", "application/pdf"),

    APPLICATION_OCTET_STREAM(null, "application/octet-stream");

    private final String extension;

    @Getter
    private final String mimeType;

    private static final Map<String, MimeType> MIME_TYPES = new HashMap<>();

    static {
        for (MimeType m : MimeType.values()) {
            if (m.extension != null) {
                MIME_TYPES.put(m.extension, m);
            }
        }
    }

    MimeType (String extension, String mimeType) {
        this.extension = extension;
        this.mimeType = mimeType;
    }

    public static MimeType fromExtension(String extension) {

        return MIME_TYPES.getOrDefault(extension, MimeType.APPLICATION_OCTET_STREAM);
    }
}
