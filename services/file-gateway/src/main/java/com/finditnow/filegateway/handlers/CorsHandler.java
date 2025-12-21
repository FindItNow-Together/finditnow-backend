package com.finditnow.filegateway.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

public class CorsHandler implements HttpHandler {
    private static final HttpString ALLOW_ORIGIN = new HttpString("Access-Control-Allow-Origin");
    private static final HttpString ALLOW_METHODS = new HttpString("Access-Control-Allow-Methods");
    private static final HttpString ALLOW_HEADERS = new HttpString("Access-Control-Allow-Headers");
    private static final HttpString ALLOW_CREDENTIALS = new HttpString("Access-Control-Allow-Credentials");
    private final HttpHandler next;

    public CorsHandler(HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.getResponseHeaders().put(ALLOW_ORIGIN, "http://localhost:3000");
        exchange.getResponseHeaders().put(ALLOW_CREDENTIALS, "true");

        exchange.getResponseHeaders().put(ALLOW_METHODS, "GET, POST, OPTIONS");
        exchange.getResponseHeaders().put(ALLOW_HEADERS, "Content-Type, Authorization");

        if (exchange.getRequestMethod().equalToString("OPTIONS")) {
            exchange.setStatusCode(204);
            return;
        }

        next.handleRequest(exchange);
    }
}
