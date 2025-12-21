package com.finditnow.filegateway.handlers;

import com.finditnow.filegateway.storage.FileStorage;
import com.finditnow.filegateway.utils.PathBuilder;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.MultiPartParserDefinition;

import java.io.InputStream;
import java.util.UUID;

public record UploadHandler(FileStorage storage, MultiPartParserDefinition parserDefinition) implements HttpHandler {


    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        FormData formData =
                exchange.getAttachment(FormDataParser.FORM_DATA);

        if (formData == null) {
            exchange.setStatusCode(400);
            return;
        }


        FormData.FormValue file = formData.getFirst("file");
        String domain = formData.getFirst("domain").getValue();
        String entityId = formData.getFirst("entityId").getValue();
        String purpose = formData.getFirst("purpose").getValue();

        if (file == null || !file.isFileItem()) {
            exchange.setStatusCode(400);
            return;
        }

        exchange.startBlocking();

        String fileName = file.getFileName();

        String extension = fileName.substring(fileName.lastIndexOf('.') + 1);

        String uuid = UUID.randomUUID().toString();
        String fileKey = PathBuilder.build(domain, entityId, purpose, uuid, extension);

        try (InputStream in = file.getFileItem().getInputStream()) {
            storage.save(in, fileKey);
        }

        exchange.getResponseSender()
                .send("{\"fileKey\":\"/" + fileKey + "\"}");
    }
}
