package com.finditnow.auth.server;

import com.finditnow.auth.handlers.CorsHandler;
import com.finditnow.auth.handlers.PathHandler;
import com.finditnow.auth.handlers.RouteHandler;
import com.finditnow.auth.service.AuthService;
import com.finditnow.auth.service.OAuthService;
import com.finditnow.auth.utils.Logger;
import com.finditnow.config.Config;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.util.Headers;

public class HTTPServer {
    private static final Logger logger = Logger.getLogger(HTTPServer.class);
    private final int httpPort = Integer.parseInt(Config.get("HTTPPort", "8080"));
    //    private final AuthHandler authHandler;
    private final PathHandler pathHandler;

    public HTTPServer(AuthService usrService, OAuthService oauth) {
        pathHandler = new PathHandler(new RouteHandler(usrService, oauth));
    }

    public void start() {
        HttpHandler root = exchange -> {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");

            try {
                pathHandler.handleRequest(exchange);
            } catch (Exception e) {
                logger.getCore().error("Unhandled exception in HTTP handler", e);
                exchange.setStatusCode(500);
                exchange.getResponseSender().send("{\"error\":\"internal_error\"}");
            }
        };

        Undertow server = Undertow.builder().addHttpListener(httpPort, "localhost").setHandler(new CorsHandler(root)).build();

        server.start();
        logger.getCore().info("Auth Server started on port: {}", httpPort);
    }
}

