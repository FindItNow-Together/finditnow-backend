package com.finditnow.filegateway.handlers;

import com.finditnow.filegateway.auth.RefreshTokenValidator;
import com.finditnow.filegateway.storage.FileStorage;
import com.finditnow.filegateway.utils.PathBuilder;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public record DownloadHandler(FileStorage storage, RefreshTokenValidator sessionValidator) implements HttpHandler {

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        exchange.startBlocking();

        String path = exchange.getRelativePath();
        String[] segments = path.split("/");

        // ["", "download", "{domain}", "{entityId}", "{purpose}", "{file}"]
        if (segments.length < 6) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("Invalid path: " + path);
            return;
        }

        String domain = segments[2];
        String entityId = segments[3];
        String purpose = segments[4];
        String file = segments[5];

        if (isPrivate(domain)) {
            sessionValidator.validate(exchange);
        }

        String fileKey = PathBuilder.build(domain, entityId, purpose, file);

        if (!storage.exists(fileKey)) {
            exchange.setStatusCode(404);
            return;
        }

        exchange.getResponseHeaders()
                .put(Headers.CONTENT_TYPE, guessMime(fileKey));

        try (InputStream in = storage.read(fileKey)) {
            in.transferTo(exchange.getOutputStream());
        }

    }

    private boolean isPrivate(String domain) {
        return "receipt".equals(domain);
    }

    private String guessMime(String fileKey) {
        try {
            return Files.probeContentType(Path.of(fileKey));
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }
}
