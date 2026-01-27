package com.finditnow.filegateway.utils;

import io.undertow.server.HttpServerExchange;

public class PathBuilder {

    public static String build(String domain, String entityId, String purpose, String uuid, String ext) {
        return domain + "/" + entityId + "/" + purpose + "/" + uuid + "." + ext;
    }

    public static String build(String domain, String entityId, String purpose, String file) {
        return domain + "/" + entityId + "/" + purpose + "/" + file;
    }

    public static String fromExchange(HttpServerExchange exchange) {
        return exchange.getPathParameters().get("domain").getFirst() + "/" +
                exchange.getPathParameters().get("entityId").getFirst() + "/" +
                exchange.getPathParameters().get("purpose").getFirst() + "/" +
                exchange.getPathParameters().get("file").getFirst();
    }
}
