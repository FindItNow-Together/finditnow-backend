package com.finditnow.auth.server;

import com.finditnow.auth.handlers.AuthHandler;
import com.finditnow.auth.handlers.PathHandler;
import com.finditnow.auth.service.AuthService;
import com.finditnow.auth.service.OAuthService;
import com.finditnow.config.Config;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HTTPServer {
    private static final Logger logger = LoggerFactory.getLogger(HTTPServer.class);
    private final int httpPort = Integer.parseInt(Config.get("HTTPPort", "8080"));
    //    private final AuthHandler authHandler;
    private final PathHandler pathHandler;

    public HTTPServer(AuthService usrService, OAuthService oauth) {
        pathHandler = new PathHandler(new AuthHandler(usrService, oauth));
    }

    public void start() {
        HttpHandler root = exchange -> {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");

            try {
                pathHandler.route(exchange);
            } catch (Exception e) {
                logger.error("Unhandled exception in HTTP handler", e);
                exchange.setStatusCode(500);
                exchange.getResponseSender().send("{\"error\":\"internal_error\"}");
            }
        };

        Undertow server = Undertow.builder().addHttpListener(httpPort, "localhost").setHandler(root).build();

        server.start();
        logger.info("Auth Server started on port: {}", httpPort);
    }
}

