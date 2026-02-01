package com.finditnow.auth.swagger;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

public class SwaggerController {

    public static void ui(HttpServerExchange exchange) {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html");
        exchange.getResponseSender().send("""
                <!DOCTYPE html>
                <html>
                <head>
                  <title>FindItNow Auth API</title>
                  <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist/swagger-ui.css" />
                </head>
                <body>
                <div id="swagger-ui"></div>

                <script src="https://unpkg.com/swagger-ui-dist/swagger-ui-bundle.js"></script>
                <script>
                  SwaggerUIBundle({
                    url: "/auth/openapi.json",
                    dom_id: "#swagger-ui"
                  });
                </script>
                </body>
                </html>
                """);
    }
}
