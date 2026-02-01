package com.finditnow.auth.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

public class CorsHandler implements HttpHandler {

    private final HttpHandler next;

    public CorsHandler(HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        exchange.getResponseHeaders().put(
                new HttpString("Access-Control-Allow-Origin"),
                "*"
        );

        exchange.getResponseHeaders().put(
                new HttpString("Access-Control-Allow-Methods"),
                "GET,POST,PUT,DELETE,OPTIONS"
        );

        exchange.getResponseHeaders().put(
                new HttpString("Access-Control-Allow-Headers"),
                "Authorization,Content-Type"
        );

        if (exchange.getRequestMethod().equalToString("OPTIONS")) {
            exchange.setStatusCode(200);
            return;
        }

        next.handleRequest(exchange);
    }
}
