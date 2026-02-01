package com.finditnow.auth.swagger;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

public class OpenApiController {

    public static void json(HttpServerExchange exchange) {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        exchange.getResponseSender().send(OpenApiSpec.JSON);
    }
}
